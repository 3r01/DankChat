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
import com.flxrs.dankchat.data.repo.channel.ChannelRepository
import com.flxrs.dankchat.data.repo.chat.ChatRepository
import com.flxrs.dankchat.data.repo.chat.UserStateRepository
import com.flxrs.dankchat.data.repo.command.CommandRepository
import com.flxrs.dankchat.data.repo.command.CommandResult
import com.flxrs.dankchat.data.twitch.chat.ConnectionState
import com.flxrs.dankchat.data.twitch.command.TwitchCommand
import com.flxrs.dankchat.main.InputState
import com.flxrs.dankchat.main.MainEvent
import com.flxrs.dankchat.main.RepeatedSendData
import com.flxrs.dankchat.main.compose.FullScreenSheetState
import com.flxrs.dankchat.preferences.DankChatPreferenceStore
import com.flxrs.dankchat.preferences.developer.DeveloperSettingsDataStore
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

@OptIn(FlowPreview::class)
@KoinViewModel
class ChatInputViewModel(
    private val chatRepository: ChatRepository,
    private val commandRepository: CommandRepository,
    private val channelRepository: ChannelRepository,
    private val userStateRepository: UserStateRepository,
    private val suggestionProvider: SuggestionProvider,
    private val preferenceStore: DankChatPreferenceStore,
    private val developerSettingsDataStore: DeveloperSettingsDataStore,
    private val mainEventBus: MainEventBus,
) : ViewModel() {

    val textFieldState = TextFieldState()

    private val _isReplying = MutableStateFlow(false)
    val isReplying: StateFlow<Boolean> = _isReplying

    private val _replyMessageId = MutableStateFlow<String?>(null)
    private val _replyName = MutableStateFlow<UserName?>(null)
    private val repeatedSend = MutableStateFlow(RepeatedSendData(enabled = false, message = ""))
    private val fullScreenSheetState = MutableStateFlow<FullScreenSheetState>(FullScreenSheetState.Closed)
    private val mentionSheetTab = MutableStateFlow(0)
    private val _isEmoteMenuOpen = MutableStateFlow(false)
    val isEmoteMenuOpen = _isEmoteMenuOpen.asStateFlow()

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

    private var _uiState: StateFlow<ChatInputUiState>? = null

    init {
        viewModelScope.launch {
            chatRepository.activeChannel.collect {
                repeatedSend.update { it.copy(enabled = false) }
            }
        }

        viewModelScope.launch {
            repeatedSend.collectLatest {
                if (it.enabled && it.message.isNotBlank()) {
                    while (isActive) {
                        val activeChannel = chatRepository.activeChannel.value ?: break
                        val delay = userStateRepository.getSendDelay(activeChannel)
                        trySendMessageOrCommand(it.message, skipSuspendingCommands = true)
                        delay(delay)
                    }
                }
            }
        }
    }

    private data class UiDependencies(
        val text: String,
        val suggestions: List<Suggestion>,
        val activeChannel: UserName?,
        val connectionState: ConnectionState,
        val isLoggedIn: Boolean
    )

    private data class SheetAndReplyState(
        val sheetState: FullScreenSheetState,
        val tab: Int,
        val isReplying: Boolean,
        val replyName: UserName?,
        val replyMessageId: String?,
        val isEmoteMenuOpen: Boolean
    )

    fun uiState(fullScreenSheetState: StateFlow<FullScreenSheetState>, mentionSheetTab: StateFlow<Int>): StateFlow<ChatInputUiState> {
        if (_uiState != null) return _uiState!!

        val baseFlow = combine(
            textFlow,
            suggestions,
            chatRepository.activeChannel,
            chatRepository.activeChannel.flatMapLatest { channel ->
                if (channel == null) flowOf(ConnectionState.DISCONNECTED)
                else chatRepository.getConnectionState(channel)
            },
            preferenceStore.isLoggedInFlow
        ) { text, suggestions, activeChannel, connectionState, isLoggedIn ->
            UiDependencies(text, suggestions, activeChannel, connectionState, isLoggedIn)
        }

        val replyStateFlow = combine(
            _isReplying,
            _replyName,
            _replyMessageId
        ) { isReplying, replyName, replyMessageId ->
            Triple(isReplying, replyName, replyMessageId)
        }

        val sheetAndReplyFlow = combine(
            fullScreenSheetState,
            mentionSheetTab,
            replyStateFlow,
            _isEmoteMenuOpen
        ) { sheetState, tab, replyState, isEmoteMenuOpen ->
            val (isReplying, replyName, replyMessageId) = replyState
            SheetAndReplyState(sheetState, tab, isReplying, replyName, replyMessageId, isEmoteMenuOpen)
        }

        _uiState = combine(
            baseFlow,
            sheetAndReplyFlow
        ) { (text, suggestions, activeChannel, connectionState, isLoggedIn), (sheetState, tab, isReplying, replyName, replyMessageId, isEmoteMenuOpen) ->
            this.fullScreenSheetState.value = sheetState
            this.mentionSheetTab.value = tab

            val isMentionsTabActive = (sheetState is FullScreenSheetState.Mention || sheetState is FullScreenSheetState.Whisper) && tab == 0
            val isInReplyThread = sheetState is FullScreenSheetState.Replies
            val effectiveIsReplying = isReplying || isInReplyThread

            val inputState = when (connectionState) {
                ConnectionState.CONNECTED -> when {
                    effectiveIsReplying -> InputState.Replying
                    else -> InputState.Default
                }
                ConnectionState.CONNECTED_NOT_LOGGED_IN -> InputState.NotLoggedIn
                ConnectionState.DISCONNECTED -> InputState.Disconnected
            }

            val canSend = text.isNotBlank() && activeChannel != null && connectionState == ConnectionState.CONNECTED && isLoggedIn && !isMentionsTabActive
            val enabled = isLoggedIn && connectionState == ConnectionState.CONNECTED && !isMentionsTabActive

            val showReplyOverlay = isReplying && !isInReplyThread
            val effectiveReplyName = replyName ?: (sheetState as? FullScreenSheetState.Replies)?.replyName

            ChatInputUiState(
                text = text,
                canSend = canSend,
                enabled = enabled,
                suggestions = suggestions,
                activeChannel = activeChannel,
                connectionState = connectionState,
                isLoggedIn = isLoggedIn,
                inputState = inputState,
                showReplyOverlay = showReplyOverlay,
                replyMessageId = replyMessageId ?: (sheetState as? FullScreenSheetState.Replies)?.replyMessageId,
                replyName = effectiveReplyName,
                isEmoteMenuOpen = isEmoteMenuOpen
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ChatInputUiState())

        return _uiState!!
    }

    fun sendMessage() {
        val text = textFieldState.text.toString()
        if (text.isNotBlank()) {
            trySendMessageOrCommand(text)
            textFieldState.clearText()
        }
    }

    fun trySendMessageOrCommand(message: String, skipSuspendingCommands: Boolean = false) = viewModelScope.launch {
        val channel = chatRepository.activeChannel.value ?: return@launch
        val chatState = fullScreenSheetState.value
        val replyIdOrNull = when {
            chatState is FullScreenSheetState.Replies -> chatState.replyMessageId
            _isReplying.value -> _replyMessageId.value
            else -> null
        }

        val commandResult = runCatching {
            when (chatState) {
                FullScreenSheetState.Whisper -> commandRepository.checkForWhisperCommand(message, skipSuspendingCommands)
                else                         -> {
                    val roomState = channelRepository.getRoomState(channel) ?: return@launch
                    val userState = userStateRepository.userState.value
                    val shouldSkip = skipSuspendingCommands || chatState is FullScreenSheetState.Replies
                    commandRepository.checkForCommands(message, channel, roomState, userState, shouldSkip)
                }
            }
        }.getOrElse {
            mainEventBus.emitEvent(MainEvent.Error(it))
            return@launch
        }

        when (commandResult) {
            is CommandResult.Accepted,
            is CommandResult.Blocked               -> Unit

            is CommandResult.IrcCommand,
            is CommandResult.NotFound              -> {
                chatRepository.sendMessage(message, replyIdOrNull)
                setReplying(false)
            }

            is CommandResult.AcceptedTwitchCommand -> {
                if (commandResult.command == TwitchCommand.Whisper) {
                    chatRepository.fakeWhisperIfNecessary(message)
                }
                if (commandResult.response != null) {
                    chatRepository.makeAndPostCustomSystemMessage(commandResult.response, channel)
                }
            }

            is CommandResult.AcceptedWithResponse  -> chatRepository.makeAndPostCustomSystemMessage(commandResult.response, channel)
            is CommandResult.Message               -> {
                chatRepository.sendMessage(commandResult.message, replyIdOrNull)
                setReplying(false)
            }
        }

        if (commandResult != CommandResult.NotFound && commandResult != CommandResult.IrcCommand) {
            chatRepository.appendLastMessage(channel, message)
        }
    }

    fun getLastMessage() {
        val lastMessage = chatRepository.getLastMessage() ?: return
        textFieldState.edit {
            replace(0, length, lastMessage)
            placeCursorAtEnd()
        }
    }

    fun setRepeatedSend(enabled: Boolean) {
        val message = textFieldState.text.toString()
        repeatedSend.update {
            RepeatedSendData(enabled, message)
        }
    }

    fun setReplying(replying: Boolean, replyMessageId: String? = null, replyName: UserName? = null) {
        _isReplying.value = replying || replyMessageId != null
        _replyMessageId.value = replyMessageId
        _replyName.value = replyName
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

    fun toggleEmoteMenu() {
        _isEmoteMenuOpen.update { !it }
    }

    fun setEmoteMenuOpen(open: Boolean) {
        _isEmoteMenuOpen.value = open
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
    val inputState: InputState = InputState.Disconnected,
    val showReplyOverlay: Boolean = false,
    val replyMessageId: String? = null,
    val replyName: UserName? = null,
    val isEmoteMenuOpen: Boolean = false
)
