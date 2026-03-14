package com.flxrs.dankchat.chat.compose.messages

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.flxrs.dankchat.chat.compose.ChatMessageUiState
import com.flxrs.dankchat.chat.compose.messages.common.SimpleMessageContainer

/**
 * Renders a system message (connected, disconnected, emote loading failures, etc.)
 */
@Composable
fun SystemMessageComposable(
    message: ChatMessageUiState.SystemMessageUi,
    fontSize: Float,
    modifier: Modifier = Modifier,
) {
    SimpleMessageContainer(
        message = message.message,
        timestamp = message.timestamp,
        fontSize = fontSize.sp,
        lightBackgroundColor = message.lightBackgroundColor,
        darkBackgroundColor = message.darkBackgroundColor,
        textAlpha = message.textAlpha,
        modifier = modifier,
    )
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
    SimpleMessageContainer(
        message = message.message,
        timestamp = message.timestamp,
        fontSize = fontSize.sp,
        lightBackgroundColor = message.lightBackgroundColor,
        darkBackgroundColor = message.darkBackgroundColor,
        textAlpha = message.textAlpha,
        modifier = modifier,
    )
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
    SimpleMessageContainer(
        message = message.message,
        timestamp = message.timestamp,
        fontSize = fontSize.sp,
        lightBackgroundColor = message.lightBackgroundColor,
        darkBackgroundColor = message.darkBackgroundColor,
        textAlpha = message.textAlpha,
        modifier = modifier,
    )
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
    SimpleMessageContainer(
        message = message.message,
        timestamp = message.timestamp,
        fontSize = fontSize.sp,
        lightBackgroundColor = message.lightBackgroundColor,
        darkBackgroundColor = message.darkBackgroundColor,
        textAlpha = message.textAlpha,
        modifier = modifier,
    )
}
