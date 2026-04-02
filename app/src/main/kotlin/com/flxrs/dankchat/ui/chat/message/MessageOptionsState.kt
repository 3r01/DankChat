package com.flxrs.dankchat.ui.chat.message

import androidx.compose.runtime.Immutable
import com.flxrs.dankchat.data.UserName

@Immutable
sealed interface MessageOptionsState {
    data object Loading : MessageOptionsState

    data object NotFound : MessageOptionsState

    data class Found(
        val messageId: String,
        val rootThreadId: String,
        val rootThreadName: UserName?,
        val rootThreadMessage: String?,
        val replyName: UserName,
        val name: UserName,
        val originalMessage: String,
        val canModerate: Boolean,
        val hasReplyThread: Boolean,
        val canReply: Boolean,
    ) : MessageOptionsState
}
