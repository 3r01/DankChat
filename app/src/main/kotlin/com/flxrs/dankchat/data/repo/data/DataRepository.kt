package com.flxrs.dankchat.data.repo.data

import android.util.Log
import com.flxrs.dankchat.data.DisplayName
import com.flxrs.dankchat.data.UserId
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.api.badges.BadgesApiClient
import com.flxrs.dankchat.data.api.bttv.BTTVApiClient
import com.flxrs.dankchat.data.api.dankchat.DankChatApiClient
import com.flxrs.dankchat.data.api.ffz.FFZApiClient
import com.flxrs.dankchat.data.api.helix.HelixApiClient
import com.flxrs.dankchat.data.api.helix.dto.StreamDto
import com.flxrs.dankchat.data.api.helix.dto.UserDto
import com.flxrs.dankchat.data.api.helix.dto.UserFollowsDto
import com.flxrs.dankchat.data.api.seventv.SevenTVApiClient
import com.flxrs.dankchat.data.api.seventv.eventapi.SevenTVEventApiClient
import com.flxrs.dankchat.data.api.seventv.eventapi.SevenTVEventMessage
import com.flxrs.dankchat.data.api.upload.UploadClient
import com.flxrs.dankchat.data.auth.AuthDataStore
import com.flxrs.dankchat.data.repo.RecentUploadsRepository
import com.flxrs.dankchat.data.repo.emote.EmoteRepository
import com.flxrs.dankchat.data.repo.emote.Emotes
import com.flxrs.dankchat.data.twitch.badge.toBadgeSets
import com.flxrs.dankchat.di.DispatchersProvider
import com.flxrs.dankchat.preferences.chat.ChatSettingsDataStore
import com.flxrs.dankchat.preferences.chat.VisibleThirdPartyEmotes
import com.flxrs.dankchat.utils.extensions.measureTimeAndLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single
import java.io.File

