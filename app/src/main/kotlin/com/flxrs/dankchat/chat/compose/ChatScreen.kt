package com.flxrs.dankchat.chat.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TooltipState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.flxrs.dankchat.R
import com.flxrs.dankchat.chat.compose.messages.AutomodMessageComposable
import com.flxrs.dankchat.chat.compose.messages.DateSeparatorComposable
import com.flxrs.dankchat.chat.compose.messages.ModerationMessageComposable
import com.flxrs.dankchat.chat.compose.messages.NoticeMessageComposable
import com.flxrs.dankchat.chat.compose.messages.PointRedemptionMessageComposable
import com.flxrs.dankchat.chat.compose.messages.PrivMessageComposable
import com.flxrs.dankchat.chat.compose.messages.SystemMessageComposable
import com.flxrs.dankchat.chat.compose.messages.UserNoticeMessageComposable
import com.flxrs.dankchat.chat.compose.messages.WhisperMessageComposable
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.twitch.emote.ChatMessageEmote
import com.flxrs.dankchat.main.compose.TourTooltip

/**
 * Main composable for rendering chat messages in a scrollable list.
 * 
 * Features:
 * - LazyColumn with reverseLayout for bottom-anchored scrolling
 * - Automatic scroll to bottom when new messages arrive
 * - FAB to manually scroll to bottom
 * - Efficient recomposition with stable keys
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    scrollModifier: Modifier = Modifier,
    onScrollToBottom: () -> Unit = {},
    onScrollDirectionChanged: (isScrollingUp: Boolean) -> Unit = {},
    scrollToMessageId: String? = null,
    onScrollToMessageHandled: () -> Unit = {},
    onJumpToMessage: ((messageId: String, channel: UserName) -> Unit)? = null,
    onAutomodAllow: (heldMessageId: String, channel: UserName) -> Unit = { _, _ -> },
    onAutomodDeny: (heldMessageId: String, channel: UserName) -> Unit = { _, _ -> },
    containerColor: Color = MaterialTheme.colorScheme.background,
    showFabs: Boolean = true,
    recoveryFabTooltipState: TooltipState? = null,
    onTourAdvance: (() -> Unit)? = null,
    onTourSkip: (() -> Unit)? = null,
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

    // Disable auto-scroll when user scrolls up, re-enable when they return to bottom
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.lastScrolledForward && shouldAutoScroll) {
            shouldAutoScroll = false
        }
        if (!listState.isScrollInProgress && isAtBottom && !shouldAutoScroll) {
            shouldAutoScroll = true
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

    // Handle scroll-to-message requests — keyed on both scrollToMessageId and whether messages
    // are available, so the scroll retries after ViewModel recreation (which briefly empties messages).
    val hasMessages = reversedMessages.isNotEmpty()
    val density = LocalDensity.current
    LaunchedEffect(scrollToMessageId, hasMessages) {
        val targetId = scrollToMessageId ?: return@LaunchedEffect
        if (!hasMessages) return@LaunchedEffect
        val index = reversedMessages.indexOfFirst { it.id == targetId }
        if (index >= 0) {
            shouldAutoScroll = false
            val topPaddingPx = with(density) { contentPadding.calculateTopPadding().roundToPx() }
            val bottomPaddingPx = with(density) { contentPadding.calculateBottomPadding().roundToPx() }
            listState.scrollToCentered(index, topPaddingPx, bottomPaddingPx)
        }
        onScrollToMessageHandled()
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = containerColor
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                reverseLayout = true,
                contentPadding = contentPadding,
                modifier = Modifier
                    .fillMaxSize()
                    .then(scrollModifier)
            ) {
                itemsIndexed(
                    items = reversedMessages,
                    key = { _, message -> message.id },
                    contentType = { _, message ->
                        when (message) {
                            is ChatMessageUiState.SystemMessageUi          -> "system"
                            is ChatMessageUiState.NoticeMessageUi          -> "notice"
                            is ChatMessageUiState.UserNoticeMessageUi      -> "usernotice"
                            is ChatMessageUiState.ModerationMessageUi      -> "moderation"
                            is ChatMessageUiState.AutomodMessageUi         -> "automod"
                            is ChatMessageUiState.PrivMessageUi            -> "privmsg"
                            is ChatMessageUiState.WhisperMessageUi         -> "whisper"
                            is ChatMessageUiState.PointRedemptionMessageUi -> "redemption"
                            is ChatMessageUiState.DateSeparatorUi          -> "datesep"
                        }
                    }
                ) { index, message ->
                    // reverseLayout=true: index 0 = bottom (newest), index+1 = visually above
                    val highlightedBelow = reversedMessages.getOrNull(index - 1)?.isHighlighted == true
                    val highlightedAbove = reversedMessages.getOrNull(index + 1)?.isHighlighted == true
                    val highlightShape = message.highlightShape(highlightedAbove, highlightedBelow)
                    ChatMessageItem(
                        message = message,
                        highlightShape = highlightShape,
                        fontSize = fontSize,
                        showChannelPrefix = showChannelPrefix,
                        animateGifs = animateGifs,
                        onUserClick = onUserClick,
                        onMessageLongClick = onMessageLongClick,
                        onEmoteClick = onEmoteClick,
                        onReplyClick = onReplyClick,
                        onWhisperReply = onWhisperReply,
                        onJumpToMessage = onJumpToMessage,
                        onAutomodAllow = onAutomodAllow,
                        onAutomodDeny = onAutomodDeny,
                    )

                    // Add divider after each message if enabled
                    if (showLineSeparator) {
                        HorizontalDivider()
                    }
                }
            }

            // FABs at bottom-end with coordinated position animation
            if (showFabs) {
                val showScrollFab = !shouldAutoScroll && messages.isNotEmpty()
                val bottomContentPadding = contentPadding.calculateBottomPadding()
                val fabBottomPadding by animateDpAsState(
                    targetValue = when {
                        showInput     -> bottomContentPadding
                        hasHelperText -> maxOf(bottomContentPadding, 48.dp)
                        else          -> maxOf(bottomContentPadding, 24.dp)
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
                    if (recoveryFabTooltipState != null) {
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                            tooltip = {
                                TourTooltip(
                                    text = stringResource(R.string.tour_recovery_fab),
                                    onAction = { onTourAdvance?.invoke() },
                                    onSkip = { onTourSkip?.invoke() },
                                    isLast = true,
                                )
                            },
                            state = recoveryFabTooltipState,
                            hasAction = true,
                        ) {
                            RecoveryFab(
                                isFullscreen = isFullscreen,
                                showInput = showInput,
                                onRecover = onRecover,
                                modifier = Modifier.padding(bottom = recoveryBottomPadding)
                            )
                        }
                    } else {
                        RecoveryFab(
                            isFullscreen = isFullscreen,
                            showInput = showInput,
                            onRecover = onRecover,
                            modifier = Modifier.padding(bottom = recoveryBottomPadding)
                        )
                    }
                    AnimatedVisibility(
                        visible = showScrollFab,
                        enter = scaleIn() + fadeIn(),
                        exit = scaleOut() + fadeOut(),
                    ) {
                        FloatingActionButton(
                            onClick = {
                                shouldAutoScroll = true
                                onScrollDirectionChanged(false)
                                onScrollToBottom()
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

private val HIGHLIGHT_CORNER_RADIUS = 8.dp

private fun ChatMessageUiState.highlightShape(highlightedAbove: Boolean, highlightedBelow: Boolean): Shape {
    if (!isHighlighted) return RectangleShape
    val top = if (highlightedAbove) 0.dp else HIGHLIGHT_CORNER_RADIUS
    val bottom = if (highlightedBelow) 0.dp else HIGHLIGHT_CORNER_RADIUS
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

/**
 * Renders a single chat message based on its type
 */

