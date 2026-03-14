package com.flxrs.dankchat.chat.mention

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flxrs.dankchat.chat.ChatItem
import com.flxrs.dankchat.chat.compose.ChatMessageMapper.toChatMessageUiState
import com.flxrs.dankchat.chat.compose.ChatMessageUiState
import com.flxrs.dankchat.data.repo.chat.ChatRepository
import com.flxrs.dankchat.preferences.appearance.AppearanceSettingsDataStore
import com.flxrs.dankchat.preferences.chat.ChatSettingsDataStore

import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.koin.android.annotation.KoinViewModel
import kotlin.time.Duration.Companion.seconds

@KoinViewModel
class MentionViewModel(
    chatRepository: ChatRepository,
    private val context: Context,
    private val appearanceSettingsDataStore: AppearanceSettingsDataStore,
    private val chatSettingsDataStore: ChatSettingsDataStore,
) : ViewModel() {
    
    val mentions: StateFlow<List<ChatItem>> = chatRepository.mentions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeout = 5.seconds), emptyList())
    val whispers: StateFlow<List<ChatItem>> = chatRepository.whispers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeout = 5.seconds), emptyList())

    val mentionsUiStates: StateFlow<List<ChatMessageUiState>> = combine(
        mentions,
        appearanceSettingsDataStore.settings,
        chatSettingsDataStore.settings
    ) { messages, appearanceSettings, chatSettings ->
        messages.map { item ->
            item.toChatMessageUiState(
                context = context,
                appearanceSettings = appearanceSettings,
                chatSettings = chatSettings,
                isAlternateBackground = false // No alternating in mentions
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeout = 5.seconds), emptyList())

    val whispersUiStates: StateFlow<List<ChatMessageUiState>> = combine(
        whispers,
        appearanceSettingsDataStore.settings,
        chatSettingsDataStore.settings
    ) { messages, appearanceSettings, chatSettings ->
        messages.map { item ->
            item.toChatMessageUiState(
                context = context,
                appearanceSettings = appearanceSettings,
                chatSettings = chatSettings,
                isAlternateBackground = false // No alternating in whispers
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeout = 5.seconds), emptyList())

    val hasMentions: StateFlow<Boolean> = chatRepository.hasMentions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeout = 5.seconds), false)
    val hasWhispers: StateFlow<Boolean> = chatRepository.hasWhispers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeout = 5.seconds), false)
}
