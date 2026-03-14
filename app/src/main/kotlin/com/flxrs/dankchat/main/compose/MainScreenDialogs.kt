package com.flxrs.dankchat.main.compose

import android.content.ClipData
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flxrs.dankchat.R
import com.flxrs.dankchat.chat.compose.BadgeUi
import com.flxrs.dankchat.chat.emote.compose.EmoteInfoComposeViewModel
import com.flxrs.dankchat.chat.message.compose.MessageOptionsComposeViewModel
import com.flxrs.dankchat.chat.message.compose.MessageOptionsParams
import com.flxrs.dankchat.chat.message.compose.MessageOptionsState
import com.flxrs.dankchat.chat.user.UserPopupComposeViewModel
import com.flxrs.dankchat.chat.user.UserPopupStateParams
import com.flxrs.dankchat.chat.user.compose.UserPopupDialog
import com.flxrs.dankchat.data.DisplayName
import com.flxrs.dankchat.data.UserId
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.repo.channel.ChannelRepository
import com.flxrs.dankchat.data.twitch.emote.ChatMessageEmote
import com.flxrs.dankchat.main.compose.dialogs.AddChannelDialog
import com.flxrs.dankchat.main.compose.dialogs.EmoteInfoDialog
import com.flxrs.dankchat.main.compose.dialogs.ManageChannelsDialog
import com.flxrs.dankchat.main.compose.dialogs.MessageOptionsDialog
import com.flxrs.dankchat.main.compose.dialogs.RoomStateDialog
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenDialogs(
    showAddChannelDialog: Boolean,
    showManageChannelsDialog: Boolean,
    showLogoutDialog: Boolean,
    showRoomStateDialog: Boolean,
    showRemoveChannelDialog: Boolean,
    showBlockChannelDialog: Boolean,
    showClearChatDialog: Boolean,
    activeChannel: UserName?,
    roomStateChannel: UserName?,
    messageOptionsParams: MessageOptionsParams?,
    emoteInfoEmotes: List<ChatMessageEmote>?,
    userPopupParams: UserPopupStateParams?,
    inputSheetState: InputSheetState,
    channelManagementViewModel: ChannelManagementViewModel,
    channelRepository: ChannelRepository,
    chatInputViewModel: ChatInputViewModel,
    sheetNavigationViewModel: SheetNavigationViewModel,
    snackbarHostState: SnackbarHostState,
    onDismissAddChannel: () -> Unit,
    onDismissManageChannels: () -> Unit,
    onDismissLogout: () -> Unit,
    onDismissRoomState: () -> Unit,
    onDismissRemoveChannel: () -> Unit,
    onDismissBlockChannel: () -> Unit,
    onDismissClearChat: () -> Unit,
    onDismissMessageOptions: () -> Unit,
    onDismissEmoteInfo: () -> Unit,
    onDismissUserPopup: () -> Unit,
    onLogout: () -> Unit,
    onOpenChannel: () -> Unit,
    onReportChannel: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onAddChannel: (UserName) -> Unit,
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboard.current
    val scope = rememberCoroutineScope()

    if (showAddChannelDialog) {
        AddChannelDialog(
            onDismiss = onDismissAddChannel,
            onAddChannel = onAddChannel
        )
    }

    if (showManageChannelsDialog) {
        val channels by channelManagementViewModel.channels.collectAsStateWithLifecycle()
        ManageChannelsDialog(
            channels = channels,
            onApplyChanges = channelManagementViewModel::applyChanges,
            onDismiss = onDismissManageChannels
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = onDismissLogout,
            title = { Text(stringResource(R.string.confirm_logout_title)) },
            text = { Text(stringResource(R.string.confirm_logout_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onLogout()
                        onDismissLogout()
                    }
                ) {
                    Text(stringResource(R.string.confirm_logout_positive_button))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissLogout) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }

    if (showRoomStateDialog && roomStateChannel != null) {
        RoomStateDialog(
            roomState = channelRepository.getRoomState(roomStateChannel),
            onSendCommand = { command ->
                chatInputViewModel.trySendMessageOrCommand(command)
            },
            onDismiss = onDismissRoomState
        )
    }

    if (showRemoveChannelDialog && activeChannel != null) {
        AlertDialog(
            onDismissRequest = onDismissRemoveChannel,
            title = { Text(stringResource(R.string.confirm_channel_removal_title)) },
            text = { Text(stringResource(R.string.confirm_channel_removal_message_named, activeChannel)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        channelManagementViewModel.removeChannel(activeChannel)
                        onDismissRemoveChannel()
                    }
                ) {
                    Text(stringResource(R.string.confirm_channel_removal_positive_button))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissRemoveChannel) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }

    if (showBlockChannelDialog && activeChannel != null) {
        AlertDialog(
            onDismissRequest = onDismissBlockChannel,
            title = { Text(stringResource(R.string.confirm_channel_block_title)) },
            text = { Text(stringResource(R.string.confirm_channel_block_message_named, activeChannel)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        channelManagementViewModel.blockChannel(activeChannel)
                        onDismissBlockChannel()
                    }
                ) {
                    Text(stringResource(R.string.confirm_user_block_positive_button))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissBlockChannel) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }

    if (showClearChatDialog && activeChannel != null) {
        AlertDialog(
            onDismissRequest = onDismissClearChat,
            title = { Text(stringResource(R.string.clear_chat)) },
            text = { Text(stringResource(R.string.confirm_user_delete_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        channelManagementViewModel.clearChat(activeChannel)
                        onDismissClearChat()
                    }
                ) {
                    Text(stringResource(R.string.dialog_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissClearChat) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }

    messageOptionsParams?.let { params ->
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
                hasReplyThread = s.hasReplyThread,
                onReply = {
                    chatInputViewModel.setReplying(true, s.messageId, s.replyName)
                },
                onViewThread = {
                    sheetNavigationViewModel.openReplies(s.rootThreadId, s.replyName)
                },
                onCopy = {
                    scope.launch {
                        clipboardManager.setClipEntry(ClipEntry(ClipData.newPlainText("message", params.fullMessage)))
                        snackbarHostState.showSnackbar(context.getString(R.string.snackbar_message_copied))
                    }
                },
                onMoreActions = {
                    sheetNavigationViewModel.openMoreActions(s.messageId, params.fullMessage)
                },
                onDelete = viewModel::deleteMessage,
                onTimeout = viewModel::timeoutUser,
                onBan = viewModel::banUser,
                onUnban = viewModel::unbanUser,
                onDismiss = onDismissMessageOptions
            )
        }
    }

    emoteInfoEmotes?.let { emotes ->
        val viewModel: EmoteInfoComposeViewModel = koinViewModel(
            key = emotes.joinToString { it.id },
            parameters = { parametersOf(emotes) }
        )
        EmoteInfoDialog(
            items = viewModel.items,
            onUseEmote = { chatInputViewModel.insertText("$it ") },
            onCopyEmote = { /* TODO: copy to clipboard */ },
            onOpenLink = { onOpenUrl(it) },
            onDismiss = onDismissEmoteInfo
        )
    }

    userPopupParams?.let { params ->
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
            onDismiss = onDismissUserPopup,
            onMention = { name, _ ->
                chatInputViewModel.insertText("@$name ")
            },
            onWhisper = { name ->
                chatInputViewModel.updateInputText("/w $name ")
            },
            onOpenChannel = { _ -> onOpenChannel() },
            onReport = { _ ->
                onReportChannel()
            }
        )
    }

    if (inputSheetState is InputSheetState.MoreActions) {
        val state = inputSheetState as InputSheetState.MoreActions
        com.flxrs.dankchat.main.compose.dialogs.MoreActionsSheet(
            messageId = state.messageId,
            fullMessage = state.fullMessage,
            onCopyFullMessage = {
                scope.launch {
                    clipboardManager.setClipEntry(ClipEntry(ClipData.newPlainText("full message", it)))
                    snackbarHostState.showSnackbar(context.getString(R.string.snackbar_message_copied))
                }
            },
            onCopyMessageId = {
                scope.launch {
                    clipboardManager.setClipEntry(ClipEntry(ClipData.newPlainText("message id", it)))
                    snackbarHostState.showSnackbar(context.getString(R.string.snackbar_message_id_copied))
                }
            },
            onDismiss = sheetNavigationViewModel::closeInputSheet,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        )
    }
}
