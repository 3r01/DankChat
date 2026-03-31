package com.flxrs.dankchat.ui.chat

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.composables.core.ScrollArea
import com.composables.core.Thumb
import com.composables.core.VerticalScrollbar
import com.composables.core.rememberScrollAreaState
import com.flxrs.dankchat.R
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.twitch.emote.ChatMessageEmote
import com.flxrs.dankchat.preferences.appearance.InputAction
import com.flxrs.dankchat.ui.chat.messages.AutomodMessageComposable
import com.flxrs.dankchat.ui.chat.messages.DateSeparatorComposable
import com.flxrs.dankchat.ui.chat.messages.ModerationMessageComposable
import com.flxrs.dankchat.ui.chat.messages.NoticeMessageComposable
import com.flxrs.dankchat.ui.chat.messages.PointRedemptionMessageComposable
import com.flxrs.dankchat.ui.chat.messages.PrivMessageComposable
import com.flxrs.dankchat.ui.chat.messages.SystemMessageComposable
import com.flxrs.dankchat.ui.chat.messages.UserNoticeMessageComposable
import com.flxrs.dankchat.ui.chat.messages.WhisperMessageComposable
import com.flxrs.dankchat.ui.main.input.TourTooltip
import com.flxrs.dankchat.utils.compose.predictiveBackScale
import kotlinx.collections.immutable.ImmutableList

data class ChatScreenCallbacks(
    val onUserClick: (userId: String?, userName: String, displayName: String, channel: String?, badges: List<BadgeUi>, isLongPress: Boolean) -> Unit,
    val onMessageLongClick: (messageId: String, channel: String?, fullMessage: String) -> Unit,
    val onEmoteClick: (emotes: List<ChatMessageEmote>) -> Unit = {},
    val onReplyClick: (rootMessageId: String, replyName: UserName) -> Unit = { _, _ -> },
    val onWhisperReply: ((userName: UserName) -> Unit)? = null,
    val onAutomodAllow: (heldMessageId: String, channel: UserName) -> Unit = { _, _ -> },
    val onAutomodDeny: (heldMessageId: String, channel: UserName) -> Unit = { _, _ -> },
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    messages: ImmutableList<ChatMessageUiState>,
    fontSize: Float,
    callbacks: ChatScreenCallbacks,
    modifier: Modifier = Modifier,
    scrollModifier: Modifier = Modifier,
    showChannelPrefix: Boolean = false,
    showLineSeparator: Boolean = false,
    animateGifs: Boolean = true,
    showInput: Boolean = true,
    isFullscreen: Boolean = false,
    onRecover: () -> Unit = {},
    fabMenuCallbacks: FabMenuCallbacks? = null,
    contentPadding: PaddingValues = PaddingValues(),
    onScrollToBottom: () -> Unit = {},
    onScrollDirectionChange: (isScrollingUp: Boolean) -> Unit = {},
    scrollToMessageId: String? = null,
    onScrollToMessageHandle: () -> Unit = {},
    containerColor: Color = MaterialTheme.colorScheme.background,
    showFabs: Boolean = true,
    recoveryFabTooltipState: TooltipState? = null,
    onTourAdvance: (() -> Unit)? = null,
    onTourSkip: (() -> Unit)? = null,
) {
    val listState = rememberLazyListState()

    // Track if we should auto-scroll to bottom (sticky state)
    var shouldAutoScroll by rememberSaveable { mutableStateOf(true) }

    // Detect if we're showing the newest messages (with reverseLayout, index 0 = newest).
    // Require zero scroll offset so items scrolled into the bottom content padding
    // (behind the input bar) don't count as "at bottom".
    val isAtBottom by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 &&
                listState.firstVisibleItemScrollOffset == 0
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
        onScrollDirectionChange(listState.lastScrolledForward)
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
        onScrollToMessageHandle()
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = containerColor,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                reverseLayout = true,
                contentPadding = contentPadding,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .then(scrollModifier),
            ) {
                itemsIndexed(
                    items = reversedMessages,
                    key = { _, message -> message.id },
                    contentType = { _, message ->
                        when (message) {
                            is ChatMessageUiState.SystemMessageUi -> "system"
                            is ChatMessageUiState.NoticeMessageUi -> "notice"
                            is ChatMessageUiState.UserNoticeMessageUi -> "usernotice"
                            is ChatMessageUiState.ModerationMessageUi -> "moderation"
                            is ChatMessageUiState.AutomodMessageUi -> "automod"
                            is ChatMessageUiState.PrivMessageUi -> "privmsg"
                            is ChatMessageUiState.WhisperMessageUi -> "whisper"
                            is ChatMessageUiState.PointRedemptionMessageUi -> "redemption"
                            is ChatMessageUiState.DateSeparatorUi -> "datesep"
                        }
                    },
                ) { index, message ->
                    // reverseLayout=true: index 0 = bottom (newest), index+1 = visually above
                    val highlightedBelow = reversedMessages.getOrNull(index - 1)?.isHighlighted == true
                    val highlightedAbove = reversedMessages.getOrNull(index + 1)?.isHighlighted == true
                    val highlightShape = message.highlightShape(highlightedAbove, highlightedBelow, showLineSeparator)
                    ChatMessageItem(
                        message = message,
                        highlightShape = highlightShape,
                        fontSize = fontSize,
                        showChannelPrefix = showChannelPrefix,
                        animateGifs = animateGifs,
                        callbacks = callbacks,
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
                    targetValue = bottomContentPadding,
                    animationSpec = if (showInput) snap() else spring(),
                    label = "fabBottomPadding",
                )
                val recoveryBottomPadding by animateDpAsState(
                    targetValue = if (showScrollFab) 56.dp + 12.dp else 0.dp,
                    label = "recoveryBottomPadding",
                )
                var fabMenuExpanded by remember { mutableStateOf(false) }

                // Dismiss scrim + back handler when fab menu is open
                if (fabMenuExpanded) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() },
                                ) {
                                    fabMenuExpanded = false
                                },
                    )
                }
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.BottomEnd)
                            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp + fabBottomPadding),
                    contentAlignment = Alignment.BottomEnd,
                ) {
                    RecoveryFabs(
                        isFullscreen = isFullscreen,
                        showInput = showInput,
                        onRecover = onRecover,
                        fabMenuCallbacks = fabMenuCallbacks,
                        menuExpanded = fabMenuExpanded,
                        onMenuExpandedChange = { fabMenuExpanded = it },
                        recoveryFabTooltipState = recoveryFabTooltipState,
                        onTourAdvance = {
                            onTourAdvance?.invoke()
                            onRecover()
                        },
                        onTourSkip = {
                            onTourSkip?.invoke()
                            onRecover()
                        },
                        modifier = Modifier.padding(bottom = recoveryBottomPadding),
                    )
                    AnimatedVisibility(
                        visible = showScrollFab,
                        enter = scaleIn() + fadeIn(),
                        exit = scaleOut() + fadeOut(),
                    ) {
                        FloatingActionButton(
                            onClick = {
                                shouldAutoScroll = true
                                onScrollDirectionChange(false)
                                onScrollToBottom()
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Scroll to bottom",
                            )
                        }
                    }
                }
            }
        }
    }
}

