package com.flxrs.dankchat.main.compose

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.max
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import android.app.Activity
import android.app.PictureInPictureParams
import android.os.Build
import android.util.Rational
import androidx.navigation.compose.currentBackStackEntryAsState
import com.flxrs.dankchat.R
import com.flxrs.dankchat.chat.compose.ChatComposable
import com.flxrs.dankchat.chat.message.compose.MessageOptionsParams
import com.flxrs.dankchat.chat.user.UserPopupStateParams
import com.flxrs.dankchat.data.DisplayName
import com.flxrs.dankchat.data.UserId
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.state.GlobalLoadingState
import com.flxrs.dankchat.data.twitch.emote.ChatMessageEmote
import com.flxrs.dankchat.main.MainActivity
import com.flxrs.dankchat.main.MainEvent
import com.flxrs.dankchat.data.repo.chat.UserStateRepository
import com.flxrs.dankchat.main.compose.sheets.EmoteMenu
import com.flxrs.dankchat.preferences.DankChatPreferenceStore
import com.flxrs.dankchat.preferences.appearance.AppearanceSettingsDataStore
import com.flxrs.dankchat.preferences.developer.DeveloperSettingsDataStore
import com.flxrs.dankchat.preferences.tools.ToolsSettingsDataStore
import com.flxrs.dankchat.preferences.components.DankBackground
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.ui.platform.LocalResources
import androidx.window.core.layout.WindowSizeClass
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

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
    val resources = LocalResources.current
    val context = LocalContext.current
    val density = LocalDensity.current
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
    val keyboardController = LocalSoftwareKeyboardController.current
    val configuration = LocalConfiguration.current
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

    // PiP state — observe via lifecycle since onPause fires when entering PiP
    val activity = context as? Activity
    var isInPipMode by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, _ ->
            isInPipMode = activity?.isInPictureInPictureMode == true
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) {
        streamViewModel.shouldEnablePipAutoMode.collect { enabled ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && activity != null) {
                activity.setPictureInPictureParams(
                    PictureInPictureParams.Builder()
                        .setAutoEnterEnabled(enabled)
                        .setAspectRatio(Rational(16, 9))
                        .build()
                )
            }
        }
    }

    // Wide split layout: side-by-side stream + chat on medium+ width windows
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val isWideWindow = windowSizeClass.isWidthAtLeastBreakpoint(
        WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND
    )
    val useWideSplitLayout = isWideWindow && currentStream != null && !isInPipMode

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
    var userPopupParams by remember { mutableStateOf<UserPopupStateParams?>(null) }
    var messageOptionsParams by remember { mutableStateOf<MessageOptionsParams?>(null) }
    var emoteInfoEmotes by remember { mutableStateOf<List<ChatMessageEmote>?>(null) }
    var showRoomStateDialog by remember { mutableStateOf(false) }
    var pendingUploadAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var showLoginOutdatedDialog by remember { mutableStateOf<UserName?>(null) }
    var showLoginExpiredDialog by remember { mutableStateOf(false) }
    var showNewWhisperDialog by remember { mutableStateOf(false) }

    val toolsSettingsDataStore: ToolsSettingsDataStore = koinInject()
    val userStateRepository: UserStateRepository = koinInject()

    val fullScreenSheetState by sheetNavigationViewModel.fullScreenSheetState.collectAsStateWithLifecycle()
    val isSheetOpen = fullScreenSheetState !is FullScreenSheetState.Closed
    val inputSheetState by sheetNavigationViewModel.inputSheetState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        mainEventBus.events.collect { event ->
            when (event) {
                is MainEvent.LogOutRequested -> showLogoutDialog = true
                is MainEvent.UploadLoading -> isUploading = true
                is MainEvent.UploadSuccess -> {
                    isUploading = false
                    val result = snackbarHostState.showSnackbar(
                        message = resources.getString(R.string.snackbar_image_uploaded, event.url),
                        actionLabel = resources.getString(R.string.snackbar_paste),
                        duration = SnackbarDuration.Long
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        chatInputViewModel.insertText(event.url)
                    }
                }
                is MainEvent.UploadFailed -> {
                    isUploading = false
                    val message = event.errorMessage?.let { resources.getString(R.string.snackbar_upload_failed_cause, it) }
                        ?: resources.getString(R.string.snackbar_upload_failed)
                    snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Long)
                }
                is MainEvent.LoginValidated -> {
                    snackbarHostState.showSnackbar(
                        message = resources.getString(R.string.snackbar_login, event.username),
                        duration = SnackbarDuration.Short
                    )
                }
                is MainEvent.LoginOutdated -> {
                    showLoginOutdatedDialog = event.username
                }
                MainEvent.LoginTokenInvalid -> {
                    showLoginExpiredDialog = true
                }
                MainEvent.LoginValidationFailed -> {
                    snackbarHostState.showSnackbar(
                        message = resources.getString(R.string.oauth_verify_failed),
                        duration = SnackbarDuration.Short
                    )
                }
                is MainEvent.OpenChannel -> {
                    channelTabViewModel.selectTab(
                        preferenceStore.channels.indexOf(event.channel)
                    )
                    (context as? MainActivity)?.clearNotificationsOfChannel(event.channel)
                }
                else -> Unit
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
                    resources.getString(R.string.snackbar_login, name)
                } else {
                    resources.getString(R.string.login) // Fallback
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
                    actionLabel = resources.getString(R.string.snackbar_retry),
                    duration = SnackbarDuration.Long
                )
            }
        }
    }

    val tabState = channelTabViewModel.uiState.collectAsStateWithLifecycle().value
    val activeChannel = tabState.tabs.getOrNull(tabState.selectedIndex)?.channel

    MainScreenDialogs(
        channelState = ChannelDialogState(
            showAddChannel = showAddChannelDialog,
            showManageChannels = showManageChannelsDialog,
            showRemoveChannel = showRemoveChannelDialog,
            showBlockChannel = showBlockChannelDialog,
            showClearChat = showClearChatDialog,
            showRoomState = showRoomStateDialog,
            activeChannel = activeChannel,
            roomStateChannel = inputState.activeChannel,
            onDismissAddChannel = { showAddChannelDialog = false },
            onDismissManageChannels = { showManageChannelsDialog = false },
            onDismissRemoveChannel = { showRemoveChannelDialog = false },
            onDismissBlockChannel = { showBlockChannelDialog = false },
            onDismissClearChat = { showClearChatDialog = false },
            onDismissRoomState = { showRoomStateDialog = false },
            onAddChannel = {
                channelManagementViewModel.addChannel(it)
                showAddChannelDialog = false
            },
        ),
        authState = AuthDialogState(
            showLogout = showLogoutDialog,
            showLoginOutdated = showLoginOutdatedDialog != null,
            showLoginExpired = showLoginExpiredDialog,
            onDismissLogout = { showLogoutDialog = false },
            onDismissLoginOutdated = { showLoginOutdatedDialog = null },
            onDismissLoginExpired = { showLoginExpiredDialog = false },
            onLogout = onLogout,
            onLogin = onLogin,
        ),
        messageState = MessageInteractionState(
            messageOptionsParams = messageOptionsParams,
            emoteInfoEmotes = emoteInfoEmotes,
            userPopupParams = userPopupParams,
            inputSheetState = inputSheetState,
            onDismissMessageOptions = { messageOptionsParams = null },
            onDismissEmoteInfo = { emoteInfoEmotes = null },
            onDismissUserPopup = { userPopupParams = null },
            onOpenChannel = onOpenChannel,
            onReportChannel = onReportChannel,
            onOpenUrl = onOpenUrl,
        ),
        snackbarHostState = snackbarHostState,
    )

    // External hosting upload disclaimer dialog
    if (pendingUploadAction != null) {
        val uploadHost = remember {
            runCatching {
                java.net.URL(toolsSettingsDataStore.current().uploaderConfig.uploadUrl).host
            }.getOrElse { "" }
        }
        AlertDialog(
            onDismissRequest = { pendingUploadAction = null },
            title = { Text(stringResource(R.string.nuuls_upload_title)) },
            text = { Text(stringResource(R.string.external_upload_disclaimer, uploadHost)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        preferenceStore.hasExternalHostingAcknowledged = true
                        val action = pendingUploadAction
                        pendingUploadAction = null
                        action?.invoke()
                    }
                ) {
                    Text(stringResource(R.string.dialog_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingUploadAction = null }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }

    // New Whisper dialog
    if (showNewWhisperDialog) {
        var whisperUsername by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewWhisperDialog = false },
            title = { Text(stringResource(R.string.whisper_new_dialog_title)) },
            text = {
                OutlinedTextField(
                    value = whisperUsername,
                    onValueChange = { whisperUsername = it },
                    label = { Text(stringResource(R.string.whisper_new_dialog_hint)) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val username = whisperUsername.trim()
                        if (username.isNotBlank()) {
                            chatInputViewModel.setWhisperTarget(UserName(username))
                            showNewWhisperDialog = false
                        }
                    }
                ) {
                    Text(stringResource(R.string.whisper_new_dialog_start))
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewWhisperDialog = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }

    val isFullscreen by mainScreenViewModel.isFullscreen.collectAsStateWithLifecycle()
    val showAppBar by mainScreenViewModel.showAppBar.collectAsStateWithLifecycle()
    val showInputState by mainScreenViewModel.showInput.collectAsStateWithLifecycle()

    // Hide/show system bars when fullscreen toggles
    val window = (context as? Activity)?.window
    val view = LocalView.current
    DisposableEffect(isFullscreen, window, view) {
        if (window == null) return@DisposableEffect onDispose { }
        val controller = WindowCompat.getInsetsController(window, view)
        if (isFullscreen) {
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            // Restore system bars when leaving composition in fullscreen
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    val currentDestination = navController.currentBackStackEntryAsState().value?.destination?.route

    val pagerState by channelPagerViewModel.uiState.collectAsStateWithLifecycle()

    val composePagerState = rememberPagerState(
        initialPage = pagerState.currentPage,
        pageCount = { pagerState.channels.size }
    )
    var inputHeightPx by remember { mutableIntStateOf(0) }
    val inputHeightDp = with(density) { inputHeightPx.toDp() }

    // Clear focus when keyboard fully reaches the bottom, but not when
    // switching to the emote menu. Prevents keyboard from reopening when
    // returning from background. Debounced to avoid premature focus loss
    // during heavy recomposition (e.g. emote loading/reparsing).
    val focusManager = LocalFocusManager.current
    LaunchedEffect(Unit) {
        snapshotFlow { imeHeightState.value == 0 && !inputState.isEmoteMenuOpen }
            .debounce(150)
            .distinctUntilChanged()
            .collect { shouldClearFocus ->
                if (shouldClearFocus) {
                    focusManager.clearFocus()
                }
            }
    }

    // Sync Compose pager with ViewModel state
    LaunchedEffect(pagerState.currentPage, pagerState.channels.size) {
        if (!composePagerState.isScrollInProgress &&
            composePagerState.currentPage != pagerState.currentPage &&
            pagerState.currentPage in 0 until composePagerState.pageCount
        ) {
            composePagerState.scrollToPage(pagerState.currentPage)
        }
    }

    // Update ViewModel when user swipes (use settledPage to avoid clearing
    // unread/mention indicators for pages scrolled through during programmatic jumps)
    LaunchedEffect(composePagerState.settledPage) {
        if (composePagerState.settledPage != pagerState.currentPage) {
            channelPagerViewModel.onPageChanged(composePagerState.settledPage)
        }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .then(if (!isFullscreen && !isInPipMode) Modifier.windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal)) else Modifier)
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

        // Shared scaffold bottom padding calculation
        val hasDialogWithInput = showAddChannelDialog || showRoomStateDialog || showManageChannelsDialog || showNewWhisperDialog
        val currentImeDp = if (hasDialogWithInput) 0.dp else with(density) { currentImeHeight.toDp() }
        val emoteMenuPadding = if (inputState.isEmoteMenuOpen) targetMenuHeight else 0.dp
        val scaffoldBottomPadding = max(currentImeDp, emoteMenuPadding)

        // Shared bottom bar content
        val bottomBar: @Composable () -> Unit = {
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
                        isUploading = isUploading,
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
                        showWhisperOverlay = inputState.showWhisperOverlay,
                        whisperTarget = inputState.whisperTarget,
                        onWhisperDismiss = {
                            chatInputViewModel.setWhisperTarget(null)
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
                        onNewWhisper = if (inputState.isWhisperTabActive) {{ showNewWhisperDialog = true }} else null,
                        showQuickActions = !isSheetOpen,
                        modifier = Modifier.onGloballyPositioned { coordinates ->
                            inputHeightPx = coordinates.size.height
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
                                modifier = Modifier
                                    .navigationBarsPadding()
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .basicMarquee(),
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                }
            }
        }

        // Shared floating toolbar
        val floatingToolbar: @Composable (Modifier, Boolean, Boolean, Boolean) -> Unit = { toolbarModifier, visible, endAligned, showTabs ->
            FloatingToolbar(
                tabState = tabState,
                composePagerState = composePagerState,
                showAppBar = showAppBar && visible,
                isFullscreen = isFullscreen,
                isLoggedIn = isLoggedIn,
                currentStream = currentStream,
                hasStreamData = hasStreamData,
                streamHeightDp = streamHeightDp,
                totalMentionCount = tabState.tabs.sumOf { it.mentionCount },
                onTabSelected = { index ->
                    channelTabViewModel.selectTab(index)
                    scope.launch { composePagerState.scrollToPage(index) }
                },
                onTabLongClick = { index ->
                    channelTabViewModel.selectTab(index)
                    scope.launch { composePagerState.scrollToPage(index) }
                },
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
                onCaptureImage = {
                    if (preferenceStore.hasExternalHostingAcknowledged) onCaptureImage() else pendingUploadAction = onCaptureImage
                },
                onCaptureVideo = {
                    if (preferenceStore.hasExternalHostingAcknowledged) onCaptureVideo() else pendingUploadAction = onCaptureVideo
                },
                onChooseMedia = {
                    if (preferenceStore.hasExternalHostingAcknowledged) onChooseMedia() else pendingUploadAction = onChooseMedia
                },
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
                onOpenSettings = onNavigateToSettings,
                endAligned = endAligned,
                showTabs = showTabs,
                modifier = toolbarModifier,
            )
        }

        // Shared emote menu layer
        val emoteMenuLayer: @Composable (Modifier) -> Unit = { menuModifier ->
            AnimatedVisibility(
                visible = inputState.isEmoteMenuOpen,
                enter = slideInVertically(animationSpec = tween(durationMillis = 140), initialOffsetY = { it }),
                exit = slideOutVertically(animationSpec = tween(durationMillis = 140), targetOffsetY = { it }),
                modifier = menuModifier
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
        }

        // Shared scaffold content (pager)
        val scaffoldContent: @Composable (PaddingValues, Dp) -> Unit = { paddingValues, chatTopPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
                val showFullScreenLoading = tabState.loading && tabState.tabs.isEmpty()
                DankBackground(visible = showFullScreenLoading)
                if (showFullScreenLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(paddingValues)
                    )
                    return@Box
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
                            .padding(top = paddingValues.calculateTopPadding())
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
                                        top = chatTopPadding + 56.dp,
                                        bottom = paddingValues.calculateBottomPadding()
                                    ),
                                    onScrollDirectionChanged = { }
                                )
                            }
                        }
                    }
                }

                // Fullscreen Overlay Sheets - inside Scaffold content for edge-to-edge
                FullScreenSheetOverlay(
                    sheetState = fullScreenSheetState,
                    isLoggedIn = isLoggedIn,
                    mentionViewModel = mentionViewModel,
                    appearanceSettingsDataStore = appearanceSettingsDataStore,
                    onDismiss = sheetNavigationViewModel::closeFullScreenSheet,
                    onDismissReplies = {
                        sheetNavigationViewModel.closeFullScreenSheet()
                        chatInputViewModel.setReplying(false)
                    },
                    onUserClick = { userPopupParams = it },
                    onMessageLongClick = { messageOptionsParams = it },
                    onEmoteClick = { emoteInfoEmotes = it },
                    onWhisperReply = chatInputViewModel::setWhisperTarget,
                    bottomContentPadding = paddingValues.calculateBottomPadding(),
                )
            }
        }

        if (useWideSplitLayout) {
            // --- Wide split layout: stream (left) | handle | chat (right) ---
            var splitFraction by remember { mutableFloatStateOf(0.6f) }
            var containerWidthPx by remember { mutableIntStateOf(0) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { containerWidthPx = it.size.width }
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    // Left pane: Stream
                    Box(modifier = Modifier
                        .weight(splitFraction)
                        .fillMaxSize()
                    ) {
                        currentStream?.let { channel ->
                            StreamView(
                                channel = channel,
                                streamViewModel = streamViewModel,
                                fillPane = true,
                                onClose = {
                                    focusManager.clearFocus()
                                    streamViewModel.closeStream()
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    // Right pane: Chat + all overlays
                    Box(modifier = Modifier
                        .weight(1f - splitFraction)
                        .fillMaxSize()
                    ) {
                    val statusBarTop = with(density) { WindowInsets.statusBars.getTop(density).toDp() }

                    Scaffold(
                        modifier = modifier
                            .fillMaxSize()
                            .padding(bottom = scaffoldBottomPadding),
                        contentWindowInsets = WindowInsets(0),
                        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                        bottomBar = bottomBar,
                    ) { paddingValues ->
                        scaffoldContent(paddingValues, statusBarTop)
                    }

                    val chatPaneWidthDp = with(density) { (containerWidthPx * (1f - splitFraction)).toInt().toDp() }
                    val showTabsInSplit = chatPaneWidthDp > 250.dp

                    floatingToolbar(
                        Modifier.align(Alignment.TopCenter),
                        !isKeyboardVisible && !inputState.isEmoteMenuOpen && !isSheetOpen,
                        false,
                        showTabsInSplit,
                    )

                    emoteMenuLayer(Modifier.align(Alignment.BottomCenter))

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

                // Draggable handle overlaid at the split edge
                DraggableHandle(
                    onDrag = { deltaPx ->
                        if (containerWidthPx > 0) {
                            splitFraction = (splitFraction + deltaPx / containerWidthPx).coerceIn(0.2f, 0.8f)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .graphicsLayer { translationX = containerWidthPx * splitFraction - 12.dp.toPx() }
                )
            }
        } else {
            // --- Normal stacked layout (portrait / narrow-without-stream / PiP) ---
            if (!isInPipMode) {
                Scaffold(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(bottom = scaffoldBottomPadding),
                    contentWindowInsets = WindowInsets(0),
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    bottomBar = bottomBar,
                ) { paddingValues ->
                    val chatTopPadding = maxOf(with(density) { WindowInsets.statusBars.getTop(density).toDp() }, streamHeightDp)
                    scaffoldContent(paddingValues, chatTopPadding)
                }
            } // end !isInPipMode

            // Stream View layer
            currentStream?.let { channel ->
                StreamView(
                    channel = channel,
                    streamViewModel = streamViewModel,
                    isInPipMode = isInPipMode,
                    onClose = {
                        focusManager.clearFocus()
                        streamViewModel.closeStream()
                    },
                    modifier = if (isInPipMode) {
                        Modifier.fillMaxSize()
                    } else {
                        Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .onGloballyPositioned { coordinates ->
                                streamHeightDp = with(density) { coordinates.size.height.toDp() }
                            }
                    }
                )
            }

            // Status bar scrim when stream is active (hidden in fullscreen and PiP)
            if (currentStream != null && !isFullscreen && !isInPipMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(with(density) { WindowInsets.statusBars.getTop(density).toDp() })
                        .background(MaterialTheme.colorScheme.surface)
                )
            }

            // Floating Toolbars - collapsible tabs (expand on swipe) + actions
            if (!isInPipMode) floatingToolbar(
                Modifier.align(Alignment.TopCenter),
                (!isWideWindow || (!isKeyboardVisible && !inputState.isEmoteMenuOpen)) && !isSheetOpen,
                true,
                true,
            )

            // Emote Menu Layer - slides up/down independently of keyboard
            // Fast tween to match system keyboard animation speed
            if (!isInPipMode) emoteMenuLayer(Modifier.align(Alignment.BottomCenter))

            if (!isInPipMode && showInputState && isKeyboardVisible) {
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
}

