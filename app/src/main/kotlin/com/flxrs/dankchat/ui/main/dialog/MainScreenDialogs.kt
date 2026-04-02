package com.flxrs.dankchat.ui.main.dialog

import android.content.ClipData
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flxrs.dankchat.R
import com.flxrs.dankchat.data.DisplayName
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.auth.StartupValidation
import com.flxrs.dankchat.data.auth.StartupValidationHolder
import com.flxrs.dankchat.data.twitch.emote.ChatMessageEmote
import com.flxrs.dankchat.ui.chat.BadgeUi
import com.flxrs.dankchat.ui.chat.emote.EmoteInfoViewModel
import com.flxrs.dankchat.ui.chat.message.MessageOptionsParams
import com.flxrs.dankchat.ui.chat.message.MessageOptionsState
import com.flxrs.dankchat.ui.chat.message.MessageOptionsViewModel
import com.flxrs.dankchat.ui.chat.user.UserPopupDialog
import com.flxrs.dankchat.ui.chat.user.UserPopupStateParams
import com.flxrs.dankchat.ui.chat.user.UserPopupViewModel
import com.flxrs.dankchat.ui.main.channel.ChannelManagementViewModel
import com.flxrs.dankchat.ui.main.input.ChatInputViewModel
import com.flxrs.dankchat.ui.main.sheet.DebugInfoSheet
import com.flxrs.dankchat.ui.main.sheet.DebugInfoViewModel
import com.flxrs.dankchat.ui.main.sheet.FullScreenSheetState
import com.flxrs.dankchat.ui.main.sheet.InputSheetState
import com.flxrs.dankchat.ui.main.sheet.SheetNavigationViewModel
import com.flxrs.dankchat.utils.compose.ConfirmationBottomSheet
import com.flxrs.dankchat.utils.compose.InfoBottomSheet
import com.flxrs.dankchat.utils.compose.InputBottomSheet
import kotlinx.collections.immutable.toImmutableList
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
    sheetsReady: Boolean,
    onAddChannel: (UserName) -> Unit,
    onLogout: () -> Unit,
    onLogin: () -> Unit,
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
    val startupValidationHolder: StartupValidationHolder = koinInject()
    val startupValidation by startupValidationHolder.state.collectAsStateWithLifecycle()

    if (dialogState.showAddChannel) {
        AddChannelDialog(
            onDismiss = dialogViewModel::dismissAddChannel,
            onAddChannel = onAddChannel,
            isChannelAlreadyAdded = channelManagementViewModel::isChannelAdded,
        )
    }

    if (dialogState.showManageChannels) {
        val channels by channelManagementViewModel.channels.collectAsStateWithLifecycle()
        ManageChannelsDialog(
            channels = channels,
            onApplyChanges = channelManagementViewModel::applyChanges,
            onChannelSelect = channelManagementViewModel::selectChannel,
            onDismiss = dialogViewModel::dismissManageChannels,
        )
    }

    if (sheetsReady && dialogState.showModActions && modActionsChannel != null) {
        ModActionsDialogContainer(
            channel = modActionsChannel,
            isStreamActive = isStreamActive,
            onSendCommand = chatInputViewModel::trySendMessageOrCommand,
            onAnnounce = { chatInputViewModel.setAnnouncing(true) },
            onDismiss = dialogViewModel::dismissModActions,
        )
    }

    if (dialogState.showRemoveChannel && activeChannel != null) {
        ConfirmationDialog(
            title = stringResource(R.string.confirm_channel_removal_message_named, activeChannel),
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
            title = stringResource(R.string.confirm_channel_block_message_named, activeChannel),
            confirmText = stringResource(R.string.confirm_user_block_positive_button),
            onConfirm = {
                channelManagementViewModel.blockChannel(activeChannel)
                dialogViewModel.dismissBlockChannel()
            },
            onDismiss = dialogViewModel::dismissBlockChannel,
        )
    }

    if (dialogState.showLogout) {
        ConfirmationBottomSheet(
            title = stringResource(R.string.confirm_logout_message),
            confirmText = stringResource(R.string.confirm_logout_positive_button),
            onConfirm = {
                onLogout()
                dialogViewModel.dismissLogout()
            },
            onDismiss = dialogViewModel::dismissLogout,
        )
    }

    if (dialogState.pendingUploadAction != null) {
        UploadDisclaimerSheet(
            host = dialogViewModel.uploadHost,
            onConfirm = {
                dialogViewModel.acknowledgeExternalHosting()
                val action = dialogState.pendingUploadAction
                dialogViewModel.setPendingUploadAction(null)
                action?.invoke()
            },
            onDismiss = { dialogViewModel.setPendingUploadAction(null) },
        )
    }

    if (dialogState.showNewWhisper) {
        InputBottomSheet(
            title = stringResource(R.string.whisper_new_dialog_title),
            hint = stringResource(R.string.whisper_new_dialog_hint),
            confirmText = stringResource(R.string.whisper_new_dialog_start),
            showClearButton = true,
            onConfirm = { username ->
                chatInputViewModel.setWhisperTarget(UserName(username))
                dialogViewModel.dismissNewWhisper()
            },
            onDismiss = dialogViewModel::dismissNewWhisper,
        )
    }

    if (startupValidation is StartupValidation.ScopesOutdated) {
        InfoBottomSheet(
            title = stringResource(R.string.login_outdated_title),
            message = stringResource(R.string.login_outdated_message),
            confirmText = stringResource(R.string.oauth_expired_login_again),
            dismissible = false,
            onConfirm = onLogin,
            onDismiss = startupValidationHolder::acknowledge,
        )
    }

    if (startupValidation is StartupValidation.TokenInvalid) {
        InfoBottomSheet(
            title = stringResource(R.string.oauth_expired_title),
            message = stringResource(R.string.oauth_expired_message),
            confirmText = stringResource(R.string.oauth_expired_login_again),
            dismissText = stringResource(R.string.confirm_logout_positive_button),
            dismissible = false,
            onConfirm = onLogin,
            onDismiss = {
                startupValidationHolder.acknowledge()
                onLogout()
            },
        )
    }

    if (sheetsReady) {
        dialogState.messageOptionsParams?.let { params ->
            MessageOptionsDialogContainer(
                params = params,
                snackbarHostState = snackbarHostState,
                onJumpToMessage = onJumpToMessage,
                onSetReplying = chatInputViewModel::setReplying,
                onOpenReplies = sheetNavigationViewModel::openReplies,
                onDismiss = dialogViewModel::dismissMessageOptions,
            )
        }
    }

    if (sheetsReady) {
        dialogState.emoteInfoEmotes?.let { emotes ->
            EmoteInfoDialogContainer(
                emotes = emotes,
                isLoggedIn = isLoggedIn,
                onInsertText = chatInputViewModel::insertText,
                onOpenUrl = onOpenUrl,
                onDismiss = dialogViewModel::dismissEmoteInfo,
            )
        }
    }

    if (sheetsReady) {
        dialogState.userPopupParams?.let { params ->
            UserPopupDialogContainer(
                params = params,
                onMention = chatInputViewModel::mentionUser,
                onWhisper = { userName ->
                    sheetNavigationViewModel.openWhispers()
                    chatInputViewModel.setWhisperTarget(userName)
                },
                onOpenUrl = onOpenUrl,
                onReportChannel = onReportChannel,
                onOpenHistory = { channel, filter ->
                    sheetNavigationViewModel.openHistory(channel, filter)
                    dialogViewModel.dismissUserPopup()
                },
                onDismiss = dialogViewModel::dismissUserPopup,
            )
        }
    }

    if (sheetsReady && inputSheetState is InputSheetState.DebugInfo) {
        val debugInfoViewModel: DebugInfoViewModel = koinViewModel()
        DebugInfoSheet(
            viewModel = debugInfoViewModel,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            onDismiss = sheetNavigationViewModel::closeInputSheet,
        )
    }
}

