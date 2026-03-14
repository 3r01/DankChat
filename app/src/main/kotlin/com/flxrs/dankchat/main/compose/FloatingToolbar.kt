package com.flxrs.dankchat.main.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.R
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FloatingToolbar(
    tabState: ChannelTabUiState,
    composePagerState: PagerState,
    showAppBar: Boolean,
    isFullscreen: Boolean,
    isLoggedIn: Boolean,
    currentStream: UserName?,
    hasStreamData: Boolean,
    streamHeightDp: Dp,
    totalMentionCount: Int,
    onTabSelected: (Int) -> Unit,
    onTabLongClick: (Int) -> Unit,
    onAddChannel: () -> Unit,
    onOpenMentions: () -> Unit,
    // Overflow menu callbacks
    onLogin: () -> Unit,
    onRelogin: () -> Unit,
    onLogout: () -> Unit,
    onManageChannels: () -> Unit,
    onOpenChannel: () -> Unit,
    onRemoveChannel: () -> Unit,
    onReportChannel: () -> Unit,
    onBlockChannel: () -> Unit,
    onCaptureImage: () -> Unit,
    onCaptureVideo: () -> Unit,
    onChooseMedia: () -> Unit,
    onReloadEmotes: () -> Unit,
    onReconnect: () -> Unit,
    onClearChat: () -> Unit,
    onToggleStream: () -> Unit,
    onOpenSettings: () -> Unit,
    endAligned: Boolean = false,
    showTabs: Boolean = true,
    modifier: Modifier = Modifier,
) {
    if (tabState.tabs.isEmpty()) return

    val density = LocalDensity.current
    var isTabsExpanded by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var overflowInitialMenu by remember { mutableStateOf<AppBarMenu>(AppBarMenu.Main) }

    val totalTabs = tabState.tabs.size
    val hasOverflow = totalTabs > 3
    val selectedIndex = composePagerState.currentPage
    val tabListState = rememberLazyListState()

    // Expand tabs when pager is swiped in a direction with more channels
    LaunchedEffect(composePagerState.isScrollInProgress) {
        if (composePagerState.isScrollInProgress && hasOverflow) {
            val canScroll = tabListState.canScrollForward || tabListState.canScrollBackward
            if (!canScroll) return@LaunchedEffect // all tabs fit, don't expand
            val offset = snapshotFlow { composePagerState.currentPageOffsetFraction }
                .first { it != 0f }
            val current = composePagerState.currentPage
            val swipingForward = offset > 0
            val swipingBackward = offset < 0
            if ((swipingForward && current < totalTabs - 1) || (swipingBackward && current > 0)) {
                isTabsExpanded = true
            }
        }
    }

    // Auto-collapse after all scrolling stops + 2s delay
    LaunchedEffect(isTabsExpanded, composePagerState.isScrollInProgress, tabListState.isScrollInProgress) {
        if (isTabsExpanded && !composePagerState.isScrollInProgress && !tabListState.isScrollInProgress) {
            delay(2000)
            isTabsExpanded = false
        }
    }

    // Reset expanded state when toolbar hides (e.g. keyboard opens in split mode)
    LaunchedEffect(showAppBar) {
        if (!showAppBar) {
            isTabsExpanded = false
            showOverflowMenu = false
        }
    }


    // Dismiss scrim for inline overflow menu
    if (showOverflowMenu) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    showOverflowMenu = false
                    overflowInitialMenu = AppBarMenu.Main
                }
        )
    }

    AnimatedVisibility(
        visible = showAppBar && !isFullscreen,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
            .fillMaxWidth()
            .padding(top = if (currentStream != null && streamHeightDp > 0.dp) streamHeightDp + 8.dp else 0.dp)
    ) {
        val hasStream = currentStream != null && streamHeightDp > 0.dp
        val scrimColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        val statusBarPx = with(density) { WindowInsets.statusBars.getTop(density).toFloat() }
        var toolbarRowHeight by remember { mutableStateOf(0f) }
        val scrimModifier = if (hasStream) {
            Modifier.fillMaxWidth()
        } else {
            Modifier
                .fillMaxWidth()
                .drawBehind {
                    if (toolbarRowHeight > 0f) {
                        val gradientHeight = statusBarPx + 8.dp.toPx() + toolbarRowHeight + 16.dp.toPx()
                        drawRect(
                            brush = Brush.verticalGradient(
                                0f to scrimColor,
                                0.75f to scrimColor,
                                1f to scrimColor.copy(alpha = 0f),
                                endY = gradientHeight
                            ),
                            size = Size(size.width, gradientHeight)
                        )
                    }
                }
                .padding(top = with(density) { WindowInsets.statusBars.getTop(density).toDp() } + 8.dp)
        }

        Box(modifier = scrimModifier) {
        // Auto-scroll whenever the selected tab isn't fully visible
        LaunchedEffect(Unit) {
            snapshotFlow {
                val currentIndex = composePagerState.currentPage
                val layoutInfo = tabListState.layoutInfo
                val viewportStart = layoutInfo.viewportStartOffset
                val viewportEnd = layoutInfo.viewportEndOffset
                val itemInfo = layoutInfo.visibleItemsInfo.find { it.index == currentIndex }
                when {
                    itemInfo == null -> currentIndex
                    itemInfo.offset < viewportStart -> currentIndex
                    itemInfo.offset + itemInfo.size > viewportEnd -> currentIndex
                    else -> -1
                }
            }.collect { targetIndex ->
                if (targetIndex >= 0) {
                    tabListState.animateScrollToItem(targetIndex)
                }
            }
        }

        // Mention indicators based on visibility
        val visibleItems = tabListState.layoutInfo.visibleItemsInfo
        val firstVisibleIndex = visibleItems.firstOrNull()?.index ?: 0
        val lastVisibleIndex = visibleItems.lastOrNull()?.index ?: (totalTabs - 1)
        val hasLeftMention = tabState.tabs.take(firstVisibleIndex).any { it.mentionCount > 0 }
        val hasRightMention = tabState.tabs.drop(lastVisibleIndex + 1).any { it.mentionCount > 0 }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .onSizeChanged {
                    val h = it.height.toFloat()
                    if (toolbarRowHeight == 0f || h < toolbarRowHeight) toolbarRowHeight = h
                },
            verticalAlignment = Alignment.Top
        ) {
            // Scrollable tabs pill
            AnimatedVisibility(
                visible = showTabs,
                modifier = Modifier.weight(1f, fill = endAligned),
                enter = expandHorizontally(expandFrom = Alignment.Start) + fadeIn(),
                exit = shrinkHorizontally(shrinkTowards = Alignment.Start) + fadeOut(),
            ) {
            Box(modifier = if (endAligned) Modifier.fillMaxWidth() else Modifier) {
            val mentionGradientColor = MaterialTheme.colorScheme.error
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.extraLarge)
                    .drawWithContent {
                    drawContent()
                    val gradientWidth = 24.dp.toPx()
                    if (hasLeftMention) {
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    mentionGradientColor.copy(alpha = 0.5f),
                                    mentionGradientColor.copy(alpha = 0f)
                                ),
                                endX = gradientWidth
                            ),
                            size = Size(gradientWidth, size.height)
                        )
                    }
                    if (hasRightMention) {
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    mentionGradientColor.copy(alpha = 0f),
                                    mentionGradientColor.copy(alpha = 0.5f)
                                ),
                                startX = size.width - gradientWidth,
                                endX = size.width
                            ),
                            topLeft = Offset(size.width - gradientWidth, 0f),
                            size = Size(gradientWidth, size.height)
                        )
                    }
                },
            ) {
                LazyRow(
                    state = tabListState,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .wrapLazyRowContent(tabListState, extraWidth = with(density) { 24.dp.roundToPx() })
                        .padding(horizontal = 12.dp)
                        .clipToBounds()
                ) {
                    itemsIndexed(
                        items = tabState.tabs,
                        key = { _, tab -> tab.channel.value }
                    ) { index, tab ->
                        val isSelected = index == selectedIndex
                        val textColor = when {
                            isSelected -> MaterialTheme.colorScheme.primary
                            tab.mentionCount > 0 || tab.hasUnread -> MaterialTheme.colorScheme.onSurface
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .combinedClickable(
                                    onClick = { onTabSelected(index) },
                                    onLongClick = {
                                        onTabLongClick(index)
                                        overflowInitialMenu = AppBarMenu.Channel
                                        showOverflowMenu = true
                                    }
                                )
                                .defaultMinSize(minHeight = 48.dp)
                                .padding(horizontal = 12.dp)
                        ) {
                            Text(
                                text = tab.displayName,
                                color = textColor,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            )
                            if (tab.mentionCount > 0) {
                                Spacer(Modifier.width(4.dp))
                                Badge()
                            }
                        }
                    }
                }
            }
            }
            }

            // Action icons + inline overflow menu (animated with expand/collapse)
            AnimatedVisibility(
                visible = !isTabsExpanded,
                enter = expandHorizontally(
                    expandFrom = Alignment.Start,
                    animationSpec = tween(350, easing = FastOutSlowInEasing)
                ) + fadeIn(tween(200)),
                exit = shrinkHorizontally(
                    shrinkTowards = Alignment.Start,
                    animationSpec = tween(350, easing = FastOutSlowInEasing)
                ) + fadeOut(tween(150))
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Spacer(Modifier.width(8.dp))

                    val pillCornerRadius by animateDpAsState(
                        targetValue = if (showOverflowMenu) 0.dp else 28.dp,
                        animationSpec = tween(200),
                        label = "pillCorner"
                    )
                    Column(modifier = Modifier.width(IntrinsicSize.Min)) {
                        Surface(
                            shape = RoundedCornerShape(
                                topStart = 28.dp,
                                topEnd = 28.dp,
                                bottomStart = pillCornerRadius,
                                bottomEnd = pillCornerRadius
                            ),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = onAddChannel) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = stringResource(R.string.add_channel)
                                    )
                                }
                                IconButton(onClick = onOpenMentions) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = stringResource(R.string.mentions_title),
                                        tint = if (totalMentionCount > 0) {
                                            MaterialTheme.colorScheme.error
                                        } else {
                                            LocalContentColor.current
                                        }
                                    )
                                }
                                IconButton(onClick = {
                                    overflowInitialMenu = AppBarMenu.Main
                                    showOverflowMenu = !showOverflowMenu
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = stringResource(R.string.more)
                                    )
                                }
                            }
                        }
                        AnimatedVisibility(
                            visible = showOverflowMenu,
                            enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                            exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(
                                    topStart = 0.dp,
                                    topEnd = 0.dp,
                                    bottomStart = 12.dp,
                                    bottomEnd = 12.dp
                                ),
                                color = MaterialTheme.colorScheme.surfaceContainer,
                            ) {
                                InlineOverflowMenu(
                                    isLoggedIn = isLoggedIn,
                                    isStreamActive = currentStream != null,
                                    hasStreamData = hasStreamData,
                                    onDismiss = {
                                        showOverflowMenu = false
                                        overflowInitialMenu = AppBarMenu.Main
                                    },
                                    initialMenu = overflowInitialMenu,
                                    onLogin = onLogin,
                                    onRelogin = onRelogin,
                                    onLogout = onLogout,
                                    onManageChannels = onManageChannels,
                                    onOpenChannel = onOpenChannel,
                                    onRemoveChannel = onRemoveChannel,
                                    onReportChannel = onReportChannel,
                                    onBlockChannel = onBlockChannel,
                                    onCaptureImage = onCaptureImage,
                                    onCaptureVideo = onCaptureVideo,
                                    onChooseMedia = onChooseMedia,
                                    onReloadEmotes = onReloadEmotes,
                                    onReconnect = onReconnect,
                                    onClearChat = onClearChat,
                                    onToggleStream = onToggleStream,
                                    onOpenSettings = onOpenSettings
                                )
                            }
                        }
                    }
                }
            }
        }
        }
    }
}

/** Measures [LazyRow] at full width (for scrolling) but reports actual content width so the pill wraps content. */
private fun Modifier.wrapLazyRowContent(listState: LazyListState, extraWidth: Int = 0) = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val items = listState.layoutInfo.visibleItemsInfo
    val contentWidth = if (items.isNotEmpty()) {
        val lastItem = items.last()
        lastItem.offset + lastItem.size + extraWidth
    } else {
        placeable.width
    }
    val width = contentWidth.coerceAtMost(placeable.width)
    layout(width, placeable.height) {
        placeable.place(0, 0)
    }
}
