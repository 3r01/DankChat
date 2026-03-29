package com.flxrs.dankchat.ui.chat.replies

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.LocalPlatformContext
import coil3.imageLoader
import com.flxrs.dankchat.ui.chat.BadgeUi
import com.flxrs.dankchat.ui.chat.ChatScreen
import com.flxrs.dankchat.ui.chat.ChatScreenCallbacks
import com.flxrs.dankchat.ui.chat.LocalEmoteAnimationCoordinator
import com.flxrs.dankchat.ui.chat.rememberEmoteAnimationCoordinator

/**
 * Standalone composable for reply thread display.
 * Extracted from RepliesChatFragment to enable pure Compose integration.
 *
 * This composable:
 * - Collects reply thread state from RepliesViewModel
 * - Collects appearance settings
 * - Handles NotFound state via onMissing callback
 * - Renders ChatScreen for Found state
 */
@Composable
fun RepliesComposable(
    repliesViewModel: RepliesViewModel,
    onUserClick: (userId: String?, userName: String, displayName: String, channel: String?, badges: List<BadgeUi>, isLongPress: Boolean) -> Unit,
    onMessageLongClick: (messageId: String, channel: String?, fullMessage: String) -> Unit,
    onMissing: () -> Unit,
    containerColor: Color,
    modifier: Modifier = Modifier,
    scrollModifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    onScrollToBottom: () -> Unit = {},
) {
    val displaySettings by repliesViewModel.chatDisplaySettings.collectAsStateWithLifecycle()
    val uiState by repliesViewModel.uiState.collectAsStateWithLifecycle(initialValue = RepliesUiState.Found(emptyList()))

    val context = LocalPlatformContext.current
    val emoteCoordinator = rememberEmoteAnimationCoordinator(context.imageLoader)

    CompositionLocalProvider(LocalEmoteAnimationCoordinator provides emoteCoordinator) {
        when (uiState) {
            is RepliesUiState.Found -> {
                ChatScreen(
                    messages = (uiState as RepliesUiState.Found).items,
                    fontSize = displaySettings.fontSize,
                    callbacks = ChatScreenCallbacks(
                        onUserClick = onUserClick,
                        onMessageLongClick = onMessageLongClick,
                    ),
                    showLineSeparator = displaySettings.showLineSeparator,
                    animateGifs = displaySettings.animateGifs,
                    modifier = modifier,
                    contentPadding = contentPadding,
                    scrollModifier = scrollModifier,
                    containerColor = containerColor,
                    onScrollToBottom = onScrollToBottom,
                )
            }

            is RepliesUiState.NotFound -> {
                LaunchedEffect(Unit) {
                    onMissing()
                }
            }
        }
    } // CompositionLocalProvider
}