@Composable
private fun UploadDisclaimerSheet(
    host: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    LocalUriHandler.current
    val disclaimerTemplate = stringResource(R.string.external_upload_disclaimer, host)
    val hostStart = disclaimerTemplate.indexOf(host)
    val annotatedText =
        buildAnnotatedString {
            append(disclaimerTemplate)
            if (hostStart >= 0) {
                addStyle(
                    style =
                        SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline,
                        ),
                    start = hostStart,
                    end = hostStart + host.length,
                )
                addLink(
                    url = LinkAnnotation.Url("https://$host"),
                    start = hostStart,
                    end = hostStart + host.length,
                )
            }
        }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp),
        ) {
            Text(
                text = stringResource(R.string.nuuls_upload_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 16.dp),
            )

            Text(
                text = annotatedText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
            ) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.dialog_cancel))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(onClick = onConfirm, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.dialog_ok))
                }
            }
        }
    }
}

@Composable
private fun ModActionsDialogContainer(
    channel: UserName,
    isStreamActive: Boolean,
    onSendCommand: (String) -> Unit,
    onAnnounce: () -> Unit,
    onDismiss: () -> Unit,
) {
    val viewModel: ModActionsViewModel =
        koinViewModel(
            key = "mod-actions-${channel.value}",
            parameters = { parametersOf(channel) },
        )
    val shieldModeActive by viewModel.shieldModeActive.collectAsStateWithLifecycle()
    ModActionsDialog(
        roomState = viewModel.roomState,
        isBroadcaster = viewModel.isBroadcaster,
        isStreamActive = isStreamActive,
        shieldModeActive = shieldModeActive,
        onSendCommand = onSendCommand,
        onAnnounce = onAnnounce,
        onDismiss = onDismiss,
    )
}

