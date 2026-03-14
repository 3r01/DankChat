package com.flxrs.dankchat.main.compose

import androidx.lifecycle.ViewModel
import com.flxrs.dankchat.data.UserName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class SheetNavigationViewModel : ViewModel() {

    private val _fullScreenSheetState = MutableStateFlow<FullScreenSheetState>(FullScreenSheetState.Closed)
    val fullScreenSheetState: StateFlow<FullScreenSheetState> = _fullScreenSheetState.asStateFlow()

    private val _inputSheetState = MutableStateFlow<InputSheetState>(InputSheetState.Closed)
    val inputSheetState: StateFlow<InputSheetState> = _inputSheetState.asStateFlow()

    fun openReplies(rootMessageId: String, replyName: UserName) {
        _fullScreenSheetState.value = FullScreenSheetState.Replies(rootMessageId, replyName)
    }

    fun openMentions() {
        _fullScreenSheetState.value = FullScreenSheetState.Mention
    }

    fun openWhispers() {
        _fullScreenSheetState.value = FullScreenSheetState.Whisper
    }

    fun openHistory(channel: UserName, initialFilter: String = "") {
        _fullScreenSheetState.value = FullScreenSheetState.History(channel, initialFilter)
    }

    fun closeFullScreenSheet() {
        _fullScreenSheetState.value = FullScreenSheetState.Closed
    }

    fun openEmoteSheet() {
        _inputSheetState.value = InputSheetState.EmoteMenu
    }

    fun openMoreActions(messageId: String, fullMessage: String) {
        _inputSheetState.value = InputSheetState.MoreActions(messageId, fullMessage)
    }

    fun closeInputSheet() {
        _inputSheetState.value = InputSheetState.Closed
    }

    fun handleBackPress(): Boolean {
        return when {
            _inputSheetState.value != InputSheetState.Closed -> {
                closeInputSheet()
                true
            }
            _fullScreenSheetState.value != FullScreenSheetState.Closed -> {
                closeFullScreenSheet()
                true
            }
            else -> false
        }
    }
}

sealed interface FullScreenSheetState {
    data object Closed : FullScreenSheetState
    data class Replies(val replyMessageId: String, val replyName: UserName) : FullScreenSheetState
    data object Mention : FullScreenSheetState
    data object Whisper : FullScreenSheetState
    data class History(val channel: UserName, val initialFilter: String = "") : FullScreenSheetState
}

sealed interface InputSheetState {
    data object Closed : InputSheetState
    data object EmoteMenu : InputSheetState
    data class MoreActions(val messageId: String, val fullMessage: String) : InputSheetState
}
