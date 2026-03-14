package com.flxrs.dankchat.chat.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.flxrs.dankchat.chat.compose.messages.ModerationMessageComposable
import com.flxrs.dankchat.chat.compose.messages.NoticeMessageComposable
import com.flxrs.dankchat.chat.compose.messages.PointRedemptionMessageComposable
import com.flxrs.dankchat.chat.compose.messages.PrivMessageComposable
import com.flxrs.dankchat.chat.compose.messages.SystemMessageComposable
import com.flxrs.dankchat.chat.compose.messages.UserNoticeMessageComposable
import com.flxrs.dankchat.chat.compose.messages.WhisperMessageComposable

/**
 * Main composable for rendering chat messages in a scrollable list.
 * 
 * Features:
 * - LazyColumn with reverseLayout for bottom-anchored scrolling
 * - Automatic scroll to bottom when new messages arrive
 * - FAB to manually scroll to bottom
 * - Efficient recomposition with stable keys
 */
@Composable
fun ChatScreen(
    messages: List<ChatMessageUiState>,
    fontSize: Float = 14f,
    modifier: Modifier = Modifier,
    onUserClick: (userId: String?, userName: String, displayName: String, channel: String?, badges: List<Any>, isLongPress: Boolean) -> Unit = { _, _, _, _, _, _ -> },
    onMessageLongClick: (messageId: String, channel: String?, fullMessage: String) -> Unit = { _, _, _ -> },
    onEmoteClick: (emotes: List<Any>) -> Unit = {},
    onReplyClick: (rootMessageId: String) -> Unit = {},
) {
    val listState = rememberLazyListState()
    
    // Track if user is at bottom
    val isAtBottom by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisibleItem?.index == listState.layoutInfo.totalItemsCount - 1
        }
    }

    // Auto-scroll to bottom when new messages arrive and user is already at bottom
    LaunchedEffect(messages.size) {
        if (isAtBottom && messages.isNotEmpty()) {
            listState.scrollToItem(messages.size - 1)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            reverseLayout = true,
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                items = messages.asReversed(),
                key = { message -> message.id }
            ) { message ->
                ChatMessageItem(
                    message = message,
                    fontSize = fontSize,
                    onUserClick = onUserClick,
                    onMessageLongClick = onMessageLongClick,
                    onEmoteClick = onEmoteClick,
                    onReplyClick = onReplyClick,
                )
            }
        }

        // Scroll to bottom FAB
        if (!isAtBottom && messages.isNotEmpty()) {
            FloatingActionButton(
                onClick = {
                    // TODO: Smooth scroll
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Scroll to bottom"
                )
            }
        }
    }
}

/**
 * Renders a single chat message based on its type
 */
@Composable
private fun ChatMessageItem(
    message: ChatMessageUiState,
    fontSize: Float,
    onUserClick: (userId: String?, userName: String, displayName: String, channel: String?, badges: List<Any>, isLongPress: Boolean) -> Unit,
    onMessageLongClick: (messageId: String, channel: String?, fullMessage: String) -> Unit,
    onEmoteClick: (emotes: List<Any>) -> Unit,
    onReplyClick: (rootMessageId: String) -> Unit,
) {
    when (message) {
        is ChatMessageUiState.SystemMessageUi -> SystemMessageComposable(
            message = message,
            fontSize = fontSize
        )
        is ChatMessageUiState.NoticeMessageUi -> NoticeMessageComposable(
            message = message,
            fontSize = fontSize
        )
        is ChatMessageUiState.UserNoticeMessageUi -> UserNoticeMessageComposable(
            message = message,
            fontSize = fontSize
        )
        is ChatMessageUiState.ModerationMessageUi -> ModerationMessageComposable(
            message = message,
            fontSize = fontSize
        )
        is ChatMessageUiState.PrivMessageUi -> PrivMessageComposable(
            message = message,
            fontSize = fontSize,
            onUserClick = { userId, userName, displayName, channel, isLongPress ->
                onUserClick(userId, userName, displayName, channel, emptyList(), isLongPress)
            },
            onMessageLongClick = onMessageLongClick,
            onEmoteClick = onEmoteClick,
            onReplyClick = onReplyClick
        )
        is ChatMessageUiState.PointRedemptionMessageUi -> PointRedemptionMessageComposable(
            message = message,
            fontSize = fontSize
        )
        is ChatMessageUiState.WhisperMessageUi -> WhisperMessageComposable(
            message = message,
            fontSize = fontSize,
            onUserClick = { userId, userName, displayName, isLongPress ->
                onUserClick(userId, userName, displayName, null, emptyList(), isLongPress)
            },
            onMessageLongClick = { messageId, fullMessage ->
                onMessageLongClick(messageId, null, fullMessage)
            }
        )
    }
}