@Stable
class FabMenuCallbacks(
    val onAction: (InputAction) -> Unit,
    val isStreamActive: Boolean,
    val hasStreamData: Boolean,
    val isFullscreen: Boolean,
    val isModerator: Boolean,
    val debugMode: Boolean,
    val enabled: Boolean,
    val hasLastMessage: Boolean,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecoveryFabs(
    isFullscreen: Boolean,
    showInput: Boolean,
    onRecover: () -> Unit,
    fabMenuCallbacks: FabMenuCallbacks?,
    menuExpanded: Boolean,
    onMenuExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    recoveryFabTooltipState: TooltipState? = null,
    onTourAdvance: (() -> Unit)? = null,
    onTourSkip: (() -> Unit)? = null,
) {
    val visible = isFullscreen || !showInput

    val escapeFab: @Composable () -> Unit = {
        SmallFloatingActionButton(
            onClick = {
                onMenuExpandedChange(false)
                onTourAdvance?.invoke()
                onRecover()
            },
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.75f),
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ) {
            Icon(
                imageVector = Icons.Default.FullscreenExit,
                contentDescription = stringResource(R.string.menu_exit_fullscreen),
            )
        }
    }

    // TooltipBox must be outside AnimatedVisibility — the scaleIn animation
    // transforms anchor bounds, causing M3 to miscalculate the caret position.
    val tooltipContent: @Composable () -> Unit = {
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // More FAB ↔ Actions menu — hidden during tour so tooltip points at escape FAB
            if (!showInput && fabMenuCallbacks != null && recoveryFabTooltipState == null) {
                AnimatedContent(
                    targetState = menuExpanded,
                    transitionSpec = {
                        (scaleIn() + fadeIn()) togetherWith (scaleOut() + fadeOut())
                    },
                    label = "FabMenuToggle",
                ) { expanded ->
                    when {
                        expanded -> {
                            var backProgress by remember { mutableFloatStateOf(0f) }
                            PredictiveBackHandler { progress ->
                                try {
                                    progress.collect { event ->
                                        backProgress = event.progress
                                    }
                                    onMenuExpandedChange(false)
                                } catch (_: Exception) {
                                    backProgress = 0f
                                }
                            }
                            FabActionsMenu(
                                callbacks = fabMenuCallbacks,
                                onDismiss = { onMenuExpandedChange(false) },
                                modifier = Modifier.predictiveBackScale(backProgress),
                            )
                        }

                        else -> {
                            SmallFloatingActionButton(
                                onClick = { onMenuExpandedChange(true) },
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.75f),
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = stringResource(R.string.more),
                                )
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = visible,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut(),
            ) {
                escapeFab()
            }
        }
    }

    if (recoveryFabTooltipState != null) {
        TooltipBox(
            positionProvider =
                TooltipDefaults.rememberTooltipPositionProvider(
                    TooltipAnchorPosition.Above,
                    spacingBetweenTooltipAndAnchor = 8.dp,
                ),
            tooltip = {
                TourTooltip(
                    text = stringResource(R.string.tour_recovery_fab),
                    onAction = { onTourAdvance?.invoke() },
                    onSkip = { onTourSkip?.invoke() },
                    isLast = true,
                )
            },
            state = recoveryFabTooltipState,
            onDismissRequest = {},
            hasAction = true,
            modifier = modifier,
        ) {
            tooltipContent()
        }
    } else {
        AnimatedVisibility(
            visible = visible,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = modifier,
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (!showInput && fabMenuCallbacks != null) {
                    AnimatedContent(
                        targetState = menuExpanded,
                        transitionSpec = {
                            (scaleIn() + fadeIn()) togetherWith (scaleOut() + fadeOut())
                        },
                        label = "FabMenuToggle",
                    ) { expanded ->
                        when {
                            expanded -> {
                                var backProgress by remember { mutableFloatStateOf(0f) }
                                PredictiveBackHandler { progress ->
                                    try {
                                        progress.collect { event ->
                                            backProgress = event.progress
                                        }
                                        onMenuExpandedChange(false)
                                    } catch (_: Exception) {
                                        backProgress = 0f
                                    }
                                }
                                FabActionsMenu(
                                    callbacks = fabMenuCallbacks,
                                    onDismiss = { onMenuExpandedChange(false) },
                                    modifier = Modifier.predictiveBackScale(backProgress),
                                )
                            }

                            else -> {
                                SmallFloatingActionButton(
                                    onClick = { onMenuExpandedChange(true) },
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.75f),
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = stringResource(R.string.more),
                                    )
                                }
                            }
                        }
                    }
                }

                escapeFab()
            }
        }
    }
}

