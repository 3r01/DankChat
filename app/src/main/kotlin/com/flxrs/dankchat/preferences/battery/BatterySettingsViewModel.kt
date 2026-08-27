package com.flxrs.dankchat.preferences.battery

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flxrs.dankchat.preferences.chat.ChatSettingsDataStore
import com.flxrs.dankchat.preferences.chat.VisibleThirdPartyEmotes
import com.flxrs.dankchat.preferences.notifications.RemotePushSettingsDataStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
import kotlin.time.Duration.Companion.seconds

@KoinViewModel
class BatterySettingsViewModel(
    private val dataStore: BatterySettingsDataStore,
    chatSettingsDataStore: ChatSettingsDataStore,
    remotePushSettingsDataStore: RemotePushSettingsDataStore,
) : ViewModel() {
    val state =
        combine(dataStore.settings, chatSettingsDataStore.settings, remotePushSettingsDataStore.settings) { battery, chat, remotePush ->
            BatterySettingsState(
                settings = battery,
                sevenTvLiveUpdatesConfigurable = chat.sevenTVLiveEmoteUpdates && VisibleThirdPartyEmotes.SevenTV in chat.visibleEmotes,
                remotePushConfigured = remotePush.isConfigured,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5.seconds),
            initialValue =
                BatterySettingsState(
                    settings = dataStore.current(),
                    sevenTvLiveUpdatesConfigurable = true,
                    remotePushConfigured = remotePushSettingsDataStore.current().isConfigured,
                ),
        )

    fun onInteraction(interaction: BatterySettingsInteraction) = viewModelScope.launch {
        runCatching {
            when (interaction) {
                is BatterySettingsInteraction.PartBusyChannels -> dataStore.update { it.copy(partBusyChannels = interaction.value) }
                is BatterySettingsInteraction.Threshold -> dataStore.update { it.copy(busyThreshold = interaction.value) }
                is BatterySettingsInteraction.Delay -> dataStore.update { it.copy(backgroundDelay = interaction.value) }
                is BatterySettingsInteraction.PauseEventConnections -> dataStore.update { it.copy(pauseEventConnections = interaction.value) }
                is BatterySettingsInteraction.PauseSevenTvLiveUpdates -> dataStore.update { it.copy(pauseSevenTvLiveUpdates = interaction.value) }
                is BatterySettingsInteraction.RemotePushDelay -> dataStore.update { it.copy(remotePushDisconnectDelay = interaction.value) }
            }
        }
    }
}

@Immutable
data class BatterySettingsState(
    val settings: BatterySettings,
    val sevenTvLiveUpdatesConfigurable: Boolean,
    val remotePushConfigured: Boolean,
)

sealed interface BatterySettingsInteraction {
    data class PartBusyChannels(
        val value: Boolean,
    ) : BatterySettingsInteraction

    data class Threshold(
        val value: BusyThreshold,
    ) : BatterySettingsInteraction

    data class Delay(
        val value: BatterySaverDelay,
    ) : BatterySettingsInteraction

    data class PauseEventConnections(
        val value: Boolean,
    ) : BatterySettingsInteraction

    data class PauseSevenTvLiveUpdates(
        val value: Boolean,
    ) : BatterySettingsInteraction

    data class RemotePushDelay(
        val value: RemotePushDisconnectDelay,
    ) : BatterySettingsInteraction
}
