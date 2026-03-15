package com.flxrs.dankchat.main.compose

import android.content.ClipData
import com.flxrs.dankchat.main.compose.dialogs.ConfirmationDialog
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flxrs.dankchat.R
import com.flxrs.dankchat.chat.compose.BadgeUi
import com.flxrs.dankchat.chat.emote.compose.EmoteInfoComposeViewModel
import com.flxrs.dankchat.chat.message.compose.MessageOptionsComposeViewModel
import com.flxrs.dankchat.chat.message.compose.MessageOptionsState
import com.flxrs.dankchat.chat.user.UserPopupComposeViewModel
import com.flxrs.dankchat.chat.user.compose.UserPopupDialog
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.repo.channel.ChannelRepository
import com.flxrs.dankchat.data.DisplayName
import com.flxrs.dankchat.main.compose.dialogs.AddChannelDialog
import com.flxrs.dankchat.main.compose.dialogs.EmoteInfoDialog
import com.flxrs.dankchat.main.compose.dialogs.ManageChannelsDialog
import com.flxrs.dankchat.main.compose.dialogs.MessageOptionsDialog
import com.flxrs.dankchat.main.compose.dialogs.MoreActionsSheet
import com.flxrs.dankchat.main.compose.dialogs.RoomStateDialog
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenDialogs(
    dialogViewModel: DialogStateViewModel,
    activeChannel: UserName?,
    roomStateChannel: UserName?,
    inputSheetState: InputSheetState,
    snackbarHostState: SnackbarHostState,
    onAddChannel: (UserName) -> Unit,
    onLogout: () -> Unit,
    onLogin: () -> Unit,
    onOpenChannel: () -> Unit,
    onReportChannel: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onJumpToMessage: (messageId: String, channel: UserName) -> Unit = { _, _ -> },
) {
    val dialogState by dialogViewModel.state.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val messageCopiedMsg = stringResource(R.string.snackbar_message_copied)
    val messageIdCopiedMsg = stringResource(R.string.snackbar_message_id_copied)

    val channelManagementViewModel: ChannelManagementViewModel = koinViewModel()
    val chatInputViewModel: ChatInputViewModel = koinViewModel()
    val sheetNavigationViewModel: SheetNavigationViewModel = koinViewModel()
    val channelRepository: ChannelRepository = koinInject()

    // region Channel dialogs

    if (dialogState.showAddChannel) {
        AddChannelDialog(
            onDismiss = dialogViewModel::dismissAddChannel,
            onAddChannel = onAddChannel
        )
    }

    if (dialogState.showManageChannels) {
        val channels by channelManagementViewModel.channels.collectAsStateWithLifecycle()
        ManageChannelsDialog(
            channels = channels,
            onApplyChanges = channelManagementViewModel::applyChanges,
            onChannelSelected = channelManagementViewModel::selectChannel,
            onDismiss = dialogViewModel::dismissManageChannels
        )
    }

    if (dialogState.showRoomState && roomStateChannel != null) {
        RoomStateDialog(
            roomState = channelRepository.getRoomState(roomStateChannel),
            onSendCommand = { command ->
                chatInputViewModel.trySendMessageOrCommand(command)
            },
            onDismiss = dialogViewModel::dismissRoomState
        )
    }

    if (dialogState.showRemoveChannel && activeChannel != null) {
        ConfirmationDialog(
            title = stringResource(R.string.confirm_channel_removal_question_named, activeChannel),
            confirmText = stringResource(R.string.confirm_channel_removal_positive_button),
            onConfirm = {
                channelManagementViewModel.removeChannel(activeChannel)
                dialogViewModel.dismissRemoveChannel()
            },
            onDismiss = dialogViewModel::dismissRemoveChannel,
        )
    }

    if (dialogState.showBlockChannel && activeChannel != null) {
        ConfirmationDialog(
            title = stringResource(R.string.confirm_channel_block_question_named, activeChannel),
            confirmText = stringResource(R.string.confirm_user_block_positive_button),
            onConfirm = {
                channelManagementViewModel.blockChannel(activeChannel)
                dialogViewModel.dismissBlockChannel()
            },
            onDismiss = dialogViewModel::dismissBlockChannel,
        )
    }

    if (dialogState.showClearChat && activeChannel != null) {
        ConfirmationDialog(
            title = stringResource(R.string.confirm_clear_chat_question),
            confirmText = stringResource(R.string.dialog_ok),
            onConfirm = {
                channelManagementViewModel.clearChat(activeChannel)
                dialogViewModel.dismissClearChat()
            },
            onDismiss = dialogViewModel::dismissClearChat,
        )
    }

    // endregion

    // region Auth dialogs

    if (dialogState.showLogout) {
        ConfirmationDialog(
            title = stringResource(R.string.confirm_logout_question),
            confirmText = stringResource(R.string.confirm_logout_positive_button),
            onConfirm = {
                onLogout()
                dialogViewModel.dismissLogout()
            },
            onDismiss = dialogViewModel::dismissLogout,
        )
    }

    if (dialogState.loginOutdated != null) {
        AlertDialog(
            onDismissRequest = dialogViewModel::dismissLoginOutdated,
            title = { Text(stringResource(R.string.login_outdated_title)) },
            text = { Text(stringResource(R.string.login_outdated_message)) },
            confirmButton = {
                TextButton(onClick = {
                    dialogViewModel.dismissLoginOutdated()
                    onLogin()
                }) {
                    Text(stringResource(R.string.oauth_expired_login_again))
                }
            },
            dismissButton = {
                TextButton(onClick = dialogViewModel::dismissLoginOutdated) {
                    Text(stringResource(R.string.dialog_dismiss))
                }
            }
        )
    }

    if (dialogState.showLoginExpired) {
        AlertDialog(
            onDismissRequest = dialogViewModel::dismissLoginExpired,
            title = { Text(stringResource(R.string.oauth_expired_title)) },
            text = { Text(stringResource(R.string.oauth_expired_message)) },
            confirmButton = {
                TextButton(onClick = {
                    dialogViewModel.dismissLoginExpired()
                    onLogin()
                }) {
                    Text(stringResource(R.string.oauth_expired_login_again))
                }
            },
            dismissButton = {
                TextButton(onClick = dialogViewModel::dismissLoginExpired) {
                    Text(stringResource(R.string.dialog_dismiss))
                }
            }
        )
    }

    // endregion

    // region Message interactions

    dialogState.messageOptionsParams?.let { params ->
        val viewModel: MessageOptionsComposeViewModel = koinViewModel(
            key = params.messageId,
            parameters = { parametersOf(params.messageId, params.channel, params.canModerate, params.canReply) }
        )
        val state by viewModel.state.collectAsStateWithLifecycle()
        (state as? MessageOptionsState.Found)?.let { s ->
            MessageOptionsDialog(
                messageId = s.messageId,
                channel = params.channel?.value,
                fullMessage = params.fullMessage,
                canModerate = s.canModerate,
                canReply = s.canReply,
                canCopy = params.canCopy,
                canJump = params.canJump,
                hasReplyThread = s.hasReplyThread,
                onJumpToMessage = {
                    params.channel?.let { channel ->
                        onJumpToMessage(params.messageId, channel)
                    }
                },
                onReply = {
                    chatInputViewModel.setReplying(true, s.messageId, s.replyName)
                },
                onViewThread = {
                    sheetNavigationViewModel.openReplies(s.rootThreadId, s.replyName)
                },
                onCopy = {
                    scope.launch {
                        clipboardManager.setClipEntry(ClipEntry(ClipData.newPlainText("message", s.originalMessage)))
                        snackbarHostState.showSnackbar(messageCopiedMsg)
                    }
                },
                onMoreActions = {
                    sheetNavigationViewModel.openMoreActions(s.messageId, params.fullMessage)
                },
                onDelete = viewModel::deleteMessage,
                onTimeout = viewModel::timeoutUser,
                onBan = viewModel::banUser,
                onUnban = viewModel::unbanUser,
                onDismiss = dialogViewModel::dismissMessageOptions
            )
        }
    }

    dialogState.emoteInfoEmotes?.let { emotes ->
        val viewModel: EmoteInfoComposeViewModel = koinViewModel(
            key = emotes.joinToString { it.id },
            parameters = { parametersOf(emotes) }
        )
        EmoteInfoDialog(
            items = viewModel.items,
            onUseEmote = { chatInputViewModel.insertText("$it ") },
            onCopyEmote = { /* TODO: copy to clipboard */ },
            onOpenLink = { onOpenUrl(it) },
            onDismiss = dialogViewModel::dismissEmoteInfo
        )
    }

    dialogState.userPopupParams?.let { params ->
        val viewModel: UserPopupComposeViewModel = koinViewModel(
            key = "${params.targetUserId}${params.channel?.value.orEmpty()}",
            parameters = { parametersOf(params) }
        )
        val state by viewModel.userPopupState.collectAsStateWithLifecycle()
        UserPopupDialog(
            state = state,
            badges = params.badges.mapIndexed { index, badge -> BadgeUi(badge.url, badge, index) },
            onBlockUser = viewModel::blockUser,
            onUnblockUser = viewModel::unblockUser,
            onDismiss = dialogViewModel::dismissUserPopup,
            onMention = { name, displayName ->
                chatInputViewModel.mentionUser(
                    user = UserName(name),
                    display = DisplayName(displayName),
                )
            },
            onWhisper = { name ->
                chatInputViewModel.updateInputText("/w $name ")
            },
            onOpenChannel = { _ -> onOpenChannel() },
            onReport = { _ ->
                onReportChannel()
            },
            onMessageHistory = { userName ->
                params.channel?.let { channel ->
                    sheetNavigationViewModel.openHistory(channel, "from:$userName")
                }
                dialogViewModel.dismissUserPopup()
            },
        )
    }

    if (inputSheetState is InputSheetState.MoreActions) {
        MoreActionsSheet(
            messageId = inputSheetState.messageId,
            fullMessage = inputSheetState.fullMessage,
            onCopyFullMessage = {
                scope.launch {
                    clipboardManager.setClipEntry(ClipEntry(ClipData.newPlainText("full message", it)))
                    snackbarHostState.showSnackbar(messageCopiedMsg)
                }
            },
            onCopyMessageId = {
                scope.launch {
                    clipboardManager.setClipEntry(ClipEntry(ClipData.newPlainText("message id", it)))
                    snackbarHostState.showSnackbar(messageIdCopiedMsg)
                }
            },
            onDismiss = sheetNavigationViewModel::closeInputSheet,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        )
    }

    // endregion
}
