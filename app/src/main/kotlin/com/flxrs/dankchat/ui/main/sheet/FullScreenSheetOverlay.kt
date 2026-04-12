package com.flxrs.dankchat.ui.main.sheet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.flxrs.dankchat.data.DisplayName
import com.flxrs.dankchat.data.UserId
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.preferences.chat.UserLongClickBehavior
import com.flxrs.dankchat.ui.chat.BadgeUi
import com.flxrs.dankchat.ui.chat.history.HistoryChannel
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
                        historyChannel = sheetState.channel,
                        initialFilter = sheetState.initialFilter,
                        onDismiss = onDismiss,
                        onUserClick = onUserClick,
                        onMessageLongClick = onMessageLongClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun HistorySheetContent(
    historyChannel: HistoryChannel,
    initialFilter: String,
    onDismiss: () -> Unit,
    onUserClick: (UserPopupStateParams) -> Unit,
    onMessageLongClick: (MessageOptionsParams) -> Unit,
) {
    val viewModel: MessageHistoryViewModel =
        koinViewModel(
            parameters = { parametersOf(historyChannel) },
        )
    LaunchedEffect(historyChannel) {
        viewModel.selectChannel(historyChannel)
    }
    val clickHandler = popupOnlyClickHandler(onUserClick)
    MessageHistorySheet(
        viewModel = viewModel,
        initialFilter = initialFilter,
        onDismiss = onDismiss,
        onUserClick = clickHandler,
        onMessageLongClick = messageOptionsHandler(onMessageLongClick, canJump = true),
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
