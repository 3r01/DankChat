package com.flxrs.dankchat.domain

import com.flxrs.dankchat.data.api.whisperhistory.WhisperHistoryApiClient
import com.flxrs.dankchat.data.api.whisperhistory.WhisperHistoryEmote
import com.flxrs.dankchat.data.api.whisperhistory.WhisperHistoryEntry
import com.flxrs.dankchat.data.auth.AuthDataStore
import com.flxrs.dankchat.data.repo.chat.ChatEventProcessor
import com.flxrs.dankchat.data.toDisplayName
import com.flxrs.dankchat.data.toUserId
import com.flxrs.dankchat.data.toUserName
import com.flxrs.dankchat.data.twitch.message.WhisperMessage
import com.flxrs.dankchat.di.DispatchersProvider
import com.flxrs.dankchat.preferences.whispers.WhisperHistorySettingsDataStore
import com.flxrs.dankchat.utils.AppLifecycleListener
import com.flxrs.dankchat.utils.AppLifecycleListener.AppLifecycle.Foreground
import com.flxrs.dankchat.utils.extensions.parseColorOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single

@Single
class WhisperHistoryCoordinator(
    private val authDataStore: AuthDataStore,
    private val settingsDataStore: WhisperHistorySettingsDataStore,
    private val apiClient: WhisperHistoryApiClient,
    private val chatEventProcessor: ChatEventProcessor,
    private val appLifecycleListener: AppLifecycleListener,
    dispatchersProvider: DispatchersProvider,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatchersProvider.io)
    private val _status = MutableStateFlow<WhisperHistoryStatus>(WhisperHistoryStatus.Disabled)
    val status = _status

    private var activeAccountId: String? = null

    fun initialize() {
        scope.launch {
            combine(
                authDataStore.settings,
                settingsDataStore.settings,
                appLifecycleListener.appState,
            ) { auth, settings, lifecycle ->
                val userId = auth.userId.takeIf { auth.isLoggedIn }
                LoadInput(
                    userId = userId,
                    token = userId?.let(settings.webOAuthTokens::get),
                    isForeground = lifecycle == Foreground,
                )
            }.distinctUntilChanged()
                .collectLatest(::load)
        }
    }

    private suspend fun load(input: LoadInput) {
        if (activeAccountId != input.userId) {
            chatEventProcessor.clearWhisperHistory()
            activeAccountId = input.userId
        }
        val userId = input.userId
        val token = input.token
        if (userId == null || token.isNullOrBlank()) {
            _status.value = WhisperHistoryStatus.Disabled
            return
        }
        if (!input.isForeground) return

        _status.value = WhisperHistoryStatus.Loading
        apiClient.getRecentWhispers(userId, token).fold(
            onSuccess = { history ->
                if (activeAccountId == userId) {
                    chatEventProcessor.addHistoricalWhispers(
                        messages = history.map(WhisperHistoryEntry::toWhisperMessage),
                        currentUserId = userId.toUserId(),
                    )
                    _status.value = WhisperHistoryStatus.Loaded(history.size)
                }
            },
            onFailure = { error ->
                _status.value = WhisperHistoryStatus.Error(error.message ?: "Unable to load whisper history")
            },
        )
    }

    private data class LoadInput(
        val userId: String?,
        val token: String?,
        val isForeground: Boolean,
    )
}

sealed interface WhisperHistoryStatus {
    data object Disabled : WhisperHistoryStatus

    data object Loading : WhisperHistoryStatus

    data class Loaded(
        val messageCount: Int,
    ) : WhisperHistoryStatus

    data class Error(
        val message: String,
    ) : WhisperHistoryStatus
}

private fun WhisperHistoryEntry.toWhisperMessage() = WhisperMessage(
    timestamp = timestamp,
    id = id,
    userId = sender.id.toUserId(),
    name = sender.login.toUserName(),
    displayName = sender.displayName.toDisplayName(),
    color = sender.color?.parseColorOrNull(),
    recipientId = recipient.id.toUserId(),
    recipientName = recipient.login.toUserName(),
    recipientDisplayName = recipient.displayName.toDisplayName(),
    recipientColor = recipient.color?.parseColorOrNull(),
    message = text,
    rawEmotes = emotes.toRawEmoteTag(),
    rawBadges = "",
)

private fun List<WhisperHistoryEmote>.toRawEmoteTag(): String = groupBy(WhisperHistoryEmote::id)
    .entries
    .joinToString("/") { (id, emotes) ->
        "$id:${emotes.joinToString(",") { "${it.from}-${it.to}" }}"
    }