@Single
class DataRepository(
    private val helixApiClient: HelixApiClient,
    private val dankChatApiClient: DankChatApiClient,
    private val badgesApiClient: BadgesApiClient,
    private val ffzApiClient: FFZApiClient,
    private val bttvApiClient: BTTVApiClient,
    private val sevenTVApiClient: SevenTVApiClient,
    private val sevenTVEventApiClient: SevenTVEventApiClient,
    private val uploadClient: UploadClient,
    private val emoteRepository: EmoteRepository,
    private val recentUploadsRepository: RecentUploadsRepository,
    private val authDataStore: AuthDataStore,
    private val chatSettingsDataStore: ChatSettingsDataStore,
    dispatchersProvider: DispatchersProvider,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatchersProvider.default)
    private val _dataLoadingFailures = MutableStateFlow(emptySet<DataLoadingFailure>())
    private val _dataUpdateEvents = MutableSharedFlow<DataUpdateEventMessage>()
    private val serviceEventChannel = Channel<ServiceEvent>(Channel.BUFFERED)

    init {
        scope.launch {
            sevenTVEventApiClient.messages.collect { event ->
                when (event) {
                    is SevenTVEventMessage.UserUpdated -> {
                        val channel = emoteRepository.getChannelForSevenTVEmoteSet(event.oldEmoteSetId) ?: return@collect
                        val details = emoteRepository.getSevenTVUserDetails(channel) ?: return@collect
                        if (details.connectionIndex != event.connectionIndex) {
                            return@collect
                        }

                        val newEmoteSet = sevenTVApiClient.getSevenTVEmoteSet(event.emoteSetId).getOrNull() ?: return@collect
                        emoteRepository.setSevenTVEmoteSet(channel, newEmoteSet)

                        sevenTVEventApiClient.unsubscribeEmoteSet(event.oldEmoteSetId)
                        sevenTVEventApiClient.subscribeEmoteSet(event.emoteSetId)
                        _dataUpdateEvents.emit(DataUpdateEventMessage.ActiveEmoteSetChanged(channel, event.actorName, newEmoteSet.name))
                    }

                    is SevenTVEventMessage.EmoteSetUpdated -> {
                        val channel = emoteRepository.getChannelForSevenTVEmoteSet(event.emoteSetId) ?: return@collect
                        emoteRepository.updateSevenTVEmotes(channel, event)
                        _dataUpdateEvents.emit(DataUpdateEventMessage.EmoteSetUpdated(channel, event))
                    }
                }
            }
        }
    }

    val serviceEvents = serviceEventChannel.receiveAsFlow()
    val dataLoadingFailures = _dataLoadingFailures.asStateFlow()
    val dataUpdateEvents = _dataUpdateEvents.asSharedFlow()

    fun clearDataLoadingFailures() = _dataLoadingFailures.update { emptySet() }

    fun getEmotes(channel: UserName): Flow<Emotes> = emoteRepository.getEmotes(channel)

    fun createFlowsIfNecessary(channels: List<UserName>) = emoteRepository.createFlowsIfNecessary(channels)

    suspend fun getUser(userId: UserId): UserDto? = helixApiClient.getUser(userId).getOrNull()

    suspend fun getChannelFollowers(
        broadcasterId: UserId,
        targetId: UserId,
    ): UserFollowsDto? = helixApiClient.getChannelFollowers(broadcasterId, targetId).getOrNull()

    suspend fun getStreams(channels: List<UserName>): List<StreamDto>? = helixApiClient.getStreams(channels).getOrNull()

    suspend fun reconnect() {
        sevenTVEventApiClient.reconnect()
    }

    suspend fun reconnectIfNecessary() {
        sevenTVEventApiClient.reconnectIfNecessary()
    }

    suspend fun removeChannels(removed: List<UserName>) {
        removed.forEach { channel ->
            val details = emoteRepository.getSevenTVUserDetails(channel) ?: return@forEach
            sevenTVEventApiClient.unsubscribeUser(details.id)
            sevenTVEventApiClient.unsubscribeEmoteSet(details.activeEmoteSetId)
        }
    }

    suspend fun uploadMedia(file: File): Result<String> = uploadClient.uploadMedia(file).mapCatching {
        recentUploadsRepository.addUpload(it)
        it.imageLink
    }

    suspend fun loadGlobalBadges(): Result<Unit> = withContext(Dispatchers.IO) {
        measureTimeAndLog(TAG, "global badges") {
            val result =
                when {
                    authDataStore.isLoggedIn -> helixApiClient.getGlobalBadges().map { it.toBadgeSets() }
                    System.currentTimeMillis() < BADGES_SUNSET_MILLIS -> badgesApiClient.getGlobalBadges().map { it.toBadgeSets() }
                    else -> return@withContext Result.success(Unit)
                }.getOrEmitFailure { DataLoadingStep.GlobalBadges }
            result.onSuccess { emoteRepository.setGlobalBadges(it) }.map { }
        }
    }

    suspend fun loadDankChatBadges(): Result<Unit> = withContext(Dispatchers.IO) {
        measureTimeAndLog(TAG, "DankChat badges") {
            dankChatApiClient
                .getDankChatBadges()
                .getOrEmitFailure { DataLoadingStep.DankChatBadges }
                .onSuccess { emoteRepository.setDankChatBadges(it) }
                .map { }
        }
    }

    suspend fun loadUserEmotes(
        userId: UserId,
        onFirstPageLoaded: (() -> Unit)? = null,
    ): Result<Unit> = emoteRepository
        .loadUserEmotes(userId, onFirstPageLoaded)
        .getOrEmitFailure { DataLoadingStep.TwitchEmotes }

    suspend fun sendShutdownCommand() {
        serviceEventChannel.send(ServiceEvent.Shutdown)
    }

    suspend fun loadChannelBadges(
        channel: UserName,
        id: UserId,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        measureTimeAndLog(TAG, "channel badges for #$id") {
            val result =
                when {
                    authDataStore.isLoggedIn -> helixApiClient.getChannelBadges(id).map { it.toBadgeSets() }
                    System.currentTimeMillis() < BADGES_SUNSET_MILLIS -> badgesApiClient.getChannelBadges(id).map { it.toBadgeSets() }
                    else -> return@withContext Result.success(Unit)
                }.getOrEmitFailure { DataLoadingStep.ChannelBadges(channel, id) }
            result.onSuccess { emoteRepository.setChannelBadges(channel, it) }.map { }
        }
    }

    suspend fun loadChannelFFZEmotes(
        channel: UserName,
        channelId: UserId,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (VisibleThirdPartyEmotes.FFZ !in chatSettingsDataStore.settings.first().visibleEmotes) {
            return@withContext Result.success(Unit)
        }

        measureTimeAndLog(TAG, "FFZ emotes for #$channel") {
            ffzApiClient
                .getFFZChannelEmotes(channelId)
                .getOrEmitFailure { DataLoadingStep.ChannelFFZEmotes(channel, channelId) }
                .onSuccess { it?.let { emoteRepository.setFFZEmotes(channel, it) } }
                .map { }
        }
    }

    suspend fun loadChannelBTTVEmotes(
        channel: UserName,
        channelDisplayName: DisplayName,
        channelId: UserId,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (VisibleThirdPartyEmotes.BTTV !in chatSettingsDataStore.settings.first().visibleEmotes) {
            return@withContext Result.success(Unit)
        }

        measureTimeAndLog(TAG, "BTTV emotes for #$channel") {
            bttvApiClient
                .getBTTVChannelEmotes(channelId)
                .getOrEmitFailure { DataLoadingStep.ChannelBTTVEmotes(channel, channelDisplayName, channelId) }
                .onSuccess { it?.let { emoteRepository.setBTTVEmotes(channel, channelDisplayName, it) } }
                .map { }
        }
    }

    suspend fun loadChannelSevenTVEmotes(
        channel: UserName,
        channelId: UserId,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (VisibleThirdPartyEmotes.SevenTV !in chatSettingsDataStore.settings.first().visibleEmotes) {
            return@withContext Result.success(Unit)
        }

        measureTimeAndLog(TAG, "7TV emotes for #$channel") {
            sevenTVApiClient
                .getSevenTVChannelEmotes(channelId)
                .getOrEmitFailure { DataLoadingStep.ChannelSevenTVEmotes(channel, channelId) }
                .onSuccess { result ->
                    result ?: return@onSuccess
                    if (result.emoteSet?.id != null) {
                        sevenTVEventApiClient.subscribeEmoteSet(result.emoteSet.id)
                    }
                    sevenTVEventApiClient.subscribeUser(result.user.id)
                    emoteRepository.setSevenTVEmotes(channel, result)
                }.map { }
        }
    }

    suspend fun loadChannelCheermotes(
        channel: UserName,
        channelId: UserId,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!authDataStore.isLoggedIn) {
            return@withContext Result.success(Unit)
        }

        measureTimeAndLog(TAG, "cheermotes for #$channel") {
            helixApiClient
                .getCheermotes(channelId)
                .getOrEmitFailure { DataLoadingStep.ChannelCheermotes(channel, channelId) }
                .onSuccess { emoteRepository.setCheermotes(channel, it) }
                .map { }
        }
    }

    suspend fun loadGlobalFFZEmotes(): Result<Unit> = withContext(Dispatchers.IO) {
        if (VisibleThirdPartyEmotes.FFZ !in chatSettingsDataStore.settings.first().visibleEmotes) {
            return@withContext Result.success(Unit)
        }

        measureTimeAndLog(TAG, "global FFZ emotes") {
            ffzApiClient
                .getFFZGlobalEmotes()
                .getOrEmitFailure { DataLoadingStep.GlobalFFZEmotes }
                .onSuccess { emoteRepository.setFFZGlobalEmotes(it) }
                .map { }
        }
    }

    suspend fun loadGlobalBTTVEmotes(): Result<Unit> = withContext(Dispatchers.IO) {
        if (VisibleThirdPartyEmotes.BTTV !in chatSettingsDataStore.settings.first().visibleEmotes) {
            return@withContext Result.success(Unit)
        }

        measureTimeAndLog(TAG, "global BTTV emotes") {
            bttvApiClient
                .getBTTVGlobalEmotes()
                .getOrEmitFailure { DataLoadingStep.GlobalBTTVEmotes }
                .onSuccess { emoteRepository.setBTTVGlobalEmotes(it) }
                .map { }
        }
    }

    suspend fun loadGlobalSevenTVEmotes(): Result<Unit> = withContext(Dispatchers.IO) {
        if (VisibleThirdPartyEmotes.SevenTV !in chatSettingsDataStore.settings.first().visibleEmotes) {
            return@withContext Result.success(Unit)
        }

        measureTimeAndLog(TAG, "global 7TV emotes") {
            sevenTVApiClient
                .getSevenTVGlobalEmotes()
                .getOrEmitFailure { DataLoadingStep.GlobalSevenTVEmotes }
                .onSuccess { emoteRepository.setSevenTVGlobalEmotes(it) }
                .map { }
        }
    }

    private fun <T> Result<T>.getOrEmitFailure(step: () -> DataLoadingStep): Result<T> = onFailure { throwable ->
        val loadingStep = step()
        Log.e(TAG, "Data request failed [$loadingStep]:", throwable)
        _dataLoadingFailures.update { it + DataLoadingFailure(loadingStep, throwable) }
    }

    companion object {
        private val TAG = DataRepository::class.java.simpleName
        private const val BADGES_SUNSET_MILLIS = 1685637000000L // 2023-06-01 16:30:00
    }
}
