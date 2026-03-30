package com.flxrs.dankchat.ui.chat

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.twitch.emote.ChatMessageEmote
import kotlinx.collections.immutable.persistentListOf
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatComposable(
    channel: UserName,
    onUserClick: (userId: String?, userName: String, displayName: String, channel: String?, badges: List<BadgeUi>, isLongPress: Boolean) -> Unit,
    onMessageLongClick: (messageId: String, channel: String?, fullMessage: String) -> Unit,
    onEmoteClick: (List<ChatMessageEmote>) -> Unit,
    onReplyClick: (String, UserName) -> Unit,
    modifier: Modifier = Modifier,
    scrollModifier: Modifier = Modifier,
    showInput: Boolean = true,
    isFullscreen: Boolean = false,
    showFabs: Boolean = true,
    onRecover: () -> Unit = {},
    fabMenuCallbacks: FabMenuCallbacks? = null,
    contentPadding: PaddingValues = PaddingValues(),
    onScrollToBottom: () -> Unit = {},
    onScrollDirectionChange: (Boolean) -> Unit = {},
    scrollToMessageId: String? = null,
    onScrollToMessageHandle: () -> Unit = {},
    recoveryFabTooltipState: TooltipState? = null,
    onTourAdvance: (() -> Unit)? = null,
    onTourSkip: (() -> Unit)? = null,
) {
    // Create ChatViewModel with channel-specific key for proper scoping
    val viewModel: ChatViewModel =
        koinViewModel(
            key = channel.value,
            parameters = { parametersOf(channel) },
        )

    val messages by viewModel.chatUiStates.collectAsStateWithLifecycle(initialValue = persistentListOf())
    val displaySettings by viewModel.chatDisplaySettings.collectAsStateWithLifecycle()

    ChatScreen(
        messages = messages,
        fontSize = displaySettings.fontSize,
        callbacks =
            ChatScreenCallbacks(
                onUserClick = onUserClick,
                onMessageLongClick = onMessageLongClick,
                onEmoteClick = onEmoteClick,
                onReplyClick = onReplyClick,
                onAutomodAllow = { heldMessageId, ch -> viewModel.manageAutomodMessage(heldMessageId, ch, allow = true) },
                onAutomodDeny = { heldMessageId, ch -> viewModel.manageAutomodMessage(heldMessageId, ch, allow = false) },
            ),
        showLineSeparator = displaySettings.showLineSeparator,
        animateGifs = displaySettings.animateGifs,
        modifier = modifier.fillMaxSize(),
        showInput = showInput,
        isFullscreen = isFullscreen,
        showFabs = showFabs,
        onRecover = onRecover,
        fabMenuCallbacks = fabMenuCallbacks,
        contentPadding = contentPadding,
        scrollModifier = scrollModifier,
        onScrollToBottom = onScrollToBottom,
        onScrollDirectionChange = onScrollDirectionChange,
        scrollToMessageId = scrollToMessageId,
        onScrollToMessageHandle = onScrollToMessageHandle,
        recoveryFabTooltipState = recoveryFabTooltipState,
        onTourAdvance = onTourAdvance,
        onTourSkip = onTourSkip,
    )
}
