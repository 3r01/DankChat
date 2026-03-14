package com.flxrs.dankchat.main.compose

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.DpOffset
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.flxrs.dankchat.main.compose.sheets.EmoteMenu

import android.content.ClipData
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
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
import androidx.compose.ui.draw.drawWithContent
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
import com.flxrs.dankchat.data.repo.channel.ChannelRepository
import com.flxrs.dankchat.data.repo.chat.UserStateRepository
import com.flxrs.dankchat.main.compose.dialogs.AddChannelDialog
import com.flxrs.dankchat.main.compose.dialogs.EmoteInfoDialog
import com.flxrs.dankchat.main.compose.dialogs.ManageChannelsDialog
import com.flxrs.dankchat.main.compose.dialogs.MessageOptionsDialog
import com.flxrs.dankchat.main.compose.dialogs.RoomStateDialog
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
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
    val streamViewModel: StreamViewModel = koinViewModel()
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

    // Close emote menu when keyboard opens, but wait for keyboard to reach
    // persisted height so scaffold padding doesn't jump during the transition
    LaunchedEffect(isImeVisible) {
        if (isImeVisible) {
            if (keyboardHeightPx > 0) {
                snapshotFlow { imeHeightState.value }
                    .first { it >= keyboardHeightPx }
            }
            chatInputViewModel.setEmoteMenuOpen(false)
        }
    }

    val inputState by chatInputViewModel.uiState(sheetNavigationViewModel.fullScreenSheetState, mentionViewModel.currentTab).collectAsStateWithLifecycle()
    val isKeyboardVisible = isImeVisible || isImeOpening
    var backProgress by remember { mutableStateOf(0f) }

    // Stream state
    val currentStream by streamViewModel.currentStreamedChannel.collectAsStateWithLifecycle()
    val hasStreamData by chatInputViewModel.hasStreamData.collectAsStateWithLifecycle()
    var streamHeightDp by remember { mutableStateOf(0.dp) }
    LaunchedEffect(currentStream) {
        if (currentStream == null) streamHeightDp = 0.dp
    }



    // Only intercept when menu is visible AND keyboard is fully GONE
    // Using currentImeHeight == 0 ensures we don't intercept during system keyboard close gestures
    PredictiveBackHandler(enabled = inputState.isEmoteMenuOpen && currentImeHeight == 0) { progress ->
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
    var showOverflowMenu by remember { mutableStateOf(false) }
    var overflowInitialMenu by remember { mutableStateOf<AppBarMenu>(AppBarMenu.Main) }
    var userPopupParams by remember { mutableStateOf<UserPopupStateParams?>(null) }
    var messageOptionsParams by remember { mutableStateOf<MessageOptionsParams?>(null) }
    var emoteInfoEmotes by remember { mutableStateOf<List<ChatMessageEmote>?>(null) }
    var showRoomStateDialog by remember { mutableStateOf(false) }

    val channelRepository: ChannelRepository = koinInject()
    val userStateRepository: UserStateRepository = koinInject()

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

    val roomStateChannel = inputState.activeChannel
    if (showRoomStateDialog && roomStateChannel != null) {
        RoomStateDialog(
            roomState = channelRepository.getRoomState(roomStateChannel),
            onSendCommand = { command ->
                chatInputViewModel.trySendMessageOrCommand(command)
            },
            onDismiss = { showRoomStateDialog = false }
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

    // Clear focus when keyboard fully reaches the bottom, but not when
    // switching to the emote menu. Prevents keyboard from reopening when
    // returning from background.
    val focusManager = LocalFocusManager.current
    LaunchedEffect(Unit) {
        snapshotFlow { imeHeightState.value == 0 && !inputState.isEmoteMenuOpen }
            .distinctUntilChanged()
            .collect { shouldClearFocus ->
                if (shouldClearFocus) {
                    focusManager.clearFocus()
                }
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

    Box(modifier = Modifier
        .fillMaxSize()
        .onGloballyPositioned { containerHeight = it.size.height }
    ) {
        // Menu content height matches keyboard content area (above nav bar)
        val targetMenuHeight = if (keyboardHeightPx > 0) {
            with(density) { keyboardHeightPx.toDp() }
        } else {
            if (isLandscape) 200.dp else 350.dp
        }.coerceAtLeast(if (isLandscape) 150.dp else 250.dp)

        // Total menu height includes nav bar so the menu visually matches
        // the keyboard's full extent. Without this, the menu is shorter than
        // the keyboard by navBarHeight, causing a visible lag during reveal.
        val navBarHeightDp = with(density) { navBars.getBottom(density).toDp() }
        val totalMenuHeight = targetMenuHeight + navBarHeightDp
        
        val currentImeDp = with(density) { currentImeHeight.toDp() }
        val emoteMenuPadding = if (inputState.isEmoteMenuOpen) targetMenuHeight else 0.dp
        val scaffoldBottomPadding = max(currentImeDp, emoteMenuPadding)

        Scaffold(
            modifier = modifier
                .fillMaxSize()
                .padding(bottom = scaffoldBottomPadding),
            contentWindowInsets = WindowInsets(0),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (showInputState) {
                        ChatInputLayout(
                            textFieldState = chatInputViewModel.textFieldState,
                            inputState = inputState.inputState,
                            enabled = inputState.enabled,
                            canSend = inputState.canSend,
                            showReplyOverlay = inputState.showReplyOverlay,
                            replyName = inputState.replyName,
                            isEmoteMenuOpen = inputState.isEmoteMenuOpen,
                            helperText = inputState.helperText,
                            isFullscreen = isFullscreen,
                            isModerator = userStateRepository.isModeratorInChannel(inputState.activeChannel),
                            isStreamActive = currentStream != null,
                            hasStreamData = hasStreamData,
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
                            onToggleFullscreen = mainScreenViewModel::toggleFullscreen,
                            onToggleInput = mainScreenViewModel::toggleInput,
                            onToggleStream = {
                                activeChannel?.let { streamViewModel.toggleStream(it) }
                            },
                            onChangeRoomState = { showRoomStateDialog = true },
                            modifier = Modifier.onGloballyPositioned { coordinates ->
                                inputHeightPx = coordinates.size.height
                                inputTopY = coordinates.positionInRoot().y
                            }
                        )
                    }

                    // Sticky helper text + nav bar spacer when input is hidden
                    if (!showInputState) {
                        val helperText = inputState.helperText
                        if (!helperText.isNullOrEmpty()) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = helperText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .navigationBarsPadding()
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Start
                                )
                            }
                        }
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
                        modifier = Modifier.padding(paddingValues)
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
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
                                    },
                                    showInput = showInputState,
                                    isFullscreen = isFullscreen,
                                    hasHelperText = !inputState.helperText.isNullOrEmpty(),
                                    onRecover = {
                                        if (isFullscreen) mainScreenViewModel.toggleFullscreen()
                                        if (!showInputState) mainScreenViewModel.toggleInput()
                                    },
                                    contentPadding = PaddingValues(
                                        top = maxOf(with(density) { WindowInsets.statusBars.getTop(density).toDp() }, streamHeightDp) + 56.dp
                                    ),
                                    onScrollDirectionChanged = { }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Stream View layer
        currentStream?.let { channel ->
            StreamView(
                channel = channel,
                streamViewModel = streamViewModel,
                onClose = {
                    focusManager.clearFocus()
                    streamViewModel.closeStream()
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        streamHeightDp = with(density) { coordinates.size.height.toDp() }
                    }
            )
        }

        // Status bar scrim
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(with(density) { WindowInsets.statusBars.getTop(density).toDp() })
                .background(if (currentStream != null) Color.Black else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
        )

        // Floating Toolbars - collapsible tabs (expand on swipe) + actions
        if (tabState.tabs.isNotEmpty()) {
            var isTabsExpanded by remember { mutableStateOf(false) }

            val maxVisibleTabs = 3
            val totalTabs = tabState.tabs.size
            val hasOverflow = totalTabs > maxVisibleTabs
            val selectedIndex = tabState.selectedIndex
            val visibleStartIndex = when {
                !hasOverflow -> 0
                selectedIndex <= 0 -> 0
                selectedIndex >= totalTabs - 1 -> maxOf(0, totalTabs - maxVisibleTabs)
                else -> selectedIndex - 1
            }
            val visibleEndIndex = minOf(visibleStartIndex + maxVisibleTabs, totalTabs)

            // Expand tabs when pager is swiped in a direction with more channels
            LaunchedEffect(composePagerState.isScrollInProgress) {
                if (composePagerState.isScrollInProgress && hasOverflow) {
                    // Wait for swipe direction to establish
                    val offset = snapshotFlow { composePagerState.currentPageOffsetFraction }
                        .first { it != 0f }
                    val current = composePagerState.currentPage
                    val swipingForward = offset > 0  // towards higher index
                    val swipingBackward = offset < 0 // towards lower index
                    if ((swipingForward && current < totalTabs - 1) || (swipingBackward && current > 0)) {
                        isTabsExpanded = true
                    }
                }
            }

            // Auto-collapse after scroll stops + 2s delay
            LaunchedEffect(isTabsExpanded, composePagerState.isScrollInProgress) {
                if (isTabsExpanded && !composePagerState.isScrollInProgress) {
                    delay(2000)
                    isTabsExpanded = false
                }
            }

            // Dismiss scrim for inline overflow menu
            if (showOverflowMenu) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            showOverflowMenu = false
                            overflowInitialMenu = AppBarMenu.Main
                        }
                )
            }

            AnimatedVisibility(
                visible = showAppBar && !isFullscreen,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(top = if (currentStream != null) streamHeightDp else with(density) { WindowInsets.statusBars.getTop(density).toDp() })
                    .padding(top = 8.dp)
            ) {
                    val tabListState = rememberLazyListState()

                    // Auto-scroll to keep selected tab visible
                    LaunchedEffect(selectedIndex) {
                        tabListState.animateScrollToItem(selectedIndex)
                    }

                    // Mention indicators based on visibility
                    val visibleItems = tabListState.layoutInfo.visibleItemsInfo
                    val firstVisibleIndex = visibleItems.firstOrNull()?.index ?: 0
                    val lastVisibleIndex = visibleItems.lastOrNull()?.index ?: (totalTabs - 1)
                    val hasLeftMention = tabState.tabs.take(firstVisibleIndex).any { it.mentionCount > 0 }
                    val hasRightMention = tabState.tabs.drop(lastVisibleIndex + 1).any { it.mentionCount > 0 }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Scrollable tabs pill
                        Surface(
                            modifier = Modifier.weight(1f, fill = false),
                            shape = MaterialTheme.shapes.extraLarge,
                            color = MaterialTheme.colorScheme.surfaceContainer,
                        ) {
                            val mentionGradientColor = MaterialTheme.colorScheme.error
                            LazyRow(
                                state = tabListState,
                                contentPadding = PaddingValues(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .padding(horizontal = 8.dp)
                                    .drawWithContent {
                                    drawContent()
                                    val gradientWidth = 24.dp.toPx()
                                    if (hasLeftMention) {
                                        drawRect(
                                            brush = Brush.horizontalGradient(
                                                colors = listOf(
                                                    mentionGradientColor.copy(alpha = 0.5f),
                                                    mentionGradientColor.copy(alpha = 0f)
                                                ),
                                                endX = gradientWidth
                                            ),
                                            size = androidx.compose.ui.geometry.Size(gradientWidth, size.height)
                                        )
                                    }
                                    if (hasRightMention) {
                                        drawRect(
                                            brush = Brush.horizontalGradient(
                                                colors = listOf(
                                                    mentionGradientColor.copy(alpha = 0f),
                                                    mentionGradientColor.copy(alpha = 0.5f)
                                                ),
                                                startX = size.width - gradientWidth,
                                                endX = size.width
                                            ),
                                            topLeft = androidx.compose.ui.geometry.Offset(size.width - gradientWidth, 0f),
                                            size = androidx.compose.ui.geometry.Size(gradientWidth, size.height)
                                        )
                                    }
                                }
                            ) {
                                itemsIndexed(
                                    items = tabState.tabs,
                                    key = { _, tab -> tab.channel.value }
                                ) { index, tab ->
                                    val isSelected = tab.isSelected
                                    val textColor = when {
                                        isSelected -> MaterialTheme.colorScheme.primary
                                        tab.mentionCount > 0 || tab.hasUnread -> MaterialTheme.colorScheme.onSurface
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .combinedClickable(
                                                onClick = {
                                                    channelTabViewModel.selectTab(index)
                                                    scope.launch { composePagerState.scrollToPage(index) }
                                                },
                                                onLongClick = {
                                                    channelTabViewModel.selectTab(index)
                                                    scope.launch { composePagerState.scrollToPage(index) }
                                                    overflowInitialMenu = AppBarMenu.Channel
                                                    showOverflowMenu = true
                                                }
                                            )
                                            .defaultMinSize(minHeight = 48.dp)
                                            .padding(horizontal = 12.dp)
                                    ) {
                                        Text(
                                            text = tab.displayName,
                                            color = textColor,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        )
                                        if (tab.mentionCount > 0) {
                                            Spacer(Modifier.width(4.dp))
                                            Badge()
                                        }
                                    }
                                }
                            }
                        }

                        // Action icons + inline overflow menu (animated with expand/collapse)
                        AnimatedVisibility(
                            visible = !isTabsExpanded,
                            enter = expandHorizontally(
                                expandFrom = Alignment.Start,
                                animationSpec = tween(350, easing = FastOutSlowInEasing)
                            ) + fadeIn(tween(200)),
                            exit = shrinkHorizontally(
                                shrinkTowards = Alignment.Start,
                                animationSpec = tween(350, easing = FastOutSlowInEasing)
                            ) + fadeOut(tween(150))
                        ) {
                            Row(verticalAlignment = Alignment.Top) {
                                Spacer(Modifier.width(8.dp))

                                val pillCornerRadius by animateDpAsState(
                                    targetValue = if (showOverflowMenu) 0.dp else 28.dp,
                                    animationSpec = tween(200),
                                    label = "pillCorner"
                                )
                                Column(modifier = Modifier.width(IntrinsicSize.Min)) {
                                    Surface(
                                        shape = RoundedCornerShape(
                                            topStart = 28.dp,
                                            topEnd = 28.dp,
                                            bottomStart = pillCornerRadius,
                                            bottomEnd = pillCornerRadius
                                        ),
                                        color = MaterialTheme.colorScheme.surfaceContainer
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(onClick = { showAddChannelDialog = true }) {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = stringResource(R.string.add_channel)
                                                )
                                            }
                                            IconButton(onClick = { sheetNavigationViewModel.openMentions() }) {
                                                Icon(
                                                    imageVector = Icons.Default.Notifications,
                                                    contentDescription = stringResource(R.string.mentions_title),
                                                    tint = if (tabState.tabs.sumOf { it.mentionCount } > 0) {
                                                        MaterialTheme.colorScheme.error
                                                    } else {
                                                        LocalContentColor.current
                                                    }
                                                )
                                            }
                                            IconButton(onClick = {
                                                overflowInitialMenu = AppBarMenu.Main
                                                showOverflowMenu = !showOverflowMenu
                                            }) {
                                                Icon(
                                                    imageVector = Icons.Default.MoreVert,
                                                    contentDescription = stringResource(R.string.more)
                                                )
                                            }
                                        }
                                    }
                                    AnimatedVisibility(
                                        visible = showOverflowMenu,
                                        enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                                        exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(
                                                topStart = 0.dp,
                                                topEnd = 0.dp,
                                                bottomStart = 12.dp,
                                                bottomEnd = 12.dp
                                            ),
                                            color = MaterialTheme.colorScheme.surfaceContainer
                                        ) {
                                            InlineOverflowMenu(
                                                isLoggedIn = isLoggedIn,
                                                isStreamActive = currentStream != null,
                                                hasStreamData = hasStreamData,
                                                onDismiss = {
                                                    showOverflowMenu = false
                                                    overflowInitialMenu = AppBarMenu.Main
                                                },
                                                initialMenu = overflowInitialMenu,
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
                                                onToggleStream = {
                                                    activeChannel?.let { streamViewModel.toggleStream(it) }
                                                },
                                                onOpenSettings = onNavigateToSettings
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

        // Emote Menu Layer - slides up/down independently of keyboard
        // Fast tween to match system keyboard animation speed
        AnimatedVisibility(
            visible = inputState.isEmoteMenuOpen,
            enter = slideInVertically(animationSpec = tween(durationMillis = 140), initialOffsetY = { it }),
            exit = slideOutVertically(animationSpec = tween(durationMillis = 140), targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(totalMenuHeight)
                    .graphicsLayer {
                        val scale = 1f - (backProgress * 0.1f)
                        scaleX = scale
                        scaleY = scale
                        alpha = 1f - backProgress
                        translationY = backProgress * 100f
                    }
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                EmoteMenu(
                    onEmoteClick = { code, _ ->
                        chatInputViewModel.insertText("$code ")
                    },
                    modifier = Modifier.fillMaxSize()
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

        if (showInputState && isKeyboardVisible) {
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