@Composable
private fun ChatMessageItem(
    message: ChatMessageUiState,
    highlightShape: Shape,
    fontSize: Float,
    showChannelPrefix: Boolean,
    animateGifs: Boolean,
    onUserClick: (userId: String?, userName: String, displayName: String, channel: String?, badges: List<BadgeUi>, isLongPress: Boolean) -> Unit,
    onMessageLongClick: (messageId: String, channel: String?, fullMessage: String) -> Unit,
    onEmoteClick: (emotes: List<ChatMessageEmote>) -> Unit,
    onReplyClick: (rootMessageId: String, replyName: UserName) -> Unit,
    onWhisperReply: ((userName: UserName) -> Unit)? = null,
    onJumpToMessage: ((messageId: String, channel: UserName) -> Unit)? = null,
    onAutomodAllow: (heldMessageId: String, channel: UserName) -> Unit = { _, _ -> },
    onAutomodDeny: (heldMessageId: String, channel: UserName) -> Unit = { _, _ -> },
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
            highlightShape = highlightShape,
            fontSize = fontSize
        )

        is ChatMessageUiState.ModerationMessageUi      -> ModerationMessageComposable(
            message = message,
            fontSize = fontSize
        )

        is ChatMessageUiState.AutomodMessageUi         -> AutomodMessageComposable(
            message = message,
            fontSize = fontSize,
            onAllow = onAutomodAllow,
            onDeny = onAutomodDeny,
        )

        is ChatMessageUiState.PrivMessageUi            -> {
            if (onJumpToMessage != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        PrivMessageComposable(
                            message = message,
                            highlightShape = highlightShape,
                            fontSize = fontSize,
                            showChannelPrefix = showChannelPrefix,
                            animateGifs = animateGifs,
                            onUserClick = onUserClick,
                            onMessageLongClick = onMessageLongClick,
                            onEmoteClick = onEmoteClick,
                            onReplyClick = onReplyClick
                        )
                    }
                    IconButton(
                        onClick = { onJumpToMessage(message.id, message.channel) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = stringResource(R.string.message_jump_to),
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                PrivMessageComposable(
                    message = message,
                    highlightShape = highlightShape,
                    fontSize = fontSize,
                    showChannelPrefix = showChannelPrefix,
                    animateGifs = animateGifs,
                    onUserClick = onUserClick,
                    onMessageLongClick = onMessageLongClick,
                    onEmoteClick = onEmoteClick,
                    onReplyClick = onReplyClick
                )
            }
        }

        is ChatMessageUiState.PointRedemptionMessageUi -> PointRedemptionMessageComposable(
            message = message,
            highlightShape = highlightShape,
            fontSize = fontSize
        )

        is ChatMessageUiState.DateSeparatorUi          -> DateSeparatorComposable(
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

/**
 * Scrolls so that [index] is vertically centered in the usable viewport area
 * (the region between [topPaddingPx] and [bottomPaddingPx]).
 *
 * Works in two instant steps that coalesce into a single visual frame:
 * 1. [scrollToItem] ensures the target item is laid out and measurable.
 * 2. Reads the item's actual position, computes the delta needed to center it,
 *    and applies the correction via [scroll].
 */
private suspend fun LazyListState.scrollToCentered(index: Int, topPaddingPx: Int, bottomPaddingPx: Int) {
    scrollToItem(index)

    val itemInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index } ?: return
    val viewportHeight = layoutInfo.viewportSize.height
    val usableTop = topPaddingPx
    val usableBottom = viewportHeight - bottomPaddingPx
    val usableCenter = (usableTop + usableBottom) / 2
    val itemCenter = itemInfo.offset + itemInfo.size / 2
    val delta = (itemCenter - usableCenter).toFloat()

    scroll { scrollBy(delta) }
}
