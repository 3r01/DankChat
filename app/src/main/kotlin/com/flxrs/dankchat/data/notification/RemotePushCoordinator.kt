package com.flxrs.dankchat.data.notification

import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.auth.AuthDataStore
import com.flxrs.dankchat.data.database.entity.MessageHighlightEntityType
import com.flxrs.dankchat.data.repo.HighlightsRepository
import com.flxrs.dankchat.data.repo.channel.ChannelRepository
import com.flxrs.dankchat.data.repo.chat.ChatChannelProvider
import com.flxrs.dankchat.di.DispatchersProvider
import com.flxrs.dankchat.preferences.DankChatPreferenceStore
import com.flxrs.dankchat.preferences.notifications.NotificationsSettings
import com.flxrs.dankchat.preferences.notifications.NotificationsSettingsDataStore
import com.flxrs.dankchat.preferences.notifications.RemotePushDeviceDataStore
import com.flxrs.dankchat.preferences.notifications.RemotePushSettings
import com.flxrs.dankchat.preferences.notifications.RemotePushSettingsDataStore
import com.flxrs.dankchat.push.BlacklistedUserRule
import com.flxrs.dankchat.push.MessageHighlightRule
import com.flxrs.dankchat.push.PushChannel
import com.flxrs.dankchat.push.PushConfiguration
import com.flxrs.dankchat.push.PushNotificationRules
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import kotlin.math.max

