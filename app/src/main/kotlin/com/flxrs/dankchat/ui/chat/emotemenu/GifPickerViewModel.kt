package com.flxrs.dankchat.ui.chat.emotemenu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flxrs.dankchat.data.api.twitchgql.TwitchGifApiClient
import com.flxrs.dankchat.data.api.twitchgql.TwitchGifConfig
import com.flxrs.dankchat.data.api.twitchgql.TwitchGifPickerItem
import com.flxrs.dankchat.data.api.twitchgql.TwitchGifSendException
import com.flxrs.dankchat.data.api.twitchgql.isAvailable
import com.flxrs.dankchat.data.auth.AuthDataStore
import com.flxrs.dankchat.data.repo.channel.ChannelRepository
import com.flxrs.dankchat.data.repo.chat.ChatChannelProvider
import com.flxrs.dankchat.data.repo.emote.GifPickerDataStore
import com.flxrs.dankchat.preferences.whispers.WhisperHistorySettingsDataStore
import com.flxrs.dankchat.ui.main.MainEvent
import com.flxrs.dankchat.ui.main.MainEventBus
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
import kotlin.math.ceil

@KoinViewModel
class GifPickerViewModel(
    private val gifApiClient: TwitchGifApiClient,
    private val channelRepository: ChannelRepository,
    private val chatChannelProvider: ChatChannelProvider,
    private val authDataStore: AuthDataStore,
    private val settingsDataStore: WhisperHistorySettingsDataStore,
    private val mainEventBus: MainEventBus,
    private val gifPickerDataStore: GifPickerDataStore,
) : ViewModel() {
    val library = gifPickerDataStore.library
    private val _state = MutableStateFlow<GifPickerState>(GifPickerState.Unavailable)
    val state: StateFlow<GifPickerState> = _state.asStateFlow()
    private val _isAvailable = MutableStateFlow(false)
    val isAvailable: StateFlow<Boolean> = _isAvailable.asStateFlow()

    private var loadJob: Job? = null
    private var config: TwitchGifConfig? = null
    private var channelId: String? = null
    private var webOAuthToken: String? = null
    private var query = ""
    private var generation = 0
    private val cooldowns = mutableMapOf<String, Long>()

    init {
        viewModelScope.launch {
            combine(
                chatChannelProvider.activeChannel,
                authDataStore.settings,
                settingsDataStore.settings,
            ) { channel, auth, settings ->
                val userId = auth.userId.takeIf { auth.isLoggedIn }
                PickerAccount(channel, userId?.let(settings.webOAuthTokens::get))
            }.collectLatest(::loadChannel)
        }
    }

    fun search(value: String) {
        if (query == value) return
        query = value
        generation++
        loadJob?.cancel()
        _state.value = GifPickerState.Loading
        loadJob =
            viewModelScope.launch {
                delay(SEARCH_DEBOUNCE_MS)
                loadGifs()
            }
    }

    fun loadMore() {
        val current = _state.value as? GifPickerState.Ready ?: return
        val offset = current.nextOffset ?: return
        if (current.isLoadingMore) return
        val currentConfig = config ?: return
        val requestedGeneration = generation
        _state.value = current.copy(isLoadingMore = true, pageError = null)
        loadJob = viewModelScope.launch {
            gifApiClient.getGifs(currentConfig, query, offset).fold(
                onSuccess = { page ->
                    if (generation != requestedGeneration) return@fold
                    _state.value = current.copy(
                        gifs = (current.gifs + page.gifs).distinctBy { it.id }.toImmutableList(),
                        nextOffset = page.nextOffset,
                        pageError = null,
                    )
                },
                onFailure = {
                    if (generation != requestedGeneration) return@fold
                    _state.value = current.copy(pageError = it.message ?: "Unable to load GIFs")
                },
            )
        }
    }

    fun send(
        gif: TwitchGifPickerItem,
        onSent: () -> Unit,
    ) {
        if (!_isAvailable.value || _state.value is GifPickerState.Sending) return
        val activeChannelId = channelId ?: return
        val token = webOAuthToken ?: return
        viewModelScope.launch {
            val secondsRemaining =
                ceil(((cooldowns[activeChannelId] ?: 0L) - System.currentTimeMillis()).coerceAtLeast(0L) / 1000.0).toInt()
            if (secondsRemaining > 0) {
                mainEventBus.emitEvent(MainEvent.Error(IllegalStateException("You cannot send another GIF for $secondsRemaining seconds")))
                return@launch
            }
            val currentState = _state.value
            if (currentState is GifPickerState.Sending) return@launch
            val oldState = (currentState as? GifPickerState.Ready)?.copy(isLoadingMore = false) ?: currentState
            loadJob?.cancel()
            val requestedGeneration = ++generation
            _state.value = GifPickerState.Sending((oldState as? GifPickerState.Ready)?.gifs ?: kotlinx.collections.immutable.persistentListOf())
            gifApiClient.sendGif(activeChannelId, gif, token).fold(
                onSuccess = { result ->
                    cooldowns[activeChannelId] = System.currentTimeMillis() + result.secondsUntilCanSend * 1000L
                    try {
                        gifPickerDataStore.recordSent(gif)
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Exception) {
                        logger.warn(error) { "Unable to save recently sent GIF" }
                    }
                    if (generation != requestedGeneration || channelId != activeChannelId || webOAuthToken != token) return@fold
                    _state.value = oldState
                    if (oldState is GifPickerState.Loading) loadJob = viewModelScope.launch { loadGifs() }
                    onSent()
                },
                onFailure = { error ->
                    val retryAfter = (error as? TwitchGifSendException)?.secondsUntilCanSend ?: 0
                    if (retryAfter > 0) cooldowns[activeChannelId] = System.currentTimeMillis() + retryAfter * 1000L
                    if (generation != requestedGeneration || channelId != activeChannelId || webOAuthToken != token) return@fold
                    _state.value = oldState
                    if (oldState is GifPickerState.Loading) loadJob = viewModelScope.launch { loadGifs() }
                    if (error.message?.contains("permission", ignoreCase = true) == true ||
                        error.message?.contains("eligible", ignoreCase = true) == true
                    ) {
                        config = null
                        _isAvailable.value = false
                    }
                    mainEventBus.emitEvent(MainEvent.Error(error))
                },
            )
        }
    }

    private suspend fun loadChannel(account: PickerAccount) {
        generation++
        loadJob?.cancel()
        config = null
        _isAvailable.value = false
        channelId = null
        webOAuthToken = account.token
        query = ""
        val channel = account.channel
        val token = account.token
        if (channel == null || token.isNullOrBlank()) {
            logger.info { "GIF picker unavailable (channel=${channel?.value}, webToken=${!token.isNullOrBlank()})" }
            _state.value = GifPickerState.Unavailable
            return
        }
        _state.value = GifPickerState.Loading
        logger.info { "Loading GIF picker configuration (channel=${channel.value})" }
        val resolvedChannel = channelRepository.getChannel(channel)
        if (resolvedChannel == null) {
            _state.value = GifPickerState.Error("Unable to resolve this channel")
            return
        }
        channelId = resolvedChannel.id.value
        gifApiClient.getConfig(resolvedChannel.id.value, token).fold(
            onSuccess = {
                if (!it.isAvailable()) {
                    logger.info {
                        "GIF picker unavailable (channel=${channel.value}, allowlisted=${it.isAllowlisted}, " +
                            "enabled=${it.isEnabled}, canSend=${it.canSend})"
                    }
                    _state.value = GifPickerState.Unavailable
                    return@fold
                }
                config = it
                _isAvailable.value = true
                logger.info {
                    "GIF picker enabled (channel=${channel.value}, allowlisted=${it.isAllowlisted}, " +
                        "enabled=${it.isEnabled}, canSend=${it.canSend})"
                }
                loadJob?.cancel()
                loadJob = viewModelScope.launch { loadGifs() }
            },
            onFailure = {
                logger.warn(it) { "GIF picker configuration unavailable (channel=${channel.value})" }
                _state.value = GifPickerState.Error(it.message ?: "GIFs are unavailable")
            },
        )
    }

    private suspend fun loadGifs() {
        val currentConfig = config ?: return
        val requestedGeneration = generation
        _state.value = GifPickerState.Loading
        gifApiClient.getGifs(currentConfig, query).fold(
            onSuccess = {
                if (generation == requestedGeneration) _state.value = GifPickerState.Ready(it.gifs.toImmutableList(), it.nextOffset)
            },
            onFailure = {
                if (generation == requestedGeneration) _state.value = GifPickerState.Error(it.message ?: "Unable to load GIFs")
            },
        )
    }

    private data class PickerAccount(
        val channel: com.flxrs.dankchat.data.UserName?,
        val token: String?,
    )

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 350L
    }
}

private val logger = KotlinLogging.logger("GifPickerViewModel")

sealed interface GifPickerState {
    data object Unavailable : GifPickerState

    data object Loading : GifPickerState

    data class Ready(
        val gifs: ImmutableList<TwitchGifPickerItem>,
        val nextOffset: Int? = null,
        val isLoadingMore: Boolean = false,
        val pageError: String? = null,
    ) : GifPickerState

    data class Sending(
        val gifs: ImmutableList<TwitchGifPickerItem>,
    ) : GifPickerState

    data class Error(
        val message: String,
    ) : GifPickerState
}