@Composable
private fun FabActionsMenu(
    callbacks: FabMenuCallbacks,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val windowHeight =
        with(density) {
            LocalWindowInfo.current.containerSize.height
                .toDp()
        }
    val menuMaxHeight = windowHeight * 0.35f
    val scrollState = rememberScrollState()

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shadowElevation = 4.dp,
        modifier = modifier.heightIn(max = menuMaxHeight),
    ) {
        ScrollArea(state = rememberScrollAreaState(scrollState)) {
            Column(
                modifier =
                    Modifier
                        .width(IntrinsicSize.Max)
                        .verticalScroll(scrollState),
            ) {
                for (action in InputAction.entries) {
                    val item =
                        getFabMenuItem(
                            action = action,
                            isStreamActive = callbacks.isStreamActive,
                            hasStreamData = callbacks.hasStreamData,
                            isFullscreen = callbacks.isFullscreen,
                            isModerator = callbacks.isModerator,
                            debugMode = callbacks.debugMode,
                        ) ?: continue

                    val actionEnabled =
                        when (action) {
                            InputAction.Search, InputAction.Fullscreen, InputAction.HideInput, InputAction.Debug -> true
                            InputAction.LastMessage -> callbacks.enabled && callbacks.hasLastMessage
                            InputAction.Stream, InputAction.ModActions -> callbacks.enabled
                        }

                    DropdownMenuItem(
                        text = { Text(stringResource(item.labelRes)) },
                        onClick = {
                            callbacks.onAction(action)
                            onDismiss()
                        },
                        enabled = actionEnabled,
                        leadingIcon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                            )
                        },
                    )
                }
            }
            if (scrollState.maxValue > 0) {
                VerticalScrollbar(
                    modifier =
                        Modifier
                            .align(Alignment.TopEnd)
                            .fillMaxHeight()
                            .width(3.dp)
                            .padding(vertical = 2.dp),
                ) {
                    Thumb(
                        Modifier.background(
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            RoundedCornerShape(100),
                        ),
                    )
                }
            }
        }
    }
}

@Immutable
private data class FabMenuItem(
    val labelRes: Int,
    val icon: ImageVector,
)

