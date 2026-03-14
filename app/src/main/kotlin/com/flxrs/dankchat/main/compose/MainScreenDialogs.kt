package com.flxrs.dankchat.main.compose

import android.content.ClipData
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
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
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.repo.channel.ChannelRepository
import com.flxrs.dankchat.data.DisplayName
import com.flxrs.dankchat.data.twitch.emote.ChatMessageEmote
import com.flxrs.dankchat.main.compose.dialogs.AddChannelDialog
import com.flxrs.dankchat.main.compose.dialogs.EmoteInfoDialog
import com.flxrs.dankchat.main.compose.dialogs.ManageChannelsDialog
import com.flxrs.dankchat.main.compose.dialogs.MessageOptionsDialog
import com.flxrs.dankchat.main.compose.dialogs.RoomStateDialog
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Stable
data class ChannelDialogState(
    val showAddChannel: Boolean,
    val showManageChannels: Boolean,
    val showRemoveChannel: Boolean,
    val showBlockChannel: Boolean,
    val showClearChat: Boolean,
    val showRoomState: Boolean,
    val activeChannel: UserName?,
    val roomStateChannel: UserName?,
    val onDismissAddChannel: () -> Unit,
    val onDismissManageChannels: () -> Unit,
    val onDismissRemoveChannel: () -> Unit,
    val onDismissBlockChannel: () -> Unit,
    val onDismissClearChat: () -> Unit,
    val onDismissRoomState: () -> Unit,
    val onAddChannel: (UserName) -> Unit,
)

@Stable
data class AuthDialogState(
    val showLogout: Boolean,
    val showLoginOutdated: Boolean,
    val showLoginExpired: Boolean,
    val onDismissLogout: () -> Unit,
    val onDismissLoginOutdated: () -> Unit,
    val onDismissLoginExpired: () -> Unit,
    val onLogout: () -> Unit,
    val onLogin: () -> Unit,
)

