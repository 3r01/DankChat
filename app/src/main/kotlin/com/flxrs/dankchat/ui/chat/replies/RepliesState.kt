package com.flxrs.dankchat.ui.chat.replies

import androidx.compose.runtime.Immutable
import com.flxrs.dankchat.data.chat.ChatItem
import com.flxrs.dankchat.ui.chat.ChatMessageUiState

@Immutable
sealed interface RepliesState {
    data object NotFound : RepliesState
    data class Found(val items: List<ChatItem>) : RepliesState
}

@Immutable
sealed interface RepliesUiState {
    data object NotFound : RepliesUiState
    data class Found(val items: List<ChatMessageUiState>) : RepliesUiState
}
