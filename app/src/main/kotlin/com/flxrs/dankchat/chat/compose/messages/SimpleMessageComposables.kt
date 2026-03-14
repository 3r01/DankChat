package com.flxrs.dankchat.chat.compose.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.flxrs.dankchat.chat.compose.ChatMessageText
import com.flxrs.dankchat.chat.compose.ChatMessageUiState

/**
 * Renders a system message (connected, disconnected, emote loading failures, etc.)
 */
@Composable
fun SystemMessageComposable(
    message: ChatMessageUiState.SystemMessageUi,
    fontSize: Float,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(Color(message.backgroundColor))
    ) {
        ChatMessageText(
            text = message.message,
            timestamp = message.timestamp,
            fontSize = fontSize.sp,
            textColor = Color.White.copy(alpha = message.textAlpha),
        )
    }
}

/**
 * Renders a notice message from Twitch
 */
@Composable
fun NoticeMessageComposable(
    message: ChatMessageUiState.NoticeMessageUi,
    fontSize: Float,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(Color(message.backgroundColor))
    ) {
        ChatMessageText(
            text = message.message,
            timestamp = message.timestamp,
            fontSize = fontSize.sp,
            textColor = Color.White.copy(alpha = message.textAlpha),
        )
    }
}

/**
 * Renders a user notice message (subscriptions, announcements, etc.)
 */
@Composable
fun UserNoticeMessageComposable(
    message: ChatMessageUiState.UserNoticeMessageUi,
    fontSize: Float,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(Color(message.backgroundColor))
    ) {
        ChatMessageText(
            text = message.message,
            timestamp = message.timestamp,
            fontSize = fontSize.sp,
            textColor = Color.White.copy(alpha = message.textAlpha),
        )
    }
}

/**
 * Renders a moderation message (timeouts, bans, deletions)
 */
@Composable
fun ModerationMessageComposable(
    message: ChatMessageUiState.ModerationMessageUi,
    fontSize: Float,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(Color(message.backgroundColor))
    ) {
        ChatMessageText(
            text = message.message,
            timestamp = message.timestamp,
            fontSize = fontSize.sp,
            textColor = Color.White.copy(alpha = message.textAlpha),
        )
    }
}
