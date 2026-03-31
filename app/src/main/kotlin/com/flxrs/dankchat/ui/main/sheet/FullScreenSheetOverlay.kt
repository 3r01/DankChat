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
import com.flxrs.dankchat.data.twitch.emote.ChatMessageEmote
import com.flxrs.dankchat.preferences.chat.UserLongClickBehavior
import com.flxrs.dankchat.ui.chat.BadgeUi
import com.flxrs.dankchat.ui.chat.history.MessageHistoryViewModel
import com.flxrs.dankchat.ui.chat.mention.MentionViewModel
import com.flxrs.dankchat.ui.chat.message.MessageOptionsParams
import com.flxrs.dankchat.ui.chat.user.UserPopupStateParams
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Suppress("ViewModelForwarding")
@Composable
fun FullScreenSheetOverlay(
    sheetState: FullScreenSheetState,
    mentionViewModel: MentionViewModel,
    onDismiss: () -> Unit,
    onDismissReplies: () -> Unit,
    onUserClick: (UserPopupStateParams) -> Unit,
    onMessageLongClick: (MessageOptionsParams) -> Unit,
    onEmoteClick: (List<ChatMessageEmote>) -> Unit,
    modifier: Modifier = Modifier,
    userLongClickBehavior: UserLongClickBehavior = UserLongClickBehavior.MentionsUser,
    onWhisperReply: (UserName) -> Unit = {},
    onUserMention: (UserName, DisplayName) -> Unit = { _, _ -> },
    bottomContentPadding: Dp = 0.dp,
) {
    val isVisible = sheetState !is FullScreenSheetState.Closed

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
        exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
        modifier = modifier.fillMaxSize(),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (sheetState) {
                is FullScreenSheetState.Closed -> {
                    Unit
                }

                is FullScreenSheetState.Mention -> {
                    MentionSheet(
                        mentionViewModel = mentionViewModel,
                        initialisWhisperTab = false,
                        onDismiss = onDismiss,
                        onUserClick = popupOnlyClickHandler(onUserClick),
                        onMessageLongClick = messageOptionsHandler(onMessageLongClick, canJump = true),
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
                        onUserClick = popupOnlyClickHandler(onUserClick),
                        onMessageLongClick = messageOptionsHandler(onMessageLongClick, canJump = false),
                        onEmoteClick = onEmoteClick,
                        onWhisperReply = onWhisperReply,
                        bottomContentPadding = bottomContentPadding,
                    )
                }

                is FullScreenSheetState.Replies -> {
                    RepliesSheet(
                        rootMessageId = sheetState.replyMessageId,
                        onDismiss = onDismissReplies,
                        onUserClick = mentionableClickHandler(onUserClick, onUserMention, userLongClickBehavior),
                        onMessageLongClick = messageOptionsHandler(onMessageLongClick, canJump = true),
                        bottomContentPadding = bottomContentPadding,
                    )
                }

                is FullScreenSheetState.History -> {
                    HistorySheetContent(
                        channel = sheetState.channel,
                        initialFilter = sheetState.initialFilter,
                        onDismiss = onDismiss,
                        onUserClick = onUserClick,
                        onMessageLongClick = onMessageLongClick,
                        onEmoteClick = onEmoteClick,
                        userLongClickBehavior = userLongClickBehavior,
                        bottomContentPadding = bottomContentPadding,
                    )
                }
            }
        }
    }
}

@Composable
private fun HistorySheetContent(
    channel: UserName,
    initialFilter: String,
    onDismiss: () -> Unit,
    onUserClick: (UserPopupStateParams) -> Unit,
    onMessageLongClick: (MessageOptionsParams) -> Unit,
    onEmoteClick: (List<ChatMessageEmote>) -> Unit,
    userLongClickBehavior: UserLongClickBehavior,
    bottomContentPadding: Dp,
) {
    val viewModel: MessageHistoryViewModel =
        koinViewModel(
            key = "history-${channel.value}",
            parameters = { parametersOf(channel) },
        )
    val clickHandler: (String?, String, String, String?, List<BadgeUi>, Boolean) -> Unit = { userId, userName, displayName, clickChannel, badges, isLongPress ->
        val shouldOpenPopup =
            when (userLongClickBehavior) {
                UserLongClickBehavior.MentionsUser -> !isLongPress
                UserLongClickBehavior.OpensPopup -> isLongPress
            }
        if (shouldOpenPopup) {
            onUserClick(buildUserPopupParams(userId, userName, displayName, clickChannel, badges))
        } else {
            viewModel.insertText("${UserName(userName).valueOrDisplayName(DisplayName(displayName))} ")
        }
    }
    MessageHistorySheet(
        viewModel = viewModel,
        channel = channel,
        initialFilter = initialFilter,
        onDismiss = onDismiss,
        onUserClick = clickHandler,
        onMessageLongClick = messageOptionsHandler(onMessageLongClick, canJump = true),
        onEmoteClick = onEmoteClick,
    )
}

private fun popupOnlyClickHandler(onUserClick: (UserPopupStateParams) -> Unit): (String?, String, String, String?, List<BadgeUi>, Boolean) -> Unit =
    { userId, userName, displayName, channel, badges, _ ->
        onUserClick(buildUserPopupParams(userId, userName, displayName, channel, badges))
    }

private fun mentionableClickHandler(
    onUserClick: (UserPopupStateParams) -> Unit,
    onUserMention: (UserName, DisplayName) -> Unit,
    userLongClickBehavior: UserLongClickBehavior,
): (String?, String, String, String?, List<BadgeUi>, Boolean) -> Unit = { userId, userName, displayName, channel, badges, isLongPress ->
    val shouldOpenPopup =
        when (userLongClickBehavior) {
            UserLongClickBehavior.MentionsUser -> !isLongPress
            UserLongClickBehavior.OpensPopup -> isLongPress
        }
    if (shouldOpenPopup) {
        onUserClick(buildUserPopupParams(userId, userName, displayName, channel, badges))
    } else {
        onUserMention(UserName(userName), DisplayName(displayName))
    }
}

private fun messageOptionsHandler(
    onMessageLongClick: (MessageOptionsParams) -> Unit,
    canJump: Boolean,
): (String, String?, String) -> Unit = { messageId, channel, fullMessage ->
    onMessageLongClick(
        MessageOptionsParams(
            messageId = messageId,
            channel = channel?.let { UserName(it) },
            fullMessage = fullMessage,
            canModerate = false,
            canReply = false,
            canCopy = true,
            canJump = canJump,
        ),
    )
}

private fun buildUserPopupParams(
    userId: String?,
    userName: String,
    displayName: String,
    channel: String?,
    badges: List<BadgeUi>,
) = UserPopupStateParams(
    targetUserId = userId?.let { UserId(it) } ?: UserId(""),
    targetUserName = UserName(userName),
    targetDisplayName = DisplayName(displayName),
    channel = channel?.let { UserName(it) },
    badges = badges.map { it.badge },
)
