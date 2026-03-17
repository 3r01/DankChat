package com.flxrs.dankchat.main.compose

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.placeCursorAtEnd
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.text.TextRange
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flxrs.dankchat.chat.suggestion.Suggestion
import com.flxrs.dankchat.chat.suggestion.SuggestionProvider
import com.flxrs.dankchat.data.DisplayName
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.repo.channel.ChannelRepository
import com.flxrs.dankchat.data.repo.chat.ChatRepository
import com.flxrs.dankchat.data.repo.chat.UserStateRepository
import com.flxrs.dankchat.data.repo.command.CommandRepository
import com.flxrs.dankchat.data.repo.command.CommandResult
import com.flxrs.dankchat.data.repo.emote.EmoteUsageRepository
import com.flxrs.dankchat.data.repo.stream.StreamDataRepository
import com.flxrs.dankchat.data.twitch.chat.ConnectionState
import com.flxrs.dankchat.data.twitch.command.TwitchCommand
import com.flxrs.dankchat.main.InputState
import com.flxrs.dankchat.main.MainEvent
import com.flxrs.dankchat.main.RepeatedSendData
import com.flxrs.dankchat.preferences.DankChatPreferenceStore
import com.flxrs.dankchat.preferences.appearance.AppearanceSettingsDataStore
import com.flxrs.dankchat.preferences.chat.ChatSettingsDataStore
import com.flxrs.dankchat.preferences.developer.DeveloperSettingsDataStore
import com.flxrs.dankchat.preferences.notifications.NotificationsSettingsDataStore
import com.flxrs.dankchat.preferences.stream.StreamsSettingsDataStore
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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
    private val chatSettingsDataStore: ChatSettingsDataStore,
    private val developerSettingsDataStore: DeveloperSettingsDataStore,
    private val streamDataRepository: StreamDataRepository,
    private val streamsSettingsDataStore: StreamsSettingsDataStore,
    private val appearanceSettingsDataStore: AppearanceSettingsDataStore,
    private val notificationsSettingsDataStore: NotificationsSettingsDataStore,
    private val emoteUsageRepository: EmoteUsageRepository,
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

    private val _whisperTarget = MutableStateFlow<UserName?>(null)
    val whisperTarget: StateFlow<UserName?> = _whisperTarget.asStateFlow()

    // Create flow from TextFieldState tracking both text and cursor position
    private val codePointCount = snapshotFlow {
        val text = textFieldState.text
        text.toString().codePointCount(0, text.length)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val textFlow = snapshotFlow { textFieldState.text.toString() }
    private val textAndCursorFlow = snapshotFlow {
        textFieldState.text.toString() to textFieldState.selection.start
    }

    // Debounce text/cursor changes for suggestion lookups
    private val debouncedTextAndCursor = textAndCursorFlow.debounce(SUGGESTION_DEBOUNCE_MS)

    // Get suggestions based on current text, cursor position, and active channel
    private val suggestions: StateFlow<List<Suggestion>> = combine(
        debouncedTextAndCursor,
        chatRepository.activeChannel
    ) { (text, cursorPos), channel ->
        Triple(text, cursorPos, channel)
    }.flatMapLatest { (text, cursorPos, channel) ->
        suggestionProvider.getSuggestions(text, cursorPos, channel)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val roomStateDisplayText: StateFlow<String?> = combine(
        chatSettingsDataStore.showChatModes,
        chatRepository.activeChannel
    ) { showModes, channel ->
        showModes to channel
    }.flatMapLatest { (showModes, channel) ->
        if (!showModes || channel == null) flowOf(null)
        else channelRepository.getRoomStateFlow(channel).map { it.toDisplayText().ifEmpty { null } }
    }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val currentStreamInfo: StateFlow<String?> = combine(
        streamsSettingsDataStore.showStreamsInfo,
        chatRepository.activeChannel,
        streamDataRepository.streamData
    ) { streamInfoEnabled, activeChannel, streamData ->
        streamData.find { it.channel == activeChannel }?.formattedData?.takeIf { streamInfoEnabled }
    }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val helperText: StateFlow<String?> = combine(
        roomStateDisplayText,
        currentStreamInfo
    ) { roomState, streamInfo ->
        listOfNotNull(roomState, streamInfo)
            .joinToString(separator = " - ")
            .ifEmpty { null }
    }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private var _uiState: StateFlow<ChatInputUiState>? = null

    init {
        viewModelScope.launch {
            chatRepository.activeChannel.collect {
                repeatedSend.update { it.copy(enabled = false) }
            }
        }

        // Clear whisper target when sheet closes or tab switches away from whispers
        viewModelScope.launch {
            combine(fullScreenSheetState, mentionSheetTab) { sheetState, tab ->
                sheetState to tab
            }.collect { (sheetState, tab) ->
                val isWhisperTab = (sheetState is FullScreenSheetState.Mention || sheetState is FullScreenSheetState.Whisper) && tab == 1
                if (!isWhisperTab && _whisperTarget.value != null) {
                    _whisperTarget.value = null
                    textFieldState.clearText()
                }
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
        val isLoggedIn: Boolean,
        val autoDisableInput: Boolean
    )

    private data class InputOverlayState(
        val sheetState: FullScreenSheetState,
        val tab: Int,
        val isReplying: Boolean,
        val replyName: UserName?,
        val replyMessageId: String?,
        val isEmoteMenuOpen: Boolean,
        val whisperTarget: UserName?
    )

    fun uiState(externalSheetState: StateFlow<FullScreenSheetState>, externalMentionTab: StateFlow<Int>): StateFlow<ChatInputUiState> {
        _uiState?.let { return it }

        // Wire up external sheet state for whisper clearing
        viewModelScope.launch {
            combine(externalSheetState, externalMentionTab) { sheetState, tab ->
                sheetState to tab
            }.collect { (sheetState, tab) ->
                fullScreenSheetState.value = sheetState
                mentionSheetTab.value = tab
            }
        }

        val baseFlow = combine(
            textFlow,
            suggestions,
            chatRepository.activeChannel,
            chatRepository.activeChannel.flatMapLatest { channel ->
                if (channel == null) flowOf(ConnectionState.DISCONNECTED)
                else chatRepository.getConnectionState(channel)
            },
            combine(preferenceStore.isLoggedInFlow, appearanceSettingsDataStore.settings.map { it.autoDisableInput }) { a, b -> a to b }
        ) { text, suggestions, activeChannel, connectionState, (isLoggedIn, autoDisableInput) ->
            UiDependencies(text, suggestions, activeChannel, connectionState, isLoggedIn, autoDisableInput)
        }

        val replyStateFlow = combine(
            _isReplying,
            _replyName,
            _replyMessageId
        ) { isReplying, replyName, replyMessageId ->
            Triple(isReplying, replyName, replyMessageId)
        }

        val inputOverlayFlow = combine(
            externalSheetState,
            externalMentionTab,
            replyStateFlow,
            _isEmoteMenuOpen,
            _whisperTarget
        ) { sheetState, tab, replyState, isEmoteMenuOpen, whisperTarget ->
            val (isReplying, replyName, replyMessageId) = replyState
            InputOverlayState(sheetState, tab, isReplying, replyName, replyMessageId, isEmoteMenuOpen, whisperTarget)
        }

        return combine(
            baseFlow,
            inputOverlayFlow,
            helperText,
            codePointCount,
        ) { (text, suggestions, activeChannel, connectionState, isLoggedIn, autoDisableInput), (sheetState, tab, isReplying, replyName, replyMessageId, isEmoteMenuOpen, whisperTarget), helperText, codePoints ->
            val isMentionsTabActive = (sheetState is FullScreenSheetState.Mention || sheetState is FullScreenSheetState.Whisper) && tab == 0
            val isWhisperTabActive = (sheetState is FullScreenSheetState.Mention || sheetState is FullScreenSheetState.Whisper) && tab == 1
            val isInReplyThread = sheetState is FullScreenSheetState.Replies
            val effectiveIsReplying = isReplying || isInReplyThread
            val canTypeInConnectionState = connectionState == ConnectionState.CONNECTED || !autoDisableInput

            val inputState = when (connectionState) {
                ConnectionState.CONNECTED               -> when {
                    isWhisperTabActive && whisperTarget != null -> InputState.Whispering
                    effectiveIsReplying                         -> InputState.Replying
                    else                                        -> InputState.Default
                }

                ConnectionState.CONNECTED_NOT_LOGGED_IN -> InputState.NotLoggedIn
                ConnectionState.DISCONNECTED            -> InputState.Disconnected
            }

            val enabled = when {
                isMentionsTabActive -> false
                isWhisperTabActive  -> isLoggedIn && canTypeInConnectionState && whisperTarget != null
                else                -> isLoggedIn && canTypeInConnectionState
            }

            val canSend = text.isNotBlank() && activeChannel != null && connectionState == ConnectionState.CONNECTED && isLoggedIn && enabled

            val showReplyOverlay = isReplying && !isInReplyThread
            val showWhisperOverlay = isWhisperTabActive && whisperTarget != null
            val effectiveReplyName = replyName ?: (sheetState as? FullScreenSheetState.Replies)?.replyName

            ChatInputUiState(
                text = text,
                canSend = canSend,
                enabled = enabled,
                hasLastMessage = chatRepository.getLastMessage() != null,
                suggestions = suggestions.toImmutableList(),
                activeChannel = activeChannel,
                connectionState = connectionState,
                isLoggedIn = isLoggedIn,
                inputState = inputState,
                showReplyOverlay = showReplyOverlay,
                replyMessageId = replyMessageId ?: (sheetState as? FullScreenSheetState.Replies)?.replyMessageId,
                replyName = effectiveReplyName,
                isEmoteMenuOpen = isEmoteMenuOpen,
                helperText = helperText,
                showWhisperOverlay = showWhisperOverlay,
                whisperTarget = whisperTarget,
                isWhisperTabActive = isWhisperTabActive,
                characterCounter = CharacterCounterState.Visible(
                    text = "$codePoints/$MESSAGE_CODE_POINT_LIMIT",
                    isOverLimit = codePoints > MESSAGE_CODE_POINT_LIMIT,
                ),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ChatInputUiState()).also { _uiState = it }
    }

    fun sendMessage() {
        val text = textFieldState.text.toString()
        if (text.isNotBlank()) {
            val whisperTarget = _whisperTarget.value
            val messageToSend = if (whisperTarget != null) {
                "/w ${whisperTarget.value} $text"
            } else {
                text
            }
            trySendMessageOrCommand(messageToSend)
            textFieldState.clearText()
        }
    }

    fun trySendMessageOrCommand(message: String, skipSuspendingCommands: Boolean = false) = viewModelScope.launch {
        val channel = chatRepository.activeChannel.value ?: return@launch
        val chatState = fullScreenSheetState.value
        val replyIdOrNull = when {
            chatState is FullScreenSheetState.Replies -> chatState.replyMessageId
            _isReplying.value                         -> _replyMessageId.value
            else                                      -> null
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

    fun setWhisperTarget(target: UserName?) {
        _whisperTarget.value = target
        if (target == null) {
            textFieldState.clearText()
        }
    }

    fun mentionUser(user: UserName, display: DisplayName) {
        val template = notificationsSettingsDataStore.current().mentionFormat.template
        val mention = "${template.replace("name", user.valueOrDisplayName(display))} "
        insertText(mention)
    }

    fun insertText(text: String) {
        textFieldState.edit {
            append(text)
            placeCursorAtEnd()
        }
    }

    fun deleteLastWord() {
        val text = textFieldState.text
        if (text.isEmpty()) return
        var end = text.length
        // Skip trailing spaces
        while (end > 0 && text[end - 1] == ' ') end--
        // Find start of word
        var start = end
        while (start > 0 && text[start - 1] != ' ') start--
        textFieldState.edit {
            replace(start, length, "")
        }
    }

    fun postSystemMessage(message: String) {
        val channel = chatRepository.activeChannel.value ?: return
        chatRepository.makeAndPostCustomSystemMessage(message, channel)
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
        val cursorPos = textFieldState.selection.start
        val result = computeSuggestionReplacement(currentText, cursorPos, suggestion.toString())

        textFieldState.edit {
            replace(result.replaceStart, result.replaceEnd, result.replacement)
            selection = TextRange(result.newCursorPos)
        }

        if (suggestion is Suggestion.EmoteSuggestion) {
            addEmoteUsage(suggestion.emote.id)
        }
    }

    fun addEmoteUsage(emoteId: String) {
        viewModelScope.launch {
            emoteUsageRepository.addEmoteUsage(emoteId)
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
        private const val MESSAGE_CODE_POINT_LIMIT = 500
    }
}

internal data class SuggestionReplacementResult(
    val replaceStart: Int,
    val replaceEnd: Int,
    val replacement: String,
    val newCursorPos: Int,
)

internal fun computeSuggestionReplacement(text: String, cursorPos: Int, suggestionText: String): SuggestionReplacementResult {
    val separator = ' '

    // Only look backwards from cursor — match what extractCurrentWord does
    var start = cursorPos
    while (start > 0 && text[start - 1] != separator) start--

    val replacement = suggestionText + separator
    return SuggestionReplacementResult(
        replaceStart = start,
        replaceEnd = cursorPos,
        replacement = replacement,
        newCursorPos = start + replacement.length,
    )
}

@Immutable
data class ChatInputUiState(
    val text: String = "",
    val canSend: Boolean = false,
    val enabled: Boolean = false,
    val hasLastMessage: Boolean = false,
    val suggestions: ImmutableList<Suggestion> = persistentListOf(),
    val activeChannel: UserName? = null,
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val isLoggedIn: Boolean = false,
    val inputState: InputState = InputState.Disconnected,
    val showReplyOverlay: Boolean = false,
    val replyMessageId: String? = null,
    val replyName: UserName? = null,
    val isEmoteMenuOpen: Boolean = false,
    val helperText: String? = null,
    val showWhisperOverlay: Boolean = false,
    val whisperTarget: UserName? = null,
    val isWhisperTabActive: Boolean = false,
    val characterCounter: CharacterCounterState = CharacterCounterState.Hidden,
)

@Stable
sealed interface CharacterCounterState {
    data object Hidden : CharacterCounterState

    @Immutable
    data class Visible(val text: String, val isOverLimit: Boolean) : CharacterCounterState
}
