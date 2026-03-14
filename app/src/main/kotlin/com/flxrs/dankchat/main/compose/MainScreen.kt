package com.flxrs.dankchat.main.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flxrs.dankchat.chat.compose.BadgeUi
import com.flxrs.dankchat.chat.compose.ChatComposable
import com.flxrs.dankchat.data.twitch.emote.ChatMessageEmote
import com.flxrs.dankchat.preferences.components.DankBackground
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.compose.koinInject

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.res.stringResource
import com.flxrs.dankchat.R
import com.flxrs.dankchat.main.MainEvent
import com.flxrs.dankchat.main.compose.dialogs.AddChannelDialog
import com.flxrs.dankchat.main.compose.dialogs.ManageChannelsDialog
import com.flxrs.dankchat.chat.user.compose.UserPopupDialog
import com.flxrs.dankchat.chat.user.UserPopupComposeViewModel
import com.flxrs.dankchat.chat.user.UserPopupStateParams
import com.flxrs.dankchat.data.UserId
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.DisplayName

import androidx.compose.ui.platform.LocalUriHandler
import androidx.navigation.NavController
import com.flxrs.dankchat.main.compose.dialogs.MessageOptionsDialog
import com.flxrs.dankchat.main.compose.dialogs.EmoteInfoDialog
import com.flxrs.dankchat.chat.message.compose.MessageOptionsParams
import com.flxrs.dankchat.chat.message.compose.MessageOptionsComposeViewModel
import com.flxrs.dankchat.chat.message.compose.MessageOptionsState
import com.flxrs.dankchat.chat.emote.compose.EmoteInfoComposeViewModel
import com.flxrs.dankchat.main.compose.sheets.RepliesSheet
import com.flxrs.dankchat.main.compose.sheets.MentionSheet
import com.flxrs.dankchat.preferences.appearance.AppearanceSettingsDataStore
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavController,
    isLoggedIn: Boolean,
    onNavigateToSettings: () -> Unit,
    onMessageLongClick: (messageId: String, channel: String?, fullMessage: String) -> Unit,
    onEmoteClick: (List<ChatMessageEmote>) -> Unit,
    onLogin: () -> Unit,
    onRelogin: () -> Unit,
    onLogout: () -> Unit,
    onOpenChannel: () -> Unit,
    onReportChannel: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onReloadEmotes: () -> Unit,
    onReconnect: () -> Unit,
    onCaptureImage: () -> Unit,
    onCaptureVideo: () -> Unit,
    onChooseMedia: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Scoped ViewModels - each handles one concern
    val mainScreenViewModel: MainScreenViewModel = koinViewModel()
    val channelManagementViewModel: ChannelManagementViewModel = koinViewModel()
    val channelTabViewModel: ChannelTabViewModel = koinViewModel()
    val channelPagerViewModel: ChannelPagerViewModel = koinViewModel()
    val chatInputViewModel: ChatInputViewModel = koinViewModel()
    val sheetNavigationViewModel: SheetNavigationViewModel = koinViewModel()
    val appearanceSettingsDataStore: AppearanceSettingsDataStore = koinInject()
    val mainEventBus: MainEventBus = koinInject()
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current

    var showAddChannelDialog by remember { mutableStateOf(false) }
    var showManageChannelsDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showRemoveChannelDialog by remember { mutableStateOf(false) }
    var showBlockChannelDialog by remember { mutableStateOf(false) }
    var showClearChatDialog by remember { mutableStateOf(false) }
    var userPopupParams by remember { mutableStateOf<UserPopupStateParams?>(null) }
    var messageOptionsParams by remember { mutableStateOf<MessageOptionsParams?>(null) }
    var emoteInfoEmotes by remember { mutableStateOf<List<ChatMessageEmote>?>(null) }

    val fullScreenSheetState by sheetNavigationViewModel.fullScreenSheetState.collectAsStateWithLifecycle()

    LaunchedEffect(fullScreenSheetState) {
        chatInputViewModel.setReplying(fullScreenSheetState is FullScreenSheetState.Replies)
    }

    LaunchedEffect(Unit) {
        mainEventBus.events.collect { event ->
            if (event is MainEvent.LogOutRequested) {
                showLogoutDialog = true
            }
        }
    }

    when (val state = fullScreenSheetState) {
        is FullScreenSheetState.Closed -> Unit
        is FullScreenSheetState.Mention -> {
            MentionSheet(
                initialisWhisperTab = false,
                appearanceSettingsDataStore = appearanceSettingsDataStore,
                onDismiss = sheetNavigationViewModel::closeFullScreenSheet,
                onUserClick = { userId, userName, displayName, channel, badges, _ ->
                    userPopupParams = UserPopupStateParams(
                        targetUserId = userId?.let { UserId(it) } ?: UserId(""),
                        targetUserName = UserName(userName),
                        targetDisplayName = DisplayName(displayName),
                        channel = channel?.let { UserName(it) },
                        badges = badges.map { it.badge }
                    )
                },
                onMessageLongClick = { messageId, channel, fullMessage ->
                    messageOptionsParams = MessageOptionsParams(
                        messageId = messageId,
                        channel = channel?.let { UserName(it) },
                        fullMessage = fullMessage,
                        canModerate = isLoggedIn,
                        canReply = isLoggedIn
                    )
                },
                onEmoteClick = { emotes ->
                    emoteInfoEmotes = emotes
                }
            )
        }
        is FullScreenSheetState.Whisper -> {
            MentionSheet(
                initialisWhisperTab = true,
                appearanceSettingsDataStore = appearanceSettingsDataStore,
                onDismiss = sheetNavigationViewModel::closeFullScreenSheet,
                onUserClick = { userId, userName, displayName, channel, badges, _ ->
                    userPopupParams = UserPopupStateParams(
                        targetUserId = userId?.let { UserId(it) } ?: UserId(""),
                        targetUserName = UserName(userName),
                        targetDisplayName = DisplayName(displayName),
                        channel = channel?.let { UserName(it) },
                        badges = badges.map { it.badge }
                    )
                },
                onMessageLongClick = { messageId, channel, fullMessage ->
                    messageOptionsParams = MessageOptionsParams(
                        messageId = messageId,
                        channel = channel?.let { UserName(it) },
                        fullMessage = fullMessage,
                        canModerate = isLoggedIn,
                        canReply = isLoggedIn
                    )
                },
                onEmoteClick = { emotes ->
                    emoteInfoEmotes = emotes
                }
            )
        }
        is FullScreenSheetState.Replies -> {
            RepliesSheet(
                rootMessageId = state.replyMessageId,
                appearanceSettingsDataStore = appearanceSettingsDataStore,
                onDismiss = sheetNavigationViewModel::closeFullScreenSheet,
                onUserClick = { userId, userName, displayName, channel, badges, _ ->
                    userPopupParams = UserPopupStateParams(
                        targetUserId = userId?.let { UserId(it) } ?: UserId(""),
                        targetUserName = UserName(userName),
                        targetDisplayName = DisplayName(displayName),
                        channel = channel?.let { UserName(it) },
                        badges = badges.map { it.badge }
                    )
                },
                onMessageLongClick = { messageId, channel, fullMessage ->
                    messageOptionsParams = MessageOptionsParams(
                        messageId = messageId,
                        channel = channel?.let { UserName(it) },
                        fullMessage = fullMessage,
                        canModerate = isLoggedIn,
                        canReply = isLoggedIn
                    )
                }
            )
        }
    }

    val tabState = channelTabViewModel.uiState.collectAsStateWithLifecycle().value
    val activeChannel = tabState.tabs.getOrNull(tabState.selectedIndex)?.channel

    if (showAddChannelDialog) {
        AddChannelDialog(
            onDismiss = { showAddChannelDialog = false },
            onAddChannel = { 
                channelManagementViewModel.addChannel(it)
                showAddChannelDialog = false
            }
        )
    }

    if (showManageChannelsDialog) {
        val channels by channelManagementViewModel.channels.collectAsStateWithLifecycle()
        ManageChannelsDialog(
            channels = channels,
            onRemoveChannel = channelManagementViewModel::removeChannel,
            onRenameChannel = channelManagementViewModel::renameChannel,
            onReorder = channelManagementViewModel::reorderChannels,
            onDismiss = { showManageChannelsDialog = false }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(stringResource(R.string.confirm_logout_title)) },
            text = { Text(stringResource(R.string.confirm_logout_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onLogout()
                        showLogoutDialog = false
                    }
                ) {
                    Text(stringResource(R.string.confirm_logout_positive_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }

    if (showRemoveChannelDialog && activeChannel != null) {
        AlertDialog(
            onDismissRequest = { showRemoveChannelDialog = false },
            title = { Text(stringResource(R.string.confirm_channel_removal_title)) },
            text = { Text(stringResource(R.string.confirm_channel_removal_message_named, activeChannel)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        channelManagementViewModel.removeChannel(activeChannel)
                        showRemoveChannelDialog = false
                    }
                ) {
                    Text(stringResource(R.string.confirm_channel_removal_positive_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveChannelDialog = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }

    if (showBlockChannelDialog && activeChannel != null) {
        AlertDialog(
            onDismissRequest = { showBlockChannelDialog = false },
            title = { Text(stringResource(R.string.confirm_channel_block_title)) },
            text = { Text(stringResource(R.string.confirm_channel_block_message_named, activeChannel)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        channelManagementViewModel.blockChannel(activeChannel)
                        showBlockChannelDialog = false
                    }
                ) {
                    Text(stringResource(R.string.confirm_user_block_positive_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlockChannelDialog = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }

    if (showClearChatDialog && activeChannel != null) {
        AlertDialog(
            onDismissRequest = { showClearChatDialog = false },
            title = { Text(stringResource(R.string.clear_chat)) },
            text = { Text(stringResource(R.string.confirm_user_delete_message)) }, // Reuse message deletion text or find better one
            confirmButton = {
                TextButton(
                    onClick = {
                        channelManagementViewModel.clearChat(activeChannel)
                        showClearChatDialog = false
                    }
                ) {
                    Text(stringResource(R.string.dialog_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearChatDialog = false }) {
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
                hasReplyThread = s.hasReplyThread,
                onReply = { 
                    sheetNavigationViewModel.openReplies(s.messageId)
                },
                onViewThread = { 
                    sheetNavigationViewModel.openReplies(s.rootThreadId)
                },
                onCopy = { /* TODO: Implement copy to clipboard */ },
                onMoreActions = { /* TODO: Implement more actions */ },
                onDelete = viewModel::deleteMessage,
                onTimeout = viewModel::timeoutUser,
                onBan = viewModel::banUser,
                onUnban = viewModel::unbanUser,
                onDismiss = { messageOptionsParams = null }
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
            onDismiss = { emoteInfoEmotes = null }
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
            onDismiss = { userPopupParams = null },
            onMention = { name, _ -> 
                chatInputViewModel.insertText("@$name ") 
            },
            onWhisper = { name ->
                sheetNavigationViewModel.openWhispers()
                chatInputViewModel.updateInputText("/w $name ")
            },
            onOpenChannel = { _ -> onOpenChannel() },
            onReport = { _ ->
                onReportChannel() 
            }
        )
    }

    val pagerState by channelPagerViewModel.uiState.collectAsStateWithLifecycle()
    val inputState by chatInputViewModel.uiState.collectAsStateWithLifecycle()

    val composePagerState = rememberPagerState(
        initialPage = pagerState.currentPage,
        pageCount = { pagerState.channels.size }
    )
    val density = LocalDensity.current
    var inputHeight by remember { mutableStateOf(0.dp) }

    // Track keyboard visibility - hide immediately when closing animation starts
    val focusManager = LocalFocusManager.current
    val imeAnimationTarget = WindowInsets.imeAnimationTarget
    val isKeyboardVisible = WindowInsets.isImeVisible &&
            imeAnimationTarget.getBottom(density) > 0

    LaunchedEffect(isKeyboardVisible) {
        if (!isKeyboardVisible) {
            focusManager.clearFocus()
        }
    }

    // Sync Compose pager with ViewModel state
    LaunchedEffect(pagerState.currentPage) {
        if (composePagerState.currentPage != pagerState.currentPage &&
            pagerState.currentPage < pagerState.channels.size
        ) {
            composePagerState.animateScrollToPage(pagerState.currentPage)
        }
    }

    // Update ViewModel when user swipes
    LaunchedEffect(composePagerState.currentPage) {
        if (composePagerState.currentPage != pagerState.currentPage) {
            channelPagerViewModel.onPageChanged(composePagerState.currentPage)
        }
    }

    Scaffold(
        topBar = {
            if (tabState.tabs.isEmpty()) {
                return@Scaffold
            }

            MainAppBar(
                isLoggedIn = isLoggedIn,
                totalMentionCount = tabState.tabs.sumOf { it.mentionCount },
                onAddChannel = { showAddChannelDialog = true },
                onOpenMentions = { sheetNavigationViewModel.openMentions() },
                onLogin = onLogin,
                onRelogin = onRelogin,
                onLogout = { showLogoutDialog = true },
                onManageChannels = { showManageChannelsDialog = true },
                onOpenChannel = onOpenChannel,
                onRemoveChannel = { showRemoveChannelDialog = true },
                onReportChannel = onReportChannel,
                onBlockChannel = { showBlockChannelDialog = true },
                onReloadEmotes = {
                    activeChannel?.let { channelManagementViewModel.reloadEmotes(it) }
                    onReloadEmotes()
                },
                onReconnect = {
                    channelManagementViewModel.reconnect()
                    onReconnect()
                },
                onClearChat = { showClearChatDialog = true },
                onCaptureImage = onCaptureImage,
                onCaptureVideo = onCaptureVideo,
                onChooseMedia = onChooseMedia,
                onOpenSettings = onNavigateToSettings
            )
        },
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            DankBackground(visible = tabState.loading)
            if (tabState.loading) {
                return@Scaffold
            }
            if (tabState.tabs.isEmpty()) {
                EmptyStateContent(
                    isLoggedIn = isLoggedIn,
                    onAddChannel = { showAddChannelDialog = true },
                    onLogin = onLogin,
                    onToggleAppBar = { /* TODO */ },
                    onToggleFullscreen = { /* TODO */ },
                    onToggleInput = { /* TODO */ },
                    modifier = Modifier.padding(paddingValues)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // Tabs - Single state from ChannelTabViewModel
                    ChannelTabRow(
                        tabs = tabState.tabs,
                        selectedIndex = tabState.selectedIndex,
                        onTabSelected = {
                            channelTabViewModel.selectTab(it)
                            scope.launch {
                                composePagerState.animateScrollToPage(it)
                            }
                        }
                    )

                    // Chat pager - State from ChannelPagerViewModel
                    HorizontalPager(
                        state = composePagerState,
                        modifier = Modifier.weight(1f)
                    ) { page ->
                        if (page in pagerState.channels.indices) {
                            val channel = pagerState.channels[page]
                            ChatComposable(
                                channel = channel,
                                onUserClick = { userId, userName, displayName, channel, badges, _ ->
                                    userPopupParams = UserPopupStateParams(
                                        targetUserId = userId?.let { UserId(it) } ?: UserId(""),
                                        targetUserName = UserName(userName),
                                        targetDisplayName = DisplayName(displayName),
                                        channel = channel?.let { UserName(it) },
                                        badges = badges.map { it.badge }
                                    )
                                },
                                onMessageLongClick = { messageId, channel, fullMessage ->
                                    messageOptionsParams = MessageOptionsParams(
                                        messageId = messageId,
                                        channel = channel?.let { UserName(it) },
                                        fullMessage = fullMessage,
                                        canModerate = isLoggedIn,
                                        canReply = isLoggedIn
                                    )
                                },
                                onEmoteClick = { emotes ->
                                    emoteInfoEmotes = emotes
                                },
                                onReplyClick = { replyMessageId ->
                                    sheetNavigationViewModel.openReplies(replyMessageId)
                                }
                            )
                        }
                    }

                    // Input - State from ChatInputViewModel
                    ChatInputLayout(
                        textFieldState = chatInputViewModel.textFieldState,
                        inputState = inputState.inputState,
                        enabled = inputState.enabled,
                        canSend = inputState.canSend,
                        onSend = chatInputViewModel::sendMessage,
                        onEmoteClick = { sheetNavigationViewModel.openEmoteSheet() },
                        modifier = Modifier.onGloballyPositioned { coordinates ->
                            inputHeight = with(density) { coordinates.size.height.toDp() }
                        }
                    )
                }
            }

            // Suggestion dropdown floats above input field
            if (isKeyboardVisible) {
                SuggestionDropdown(
                    suggestions = inputState.suggestions,
                    onSuggestionClick = chatInputViewModel::applySuggestion,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(paddingValues)
                        .padding(bottom = inputHeight)
                )
            }
        }
    }
}