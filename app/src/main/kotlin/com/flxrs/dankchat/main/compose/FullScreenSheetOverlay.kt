package com.flxrs.dankchat.main.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.flxrs.dankchat.chat.compose.BadgeUi
import com.flxrs.dankchat.chat.history.compose.MessageHistoryComposeViewModel
import com.flxrs.dankchat.chat.mention.compose.MentionComposeViewModel
import com.flxrs.dankchat.chat.message.compose.MessageOptionsParams
import com.flxrs.dankchat.chat.user.UserPopupStateParams
import com.flxrs.dankchat.data.DisplayName
import com.flxrs.dankchat.data.UserId
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.twitch.emote.ChatMessageEmote
import com.flxrs.dankchat.main.compose.sheets.MentionSheet
import com.flxrs.dankchat.main.compose.sheets.MessageHistorySheet
import com.flxrs.dankchat.main.compose.sheets.RepliesSheet
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun FullScreenSheetOverlay(
    sheetState: FullScreenSheetState,
    isLoggedIn: Boolean,
    mentionViewModel: MentionComposeViewModel,
    onDismiss: () -> Unit,
    onDismissReplies: () -> Unit,
    onUserClick: (UserPopupStateParams) -> Unit,
    onMessageLongClick: (MessageOptionsParams) -> Unit,
    onEmoteClick: (List<ChatMessageEmote>) -> Unit,
    modifier: Modifier = Modifier,
    onWhisperReply: (UserName) -> Unit = {},
    onJumpToMessage: ((messageId: String, channel: UserName) -> Unit)? = null,
    bottomContentPadding: Dp = 0.dp,
) {
    // Pre-resolve history VM outside AnimatedVisibility to avoid Koin creating
    // duplicate instances during the enter animation (causes mid-animation stutter)
    val historyState = sheetState as? FullScreenSheetState.History
    val currentHistoryViewModel: MessageHistoryComposeViewModel? = historyState?.let {
        koinViewModel(
            key = it.channel.value,
            parameters = { parametersOf(it.channel) },
        )
    }

    // Remember the last active (non-Closed) state and history VM so content persists
    // during exit animation. Without this, when sheetState changes to Closed the `when`
    // block would render nothing, causing a flash while the exit animation is still playing.
    var lastActiveState by remember { mutableStateOf(sheetState) }
    var lastHistoryViewModel by remember { mutableStateOf(currentHistoryViewModel) }
    if (sheetState !is FullScreenSheetState.Closed) {
        lastActiveState = sheetState
    }
    if (currentHistoryViewModel != null) {
        lastHistoryViewModel = currentHistoryViewModel
    }

    val isVisible = sheetState !is FullScreenSheetState.Closed

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
        exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            val userClickHandler: (String?, String, String, String?, List<BadgeUi>, Boolean) -> Unit = { userId, userName, displayName, channel, badges, _ ->
                onUserClick(
                    UserPopupStateParams(
                        targetUserId = userId?.let { UserId(it) } ?: UserId(""),
                        targetUserName = UserName(userName),
                        targetDisplayName = DisplayName(displayName),
                        channel = channel?.let { UserName(it) },
                        badges = badges.map { it.badge }
                    )
                )
            }

            // Use lastActiveState so content stays visible during the exit animation
            val renderState = when {
                isVisible -> sheetState
                else      -> lastActiveState
            }

            when (renderState) {
                is FullScreenSheetState.Closed -> Unit
                is FullScreenSheetState.Mention -> {
                    MentionSheet(
                        mentionViewModel = mentionViewModel,
                        initialisWhisperTab = false,

                        onDismiss = onDismiss,
                        onUserClick = userClickHandler,
                        onMessageLongClick = { messageId, channel, fullMessage ->
                            onMessageLongClick(
                                MessageOptionsParams(
                                    messageId = messageId,
                                    channel = channel?.let { UserName(it) },
                                    fullMessage = fullMessage,
                                    canModerate = false,
                                    canReply = false,
                                    canCopy = true,
                                    canJump = true,
                                )
                            )
                        },
                        onEmoteClick = onEmoteClick,
                        onWhisperReply = onWhisperReply,
                        onJumpToMessage = onJumpToMessage,
                        bottomContentPadding = bottomContentPadding,
                    )
                }

                is FullScreenSheetState.Whisper -> {
                    MentionSheet(
                        mentionViewModel = mentionViewModel,
                        initialisWhisperTab = true,

                        onDismiss = onDismiss,
                        onUserClick = userClickHandler,
                        onMessageLongClick = { messageId, channel, fullMessage ->
                            onMessageLongClick(
                                MessageOptionsParams(
                                    messageId = messageId,
                                    channel = channel?.let { UserName(it) },
                                    fullMessage = fullMessage,
                                    canModerate = false,
                                    canReply = false,
                                    canCopy = true,
                                    canJump = false,
                                )
                            )
                        },
                        onEmoteClick = onEmoteClick,
                        onWhisperReply = onWhisperReply,
                        onJumpToMessage = onJumpToMessage,
                        bottomContentPadding = bottomContentPadding,
                    )
                }

                is FullScreenSheetState.Replies -> {
                    RepliesSheet(
                        rootMessageId = renderState.replyMessageId,

                        onDismiss = onDismissReplies,
                        onUserClick = userClickHandler,
                        onMessageLongClick = { messageId, channel, fullMessage ->
                            onMessageLongClick(
                                MessageOptionsParams(
                                    messageId = messageId,
                                    channel = channel?.let { UserName(it) },
                                    fullMessage = fullMessage,
                                    canModerate = isLoggedIn,
                                    canReply = isLoggedIn,
                                    canCopy = true
                                )
                            )
                        },
                        bottomContentPadding = bottomContentPadding,
                    )
                }

                is FullScreenSheetState.History -> {
                    MessageHistorySheet(
                        viewModel = (currentHistoryViewModel ?: lastHistoryViewModel)!!,
                        channel = renderState.channel,
                        initialFilter = renderState.initialFilter,

                        onDismiss = onDismiss,
                        onUserClick = userClickHandler,
                        onMessageLongClick = { messageId, channel, fullMessage ->
                            onMessageLongClick(
                                MessageOptionsParams(
                                    messageId = messageId,
                                    channel = channel?.let { UserName(it) },
                                    fullMessage = fullMessage,
                                    canModerate = false,
                                    canReply = false,
                                    canCopy = true,
                                    canJump = true,
                                )
                            )
                        },
                        onEmoteClick = onEmoteClick,
                        onJumpToMessage = onJumpToMessage,
                    )
                }
            }
        }
    }
}