private fun getFabMenuItem(
    action: InputAction,
    isStreamActive: Boolean,
    hasStreamData: Boolean,
    isFullscreen: Boolean,
    isModerator: Boolean,
    debugMode: Boolean,
): FabMenuItem? = when (action) {
    InputAction.Search -> {
        FabMenuItem(R.string.input_action_search, Icons.Default.Search)
    }

    InputAction.LastMessage -> {
        FabMenuItem(R.string.input_action_last_message, Icons.Default.History)
    }

    InputAction.Stream -> {
        when {
            hasStreamData || isStreamActive -> {
                FabMenuItem(
                    if (isStreamActive) R.string.menu_hide_stream else R.string.menu_show_stream,
                    if (isStreamActive) Icons.Default.VideocamOff else Icons.Default.Videocam,
                )
            }

            else -> {
                null
            }
        }
    }

    InputAction.ModActions -> {
        when {
            isModerator -> FabMenuItem(R.string.menu_mod_actions, Icons.Default.Shield)
            else -> null
        }
    }

    InputAction.Fullscreen -> {
        FabMenuItem(
            if (isFullscreen) R.string.menu_exit_fullscreen else R.string.menu_fullscreen,
            if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
        )
    }

    InputAction.HideInput -> {
        null
    }

    // Already hidden, no point showing this
    InputAction.Debug -> {
        when {
            debugMode -> FabMenuItem(R.string.input_action_debug, Icons.Default.BugReport)
            else -> null
        }
    }
}

private val HIGHLIGHT_CORNER_RADIUS = 8.dp

private fun ChatMessageUiState.highlightShape(
    highlightedAbove: Boolean,
    highlightedBelow: Boolean,
    showLineSeparator: Boolean,
): Shape {
    if (!isHighlighted) return RectangleShape
    if (showLineSeparator) return RectangleShape
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
    callbacks: ChatScreenCallbacks,
) {
    when (message) {
        is ChatMessageUiState.SystemMessageUi -> {
            SystemMessageComposable(
                message = message,
                fontSize = fontSize,
            )
        }

        is ChatMessageUiState.NoticeMessageUi -> {
            NoticeMessageComposable(
                message = message,
                fontSize = fontSize,
            )
        }

        is ChatMessageUiState.UserNoticeMessageUi -> {
            UserNoticeMessageComposable(
                message = message,
                highlightShape = highlightShape,
                fontSize = fontSize,
            )
        }

        is ChatMessageUiState.ModerationMessageUi -> {
            ModerationMessageComposable(
                message = message,
                fontSize = fontSize,
            )
        }

        is ChatMessageUiState.AutomodMessageUi -> {
            AutomodMessageComposable(
                message = message,
                fontSize = fontSize,
                onAllow = callbacks.onAutomodAllow,
                onDeny = callbacks.onAutomodDeny,
            )
        }

        is ChatMessageUiState.PrivMessageUi -> {
            PrivMessageComposable(
                message = message,
                highlightShape = highlightShape,
                fontSize = fontSize,
                showChannelPrefix = showChannelPrefix,
                animateGifs = animateGifs,
                onUserClick = callbacks.onUserClick,
                onMessageLongClick = callbacks.onMessageLongClick,
                onEmoteClick = callbacks.onEmoteClick,
                onReplyClick = callbacks.onReplyClick,
            )
        }

        is ChatMessageUiState.PointRedemptionMessageUi -> {
            PointRedemptionMessageComposable(
                message = message,
                highlightShape = highlightShape,
                fontSize = fontSize,
            )
        }

        is ChatMessageUiState.DateSeparatorUi -> {
            DateSeparatorComposable(
                message = message,
                fontSize = fontSize,
            )
        }

        is ChatMessageUiState.WhisperMessageUi -> {
            WhisperMessageComposable(
                message = message,
                fontSize = fontSize,
                animateGifs = animateGifs,
                onUserClick = { userId, userName, displayName, badges, isLongPress ->
                    callbacks.onUserClick(userId, userName, displayName, null, badges, isLongPress)
                },
                onMessageLongClick = { messageId, fullMessage ->
                    callbacks.onMessageLongClick(messageId, null, fullMessage)
                },
                onEmoteClick = callbacks.onEmoteClick,
                onWhisperReply = callbacks.onWhisperReply,
            )
        }
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
private suspend fun LazyListState.scrollToCentered(
    index: Int,
    topPaddingPx: Int,
    bottomPaddingPx: Int,
) {
    scrollToItem(index)

    val itemInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index } ?: return
    val viewportHeight = layoutInfo.viewportSize.height
    val usableBottom = viewportHeight - bottomPaddingPx
    val usableCenter = (topPaddingPx + usableBottom) / 2
    val itemCenter = itemInfo.offset + itemInfo.size / 2
    val delta = (itemCenter - usableCenter).toFloat()

    scroll { scrollBy(delta) }
}
