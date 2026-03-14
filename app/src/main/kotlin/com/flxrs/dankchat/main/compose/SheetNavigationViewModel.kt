package com.flxrs.dankchat.main.compose

import androidx.lifecycle.ViewModel
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

    fun openReplies(rootMessageId: String) {
        _fullScreenSheetState.value = FullScreenSheetState.Replies(rootMessageId)
    }

    fun openMentions() {
        _fullScreenSheetState.value = FullScreenSheetState.Mention
    }

    fun openWhispers() {
        _fullScreenSheetState.value = FullScreenSheetState.Whisper
    }

    fun closeFullScreenSheet() {
        _fullScreenSheetState.value = FullScreenSheetState.Closed
    }

    fun openEmoteSheet() {
        _inputSheetState.value = InputSheetState.EmoteMenu
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
    data class Replies(val replyMessageId: String) : FullScreenSheetState
    data object Mention : FullScreenSheetState
    data object Whisper : FullScreenSheetState
}

sealed interface InputSheetState {
    data object Closed : InputSheetState
    data object EmoteMenu : InputSheetState
}
