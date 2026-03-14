package com.flxrs.dankchat.chat

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flxrs.dankchat.chat.compose.ChatMessageMapper
import com.flxrs.dankchat.chat.compose.ChatMessageMapper.toChatMessageUiState
import com.flxrs.dankchat.chat.compose.ChatMessageUiState
import com.flxrs.dankchat.data.repo.chat.ChatRepository
import com.flxrs.dankchat.preferences.appearance.AppearanceSettingsDataStore
import com.flxrs.dankchat.preferences.chat.ChatSettingsDataStore
import com.flxrs.dankchat.utils.extensions.isEven
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.stateIn
import org.koin.android.annotation.KoinViewModel
import kotlin.time.Duration.Companion.seconds

@KoinViewModel
class ChatViewModel(
    savedStateHandle: SavedStateHandle,
    repository: ChatRepository,
    private val context: Context,
    private val appearanceSettingsDataStore: AppearanceSettingsDataStore,
    private val chatSettingsDataStore: ChatSettingsDataStore,
) : ViewModel() {

    private val args = ChatFragmentArgs.fromSavedStateHandle(savedStateHandle)
    
    val chat: StateFlow<List<ChatItem>> = (args.channel?.let(repository::getChat) ?: emptyFlow())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeout = 5.seconds), emptyList())

    // Compose UI states
    val chatUiStates: StateFlow<List<ChatMessageUiState>> = combine(
        chat,
        appearanceSettingsDataStore.settings,
        chatSettingsDataStore.settings
    ) { messages, appearanceSettings, chatSettings ->
        var messageCount = 0
        messages.mapIndexed { index, item ->
            val isAlternateBackground = when (index) {
                messages.lastIndex -> messageCount++.isEven
                else -> (index - messages.size - 1).isEven
            }
            
            item.toChatMessageUiState(
                context = context,
                appearanceSettings = appearanceSettings,
                chatSettings = chatSettings,
                isAlternateBackground = isAlternateBackground && appearanceSettings.checkeredMessages
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeout = 5.seconds), emptyList())

    companion object {
        private val TAG = ChatViewModel::class.java.simpleName
    }
}
