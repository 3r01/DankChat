package com.flxrs.dankchat.ui.main.sheet

import androidx.compose.runtime.Immutable
import com.flxrs.dankchat.data.UserName

sealed interface FullScreenSheetState {
    data object Closed : FullScreenSheetState

    @Immutable
    data class Replies(val replyMessageId: String, val replyName: UserName) : FullScreenSheetState
    data object Mention : FullScreenSheetState
    data object Whisper : FullScreenSheetState

    @Immutable
    data class History(val channel: UserName, val initialFilter: String = "") : FullScreenSheetState
}

sealed interface InputSheetState {
    data object Closed : InputSheetState
    data object EmoteMenu : InputSheetState
    data object DebugInfo : InputSheetState
}

@Immutable
data class SheetNavigationState(
    val fullScreenSheet: FullScreenSheetState = FullScreenSheetState.Closed,
    val inputSheet: InputSheetState = InputSheetState.Closed,
)