@Composable
private fun MessageOptionsDialogContainer(
    params: MessageOptionsParams,
    snackbarHostState: SnackbarHostState,
    onJumpToMessage: (String, UserName) -> Unit,
    onSetReplying: (Boolean, String, UserName, String) -> Unit,
    onOpenReplies: (String, UserName) -> Unit,
    onDismiss: () -> Unit,
) {
    val viewModel: MessageOptionsViewModel =
        koinViewModel(
            key = params.messageId,
            parameters = { parametersOf(params.messageId, params.channel, params.canModerate, params.canReply) },
        )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val messageCopiedMsg = stringResource(R.string.snackbar_message_copied)
    val messageIdCopiedMsg = stringResource(R.string.snackbar_message_id_copied)

    (state as? MessageOptionsState.Found)?.let { s ->
        MessageOptionsDialog(
            channel = params.channel?.value,
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
            onReply = { onSetReplying(true, s.messageId, s.replyName, s.originalMessage) },
            onReplyToOriginal = { onSetReplying(true, s.rootThreadId, s.rootThreadName ?: s.replyName, s.rootThreadMessage.orEmpty()) },
            onViewThread = { onOpenReplies(s.rootThreadId, s.replyName) },
            onCopy = {
                scope.launch {
                    clipboardManager.setClipEntry(ClipEntry(ClipData.newPlainText("message", s.originalMessage)))
                    snackbarHostState.showSnackbar(messageCopiedMsg)
                }
            },
            onCopyFullMessage = {
                scope.launch {
                    clipboardManager.setClipEntry(ClipEntry(ClipData.newPlainText("full message", params.fullMessage)))
                    snackbarHostState.showSnackbar(messageCopiedMsg)
                }
            },
            onCopyMessageId = {
                scope.launch {
                    clipboardManager.setClipEntry(ClipEntry(ClipData.newPlainText("message id", s.messageId)))
                    snackbarHostState.showSnackbar(messageIdCopiedMsg)
                }
            },
            onDelete = viewModel::deleteMessage,
            onTimeout = viewModel::timeoutUser,
            onBan = viewModel::banUser,
            onUnban = viewModel::unbanUser,
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun EmoteInfoDialogContainer(
    emotes: List<ChatMessageEmote>,
    isLoggedIn: Boolean,
    onInsertText: (String) -> Unit,
    onOpenUrl: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val viewModel: EmoteInfoViewModel =
        koinViewModel(
            key = emotes.joinToString { it.id },
            parameters = { parametersOf(emotes) },
        )
    val sheetNavigationViewModel: SheetNavigationViewModel = koinViewModel()
    val chatInputViewModel: ChatInputViewModel = koinViewModel()
    val sheetState by sheetNavigationViewModel.fullScreenSheetState.collectAsStateWithLifecycle()
    val whisperTarget by chatInputViewModel.whisperTarget.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboard.current
    val scope = rememberCoroutineScope()

    val canUseEmote =
        isLoggedIn &&
            when (sheetState) {
                is FullScreenSheetState.Closed,
                is FullScreenSheetState.Replies,
                -> true

                is FullScreenSheetState.Mention,
                is FullScreenSheetState.Whisper,
                -> whisperTarget != null

                is FullScreenSheetState.History -> false
            }
    EmoteInfoDialog(
        items = viewModel.items,
        isLoggedIn = canUseEmote,
        onUseEmote = { onInsertText("$it ") },
        onCopyEmote = {
            scope.launch {
                clipboardManager.setClipEntry(ClipEntry(ClipData.newPlainText("emote", it)))
            }
        },
        onOpenLink = onOpenUrl,
        onDismiss = onDismiss,
    )
}

@Composable
private fun UserPopupDialogContainer(
    params: UserPopupStateParams,
    onMention: (UserName, DisplayName) -> Unit,
    onWhisper: (UserName) -> Unit,
    onOpenUrl: (String) -> Unit,
    onReportChannel: () -> Unit,
    onOpenHistory: (UserName, String) -> Unit,
    onDismiss: () -> Unit,
) {
    val viewModel: UserPopupViewModel =
        koinViewModel(
            key = "${params.targetUserId}${params.channel?.value.orEmpty()}",
            parameters = { parametersOf(params) },
        )
    val state by viewModel.userPopupState.collectAsStateWithLifecycle()
    UserPopupDialog(
        state = state,
        badges = params.badges.mapIndexed { index, badge -> BadgeUi(badge.url, badge, index) }.toImmutableList(),
        isOwnUser = viewModel.isOwnUser,
        onBlockUser = viewModel::blockUser,
        onUnblockUser = viewModel::unblockUser,
        onDismiss = onDismiss,
        onMention = { name, displayName ->
            onMention(UserName(name), DisplayName(displayName))
        },
        onWhisper = { name -> onWhisper(UserName(name)) },
        onOpenChannel = { userName -> onOpenUrl("https://twitch.tv/$userName") },
        onReport = { _ -> onReportChannel() },
        onMessageHistory = { userName ->
            params.channel?.let { channel ->
                onOpenHistory(channel, "from:$userName")
            }
        },
    )
}
