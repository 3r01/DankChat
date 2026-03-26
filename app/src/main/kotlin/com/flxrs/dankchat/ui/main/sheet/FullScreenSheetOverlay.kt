package com.flxrs.dankchat.ui.main.sheet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.flxrs.dankchat.data.DisplayName
import com.flxrs.dankchat.data.UserId
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.preferences.chat.UserLongClickBehavior
import com.flxrs.dankchat.data.twitch.emote.ChatMessageEmote
import com.flxrs.dankchat.ui.chat.BadgeUi
import com.flxrs.dankchat.ui.chat.history.MessageHistoryViewModel
import com.flxrs.dankchat.ui.chat.mention.MentionViewModel
import com.flxrs.dankchat.ui.chat.message.MessageOptionsParams
import com.flxrs.dankchat.ui.chat.user.UserPopupStateParams
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun FullScreenSheetOverlay(
    sheetState: FullScreenSheetState,
    isLoggedIn: Boolean,
    mentionViewModel: MentionViewModel,
    onDismiss: () -> Unit,
    onDismissReplies: () -> Unit,
    onUserClick: (UserPopupStateParams) -> Unit,
    onMessageLongClick: (MessageOptionsParams) -> Unit,
    onEmoteClick: (List<ChatMessageEmote>) -> Unit,
    userLongClickBehavior: UserLongClickBehavior = UserLongClickBehavior.MentionsUser,
    modifier: Modifier = Modifier,
    onWhisperReply: (UserName) -> Unit = {},
    onUserMention: (UserName, DisplayName) -> Unit = { _, _ -> },
    bottomContentPadding: Dp = 0.dp,
) {
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
            val popupOnlyClickHandler: (String?, String, String, String?, List<BadgeUi>, Boolean) -> Unit = { userId, userName, displayName, channel, badges, _ ->
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

            val mentionableClickHandler: (String?, String, String, String?, List<BadgeUi>, Boolean) -> Unit = { userId, userName, displayName, channel, badges, isLongPress ->
                val shouldOpenPopup = when (userLongClickBehavior) {
                    UserLongClickBehavior.MentionsUser -> !isLongPress
                    UserLongClickBehavior.OpensPopup   -> isLongPress
                }
                if (shouldOpenPopup) {
                    onUserClick(
                        UserPopupStateParams(
                            targetUserId = userId?.let { UserId(it) } ?: UserId(""),
                            targetUserName = UserName(userName),
                            targetDisplayName = DisplayName(displayName),
                            channel = channel?.let { UserName(it) },
                            badges = badges.map { it.badge }
                        )
                    )
                } else {
                    onUserMention(UserName(userName), DisplayName(displayName))
                }
            }

            when (sheetState) {
                is FullScreenSheetState.Closed -> Unit
                is FullScreenSheetState.Mention -> {
                    MentionSheet(
                        mentionViewModel = mentionViewModel,
                        initialisWhisperTab = false,

                        onDismiss = onDismiss,
                        onUserClick = popupOnlyClickHandler,
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
                        bottomContentPadding = bottomContentPadding,
                    )
                }

                is FullScreenSheetState.Whisper -> {
                    MentionSheet(
                        mentionViewModel = mentionViewModel,
                        initialisWhisperTab = true,

                        onDismiss = onDismiss,
                        onUserClick = popupOnlyClickHandler,
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
                        bottomContentPadding = bottomContentPadding,
                    )
                }

                is FullScreenSheetState.Replies -> {
                    RepliesSheet(
                        rootMessageId = sheetState.replyMessageId,

                        onDismiss = onDismissReplies,
                        onUserClick = mentionableClickHandler,
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
                        bottomContentPadding = bottomContentPadding,
                    )
                }

                is FullScreenSheetState.History -> {
                    val viewModel: MessageHistoryViewModel = koinViewModel(
                        key = sheetState.channel.value,
                        parameters = { parametersOf(sheetState.channel) },
                    )
                    val historyClickHandler: (String?, String, String, String?, List<BadgeUi>, Boolean) -> Unit = { userId, userName, displayName, channel, badges, isLongPress ->
                        val shouldOpenPopup = when (userLongClickBehavior) {
                            UserLongClickBehavior.MentionsUser -> !isLongPress
                            UserLongClickBehavior.OpensPopup   -> isLongPress
                        }
                        if (shouldOpenPopup) {
                            onUserClick(
                                UserPopupStateParams(
                                    targetUserId = userId?.let { UserId(it) } ?: UserId(""),
                                    targetUserName = UserName(userName),
                                    targetDisplayName = DisplayName(displayName),
                                    channel = channel?.let { UserName(it) },
                                    badges = badges.map { it.badge }
                                )
                            )
                        } else {
                            viewModel.insertText("${UserName(userName).valueOrDisplayName(DisplayName(displayName))} ")
                        }
                    }
                    MessageHistorySheet(
                        viewModel = viewModel,
                        channel = sheetState.channel,
                        initialFilter = sheetState.initialFilter,

                        onDismiss = onDismiss,
                        onUserClick = historyClickHandler,
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
                    )
                }
            }
        }
    }
}
