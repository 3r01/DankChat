package com.flxrs.dankchat.chat.mention.compose

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.Immutable
import com.flxrs.dankchat.chat.ChatItem
import com.flxrs.dankchat.chat.compose.ChatDisplaySettings
import com.flxrs.dankchat.chat.compose.ChatMessageMapper
import com.flxrs.dankchat.chat.compose.ChatMessageUiState
import com.flxrs.dankchat.data.repo.chat.ChatRepository
import com.flxrs.dankchat.preferences.DankChatPreferenceStore
import com.flxrs.dankchat.preferences.appearance.AppearanceSettingsDataStore
import com.flxrs.dankchat.preferences.chat.ChatSettingsDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import com.flxrs.dankchat.utils.extensions.isEven
import org.koin.android.annotation.KoinViewModel
import kotlin.time.Duration.Companion.seconds

@KoinViewModel
class MentionComposeViewModel(
    chatRepository: ChatRepository,
    private val chatMessageMapper: ChatMessageMapper,
    private val context: Context,
    private val appearanceSettingsDataStore: AppearanceSettingsDataStore,
    private val chatSettingsDataStore: ChatSettingsDataStore,
    private val preferenceStore: DankChatPreferenceStore,
) : ViewModel() {

    val chatDisplaySettings: StateFlow<ChatDisplaySettings> = combine(
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

    val mentions: StateFlow<List<ChatItem>> = chatRepository.mentions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000), emptyList())
    val whispers: StateFlow<List<ChatItem>> = chatRepository.whispers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000), emptyList())

    val mentionsUiStates: Flow<List<ChatMessageUiState>> = combine(
        mentions,
        appearanceSettingsDataStore.settings,
        chatSettingsDataStore.settings,
    ) { messages, appearanceSettings, chatSettings ->
        messages.mapIndexed { index, item ->
            val altBg = index.isEven && appearanceSettings.checkeredMessages
            chatMessageMapper.mapToUiState(
                item = item,
                context = context,
                appearanceSettings = appearanceSettings,
                chatSettings = chatSettings,
                preferenceStore = preferenceStore,
                isAlternateBackground = altBg
            )
        }
    }.flowOn(Dispatchers.Default)

    val whispersUiStates: Flow<List<ChatMessageUiState>> = combine(
        whispers,
        appearanceSettingsDataStore.settings,
        chatSettingsDataStore.settings,
    ) { messages, appearanceSettings, chatSettings ->
        messages.mapIndexed { index, item ->
            val altBg = index.isEven && appearanceSettings.checkeredMessages
            chatMessageMapper.mapToUiState(
                item = item,
                context = context,
                appearanceSettings = appearanceSettings,
                chatSettings = chatSettings,
                preferenceStore = preferenceStore,
                isAlternateBackground = altBg
            )
        }
    }.flowOn(Dispatchers.Default)

    val hasMentions: StateFlow<Boolean> = chatRepository.hasMentions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000), false)
    val hasWhispers: StateFlow<Boolean> = chatRepository.hasWhispers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000), false)
}
