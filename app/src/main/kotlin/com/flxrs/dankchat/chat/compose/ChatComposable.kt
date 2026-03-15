package com.flxrs.dankchat.chat.compose

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.LocalPlatformContext
import coil3.imageLoader
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.twitch.emote.ChatMessageEmote
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Standalone composable for chat display.
 * Extracted from ChatFragment to enable pure Compose integration.
 * 
 * This composable:
 * - Creates its own ChatComposeViewModel scoped to the channel
 * - Collects messages from ViewModel
 * - Collects settings from data stores
 * - Renders ChatScreen with all event handlers
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatComposable(
    channel: UserName,
    onUserClick: (userId: String?, userName: String, displayName: String, channel: String?, badges: List<BadgeUi>, isLongPress: Boolean) -> Unit,
    onMessageLongClick: (messageId: String, channel: String?, fullMessage: String) -> Unit,
    onEmoteClick: (List<ChatMessageEmote>) -> Unit,
    onReplyClick: (String, UserName) -> Unit,
    modifier: Modifier = Modifier,
    showInput: Boolean = true,
    isFullscreen: Boolean = false,
    hasHelperText: Boolean = false,
    showFabs: Boolean = true,
    onRecover: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(),
    scrollModifier: Modifier = Modifier,
    onScrollToBottom: () -> Unit = {},
    onScrollDirectionChanged: (Boolean) -> Unit = {},
    scrollToMessageId: String? = null,
    onScrollToMessageHandled: () -> Unit = {},
    recoveryFabTooltipState: TooltipState? = null,
    onTourAdvance: (() -> Unit)? = null,
    onTourSkip: (() -> Unit)? = null,
) {
    // Create ChatComposeViewModel with channel-specific key for proper scoping
    val viewModel: ChatComposeViewModel = koinViewModel(
        key = channel.value,
        parameters = { parametersOf(channel) }
    )

    val messages by viewModel.chatUiStates.collectAsStateWithLifecycle(initialValue = emptyList())
    val displaySettings by viewModel.chatDisplaySettings.collectAsStateWithLifecycle()

    // Create singleton coordinator using the app's ImageLoader (with disk cache, AnimatedImageDecoder, etc.)
    val context = LocalPlatformContext.current
    val emoteCoordinator = rememberEmoteAnimationCoordinator(context.imageLoader)

    CompositionLocalProvider(LocalEmoteAnimationCoordinator provides emoteCoordinator) {
    ChatScreen(
        messages = messages,
        fontSize = displaySettings.fontSize,
        showLineSeparator = displaySettings.showLineSeparator,
        animateGifs = displaySettings.animateGifs,
        modifier = modifier.fillMaxSize(),
        onUserClick = onUserClick,
        onMessageLongClick = onMessageLongClick,
        onEmoteClick = onEmoteClick,
        onReplyClick = onReplyClick,
        showInput = showInput,
        isFullscreen = isFullscreen,
        hasHelperText = hasHelperText,
        showFabs = showFabs,
        onRecover = onRecover,
        contentPadding = contentPadding,
        scrollModifier = scrollModifier,
        onScrollToBottom = onScrollToBottom,
        onScrollDirectionChanged = onScrollDirectionChanged,
        scrollToMessageId = scrollToMessageId,
        onScrollToMessageHandled = onScrollToMessageHandled,
        onAutomodAllow = { heldMessageId, ch -> viewModel.manageAutomodMessage(heldMessageId, ch, allow = true) },
        onAutomodDeny = { heldMessageId, ch -> viewModel.manageAutomodMessage(heldMessageId, ch, allow = false) },
        recoveryFabTooltipState = recoveryFabTooltipState,
        onTourAdvance = onTourAdvance,
        onTourSkip = onTourSkip,
    )
    } // CompositionLocalProvider
}
