package com.flxrs.dankchat.main.compose

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.flxrs.dankchat.main.compose.sheets.EmoteMenu

import android.content.ClipData
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.flxrs.dankchat.R
import com.flxrs.dankchat.chat.compose.BadgeUi
import com.flxrs.dankchat.chat.compose.ChatComposable
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
import com.flxrs.dankchat.data.state.GlobalLoadingState
import com.flxrs.dankchat.data.twitch.emote.ChatMessageEmote
import com.flxrs.dankchat.main.compose.FullScreenSheetState
import com.flxrs.dankchat.main.compose.InputSheetState
import com.flxrs.dankchat.main.MainEvent
import com.flxrs.dankchat.main.compose.dialogs.AddChannelDialog
import com.flxrs.dankchat.main.compose.dialogs.EmoteInfoDialog
import com.flxrs.dankchat.main.compose.dialogs.ManageChannelsDialog
import com.flxrs.dankchat.main.compose.dialogs.MessageOptionsDialog
import com.flxrs.dankchat.main.compose.sheets.EmoteMenuSheet
import com.flxrs.dankchat.main.compose.sheets.MentionSheet
import com.flxrs.dankchat.main.compose.sheets.RepliesSheet
import com.flxrs.dankchat.preferences.DankChatPreferenceStore
import com.flxrs.dankchat.preferences.appearance.AppearanceSettingsDataStore
import com.flxrs.dankchat.preferences.developer.DeveloperSettingsDataStore
import com.flxrs.dankchat.preferences.components.DankBackground
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavController,
    isLoggedIn: Boolean,
    onNavigateToSettings: () -> Unit,
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
    val context = LocalContext.current
    val density = LocalDensity.current
    val clipboardManager = LocalClipboard.current
    // Scoped ViewModels - each handles one concern
    val mainScreenViewModel: MainScreenViewModel = koinViewModel()
    val channelManagementViewModel: ChannelManagementViewModel = koinViewModel()
    val channelTabViewModel: ChannelTabViewModel = koinViewModel()
    val channelPagerViewModel: ChannelPagerViewModel = koinViewModel()
    val chatInputViewModel: ChatInputViewModel = koinViewModel()
    val sheetNavigationViewModel: SheetNavigationViewModel = koinViewModel()
    val mentionViewModel: com.flxrs.dankchat.chat.mention.compose.MentionComposeViewModel = koinViewModel()
    val appearanceSettingsDataStore: AppearanceSettingsDataStore = koinInject()
    val developerSettingsDataStore: DeveloperSettingsDataStore = koinInject()
    val preferenceStore: DankChatPreferenceStore = koinInject()
    val mainEventBus: MainEventBus = koinInject()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    val developerSettings by developerSettingsDataStore.settings.collectAsStateWithLifecycle(initialValue = developerSettingsDataStore.current())
    val isRepeatedSendEnabled = developerSettings.repeatedSending

    var keyboardHeightPx by remember(isLandscape) {
        val persisted = if (isLandscape) preferenceStore.keyboardHeightLandscape else preferenceStore.keyboardHeightPortrait
        mutableIntStateOf(persisted)
    }
    
    val ime = WindowInsets.ime
    val navBars = WindowInsets.navigationBars
    val imeTarget = WindowInsets.imeAnimationTarget
    val currentImeHeight = (ime.getBottom(density) - navBars.getBottom(density)).coerceAtLeast(0)
    
    // Target height for stability during opening animation
    val targetImeHeight = (imeTarget.getBottom(density) - navBars.getBottom(density)).coerceAtLeast(0)
    val isImeOpening = targetImeHeight > 0
    
    val imeHeightState = androidx.compose.runtime.rememberUpdatedState(currentImeHeight)
    val isImeVisible = WindowInsets.isImeVisible

    LaunchedEffect(isLandscape, density) {
        snapshotFlow { 
            (imeTarget.getBottom(density) - navBars.getBottom(density)).coerceAtLeast(0)
        }
            .debounce(300)
            .collect { height ->
                val minHeight = with(density) { 100.dp.toPx() }
                if (height > minHeight) {
                    keyboardHeightPx = height
                    if (isLandscape) {
                        preferenceStore.keyboardHeightLandscape = height
                    } else {
                        preferenceStore.keyboardHeightPortrait = height
                    }
                }
            }
    }

    LaunchedEffect(isImeVisible) {
        if (isImeVisible) {
            chatInputViewModel.setEmoteMenuOpen(false)
        }
    }

    val inputState by chatInputViewModel.uiState(sheetNavigationViewModel.fullScreenSheetState, mentionViewModel.currentTab).collectAsStateWithLifecycle()
    val isKeyboardVisible = isImeVisible || isImeOpening
    var backProgress by remember { mutableStateOf(0f) }

    // Disable if Keyboard is open or opening to prevent conflict
    PredictiveBackHandler(enabled = inputState.isEmoteMenuOpen && !isKeyboardVisible) { progress ->
        try {
            progress.collect { event ->
                backProgress = event.progress
            }
            chatInputViewModel.setEmoteMenuOpen(false)
            backProgress = 0f
        } catch (e: Exception) {
            backProgress = 0f
        }
    }

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
    val inputSheetState by sheetNavigationViewModel.inputSheetState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        mainEventBus.events.collect { event ->
            when (event) {
                is MainEvent.LogOutRequested -> showLogoutDialog = true
                else                         -> Unit
            }
        }
    }

    // Handle Login Result
    val navBackStackEntry = navController.currentBackStackEntry
    val loginSuccess by navBackStackEntry?.savedStateHandle?.getStateFlow<Boolean?>("login_success", null)?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(null) }
    LaunchedEffect(loginSuccess) {
        if (loginSuccess == true) {
            channelManagementViewModel.reconnect()
            mainScreenViewModel.reloadGlobalData()
            navBackStackEntry?.savedStateHandle?.remove<Boolean>("login_success")
            scope.launch {
                val name = preferenceStore.userName
                val message = if (name != null) {
                    context.getString(R.string.snackbar_login, name)
                } else {
                    context.getString(R.string.login) // Fallback
                }
                snackbarHostState.showSnackbar(message)
            }
        }
    }

    // Handle data loading errors
    val loadingState by mainScreenViewModel.globalLoadingState.collectAsStateWithLifecycle()
    LaunchedEffect(loadingState) {
        if (loadingState is GlobalLoadingState.Failed) {
            val state = loadingState as GlobalLoadingState.Failed
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = state.message,
                    actionLabel = context.getString(R.string.snackbar_retry)
                )
            }
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
            onApplyChanges = channelManagementViewModel::applyChanges,
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

    val isFullscreen by mainScreenViewModel.isFullscreen.collectAsStateWithLifecycle()
    val showAppBar by mainScreenViewModel.showAppBar.collectAsStateWithLifecycle()
    val showInputState by mainScreenViewModel.showInput.collectAsStateWithLifecycle()

    val currentDestination = navController.currentBackStackEntryAsState().value?.destination?.route

    val pagerState by channelPagerViewModel.uiState.collectAsStateWithLifecycle()

    val composePagerState = rememberPagerState(
        initialPage = pagerState.currentPage,
        pageCount = { pagerState.channels.size }
    )
    var inputHeightPx by remember { mutableIntStateOf(0) }
    var inputTopY by remember { mutableStateOf(0f) }
    var containerHeight by remember { mutableIntStateOf(0) }
    val inputHeightDp = with(density) { inputHeightPx.toDp() }
    val sheetBottomPadding = with(density) { (containerHeight - inputTopY).toDp() }

    // Track keyboard visibility - clear focus only when keyboard is fully closed
    val focusManager = LocalFocusManager.current
    val imeAnimationTarget = WindowInsets.imeAnimationTarget
    val isKeyboardAtBottom = imeAnimationTarget.getBottom(density) == 0

    LaunchedEffect(isKeyboardAtBottom) {
        if (isKeyboardAtBottom) {
            focusManager.clearFocus()
        }
    }

    // Sync Compose pager with ViewModel state
    LaunchedEffect(pagerState.currentPage) {
        if (!composePagerState.isScrollInProgress &&
            composePagerState.currentPage != pagerState.currentPage &&
            pagerState.currentPage < pagerState.channels.size
        ) {
            composePagerState.scrollToPage(pagerState.currentPage)
        }
    }

    // Update ViewModel when user swipes
    LaunchedEffect(composePagerState.currentPage) {
        if (composePagerState.currentPage != pagerState.currentPage) {
            channelPagerViewModel.onPageChanged(composePagerState.currentPage)
        }
    }

    val systemBarsPaddingModifier = if (isFullscreen) Modifier else Modifier.statusBarsPadding()

    Box(modifier = Modifier
        .fillMaxSize()
        .onGloballyPositioned { containerHeight = it.size.height }
    ) {
        val currentImeHeightDp = with(density) { currentImeHeight.toDp() }
        val targetImeHeightDp = with(density) { targetImeHeight.toDp() }
        
        val targetMenuHeight = if (keyboardHeightPx > 0) {
            with(density) { keyboardHeightPx.toDp() }
        } else {
            if (isLandscape) 200.dp else 350.dp
        }.coerceAtLeast(if (isLandscape) 150.dp else 250.dp)
        
        val scaffoldBottomPadding = max(
            targetImeHeightDp, 
            max(currentImeHeightDp, if (inputState.isEmoteMenuOpen) targetMenuHeight else 0.dp)
        )

        Scaffold(
            modifier = modifier
                .fillMaxSize()
                .then(systemBarsPaddingModifier)
                .padding(bottom = scaffoldBottomPadding),
            contentWindowInsets = WindowInsets.statusBars,
            topBar = {
                if (tabState.tabs.isEmpty()) {
                    return@Scaffold
                }

                AnimatedVisibility(
                    visible = showAppBar && !isFullscreen,
                    enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
                ) {
                    MainAppBar(
                        isLoggedIn = isLoggedIn,
                        totalMentionCount = tabState.tabs.sumOf { it.mentionCount },
                        onAddChannel = { showAddChannelDialog = true },
                        onOpenMentions = { sheetNavigationViewModel.openMentions() },
                        onOpenWhispers = { sheetNavigationViewModel.openWhispers() },
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
                        onOpenSettings = onNavigateToSettings
                    )
                }
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                AnimatedVisibility(
                    visible = showInputState && !isFullscreen,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        ChatInputLayout(
                            textFieldState = chatInputViewModel.textFieldState,
                            inputState = inputState.inputState,
                            enabled = inputState.enabled,
                            canSend = inputState.canSend,
                            showReplyOverlay = inputState.showReplyOverlay,
                            replyName = inputState.replyName,
                            isEmoteMenuOpen = inputState.isEmoteMenuOpen,
                            onSend = chatInputViewModel::sendMessage,
                            onLastMessageClick = chatInputViewModel::getLastMessage,
                            onEmoteClick = {
                                if (!inputState.isEmoteMenuOpen) {
                                    keyboardController?.hide()
                                    chatInputViewModel.setEmoteMenuOpen(true)
                                } else {
                                    keyboardController?.show()
                                }
                            },
                            onReplyDismiss = {
                                chatInputViewModel.setReplying(false)
                            },
                            modifier = Modifier.onGloballyPositioned { coordinates ->
                                inputHeightPx = coordinates.size.height
                                inputTopY = coordinates.positionInRoot().y
                            }
                        )
                    }
                }
            }
        ) { paddingValues ->
            // Main content
            Box(modifier = Modifier.fillMaxSize()) {
                val showFullScreenLoading = tabState.loading && tabState.tabs.isEmpty()
                DankBackground(visible = showFullScreenLoading)
                if (showFullScreenLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(paddingValues)
                    )
                    return@Scaffold
                }
                if (tabState.tabs.isEmpty() && !tabState.loading) {
                    EmptyStateContent(
                        isLoggedIn = isLoggedIn,
                        onAddChannel = { showAddChannelDialog = true },
                        onLogin = onLogin,
                        onToggleAppBar = mainScreenViewModel::toggleAppBar,
                        onToggleFullscreen = mainScreenViewModel::toggleFullscreen,
                        onToggleInput = mainScreenViewModel::toggleInput,
                        modifier = Modifier.padding(paddingValues)
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        if (tabState.loading) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                        AnimatedVisibility(
                            visible = !isFullscreen,
                            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
                        ) {
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
                        }
                        HorizontalPager(
                            state = composePagerState,
                            modifier = Modifier.fillMaxSize(),
                            key = { index -> pagerState.channels.getOrNull(index)?.value ?: index }
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
                                            canReply = isLoggedIn,
                                            canCopy = true
                                        )
                                    },
                                    onEmoteClick = { emotes ->
                                        emoteInfoEmotes = emotes
                                    },
                                    onReplyClick = { replyMessageId, replyName ->
                                        sheetNavigationViewModel.openReplies(replyMessageId, replyName)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Emote Menu Layer
        // Always draw if open OR keyboard is present to be ready underneath
        if (inputState.isEmoteMenuOpen || isKeyboardVisible) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(targetMenuHeight)
                    .graphicsLayer {
                        val scale = 1f - (backProgress * 0.1f)
                        scaleX = scale
                        scaleY = scale
                        alpha = 1f - backProgress
                        translationY = backProgress * 100f
                    }
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                EmoteMenu(
                    onEmoteClick = { code, _ ->
                        chatInputViewModel.insertText("$code ")
                    }
                )
            }
        }

        // Fullscreen Overlay Sheets
        androidx.compose.animation.AnimatedVisibility(
            visible = fullScreenSheetState !is FullScreenSheetState.Closed,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = sheetBottomPadding)
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                when (val state = fullScreenSheetState) {
                    is FullScreenSheetState.Closed -> Unit
                    is FullScreenSheetState.Mention -> {
                        MentionSheet(
                            mentionViewModel = mentionViewModel,
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
                                    canReply = isLoggedIn,
                                    canCopy = false
                                )
                            },
                            onEmoteClick = { emotes ->
                                emoteInfoEmotes = emotes
                            }
                        )
                    }
                    is FullScreenSheetState.Whisper -> {
                        MentionSheet(
                            mentionViewModel = mentionViewModel,
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
                                    canReply = isLoggedIn,
                                    canCopy = false
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
                            onDismiss = {
                                sheetNavigationViewModel.closeFullScreenSheet()
                                chatInputViewModel.setReplying(false)
                            },
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
                                    canReply = isLoggedIn,
                                    canCopy = true
                                )
                            }
                        )
                    }
                }
            }
        }

        if (showInputState && !isFullscreen && isKeyboardVisible) {
            SuggestionDropdown(
                suggestions = inputState.suggestions,
                onSuggestionClick = chatInputViewModel::applySuggestion,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(bottom = inputHeightDp + 2.dp)
            )
        }
    }
}