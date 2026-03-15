package com.flxrs.dankchat.main.compose.sheets

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.flxrs.dankchat.R
import com.flxrs.dankchat.chat.compose.BadgeUi
import com.flxrs.dankchat.chat.mention.compose.MentionComposable
import com.flxrs.dankchat.chat.mention.compose.MentionComposeViewModel
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.twitch.emote.ChatMessageEmote
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun MentionSheet(
    mentionViewModel: MentionComposeViewModel,
    initialisWhisperTab: Boolean,
    onDismiss: () -> Unit,
    onUserClick: (userId: String?, userName: String, displayName: String, channel: String?, badges: List<BadgeUi>, isLongPress: Boolean) -> Unit,
    onMessageLongClick: (messageId: String, channel: String?, fullMessage: String) -> Unit,
    onEmoteClick: (List<ChatMessageEmote>) -> Unit,
    onWhisperReply: ((userName: UserName) -> Unit)? = null,
    onJumpToMessage: ((messageId: String, channel: UserName) -> Unit)? = null,
    bottomContentPadding: Dp = 0.dp,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val pagerState = rememberPagerState(
        initialPage = if (initialisWhisperTab) 1 else 0,
        pageCount = { 2 }
    )
    var backProgress by remember { mutableFloatStateOf(0f) }

    val statusBarHeight = with(density) { WindowInsets.statusBars.getTop(density).toDp() }
    // Toolbar area: status bar + padding + pill height + padding
    val toolbarTopPadding = statusBarHeight + 8.dp + 48.dp + 16.dp
    val sheetBackgroundColor = lerp(
        MaterialTheme.colorScheme.surfaceContainer,
        MaterialTheme.colorScheme.surfaceContainerHigh,
        fraction = 0.75f,
    )

    LaunchedEffect(pagerState.currentPage) {
        mentionViewModel.setCurrentTab(pagerState.currentPage)
    }

    PredictiveBackHandler { progress ->
        try {
            progress.collect { event ->
                backProgress = event.progress
            }
            onDismiss()
        } catch (e: CancellationException) {
            backProgress = 0f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(sheetBackgroundColor)
            .graphicsLayer {
                val scale = 1f - (backProgress * 0.1f)
                scaleX = scale
                scaleY = scale
                alpha = 1f - backProgress
                translationY = backProgress * 100f
            }
    ) {
        // Chat content - edge to edge
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            MentionComposable(
                mentionViewModel = mentionViewModel,
                isWhisperTab = page == 1,
                onUserClick = onUserClick,
                onMessageLongClick = onMessageLongClick,
                onEmoteClick = onEmoteClick,
                onWhisperReply = if (page == 1) onWhisperReply else null,
                onJumpToMessage = if (page == 0) onJumpToMessage else null,
                containerColor = sheetBackgroundColor,
                contentPadding = PaddingValues(top = toolbarTopPadding, bottom = bottomContentPadding),
            )
        }

        // Floating toolbar with gradient scrim
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        0f to sheetBackgroundColor.copy(alpha = 0.7f),
                        0.75f to sheetBackgroundColor.copy(alpha = 0.7f),
                        1f to sheetBackgroundColor.copy(alpha = 0f)
                    )
                )
                .padding(top = statusBarHeight + 8.dp)
                .padding(bottom = 16.dp)
                .padding(horizontal = 8.dp),
        ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Back navigation pill
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back)
                    )
                }
            }

            // Tab pill
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Row {
                    val tabs = listOf(R.string.mentions, R.string.whispers)
                    tabs.forEachIndexed { index, stringRes ->
                        val isSelected = pagerState.currentPage == index
                        val textColor = when {
                            isSelected -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { scope.launch { pagerState.animateScrollToPage(index) } }
                                .defaultMinSize(minHeight = 48.dp)
                                .padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = stringResource(stringRes),
                                color = textColor,
                                style = MaterialTheme.typography.titleSmall,
                            )
                        }
                    }
                }
            }
        }
        }
    }
}
