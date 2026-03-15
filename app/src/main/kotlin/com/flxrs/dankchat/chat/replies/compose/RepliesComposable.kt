package com.flxrs.dankchat.chat.replies.compose

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.LocalPlatformContext
import coil3.imageLoader
import com.flxrs.dankchat.chat.compose.BadgeUi
import com.flxrs.dankchat.chat.compose.ChatScreen
import com.flxrs.dankchat.chat.compose.LocalEmoteAnimationCoordinator
import com.flxrs.dankchat.chat.compose.rememberEmoteAnimationCoordinator
import com.flxrs.dankchat.chat.replies.RepliesUiState
import androidx.compose.ui.graphics.Color
import com.flxrs.dankchat.chat.compose.ChatDisplaySettings

/**
 * Standalone composable for reply thread display.
 * Extracted from RepliesChatFragment to enable pure Compose integration.
 * 
 * This composable:
 * - Collects reply thread state from RepliesComposeViewModel
 * - Collects appearance settings
 * - Handles NotFound state via onNotFound callback
 * - Renders ChatScreen for Found state
 */
@Composable
fun RepliesComposable(
    repliesViewModel: RepliesComposeViewModel,
    onUserClick: (userId: String?, userName: String, displayName: String, channel: String?, badges: List<BadgeUi>, isLongPress: Boolean) -> Unit,
    onMessageLongClick: (messageId: String, channel: String?, fullMessage: String) -> Unit,
    onNotFound: () -> Unit,
    containerColor: Color,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
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
                showLineSeparator = displaySettings.showLineSeparator,
                animateGifs = displaySettings.animateGifs,
                modifier = modifier,
                onUserClick = onUserClick,
                onMessageLongClick = onMessageLongClick,
                onEmoteClick = { /* no-op for replies */ },
                contentPadding = contentPadding,
                containerColor = containerColor,
            )
        }
        is RepliesUiState.NotFound -> {
            LaunchedEffect(Unit) {
                onNotFound()
            }
        }
    }
    } // CompositionLocalProvider
}