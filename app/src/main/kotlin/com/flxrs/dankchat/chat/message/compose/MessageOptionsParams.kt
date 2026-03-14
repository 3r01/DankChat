package com.flxrs.dankchat.chat.message.compose

import com.flxrs.dankchat.data.UserName

data class MessageOptionsParams(
    val messageId: String,
    val channel: UserName?,
    val fullMessage: String,
    val canModerate: Boolean,
    val canReply: Boolean,
    val canCopy: Boolean = true,
)
