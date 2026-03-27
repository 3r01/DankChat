package com.flxrs.dankchat.ui.main.dialog

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
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flxrs.dankchat.R
import com.flxrs.dankchat.data.DisplayName
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.repo.channel.ChannelRepository
import com.flxrs.dankchat.preferences.DankChatPreferenceStore
import com.flxrs.dankchat.ui.chat.BadgeUi
import com.flxrs.dankchat.ui.chat.emote.EmoteInfoViewModel
import com.flxrs.dankchat.ui.chat.message.MessageOptionsState
import com.flxrs.dankchat.ui.chat.message.MessageOptionsViewModel
import com.flxrs.dankchat.ui.chat.user.UserPopupDialog
import com.flxrs.dankchat.ui.chat.user.UserPopupViewModel
import com.flxrs.dankchat.ui.main.channel.ChannelManagementViewModel
import com.flxrs.dankchat.ui.main.input.ChatInputViewModel
import com.flxrs.dankchat.ui.main.sheet.FullScreenSheetState
import com.flxrs.dankchat.ui.main.sheet.InputSheetState
import com.flxrs.dankchat.ui.main.sheet.DebugInfoSheet
import com.flxrs.dankchat.ui.main.sheet.DebugInfoViewModel
import com.flxrs.dankchat.ui.main.sheet.MoreActionsSheet
import com.flxrs.dankchat.ui.main.sheet.SheetNavigationViewModel
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenDialogs(
    dialogViewModel: DialogStateViewModel,
    isLoggedIn: Boolean,
    activeChannel: UserName?,
    modActionsChannel: UserName?,
    isStreamActive: Boolean,
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

    if (dialogState.showModActions && modActionsChannel != null) {
        val preferenceStore: DankChatPreferenceStore = koinInject()
        val roomState = channelRepository.getRoomState(modActionsChannel)
        val isBroadcaster = preferenceStore.userIdString == roomState?.channelId
        val modActionsViewModel: ModActionsViewModel = koinViewModel(
            key = "mod-actions-${modActionsChannel.value}",
            parameters = { parametersOf(modActionsChannel) }
        )
        val shieldModeActive by modActionsViewModel.shieldModeActive.collectAsStateWithLifecycle()
        ModActionsDialog(
            roomState = roomState,
            isBroadcaster = isBroadcaster,
            isStreamActive = isStreamActive,
            shieldModeActive = shieldModeActive,
            onSendCommand = { command ->
                chatInputViewModel.trySendMessageOrCommand(command)
            },
            onAnnounce = { chatInputViewModel.setAnnouncing(true) },
            onDismiss = dialogViewModel::dismissModActions
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

    dialogState.messageOptionsParams?.let { params ->
        val viewModel: MessageOptionsViewModel = koinViewModel(
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
                onReplyToOriginal = {
                    chatInputViewModel.setReplying(true, s.rootThreadId, s.rootThreadName ?: s.replyName)
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
        val viewModel: EmoteInfoViewModel = koinViewModel(
            key = emotes.joinToString { it.id },
            parameters = { parametersOf(emotes) }
        )
        val sheetState by sheetNavigationViewModel.fullScreenSheetState.collectAsStateWithLifecycle()
        val whisperTarget by chatInputViewModel.whisperTarget.collectAsStateWithLifecycle()
        val canUseEmote = isLoggedIn && when (sheetState) {
            is FullScreenSheetState.Closed,
            is FullScreenSheetState.Replies -> true

            is FullScreenSheetState.Mention,
            is FullScreenSheetState.Whisper -> whisperTarget != null

            is FullScreenSheetState.History -> false
        }
        EmoteInfoDialog(
            items = viewModel.items,
            isLoggedIn = canUseEmote,
            onUseEmote = { chatInputViewModel.insertText("$it ") },
            onCopyEmote = {
                scope.launch {
                    clipboardManager.setClipEntry(ClipEntry(ClipData.newPlainText("emote", it)))
                }
            },
            onOpenLink = { onOpenUrl(it) },
            onDismiss = dialogViewModel::dismissEmoteInfo
        )
    }

    dialogState.userPopupParams?.let { params ->
        val viewModel: UserPopupViewModel = koinViewModel(
            key = "${params.targetUserId}${params.channel?.value.orEmpty()}",
            parameters = { parametersOf(params) }
        )
        val state by viewModel.userPopupState.collectAsStateWithLifecycle()
        val preferenceStore: DankChatPreferenceStore = koinInject()
        val isOwnUser = preferenceStore.userIdString == params.targetUserId
        UserPopupDialog(
            state = state,
            badges = params.badges.mapIndexed { index, badge -> BadgeUi(badge.url, badge, index) },
            isOwnUser = isOwnUser,
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
                sheetNavigationViewModel.openWhispers()
                chatInputViewModel.setWhisperTarget(UserName(name))
            },
            onOpenChannel = { userName -> onOpenUrl("https://twitch.tv/$userName") },
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

    if (inputSheetState is InputSheetState.DebugInfo) {
        val debugInfoViewModel: DebugInfoViewModel = koinViewModel()
        DebugInfoSheet(
            viewModel = debugInfoViewModel,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            onDismiss = sheetNavigationViewModel::closeInputSheet,
        )
    }
}
