package com.flxrs.dankchat.ui.chat.mention

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.LocalPlatformContext
import coil3.imageLoader
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.twitch.emote.ChatMessageEmote
import com.flxrs.dankchat.ui.chat.BadgeUi
import com.flxrs.dankchat.ui.chat.ChatScreen
import com.flxrs.dankchat.ui.chat.ChatScreenCallbacks
import com.flxrs.dankchat.ui.chat.LocalEmoteAnimationCoordinator
import com.flxrs.dankchat.ui.chat.rememberEmoteAnimationCoordinator

/**
 * Standalone composable for mentions/whispers display.
 * Extracted from MentionChatFragment to enable pure Compose integration.
 * 
 * This composable:
 * - Collects mentions or whispers from MentionViewModel based on isWhisperTab
 * - Collects appearance settings
 * - Renders ChatScreen with channel prefix for mentions only
 */
@Composable
fun MentionComposable(
    mentionViewModel: MentionViewModel,
    isWhisperTab: Boolean,
    onUserClick: (userId: String?, userName: String, displayName: String, channel: String?, badges: List<BadgeUi>, isLongPress: Boolean) -> Unit,
    onMessageLongClick: (messageId: String, channel: String?, fullMessage: String) -> Unit,
    onEmoteClick: (List<ChatMessageEmote>) -> Unit,
    modifier: Modifier = Modifier,
    onWhisperReply: ((userName: UserName) -> Unit)? = null,
    containerColor: Color,
    contentPadding: PaddingValues = PaddingValues(),
    scrollModifier: Modifier = Modifier,
    onScrollToBottom: () -> Unit = {},
) {
    val displaySettings by mentionViewModel.chatDisplaySettings.collectAsStateWithLifecycle()
    val messages by when {
        isWhisperTab -> mentionViewModel.whispersUiStates.collectAsStateWithLifecycle(initialValue = emptyList())
        else         -> mentionViewModel.mentionsUiStates.collectAsStateWithLifecycle(initialValue = emptyList())
    }

    val context = LocalPlatformContext.current
    val emoteCoordinator = rememberEmoteAnimationCoordinator(context.imageLoader)

    CompositionLocalProvider(LocalEmoteAnimationCoordinator provides emoteCoordinator) {
        ChatScreen(
            messages = messages,
            fontSize = displaySettings.fontSize,
            callbacks = ChatScreenCallbacks(
                onUserClick = onUserClick,
                onMessageLongClick = onMessageLongClick,
                onEmoteClick = onEmoteClick,
                onWhisperReply = if (isWhisperTab) onWhisperReply else null,
            ),
            showLineSeparator = displaySettings.showLineSeparator,
            animateGifs = displaySettings.animateGifs,
            showChannelPrefix = !isWhisperTab,
            modifier = modifier,
            contentPadding = contentPadding,
            scrollModifier = scrollModifier,
            containerColor = containerColor,
            onScrollToBottom = onScrollToBottom,
        )
    } // CompositionLocalProvider
}
