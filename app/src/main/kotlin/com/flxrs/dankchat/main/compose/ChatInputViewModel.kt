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
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

@OptIn(FlowPreview::class)
@KoinViewModel
class ChatInputViewModel(
    private val chatRepository: ChatRepository,
    private val suggestionProvider: SuggestionProvider,
) : ViewModel() {

    val textFieldState = TextFieldState()

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
        chatRepository.activeChannel
    ) { text, suggestions, activeChannel ->
        ChatInputUiState(
            text = text,
            canSend = text.isNotBlank() && activeChannel != null,
            suggestions = suggestions,
            activeChannel = activeChannel
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
    val suggestions: List<Suggestion> = emptyList(),
    val activeChannel: UserName? = null
)