@Single
class RemotePushCoordinator(
    private val remotePushSettingsDataStore: RemotePushSettingsDataStore,
    private val notificationsSettingsDataStore: NotificationsSettingsDataStore,
    private val authDataStore: AuthDataStore,
    private val preferences: DankChatPreferenceStore,
    private val chatChannelProvider: ChatChannelProvider,
    private val channelRepository: ChannelRepository,
    private val highlightsRepository: HighlightsRepository,
    private val remotePushClient: RemotePushClient,
    private val remotePushDeviceDataStore: RemotePushDeviceDataStore,
    dispatchersProvider: DispatchersProvider,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatchersProvider.io)
    private val _status = MutableStateFlow<RemotePushStatus>(RemotePushStatus.Disabled)
    val status = _status

    private var lastRevision = 0L

    fun initialize() {
        scope.launch {
            configurationInputs()
                .distinctUntilChanged()
                .collect { input -> sync(input) }
        }
    }

    fun isEnabled() = remotePushSettingsDataStore.current().isConfigured

    private fun configurationInputs() = combine(
        combine(
            remotePushSettingsDataStore.settings,
            notificationsSettingsDataStore.settings,
            chatChannelProvider.channels.filterNotNull(),
            preferences.currentUserAndDisplayFlow,
            remotePushDeviceDataStore.device,
        ) { remote, notifications, channels, userAndDisplay, device ->
            BaseInput(remote, notifications, channels, userAndDisplay.first?.value, userAndDisplay.second?.value, device.firebaseInstallationId)
        },
        highlightsRepository.messageHighlights,
        highlightsRepository.userHighlights,
        highlightsRepository.badgeHighlights,
        highlightsRepository.blacklistedUsers,
    ) { base, messageHighlights, userHighlights, badgeHighlights, blacklistedUsers ->
        ConfigurationInput(
            base = base,
            messageHighlights =
                messageHighlights
                    .filter { it.enabled && it.createNotification }
                    .map { HighlightInput(it.type, it.pattern, it.isRegex, it.isCaseSensitive) },
            userHighlights = userHighlights.filter { it.enabled && it.createNotification }.map { it.username },
            badgeHighlights = badgeHighlights.filter { it.enabled && it.createNotification }.map { it.badgeName },
            blacklistedUsers = blacklistedUsers.filter { it.enabled }.map { BlacklistedUserRule(it.username, it.isRegex) },
        )
    }

    private suspend fun sync(input: ConfigurationInput) {
        val remote = input.base.remote
        if (!remote.isConfigured) {
            if (!remote.enabled && remote.serverUrl.isNotBlank() && remote.enrollmentToken.isNotBlank() && input.base.firebaseInstallationId.isNotBlank()) {
                remotePushClient.unregisterDevice(remote, input.base.firebaseInstallationId)
            }
            _status.value = RemotePushStatus.Disabled
            return
        }
        if (!remote.serverUrl.startsWith("https://")) {
            _status.value = RemotePushStatus.Error("Server URL must use HTTPS")
            return
        }
        val userId = authDataStore.userIdString?.value
        val userName = input.base.userName
        if (userId == null || userName == null) {
            _status.value = RemotePushStatus.WaitingForLogin
            return
        }

        _status.value = RemotePushStatus.Syncing
        if (input.base.firebaseInstallationId.isBlank()) {
            _status.value = RemotePushStatus.WaitingForDevice
            return
        }
        remotePushClient.registerDevice(remote, input.base.firebaseInstallationId).getOrElse { error ->
            _status.value = RemotePushStatus.Error(error.message ?: "Device registration failed")
            return
        }
        val notificationsEnabled = input.base.notifications.showNotifications
        val channels =
            if (notificationsEnabled) {
                channelRepository.getChannels(input.base.channels).map { PushChannel(it.id.value, it.name.value) }
            } else {
                emptyList()
            }
        val username = input.messageHighlights.find { it.type == MessageHighlightEntityType.Username }
        val reply = input.messageHighlights.find { it.type == MessageHighlightEntityType.Reply }
        val configuration =
            PushConfiguration(
                revision = nextRevision(),
                twitchUserId = userId,
                userName = userName,
                displayName = input.base.displayName,
                notifyWhispers = notificationsEnabled && input.base.notifications.showWhisperNotifications,
                channels = channels,
                rules =
                    PushNotificationRules(
                        notifyOnUsername = username != null,
                        notifyOnParticipatedReply = reply != null,
                        messageHighlights =
                            input.messageHighlights
                                .filter { it.type == MessageHighlightEntityType.Custom && it.pattern.isNotBlank() }
                                .map { MessageHighlightRule(it.pattern, it.isRegex, it.isCaseSensitive) },
                        userHighlights = input.userHighlights.filter(String::isNotBlank),
                        badgeHighlights = input.badgeHighlights.filter(String::isNotBlank),
                        blacklistedUsers = input.blacklistedUsers.filter { it.pattern.isNotBlank() },
                    ),
            )
        remotePushClient
            .syncConfiguration(remote, configuration)
            .onSuccess { revision -> _status.value = RemotePushStatus.Synced(revision) }
            .onFailure { error -> _status.value = RemotePushStatus.Error(error.message ?: "Configuration sync failed") }
    }

    private fun nextRevision(): Long = max(lastRevision + 1, System.currentTimeMillis()).also { lastRevision = it }

    private data class BaseInput(
        val remote: RemotePushSettings,
        val notifications: NotificationsSettings,
        val channels: List<UserName>,
        val userName: String?,
        val displayName: String?,
        val firebaseInstallationId: String,
    )

    private data class HighlightInput(
        val type: MessageHighlightEntityType,
        val pattern: String,
        val isRegex: Boolean,
        val isCaseSensitive: Boolean,
    )

    private data class ConfigurationInput(
        val base: BaseInput,
        val messageHighlights: List<HighlightInput>,
        val userHighlights: List<String>,
        val badgeHighlights: List<String>,
        val blacklistedUsers: List<BlacklistedUserRule>,
    )
}

sealed interface RemotePushStatus {
    data object Disabled : RemotePushStatus

    data object WaitingForLogin : RemotePushStatus

    data object WaitingForDevice : RemotePushStatus

    data object Syncing : RemotePushStatus

    data class Synced(
        val revision: Long,
    ) : RemotePushStatus

    data class Error(
        val message: String,
    ) : RemotePushStatus
}
