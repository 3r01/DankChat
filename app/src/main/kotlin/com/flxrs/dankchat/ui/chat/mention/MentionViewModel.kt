package com.flxrs.dankchat.ui.chat.mention

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flxrs.dankchat.data.chat.ChatItem
import com.flxrs.dankchat.data.repo.chat.ChatNotificationRepository
import com.flxrs.dankchat.preferences.DankChatPreferenceStore
import com.flxrs.dankchat.preferences.appearance.AppearanceSettingsDataStore
import com.flxrs.dankchat.preferences.chat.ChatSettingsDataStore
import com.flxrs.dankchat.ui.chat.ChatDisplaySettings
import com.flxrs.dankchat.ui.chat.ChatMessageMapper
import com.flxrs.dankchat.ui.chat.ChatMessageUiState
import com.flxrs.dankchat.utils.extensions.isEven
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class MentionViewModel(
    chatNotificationRepository: ChatNotificationRepository,
    private val chatMessageMapper: ChatMessageMapper,
    private val preferenceStore: DankChatPreferenceStore,
    appearanceSettingsDataStore: AppearanceSettingsDataStore,
    chatSettingsDataStore: ChatSettingsDataStore,
) : ViewModel() {
    val chatDisplaySettings: StateFlow<ChatDisplaySettings> =
        combine(
            appearanceSettingsDataStore.settings,
            chatSettingsDataStore.settings,
        ) { appearance, chat ->
            ChatDisplaySettings(
                fontSize = appearance.fontSize.toFloat(),
                showLineSeparator = appearance.lineSeparator,
                animateGifs = chat.animateGifs,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ChatDisplaySettings())

    private val _currentTab = MutableStateFlow(0)
    val currentTab: StateFlow<Int> = _currentTab

    fun setCurrentTab(index: Int) {
        _currentTab.value = index
    }

    val mentions: StateFlow<ImmutableList<ChatItem>> =
        chatNotificationRepository.mentions
            .map { it.toImmutableList() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000), persistentListOf())
    val whispers: StateFlow<ImmutableList<ChatItem>> =
        chatNotificationRepository.whispers
            .map { it.toImmutableList() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000), persistentListOf())

    val mentionsUiStates: Flow<ImmutableList<ChatMessageUiState>> =
        combine(
            mentions,
            appearanceSettingsDataStore.settings,
            chatSettingsDataStore.settings,
        ) { messages, appearanceSettings, chatSettings ->
            messages.mapIndexed { index, item ->
                val altBg = index.isEven && appearanceSettings.checkeredMessages
                chatMessageMapper.mapToUiState(
                    item = item,
                    chatSettings = chatSettings,
                    preferenceStore = preferenceStore,
                    isAlternateBackground = altBg,
                )
            }.toImmutableList()
        }.flowOn(Dispatchers.Default)

    val whispersUiStates: Flow<ImmutableList<ChatMessageUiState>> =
        combine(
            whispers,
            appearanceSettingsDataStore.settings,
            chatSettingsDataStore.settings,
        ) { messages, appearanceSettings, chatSettings ->
            messages.mapIndexed { index, item ->
                val altBg = index.isEven && appearanceSettings.checkeredMessages
                chatMessageMapper.mapToUiState(
                    item = item,
                    chatSettings = chatSettings,
                    preferenceStore = preferenceStore,
                    isAlternateBackground = altBg,
                )
            }.toImmutableList()
        }.flowOn(Dispatchers.Default)
}