@Stable
data class MessageInteractionState(
    val messageOptionsParams: MessageOptionsParams?,
    val emoteInfoEmotes: List<ChatMessageEmote>?,
    val userPopupParams: UserPopupStateParams?,
    val inputSheetState: InputSheetState,
    val onDismissMessageOptions: () -> Unit,
    val onDismissEmoteInfo: () -> Unit,
    val onDismissUserPopup: () -> Unit,
    val onOpenChannel: () -> Unit,
    val onReportChannel: () -> Unit,
    val onOpenUrl: (String) -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenDialogs(
    channelState: ChannelDialogState,
    authState: AuthDialogState,
    messageState: MessageInteractionState,
    snackbarHostState: SnackbarHostState,
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboard.current
    val scope = rememberCoroutineScope()

    val channelManagementViewModel: ChannelManagementViewModel = koinViewModel()
    val chatInputViewModel: ChatInputViewModel = koinViewModel()
    val sheetNavigationViewModel: SheetNavigationViewModel = koinViewModel()
    val channelRepository: ChannelRepository = koinInject()

    // region Channel dialogs

    if (channelState.showAddChannel) {
        AddChannelDialog(
            onDismiss = channelState.onDismissAddChannel,
            onAddChannel = channelState.onAddChannel
        )
    }

    if (channelState.showManageChannels) {
        val channels by channelManagementViewModel.channels.collectAsStateWithLifecycle()
        ManageChannelsDialog(
            channels = channels,
            onApplyChanges = channelManagementViewModel::applyChanges,
            onDismiss = channelState.onDismissManageChannels
        )
    }

    if (channelState.showRoomState && channelState.roomStateChannel != null) {
        RoomStateDialog(
            roomState = channelRepository.getRoomState(channelState.roomStateChannel),
            onSendCommand = { command ->
                chatInputViewModel.trySendMessageOrCommand(command)
            },
            onDismiss = channelState.onDismissRoomState
        )
    }

    if (channelState.showRemoveChannel && channelState.activeChannel != null) {
        AlertDialog(
            onDismissRequest = channelState.onDismissRemoveChannel,
            title = { Text(stringResource(R.string.confirm_channel_removal_title)) },
            text = { Text(stringResource(R.string.confirm_channel_removal_message_named, channelState.activeChannel)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        channelManagementViewModel.removeChannel(channelState.activeChannel)
                        channelState.onDismissRemoveChannel()
                    }
                ) {
                    Text(stringResource(R.string.confirm_channel_removal_positive_button))
                }
            },
            dismissButton = {
                TextButton(onClick = channelState.onDismissRemoveChannel) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }

    if (channelState.showBlockChannel && channelState.activeChannel != null) {
        AlertDialog(
            onDismissRequest = channelState.onDismissBlockChannel,
            title = { Text(stringResource(R.string.confirm_channel_block_title)) },
            text = { Text(stringResource(R.string.confirm_channel_block_message_named, channelState.activeChannel)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        channelManagementViewModel.blockChannel(channelState.activeChannel)
                        channelState.onDismissBlockChannel()
                    }
                ) {
                    Text(stringResource(R.string.confirm_user_block_positive_button))
                }
            },
            dismissButton = {
                TextButton(onClick = channelState.onDismissBlockChannel) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }

    if (channelState.showClearChat && channelState.activeChannel != null) {
        AlertDialog(
            onDismissRequest = channelState.onDismissClearChat,
            title = { Text(stringResource(R.string.clear_chat)) },
            text = { Text(stringResource(R.string.confirm_user_delete_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        channelManagementViewModel.clearChat(channelState.activeChannel)
                        channelState.onDismissClearChat()
                    }
                ) {
                    Text(stringResource(R.string.dialog_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = channelState.onDismissClearChat) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }

    // endregion

    // region Auth dialogs

    if (authState.showLogout) {
        AlertDialog(
            onDismissRequest = authState.onDismissLogout,
            title = { Text(stringResource(R.string.confirm_logout_title)) },
            text = { Text(stringResource(R.string.confirm_logout_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        authState.onLogout()
                        authState.onDismissLogout()
                    }
                ) {
                    Text(stringResource(R.string.confirm_logout_positive_button))
                }
            },
            dismissButton = {
                TextButton(onClick = authState.onDismissLogout) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }

    if (authState.showLoginOutdated) {
        AlertDialog(
            onDismissRequest = authState.onDismissLoginOutdated,
            title = { Text(stringResource(R.string.login_outdated_title)) },
            text = { Text(stringResource(R.string.login_outdated_message)) },
            confirmButton = {
                TextButton(onClick = {
                    authState.onDismissLoginOutdated()
                    authState.onLogin()
                }) {
                    Text(stringResource(R.string.oauth_expired_login_again))
                }
            },
            dismissButton = {
                TextButton(onClick = authState.onDismissLoginOutdated) {
                    Text(stringResource(R.string.dialog_dismiss))
                }
            }
        )
    }

    if (authState.showLoginExpired) {
        AlertDialog(
            onDismissRequest = authState.onDismissLoginExpired,
            title = { Text(stringResource(R.string.oauth_expired_title)) },
            text = { Text(stringResource(R.string.oauth_expired_message)) },
            confirmButton = {
                TextButton(onClick = {
                    authState.onDismissLoginExpired()
                    authState.onLogin()
                }) {
                    Text(stringResource(R.string.oauth_expired_login_again))
                }
            },
            dismissButton = {
                TextButton(onClick = authState.onDismissLoginExpired) {
                    Text(stringResource(R.string.dialog_dismiss))
                }
            }
        )
    }

    // endregion

    // region Message interactions

    messageState.messageOptionsParams?.let { params ->
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
                onDismiss = messageState.onDismissMessageOptions
            )
        }
    }

    messageState.emoteInfoEmotes?.let { emotes ->
        val viewModel: EmoteInfoComposeViewModel = koinViewModel(
            key = emotes.joinToString { it.id },
            parameters = { parametersOf(emotes) }
        )
        EmoteInfoDialog(
            items = viewModel.items,
            onUseEmote = { chatInputViewModel.insertText("$it ") },
            onCopyEmote = { /* TODO: copy to clipboard */ },
            onOpenLink = { messageState.onOpenUrl(it) },
            onDismiss = messageState.onDismissEmoteInfo
        )
    }

    messageState.userPopupParams?.let { params ->
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
            onDismiss = messageState.onDismissUserPopup,
            onMention = { name, displayName ->
                chatInputViewModel.mentionUser(
                    user = UserName(name),
                    display = DisplayName(displayName),
                )
            },
            onWhisper = { name ->
                chatInputViewModel.updateInputText("/w $name ")
            },
            onOpenChannel = { _ -> messageState.onOpenChannel() },
            onReport = { _ ->
                messageState.onReportChannel()
            }
        )
    }

    val inputSheet = messageState.inputSheetState
    if (inputSheet is InputSheetState.MoreActions) {
        com.flxrs.dankchat.main.compose.dialogs.MoreActionsSheet(
            messageId = inputSheet.messageId,
            fullMessage = inputSheet.fullMessage,
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

    // endregion
}
