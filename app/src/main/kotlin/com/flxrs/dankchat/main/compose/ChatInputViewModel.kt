package com.flxrs.dankchat.main.compose

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.placeCursorAtEnd
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flxrs.dankchat.chat.suggestion.Suggestion
import com.flxrs.dankchat.chat.suggestion.SuggestionProvider
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.repo.chat.ChatRepository
import com.flxrs.dankchat.data.twitch.chat.ConnectionState
import com.flxrs.dankchat.main.InputState
import com.flxrs.dankchat.preferences.DankChatPreferenceStore
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

@OptIn(FlowPreview::class)
@KoinViewModel
class ChatInputViewModel(
    private val chatRepository: ChatRepository,
    private val suggestionProvider: SuggestionProvider,
    private val preferenceStore: DankChatPreferenceStore,
) : ViewModel() {

    val textFieldState = TextFieldState()

    private val _isReplying = MutableStateFlow(false)
    val isReplying: StateFlow<Boolean> = _isReplying

    // Create flow from TextFieldState
    private val textFlow = snapshotFlow { textFieldState.text.toString() }

    // Debounce text changes for suggestion lookups
    private val debouncedText = textFlow.debounce(SUGGESTION_DEBOUNCE_MS)

    // Get suggestions based on current text and active channel
    private val suggestions: StateFlow<List<Suggestion>> = combine(
        debouncedText,
        chatRepository.activeChannel
    ) { text, channel ->
        text to channel
    }.flatMapLatest { (text, channel) ->
        suggestionProvider.getSuggestions(text, channel)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<ChatInputUiState> = combine(
        textFlow,
        suggestions,
        chatRepository.activeChannel,
        chatRepository.activeChannel.flatMapLatest { channel ->
            if (channel == null) flowOf(ConnectionState.DISCONNECTED)
            else chatRepository.getConnectionState(channel)
        },
        combine(preferenceStore.isLoggedInFlow, isReplying) { loggedIn, replying -> loggedIn to replying }
    ) { text, suggestions, activeChannel, connectionState, (isLoggedIn, isReplying) ->
        val inputState = when (connectionState) {
            ConnectionState.CONNECTED -> when {
                isReplying -> InputState.Replying
                else -> InputState.Default
            }
            ConnectionState.CONNECTED_NOT_LOGGED_IN -> InputState.NotLoggedIn
            ConnectionState.DISCONNECTED -> InputState.Disconnected
        }

        val canSend = text.isNotBlank() && activeChannel != null && connectionState == ConnectionState.CONNECTED && isLoggedIn
        val enabled = isLoggedIn && connectionState == ConnectionState.CONNECTED

        ChatInputUiState(
            text = text,
            canSend = canSend,
            enabled = enabled,
            suggestions = suggestions,
            activeChannel = activeChannel,
            connectionState = connectionState,
            isLoggedIn = isLoggedIn,
            inputState = inputState
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ChatInputUiState())

    fun sendMessage() {
        val text = textFieldState.text.toString()
        val channel = uiState.value.activeChannel
        if (text.isNotBlank() && channel != null) {
            viewModelScope.launch {
                chatRepository.sendMessage(channel.value, text)
                textFieldState.clearText()
            }
        }
    }

    fun setReplying(replying: Boolean) {
        _isReplying.value = replying
    }

    fun insertText(text: String) {
        textFieldState.edit {
            append(text)
            placeCursorAtEnd()
        }
    }

    fun updateInputText(text: String) {
        textFieldState.edit {
            replace(0, length, text)
            placeCursorAtEnd()
        }
    }

    fun clearInput() {
        textFieldState.clearText()
    }

    /**
     * Apply a suggestion to the current input text.
     * Replaces the current word with the suggestion and places cursor at the end.
     */
    fun applySuggestion(suggestion: Suggestion) {
        val currentText = textFieldState.text.toString()
        val cursorPos = currentText.length // Assume cursor at end for simplicity
        val separator = ' '
        
        // Find start of current word
        var start = cursorPos
        while (start > 0 && currentText[start - 1] != separator) start--
        
        // Build new text with replacement
        val replacement = suggestion.toString() + separator
        val newText = currentText.substring(0, start) + replacement
        
        // Replace all text and place cursor at end
        textFieldState.edit {
            replace(0, length, newText)
            placeCursorAtEnd()
        }
    }

    companion object {
        private const val SUGGESTION_DEBOUNCE_MS = 20L
    }
}

data class ChatInputUiState(
    val text: String = "",
    val canSend: Boolean = false,
    val enabled: Boolean = false,
    val suggestions: List<Suggestion> = emptyList(),
    val activeChannel: UserName? = null,
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val isLoggedIn: Boolean = false,
    val inputState: InputState = InputState.Disconnected
)
