package com.flxrs.dankchat.chat.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.flxrs.dankchat.R
import com.flxrs.dankchat.chat.compose.messages.ModerationMessageComposable
import com.flxrs.dankchat.chat.compose.messages.NoticeMessageComposable
import com.flxrs.dankchat.chat.compose.messages.PointRedemptionMessageComposable
import com.flxrs.dankchat.chat.compose.messages.PrivMessageComposable
import com.flxrs.dankchat.chat.compose.messages.SystemMessageComposable
import com.flxrs.dankchat.chat.compose.messages.UserNoticeMessageComposable
import com.flxrs.dankchat.chat.compose.messages.WhisperMessageComposable
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.twitch.emote.ChatMessageEmote

/**
 * Main composable for rendering chat messages in a scrollable list.
 * 
 * Features:
 * - LazyColumn with reverseLayout for bottom-anchored scrolling
 * - Automatic scroll to bottom when new messages arrive
 * - FAB to manually scroll to bottom
 * - Efficient recomposition with stable keys
 */
@Composable
fun ChatScreen(
    messages: List<ChatMessageUiState>,
    fontSize: Float,
    onUserClick: (userId: String?, userName: String, displayName: String, channel: String?, badges: List<BadgeUi>, isLongPress: Boolean) -> Unit,
    onMessageLongClick: (messageId: String, channel: String?, fullMessage: String) -> Unit,
    modifier: Modifier = Modifier,
    showChannelPrefix: Boolean = false,
    showLineSeparator: Boolean = false,
    animateGifs: Boolean = true,
    onEmoteClick: (emotes: List<ChatMessageEmote>) -> Unit = {},
    onReplyClick: (rootMessageId: String, replyName: UserName) -> Unit = { _, _ -> },
    onWhisperReply: ((userName: UserName) -> Unit)? = null,
    showInput: Boolean = true,
    isFullscreen: Boolean = false,
    hasHelperText: Boolean = false,
    onRecover: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(),
    onScrollDirectionChanged: (isScrollingUp: Boolean) -> Unit = {},
) {
    val listState = rememberLazyListState()

    // Track if we should auto-scroll to bottom (sticky state)
    var shouldAutoScroll by rememberSaveable { mutableStateOf(true) }

    // Detect if we're showing the newest messages (with reverseLayout, index 0 = newest)
    val isAtBottom by remember {
        derivedStateOf {
            val firstVisibleItem = listState.layoutInfo.visibleItemsInfo.firstOrNull()
            firstVisibleItem?.index == 0
        }
    }

    // Disable auto-scroll when user scrolls forward (up in chat)
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.lastScrolledForward && shouldAutoScroll) {
            shouldAutoScroll = false
        }
        onScrollDirectionChanged(listState.lastScrolledForward)
    }

    // Auto-scroll when new messages arrive or when re-enabled
    LaunchedEffect(shouldAutoScroll, messages) {
        if (shouldAutoScroll) {
            listState.scrollToItem(0)
        }
    }

    val reversedMessages = remember(messages) { messages.asReversed() }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                reverseLayout = true,
                contentPadding = contentPadding,
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = reversedMessages,
                    key = { message -> message.id },
                    contentType = { message ->
                        when (message) {
                            is ChatMessageUiState.SystemMessageUi          -> "system"
                            is ChatMessageUiState.NoticeMessageUi          -> "notice"
                            is ChatMessageUiState.UserNoticeMessageUi      -> "usernotice"
                            is ChatMessageUiState.ModerationMessageUi      -> "moderation"
                            is ChatMessageUiState.PrivMessageUi            -> "privmsg"
                            is ChatMessageUiState.WhisperMessageUi         -> "whisper"
                            is ChatMessageUiState.PointRedemptionMessageUi -> "redemption"
                        }
                    }
                ) { message ->
                    ChatMessageItem(
                        message = message,
                        fontSize = fontSize,
                        showChannelPrefix = showChannelPrefix,
                        animateGifs = animateGifs,
                        onUserClick = onUserClick,
                        onMessageLongClick = onMessageLongClick,
                        onEmoteClick = onEmoteClick,
                        onReplyClick = onReplyClick,
                        onWhisperReply = onWhisperReply,
                    )

                    // Add divider after each message if enabled
                    if (showLineSeparator) {
                        HorizontalDivider()
                    }
                }
            }

            // FABs at bottom-end with coordinated position animation
            val showScrollFab = !isAtBottom && messages.isNotEmpty()
            val bottomContentPadding = contentPadding.calculateBottomPadding()
            val fabBottomPadding by animateDpAsState(
                targetValue = when {
                    showInput -> bottomContentPadding
                    hasHelperText -> 48.dp
                    else -> 24.dp
                },
                animationSpec = if (showInput) snap() else spring(),
                label = "fabBottomPadding"
            )
            val recoveryBottomPadding by animateDpAsState(
                targetValue = if (showScrollFab) 56.dp + 12.dp else 0.dp,
                label = "recoveryBottomPadding"
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp + fabBottomPadding),
                contentAlignment = Alignment.BottomEnd
            ) {
                RecoveryFab(
                    isFullscreen = isFullscreen,
                    showInput = showInput,
                    onRecover = onRecover,
                    modifier = Modifier.padding(bottom = recoveryBottomPadding)
                )
                AnimatedVisibility(
                    visible = showScrollFab,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut(),
                ) {
                    FloatingActionButton(
                        onClick = {
                            shouldAutoScroll = true
                            onScrollDirectionChanged(false)
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Scroll to bottom"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecoveryFab(
    isFullscreen: Boolean,
    showInput: Boolean,
    onRecover: () -> Unit,
    modifier: Modifier = Modifier
) {
    val visible = isFullscreen || !showInput
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn() + fadeIn(),
        exit = scaleOut() + fadeOut(),
        modifier = modifier
    ) {
        SmallFloatingActionButton(
            onClick = onRecover,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ) {
            Icon(
                imageVector = Icons.Default.FullscreenExit,
                contentDescription = stringResource(R.string.menu_exit_fullscreen)
            )
        }
    }
}

/**
 * Renders a single chat message based on its type
 */
@Composable
private fun ChatMessageItem(
    message: ChatMessageUiState,
    fontSize: Float,
    showChannelPrefix: Boolean,
    animateGifs: Boolean,
    onUserClick: (userId: String?, userName: String, displayName: String, channel: String?, badges: List<BadgeUi>, isLongPress: Boolean) -> Unit,
    onMessageLongClick: (messageId: String, channel: String?, fullMessage: String) -> Unit,
    onEmoteClick: (emotes: List<ChatMessageEmote>) -> Unit,
    onReplyClick: (rootMessageId: String, replyName: UserName) -> Unit,
    onWhisperReply: ((userName: UserName) -> Unit)? = null,
) {
    when (message) {
        is ChatMessageUiState.SystemMessageUi          -> SystemMessageComposable(
            message = message,
            fontSize = fontSize
        )

        is ChatMessageUiState.NoticeMessageUi          -> NoticeMessageComposable(
            message = message,
            fontSize = fontSize
        )

        is ChatMessageUiState.UserNoticeMessageUi      -> UserNoticeMessageComposable(
            message = message,
            fontSize = fontSize
        )

        is ChatMessageUiState.ModerationMessageUi      -> ModerationMessageComposable(
            message = message,
            fontSize = fontSize
        )

        is ChatMessageUiState.PrivMessageUi            -> PrivMessageComposable(
            message = message,
            fontSize = fontSize,
            showChannelPrefix = showChannelPrefix,
            animateGifs = animateGifs,
            onUserClick = onUserClick,
            onMessageLongClick = onMessageLongClick,
            onEmoteClick = onEmoteClick,
            onReplyClick = onReplyClick
        )

        is ChatMessageUiState.PointRedemptionMessageUi -> PointRedemptionMessageComposable(
            message = message,
            fontSize = fontSize
        )

        is ChatMessageUiState.WhisperMessageUi         -> WhisperMessageComposable(
            message = message,
            fontSize = fontSize,
            animateGifs = animateGifs,
            onUserClick = { userId, userName, displayName, badges, isLongPress ->
                onUserClick(userId, userName, displayName, null, badges, isLongPress)
            },
            onMessageLongClick = { messageId, fullMessage ->
                onMessageLongClick(messageId, null, fullMessage)
            },
            onEmoteClick = onEmoteClick,
            onWhisperReply = onWhisperReply
        )
    }
}
