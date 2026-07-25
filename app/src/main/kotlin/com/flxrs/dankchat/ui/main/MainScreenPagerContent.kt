package com.flxrs.dankchat.ui.main

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.TooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.preferences.components.DankBackground
import com.flxrs.dankchat.ui.chat.ChatComposable
import com.flxrs.dankchat.ui.chat.FabMenuCallbacks
import com.flxrs.dankchat.ui.main.channel.ChannelPagerUiState
import com.flxrs.dankchat.ui.main.channel.ChannelTabUiState
import com.flxrs.dankchat.ui.tour.TourStep
import kotlinx.collections.immutable.ImmutableMap

@Stable
internal class ChatPagerCallbacks(
    val onOpenReplies: (String, UserName) -> Unit,
    val onRecover: () -> Unit,
    val onScrollToBottom: () -> Unit,
    val onTourAdvance: () -> Unit,
    val onTourSkip: () -> Unit,
    val scrollConnection: NestedScrollConnection? = null,
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun MainScreenPagerContent(
    paddingValues: PaddingValues,
    chatTopPadding: Dp,
    tabState: ChannelTabUiState,
    composePagerState: PagerState,
    pagerState: ChannelPagerUiState,
    isLoggedIn: Boolean,
    showInput: Boolean,
    isFullscreen: Boolean,
    swipeNavigation: Boolean,
    isSheetOpen: Boolean,
    inputHeightDp: Dp,
    helperTextHeightDp: Dp,
    navBarHeightDp: Dp,
    effectiveRoundedCorner: Dp,
    scrollTargets: ImmutableMap<UserName, String>,
    onClearScrollTarget: (UserName) -> Unit,
    callbacks: ChatPagerCallbacks,
    fabMenuCallbacks: FabMenuCallbacks?,
    showPinnedMessage: Boolean,
    isToolbarMenuOpen: Boolean,
    currentTourStep: TourStep?,
    recoveryFabTooltipState: TooltipState?,
    onAddChannel: () -> Unit,
    onLogin: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        val showFullScreenLoading = tabState.loading && tabState.tabs.isEmpty()
        DankBackground(visible = showFullScreenLoading)
        if (showFullScreenLoading) {
            LinearProgressIndicator(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(paddingValues),
            )
            return@Box
        }
        if (tabState.tabs.isEmpty() && !tabState.loading) {
            EmptyStateContent(
                isLoggedIn = isLoggedIn,
                onAddChannel = onAddChannel,
                onLogin = onLogin,
                modifier = Modifier.padding(paddingValues),
            )
        } else {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(top = paddingValues.calculateTopPadding()),
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    HorizontalPager(
                        state = composePagerState,
                        modifier = Modifier.fillMaxSize().edgeGestureGuard(),
                        userScrollEnabled = swipeNavigation,
                        key = { index -> pagerState.channels.getOrNull(index)?.value ?: index },
                    ) { page ->
                        if (page in pagerState.channels.indices) {
                            val channel = pagerState.channels[page]
                            val isNearCurrentPage = (page - composePagerState.currentPage).let { it in -1..1 }
                            ChatComposable(
                                channel = channel,
                                isCollectionActive = isNearCurrentPage,
                                onReplyClick = { replyMessageId, replyName ->
                                    callbacks.onOpenReplies(replyMessageId, replyName)
                                },
                                showInput = showInput,
                                isFullscreen = isFullscreen,
                                showFabs = !isSheetOpen,
                                onRecover = callbacks.onRecover,
                                fabMenuCallbacks = fabMenuCallbacks,
                                showPinnedMessage = showPinnedMessage,
                                isToolbarMenuOpen = isToolbarMenuOpen,
                                contentPadding =
                                    PaddingValues(
                                        top = chatTopPadding + if (isFullscreen) 0.dp else 56.dp,
                                        bottom =
                                            paddingValues.calculateBottomPadding() +
                                                when {
                                                    showInput -> {
                                                        inputHeightDp
                                                    }

                                                    !isFullscreen -> {
                                                        when {
                                                            helperTextHeightDp > 0.dp -> helperTextHeightDp
                                                            else -> max(navBarHeightDp, effectiveRoundedCorner)
                                                        }
                                                    }

                                                    else -> {
                                                        when {
                                                            helperTextHeightDp > 0.dp -> helperTextHeightDp
                                                            else -> effectiveRoundedCorner
                                                        }
                                                    }
                                                },
                                    ),
                                scrollModifier = if (callbacks.scrollConnection != null) Modifier.nestedScroll(callbacks.scrollConnection) else Modifier,
                                onScrollToBottom = callbacks.onScrollToBottom,
                                onScrollDirectionChange = { },
                                scrollToMessageId = scrollTargets[channel],
                                onScrollToMessageHandle = { onClearScrollTarget(channel) },
                                recoveryFabTooltipState = if (currentTourStep == TourStep.RecoveryFab) recoveryFabTooltipState else null,
                                onTourAdvance = callbacks.onTourAdvance,
                                onTourSkip = callbacks.onTourSkip,
                            )
                        }
                    }
                }
            }
        }
    }
}
