package com.flxrs.dankchat.preferences.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flxrs.dankchat.data.notification.RemotePushCoordinator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
import kotlin.time.Duration.Companion.seconds

@KoinViewModel
class NotificationsSettingsViewModel(
    private val notificationsSettingsDataStore: NotificationsSettingsDataStore,
    private val remotePushSettingsDataStore: RemotePushSettingsDataStore,
    remotePushCoordinator: RemotePushCoordinator,
) : ViewModel() {
    val settings =
        notificationsSettingsDataStore.settings
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5.seconds),
                initialValue = notificationsSettingsDataStore.current(),
            )
    val remoteSettings = remotePushSettingsDataStore.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), remotePushSettingsDataStore.current())
    val remoteStatus = remotePushCoordinator.status

    fun onInteraction(interaction: NotificationsSettingsInteraction) = viewModelScope.launch {
        runCatching {
            when (interaction) {
                is NotificationsSettingsInteraction.Notifications -> notificationsSettingsDataStore.update { it.copy(showNotifications = interaction.value) }
                is NotificationsSettingsInteraction.WhisperNotifications -> notificationsSettingsDataStore.update { it.copy(showWhisperNotifications = interaction.value) }
                is NotificationsSettingsInteraction.Mention -> notificationsSettingsDataStore.update { it.copy(mentionFormat = interaction.value) }
                is NotificationsSettingsInteraction.RemoteEnabled -> remotePushSettingsDataStore.update { it.copy(enabled = interaction.value) }
                is NotificationsSettingsInteraction.RemoteServerUrl -> remotePushSettingsDataStore.update { it.copy(serverUrl = interaction.value.trim()) }
                is NotificationsSettingsInteraction.RemoteEnrollmentToken -> remotePushSettingsDataStore.update { it.copy(enrollmentToken = interaction.value.trim()) }
            }
        }
    }
}

sealed interface NotificationsSettingsInteraction {
    data class Notifications(
        val value: Boolean,
    ) : NotificationsSettingsInteraction

    data class WhisperNotifications(
        val value: Boolean,
    ) : NotificationsSettingsInteraction

    data class Mention(
        val value: MentionFormat,
    ) : NotificationsSettingsInteraction

    data class RemoteEnabled(
        val value: Boolean,
    ) : NotificationsSettingsInteraction

    data class RemoteServerUrl(
        val value: String,
    ) : NotificationsSettingsInteraction

    data class RemoteEnrollmentToken(
        val value: String,
    ) : NotificationsSettingsInteraction
}
