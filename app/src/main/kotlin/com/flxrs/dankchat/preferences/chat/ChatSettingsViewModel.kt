package com.flxrs.dankchat.preferences.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import kotlin.time.Duration.Companion.seconds

@KoinViewModel
class ChatSettingsViewModel(
    private val chatSettingsDataStore: ChatSettingsDataStore,
) : ViewModel() {
    private val _events = MutableSharedFlow<ChatSettingsEvent>()
    val events = _events.asSharedFlow()

    private val initial = chatSettingsDataStore.current()
    val settings =
        chatSettingsDataStore.settings
            .map { it.toState() }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5.seconds),
                initialValue = initial.toState(),
            )

    fun onInteraction(interaction: ChatSettingsInteraction) = viewModelScope.launch {
        runCatching {
            when (interaction) {
                is ChatSettingsInteraction.Suggestions -> {
                    chatSettingsDataStore.update { it.copy(suggestions = interaction.value) }
                }

                is ChatSettingsInteraction.SupibotSuggestions -> {
                    chatSettingsDataStore.update { it.copy(supibotSuggestions = interaction.value) }
                }

                is ChatSettingsInteraction.CustomCommands -> {
                    chatSettingsDataStore.update { it.copy(customCommands = interaction.value) }
                }

                is ChatSettingsInteraction.AnimateGifs -> {
                    chatSettingsDataStore.update { it.copy(animateGifs = interaction.value) }
                }

                is ChatSettingsInteraction.ScrollbackLength -> {
                    chatSettingsDataStore.update { it.copy(scrollbackLength = interaction.value) }
                }

                is ChatSettingsInteraction.ShowUsernames -> {
                    chatSettingsDataStore.update { it.copy(showUsernames = interaction.value) }
                }

                is ChatSettingsInteraction.UserLongClick -> {
                    chatSettingsDataStore.update { it.copy(userLongClickBehavior = interaction.value) }
                }

                is ChatSettingsInteraction.ColorizeNicknames -> {
                    chatSettingsDataStore.update { it.copy(colorizeNicknames = interaction.value) }
                }

                is ChatSettingsInteraction.ShowTimedOutMessages -> {
                    chatSettingsDataStore.update { it.copy(showTimedOutMessages = interaction.value) }
                }

                is ChatSettingsInteraction.ShowTimestamps -> {
                    chatSettingsDataStore.update { it.copy(showTimestamps = interaction.value) }
                }

                is ChatSettingsInteraction.TimestampFormat -> {
                    chatSettingsDataStore.update { it.copy(timestampFormat = interaction.value) }
                }

                is ChatSettingsInteraction.Badges -> {
                    chatSettingsDataStore.update { it.copy(visibleBadges = interaction.value) }
                }

                is ChatSettingsInteraction.Emotes -> {
                    chatSettingsDataStore.update { it.copy(visibleEmotes = interaction.value) }
                    if (initial.visibleEmotes != interaction.value) {
                        _events.emit(ChatSettingsEvent.RestartRequired)
                    }
                }

                is ChatSettingsInteraction.AllowUnlisted -> {
                    chatSettingsDataStore.update { it.copy(allowUnlistedSevenTvEmotes = interaction.value) }
                    if (initial.allowUnlistedSevenTvEmotes != interaction.value) {
                        _events.emit(ChatSettingsEvent.RestartRequired)
                    }
                }

                is ChatSettingsInteraction.LiveEmoteUpdates -> {
                    chatSettingsDataStore.update { it.copy(sevenTVLiveEmoteUpdates = interaction.value) }
                }

                is ChatSettingsInteraction.LiveEmoteUpdatesBehavior -> {
                    chatSettingsDataStore.update { it.copy(sevenTVLiveEmoteUpdatesBehavior = interaction.value) }
                }

                is ChatSettingsInteraction.MessageHistory -> {
                    chatSettingsDataStore.update { it.copy(loadMessageHistory = interaction.value) }
                }

                is ChatSettingsInteraction.MessageHistoryAfterReconnect -> {
                    chatSettingsDataStore.update { it.copy(loadMessageHistoryOnReconnect = interaction.value) }
                }

                is ChatSettingsInteraction.ChatModes -> {
                    chatSettingsDataStore.update { it.copy(showChatModes = interaction.value) }
                }
            }
        }
    }
}

private fun ChatSettings.toState() = ChatSettingsState(
    suggestions = suggestions,
    supibotSuggestions = supibotSuggestions,
    customCommands = customCommands.toImmutableList(),
    animateGifs = animateGifs,
    scrollbackLength = scrollbackLength,
    showUsernames = showUsernames,
    userLongClickBehavior = userLongClickBehavior,
    colorizeNicknames = colorizeNicknames,
    showTimedOutMessages = showTimedOutMessages,
    showTimestamps = showTimestamps,
    timestampFormat = timestampFormat,
    visibleBadges = visibleBadges.toImmutableList(),
    visibleEmotes = visibleEmotes.toImmutableList(),
    allowUnlistedSevenTvEmotes = allowUnlistedSevenTvEmotes,
    sevenTVLiveEmoteUpdates = sevenTVLiveEmoteUpdates,
    sevenTVLiveEmoteUpdatesBehavior = sevenTVLiveEmoteUpdatesBehavior,
    loadMessageHistory = loadMessageHistory,
    loadMessageHistoryAfterReconnect = loadMessageHistoryOnReconnect,
    messageHistoryDashboardUrl = RECENT_MESSAGES_DASHBOARD,
    showChatModes = showChatModes,
)

private const val RECENT_MESSAGES_DASHBOARD = "https://recent-messages.robotty.de"
