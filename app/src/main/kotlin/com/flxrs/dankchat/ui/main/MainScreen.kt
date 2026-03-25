package com.flxrs.dankchat.ui.main

import android.app.Activity
import android.app.PictureInPictureParams
import android.os.Build
import android.util.Rational
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.window.core.layout.WindowSizeClass
import com.flxrs.dankchat.R
import com.flxrs.dankchat.data.DisplayName
import com.flxrs.dankchat.data.UserId
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.preferences.DankChatPreferenceStore
import com.flxrs.dankchat.preferences.appearance.InputAction
import com.flxrs.dankchat.preferences.components.DankBackground
import com.flxrs.dankchat.preferences.tools.ToolsSettingsDataStore
import com.flxrs.dankchat.ui.chat.ChatComposable
import com.flxrs.dankchat.ui.chat.ScrollDirectionTracker
import com.flxrs.dankchat.ui.chat.mention.MentionViewModel
import com.flxrs.dankchat.ui.chat.message.MessageOptionsParams
import com.flxrs.dankchat.ui.chat.swipeDownToHide
import com.flxrs.dankchat.ui.chat.user.UserPopupStateParams
import com.flxrs.dankchat.ui.main.channel.ChannelManagementViewModel
import com.flxrs.dankchat.ui.main.channel.ChannelPagerViewModel
import com.flxrs.dankchat.ui.main.channel.ChannelTabViewModel
import com.flxrs.dankchat.ui.main.dialog.DialogStateViewModel
import com.flxrs.dankchat.ui.main.dialog.MainScreenDialogs
import com.flxrs.dankchat.ui.main.input.CharacterCounterState
import com.flxrs.dankchat.ui.main.input.ChatBottomBar
import com.flxrs.dankchat.ui.main.input.ChatInputViewModel
import com.flxrs.dankchat.ui.main.input.SuggestionDropdown
import com.flxrs.dankchat.ui.main.input.TourOverlayState
import com.flxrs.dankchat.ui.main.sheet.EmoteMenu
import com.flxrs.dankchat.ui.main.sheet.FullScreenSheetOverlay
import com.flxrs.dankchat.ui.main.sheet.FullScreenSheetState
import com.flxrs.dankchat.ui.main.sheet.SheetNavigationViewModel
import com.flxrs.dankchat.ui.main.stream.StreamView
import com.flxrs.dankchat.ui.main.stream.StreamViewModel
import com.flxrs.dankchat.ui.tour.FeatureTourViewModel
import com.flxrs.dankchat.ui.tour.PostOnboardingStep
import com.flxrs.dankchat.ui.tour.TourStep
import com.flxrs.dankchat.utils.compose.rememberRoundedCornerBottomPadding
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

private val ROUNDED_CORNER_THRESHOLD = 8.dp

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
    val messageNotInHistoryMsg = stringResource(R.string.message_not_in_history)
    val layoutDirection = LocalLayoutDirection.current
    // Scoped ViewModels - each handles one concern
    val mainScreenViewModel: MainScreenViewModel = koinViewModel()
    val channelManagementViewModel: ChannelManagementViewModel = koinViewModel()
    val channelTabViewModel: ChannelTabViewModel = koinViewModel()
    val channelPagerViewModel: ChannelPagerViewModel = koinViewModel()
    val chatInputViewModel: ChatInputViewModel = koinViewModel()
    val sheetNavigationViewModel: SheetNavigationViewModel = koinViewModel()
    val streamViewModel: StreamViewModel = koinViewModel()
    val dialogViewModel: DialogStateViewModel = koinViewModel()
    val mentionViewModel: MentionViewModel = koinViewModel()
    val preferenceStore: DankChatPreferenceStore = koinInject()
    val mainEventBus: MainEventBus = koinInject()
    val featureTourViewModel: FeatureTourViewModel = koinViewModel()
    val featureTourState by featureTourViewModel.uiState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val scrollTargets = remember { mutableStateMapOf<UserName, String>() }
    // Lazy ref for composePagerState, used in jump handlers declared before the pager
    var composePagerStateRef by remember { mutableStateOf<PagerState?>(null) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    val mainState by mainScreenViewModel.uiState.collectAsStateWithLifecycle()

    val ime = WindowInsets.ime
    val navBars = WindowInsets.navigationBars
    val imeTarget = WindowInsets.imeAnimationTarget
    val currentImeHeight = (ime.getBottom(density) - navBars.getBottom(density)).coerceAtLeast(0)

    // Target height for stability during opening animation
    val targetImeHeight = (imeTarget.getBottom(density) - navBars.getBottom(density)).coerceAtLeast(0)
    val isImeOpening = targetImeHeight > 0

    val imeHeightState = rememberUpdatedState(currentImeHeight)
    val isImeVisible = WindowInsets.isImeVisible

    // Keyboard height tracking — VM handles debounce + persistence
    LaunchedEffect(isLandscape) { mainScreenViewModel.initKeyboardHeight(isLandscape) }
    val keyboardHeightPx by mainScreenViewModel.keyboardHeightPx.collectAsStateWithLifecycle()
    val minKeyboardHeightPx = with(density) { 100.dp.toPx() }
    mainScreenViewModel.trackKeyboardHeight(targetImeHeight, isLandscape, minKeyboardHeightPx)

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
    var backProgress by remember { mutableFloatStateOf(0f) }

    // Stream state
    val streamVmState by streamViewModel.streamState.collectAsStateWithLifecycle()
    val currentStream = streamVmState.currentStream
    val hasStreamData = streamVmState.hasStreamData
    val imeTargetBottom = with(density) { WindowInsets.imeAnimationTarget.getBottom(density) }
    val streamState = rememberStreamToolbarState(currentStream, isKeyboardVisible, imeTargetBottom)

    // PiP state — observe via lifecycle since onPause fires when entering PiP
    val activity = context as? Activity
    var isInPipMode by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, _ ->
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

    val dialogState by dialogViewModel.state.collectAsStateWithLifecycle()

    val toolsSettingsDataStore: ToolsSettingsDataStore = koinInject()

    val sheetNavState by sheetNavigationViewModel.sheetState.collectAsStateWithLifecycle()
    val fullScreenSheetState = sheetNavState.fullScreenSheet
    val isSheetOpen = fullScreenSheetState !is FullScreenSheetState.Closed
    val isHistorySheet = fullScreenSheetState is FullScreenSheetState.History
    val inputSheetState = sheetNavState.inputSheet

    MainScreenEventHandler(
        snackbarHostState = snackbarHostState,
        mainEventBus = mainEventBus,
        dialogViewModel = dialogViewModel,
        chatInputViewModel = chatInputViewModel,
        channelTabViewModel = channelTabViewModel,
        mainScreenViewModel = mainScreenViewModel,
        preferenceStore = preferenceStore,
    )

    val tabState = channelTabViewModel.uiState.collectAsStateWithLifecycle().value
    val activeChannel = tabState.tabs.getOrNull(tabState.selectedIndex)?.channel

    // Post-onboarding flow: toolbar hint → feature tour
    val channelsReady = !tabState.loading
    val channelsEmpty = tabState.tabs.isEmpty() && channelsReady

    // Notify tour VM when channel state changes
    LaunchedEffect(channelsReady, channelsEmpty) {
        featureTourViewModel.onChannelsChanged(empty = channelsEmpty, ready = channelsReady)
    }

    // Drive tooltip dismissals and tour start from the typed step.
    // Tooltip .show() calls live in FloatingToolbar.
    LaunchedEffect(featureTourState.postOnboardingStep) {
        when (featureTourState.postOnboardingStep) {
            PostOnboardingStep.FeatureTour                       -> {
                featureTourViewModel.addChannelTooltipState.dismiss()
                featureTourViewModel.startTour()
            }

            PostOnboardingStep.Complete, PostOnboardingStep.Idle -> {
                featureTourViewModel.addChannelTooltipState.dismiss()
            }

            PostOnboardingStep.ToolbarPlusHint                   -> Unit
        }
    }

    // Sync tour's gestureInputHidden with MainScreenViewModel (only during active tour
    // to avoid resetting the persisted state on Activity recreation)
    LaunchedEffect(featureTourState.gestureInputHidden, featureTourState.isTourActive) {
        if (featureTourState.isTourActive) {
            mainScreenViewModel.setGestureInputHidden(featureTourState.gestureInputHidden)
        }
    }

    MainScreenDialogs(
        dialogViewModel = dialogViewModel,
        isLoggedIn = isLoggedIn,
        activeChannel = activeChannel,
        roomStateChannel = inputState.activeChannel,
        inputSheetState = inputSheetState,
        snackbarHostState = snackbarHostState,
        onAddChannel = {
            channelManagementViewModel.addChannel(it)
            dialogViewModel.dismissAddChannel()
        },
        onLogout = onLogout,
        onLogin = onLogin,
        onOpenChannel = onOpenChannel,
        onReportChannel = onReportChannel,
        onOpenUrl = onOpenUrl,
        onJumpToMessage = { messageId, channel ->
            val target = channelPagerViewModel.resolveJumpTarget(channel, messageId)
            if (target != null) {
                dialogViewModel.dismissMessageOptions()
                sheetNavigationViewModel.closeFullScreenSheet()
                scrollTargets[target.channel] = target.messageId
                scope.launch { composePagerStateRef?.scrollToPage(target.channelIndex) }
            } else {
                scope.launch {
                    snackbarHostState.showSnackbar(messageNotInHistoryMsg)
                }
            }
        },
    )

    // External hosting upload disclaimer dialog
    if (dialogState.pendingUploadAction != null) {
        val uploadHost = remember {
            runCatching {
                java.net.URL(toolsSettingsDataStore.current().uploaderConfig.uploadUrl).host
            }.getOrElse { "" }
        }
        AlertDialog(
            onDismissRequest = { dialogViewModel.setPendingUploadAction(null) },
            title = { Text(stringResource(R.string.nuuls_upload_title)) },
            text = { Text(stringResource(R.string.external_upload_disclaimer, uploadHost)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        preferenceStore.hasExternalHostingAcknowledged = true
                        val action = dialogState.pendingUploadAction
                        dialogViewModel.setPendingUploadAction(null)
                        action?.invoke()
                    }
                ) {
                    Text(stringResource(R.string.dialog_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { dialogViewModel.setPendingUploadAction(null) }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }

    // New Whisper dialog
    if (dialogState.showNewWhisper) {
        var whisperUsername by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = dialogViewModel::dismissNewWhisper,
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
                            dialogViewModel.dismissNewWhisper()
                        }
                    }
                ) {
                    Text(stringResource(R.string.whisper_new_dialog_start))
                }
            },
            dismissButton = {
                TextButton(onClick = dialogViewModel::dismissNewWhisper) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }

    val isFullscreen = mainState.isFullscreen
    val effectiveShowInput = mainState.effectiveShowInput
    val effectiveShowAppBar = mainState.effectiveShowAppBar

    // Auto-advance tour when input is hidden during the SwipeGesture step (e.g. by actual swipe)
    LaunchedEffect(mainState.gestureInputHidden, featureTourState.currentTourStep) {
        if (mainState.gestureInputHidden && featureTourState.currentTourStep == TourStep.SwipeGesture) {
            featureTourViewModel.advance()
        }
    }

    // Keep toolbar visible during tour
    LaunchedEffect(featureTourState.isTourActive, mainState.gestureToolbarHidden) {
        if (featureTourState.isTourActive && mainState.gestureToolbarHidden) {
            mainScreenViewModel.setGestureToolbarHidden(false)
        }
    }

    val toolbarTracker = remember {
        ScrollDirectionTracker(
            hideThresholdPx = with(density) { 100.dp.toPx() },
            showThresholdPx = with(density) { 36.dp.toPx() },
            onHide = { mainScreenViewModel.setGestureToolbarHidden(true) },
            onShow = { mainScreenViewModel.setGestureToolbarHidden(false) },
        )
    }
    val chatScrollModifier = Modifier
        .nestedScroll(toolbarTracker)

    val swipeDownThresholdPx = with(density) { 56.dp.toPx() }

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

    val pagerState by channelPagerViewModel.uiState.collectAsStateWithLifecycle()

    val composePagerState = rememberPagerState(
        initialPage = pagerState.currentPage,
        pageCount = { pagerState.channels.size }
    ).also { composePagerStateRef = it }
    var inputHeightPx by remember { mutableIntStateOf(0) }
    var helperTextHeightPx by remember { mutableIntStateOf(0) }
    var inputOverflowExpanded by remember { mutableStateOf(false) }
    if (!effectiveShowInput) inputHeightPx = 0
    if (effectiveShowInput || inputState.helperText.isNullOrEmpty()) helperTextHeightPx = 0
    val inputHeightDp = with(density) { inputHeightPx.toDp() }
    val helperTextHeightDp = with(density) { helperTextHeightPx.toDp() }
    // scaffoldBottomContentPadding removed — input bar rendered outside Scaffold

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

    // Clear focus after stream closes — the layout shift from removing StreamView
    // can cause the TextField to regain focus and open the keyboard.
    LaunchedEffect(currentStream) {
        if (currentStream == null) {
            keyboardController?.hide()
            focusManager.clearFocus()
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

    // Pager swipe reveals toolbar
    LaunchedEffect(composePagerState.isScrollInProgress) {
        if (composePagerState.isScrollInProgress) {
            mainScreenViewModel.setGestureToolbarHidden(false)
        }
    }

    Box(
        modifier = Modifier
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
        val roundedCornerBottomPadding = rememberRoundedCornerBottomPadding()
        val effectiveRoundedCorner = when {
            roundedCornerBottomPadding >= ROUNDED_CORNER_THRESHOLD -> roundedCornerBottomPadding
            else                                                   -> 0.dp
        }
        val totalMenuHeight = targetMenuHeight + navBarHeightDp

        // Shared scaffold bottom padding calculation
        val hasDialogWithInput = dialogState.showAddChannel || dialogState.showRoomState || dialogState.showManageChannels || dialogState.showNewWhisper
        val currentImeDp = if (hasDialogWithInput) 0.dp else with(density) { currentImeHeight.toDp() }
        val emoteMenuPadding = if (inputState.isEmoteMenuOpen) targetMenuHeight else 0.dp
        val scaffoldBottomPadding = max(currentImeDp, emoteMenuPadding)

        // Shared bottom bar content
        val bottomBar: @Composable () -> Unit = {
            ChatBottomBar(
                showInput = effectiveShowInput && !isHistorySheet,
                textFieldState = chatInputViewModel.textFieldState,
                inputState = inputState,
                isUploading = dialogState.isUploading,
                isLoading = tabState.loading,
                isFullscreen = isFullscreen,
                isModerator = mainScreenViewModel.isModeratorInChannel(inputState.activeChannel),
                isStreamActive = currentStream != null,
                hasStreamData = hasStreamData,
                isSheetOpen = isSheetOpen,
                inputActions = when (fullScreenSheetState) {
                    is FullScreenSheetState.Replies -> persistentListOf(InputAction.LastMessage)
                    is FullScreenSheetState.Whisper,
                    is FullScreenSheetState.Mention -> when {
                        inputState.isWhisperTabActive && inputState.whisperTarget != null -> persistentListOf(InputAction.LastMessage)
                        else                                                              -> persistentListOf()
                    }

                    is FullScreenSheetState.History,
                    is FullScreenSheetState.Closed  -> mainState.inputActions
                },
                characterCounter = if (mainState.showCharacterCounter) inputState.characterCounter else CharacterCounterState.Hidden,
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
                onWhisperDismiss = { chatInputViewModel.setWhisperTarget(null) },
                onReplyDismiss = { chatInputViewModel.setReplying(false) },
                onToggleFullscreen = mainScreenViewModel::toggleFullscreen,
                onToggleInput = mainScreenViewModel::toggleInput,
                onToggleStream = {
                    when {
                        currentStream != null -> streamViewModel.closeStream()
                        else                  -> activeChannel?.let { streamViewModel.toggleStream(it) }
                    }
                },
                onChangeRoomState = dialogViewModel::showRoomState,
                onSearchClick = { activeChannel?.let { sheetNavigationViewModel.openHistory(it) } },
                onNewWhisper = if (inputState.isWhisperTabActive) {
                    dialogViewModel::showNewWhisper
                } else null,
                onInputActionsChanged = mainScreenViewModel::updateInputActions,
                overflowExpanded = inputOverflowExpanded,
                onOverflowExpandedChanged = { inputOverflowExpanded = it },
                onInputHeightChanged = { inputHeightPx = it },
                onHelperTextHeightChanged = { helperTextHeightPx = it },
                isInSplitLayout = useWideSplitLayout,
                instantHide = isHistorySheet,
                tourState = remember(featureTourState.currentTourStep, featureTourState.forceOverflowOpen) {
                    TourOverlayState(
                        inputActionsTooltipState = if (featureTourState.currentTourStep == TourStep.InputActions) featureTourViewModel.inputActionsTooltipState else null,
                        overflowMenuTooltipState = if (featureTourState.currentTourStep == TourStep.OverflowMenu) featureTourViewModel.overflowMenuTooltipState else null,
                        configureActionsTooltipState = if (featureTourState.currentTourStep == TourStep.ConfigureActions) featureTourViewModel.configureActionsTooltipState else null,
                        swipeGestureTooltipState = if (featureTourState.currentTourStep == TourStep.SwipeGesture) featureTourViewModel.swipeGestureTooltipState else null,
                        forceOverflowOpen = featureTourState.forceOverflowOpen,
                        onAdvance = featureTourViewModel::advance,
                        onSkip = featureTourViewModel::skipTour,
                    )
                },
            )
        }

        // Shared toolbar action handler
        val handleToolbarAction: (ToolbarAction) -> Unit = { action ->
            when (action) {
                is ToolbarAction.SelectTab    -> {
                    channelTabViewModel.selectTab(action.index)
                    scope.launch { composePagerState.scrollToPage(action.index) }
                }

                is ToolbarAction.LongClickTab -> {
                    channelTabViewModel.selectTab(action.index)
                    scope.launch { composePagerState.scrollToPage(action.index) }
                }

                ToolbarAction.AddChannel      -> {
                    featureTourViewModel.onAddedChannelFromToolbar()
                    dialogViewModel.showAddChannel()
                }

                ToolbarAction.OpenMentions    -> {
                    sheetNavigationViewModel.openMentions()
                    channelTabViewModel.clearAllMentionCounts()
                }

                ToolbarAction.Login           -> onLogin()
                ToolbarAction.Relogin         -> onRelogin()
                ToolbarAction.Logout          -> dialogViewModel.showLogout()
                ToolbarAction.ManageChannels  -> dialogViewModel.showManageChannels()
                ToolbarAction.OpenChannel     -> onOpenChannel()
                ToolbarAction.RemoveChannel   -> dialogViewModel.showRemoveChannel()
                ToolbarAction.ReportChannel   -> onReportChannel()
                ToolbarAction.BlockChannel    -> dialogViewModel.showBlockChannel()
                ToolbarAction.CaptureImage    -> {
                    if (preferenceStore.hasExternalHostingAcknowledged) onCaptureImage() else dialogViewModel.setPendingUploadAction(onCaptureImage)
                }

                ToolbarAction.CaptureVideo    -> {
                    if (preferenceStore.hasExternalHostingAcknowledged) onCaptureVideo() else dialogViewModel.setPendingUploadAction(onCaptureVideo)
                }

                ToolbarAction.ChooseMedia     -> {
                    if (preferenceStore.hasExternalHostingAcknowledged) onChooseMedia() else dialogViewModel.setPendingUploadAction(onChooseMedia)
                }

                ToolbarAction.ReloadEmotes    -> {
                    activeChannel?.let { channelManagementViewModel.reloadEmotes(it) }
                    onReloadEmotes()
                }

                ToolbarAction.Reconnect       -> {
                    channelManagementViewModel.reconnect()
                    onReconnect()
                }

                ToolbarAction.ClearChat       -> dialogViewModel.showClearChat()
                ToolbarAction.OpenSettings    -> onNavigateToSettings()
            }
        }

        // Shared floating toolbar
        val floatingToolbar: @Composable (Modifier, Boolean, Boolean, Boolean) -> Unit = { toolbarModifier, visible, endAligned, showTabs ->
            FloatingToolbar(
                tabState = tabState,
                composePagerState = composePagerState,
                showAppBar = effectiveShowAppBar && visible,
                isFullscreen = isFullscreen,
                isLoggedIn = isLoggedIn,
                currentStream = currentStream,
                hasStreamData = hasStreamData,
                streamHeightDp = streamState.heightDp,
                totalMentionCount = tabState.tabs.sumOf { it.mentionCount },
                onAction = handleToolbarAction,
                endAligned = endAligned,
                showTabs = showTabs,
                addChannelTooltipState = if (featureTourState.postOnboardingStep is PostOnboardingStep.ToolbarPlusHint) featureTourViewModel.addChannelTooltipState else null,
                onAddChannelTooltipDismissed = featureTourViewModel::onToolbarHintDismissed,
                onSkipTour = featureTourViewModel::skipTour,
                streamToolbarAlpha = streamState.effectiveAlpha,
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
                        onEmoteClick = { code, id ->
                            chatInputViewModel.insertText("$code ")
                            chatInputViewModel.addEmoteUsage(id)
                        },
                        onBackspace = chatInputViewModel::deleteLastWord,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Shared scaffold content (pager)
        val scaffoldContent: @Composable (PaddingValues, Dp) -> Unit = { paddingValues, chatTopPadding ->
            // Input bar is rendered outside Scaffold, so calculateBottomPadding() is 0 here
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
                        onAddChannel = dialogViewModel::showAddChannel,
                        onLogin = onLogin,
                        modifier = Modifier.padding(paddingValues),
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = paddingValues.calculateTopPadding())
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
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
                                            dialogViewModel.showUserPopup(
                                                UserPopupStateParams(
                                                    targetUserId = userId?.let { UserId(it) } ?: UserId(""),
                                                    targetUserName = UserName(userName),
                                                    targetDisplayName = DisplayName(displayName),
                                                    channel = channel?.let { UserName(it) },
                                                    badges = badges.map { it.badge }
                                                ))
                                        },
                                        onMessageLongClick = { messageId, channel, fullMessage ->
                                            dialogViewModel.showMessageOptions(
                                                MessageOptionsParams(
                                                    messageId = messageId,
                                                    channel = channel?.let { UserName(it) },
                                                    fullMessage = fullMessage,
                                                    canModerate = isLoggedIn,
                                                    canReply = isLoggedIn,
                                                    canCopy = true
                                                )
                                            )
                                        },
                                        onEmoteClick = { emotes ->
                                            dialogViewModel.showEmoteInfo(emotes)
                                        },
                                        onReplyClick = { replyMessageId, replyName ->
                                            sheetNavigationViewModel.openReplies(replyMessageId, replyName)
                                        },
                                        showInput = effectiveShowInput,
                                        isFullscreen = isFullscreen,
                                        showFabs = !isSheetOpen,
                                        onRecover = {
                                            if (isFullscreen) mainScreenViewModel.toggleFullscreen()
                                            if (!mainState.showInput) mainScreenViewModel.toggleInput()
                                            mainScreenViewModel.resetGestureState()
                                        },
                                        contentPadding = PaddingValues(
                                            top = chatTopPadding + 56.dp,
                                            bottom = paddingValues.calculateBottomPadding() + when {
                                                effectiveShowInput -> inputHeightDp
                                                !isFullscreen      -> when {
                                                    helperTextHeightDp > 0.dp -> helperTextHeightDp
                                                    else                      -> max(navBarHeightDp, effectiveRoundedCorner)
                                                }

                                                else               -> when {
                                                    helperTextHeightDp > 0.dp -> helperTextHeightDp
                                                    else                      -> effectiveRoundedCorner
                                                }
                                            }
                                        ),
                                        scrollModifier = chatScrollModifier,
                                        onScrollToBottom = { mainScreenViewModel.setGestureToolbarHidden(false) },
                                        onScrollDirectionChanged = { },
                                        scrollToMessageId = scrollTargets[channel],
                                        onScrollToMessageHandled = { scrollTargets.remove(channel) },
                                        recoveryFabTooltipState = if (featureTourState.currentTourStep == TourStep.RecoveryFab) featureTourViewModel.recoveryFabTooltipState else null,
                                        onTourAdvance = featureTourViewModel::advance,
                                        onTourSkip = featureTourViewModel::skipTour,
                                    )
                                }
                            }

                            // Edge gesture guards — consume touch to prevent pager swipes near screen edges.
                            // Uses physical left/right (not logical start/end) since system gesture
                            // insets are always physical regardless of layout direction.
                            val systemGestureInsets = WindowInsets.systemGestures
                            val edgeGuardModifier = Modifier
                                .fillMaxHeight()
                                .pointerInput(Unit) {
                                    awaitEachGesture {
                                        val down = awaitFirstDown(pass = PointerEventPass.Initial)
                                        down.consume()
                                        do {
                                            val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                                            event.changes.forEach { it.consume() }
                                        } while (event.changes.any { it.pressed })
                                    }
                                }

                            // Left edge guard
                            Box(
                                modifier = Modifier
                                    .align(AbsoluteAlignment.CenterLeft)
                                    .width(with(density) { systemGestureInsets.getLeft(density, layoutDirection).toDp() })
                                    .then(edgeGuardModifier)
                            )
                            // Right edge guard
                            Box(
                                modifier = Modifier
                                    .align(AbsoluteAlignment.CenterRight)
                                    .width(with(density) { systemGestureInsets.getRight(density, layoutDirection).toDp() })
                                    .then(edgeGuardModifier)
                            )
                        }
                    }
                }
            }
        }

        // Shared fullscreen sheet overlay
        val fullScreenSheetOverlay: @Composable (Dp) -> Unit = { bottomPadding ->
            val effectiveBottomPadding = when {
                !effectiveShowInput -> bottomPadding + max(navBarHeightDp, effectiveRoundedCorner)
                else                -> bottomPadding
            }
            FullScreenSheetOverlay(
                sheetState = fullScreenSheetState,
                isLoggedIn = isLoggedIn,
                mentionViewModel = mentionViewModel,
                onDismiss = sheetNavigationViewModel::closeFullScreenSheet,
                onDismissReplies = {
                    sheetNavigationViewModel.closeFullScreenSheet()
                    chatInputViewModel.setReplying(false)
                },
                onUserClick = dialogViewModel::showUserPopup,
                onMessageLongClick = dialogViewModel::showMessageOptions,
                onEmoteClick = dialogViewModel::showEmoteInfo,
                onWhisperReply = chatInputViewModel::setWhisperTarget,
                bottomContentPadding = effectiveBottomPadding,
            )
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
                    Box(
                        modifier = Modifier
                            .weight(splitFraction)
                            .fillMaxSize()
                    ) {
                        StreamView(
                            channel = currentStream,
                            streamViewModel = streamViewModel,
                            fillPane = true,
                            onClose = {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                                streamViewModel.closeStream()
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Right pane: Chat + all overlays
                    Box(
                        modifier = Modifier
                            .weight(1f - splitFraction)
                            .fillMaxSize()
                    ) {
                        val statusBarTop = with(density) { WindowInsets.statusBars.getTop(density).toDp() }

                        Scaffold(
                            modifier = modifier
                                .fillMaxSize()
                                .padding(bottom = scaffoldBottomPadding),
                            contentWindowInsets = WindowInsets(0),
                            snackbarHost = {
                                SnackbarHost(
                                    hostState = snackbarHostState,
                                    modifier = Modifier.padding(bottom = inputHeightDp),
                                )
                            },
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

                        // Status bar scrim when toolbar is gesture-hidden
                        if (!isFullscreen && mainState.gestureToolbarHidden) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .fillMaxWidth()
                                    .height(with(density) { WindowInsets.statusBars.getTop(density).toDp() })
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                            )
                        }

                        fullScreenSheetOverlay(inputHeightDp + scaffoldBottomPadding)

                        // Dismiss scrim for input overflow menu
                        if (inputOverflowExpanded) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable(
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() },
                                    ) {
                                        if (!featureTourState.forceOverflowOpen) {
                                            inputOverflowExpanded = false
                                        }
                                    }
                            )
                        }

                        // Input bar - rendered after sheet overlay so it's on top
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = scaffoldBottomPadding)
                                .swipeDownToHide(
                                    enabled = effectiveShowInput,
                                    thresholdPx = swipeDownThresholdPx,
                                    onHide = { mainScreenViewModel.setGestureInputHidden(true) },
                                )
                        ) {
                            bottomBar()
                        }

                        emoteMenuLayer(Modifier.align(Alignment.BottomCenter))

                        if (effectiveShowInput && isKeyboardVisible) {
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
                    snackbarHost = {
                        SnackbarHost(
                            hostState = snackbarHostState,
                            modifier = Modifier.padding(bottom = inputHeightDp),
                        )
                    },
                ) { paddingValues ->
                    val chatTopPadding = maxOf(with(density) { WindowInsets.statusBars.getTop(density).toDp() }, streamState.heightDp * streamState.alpha.value)
                    scaffoldContent(paddingValues, chatTopPadding)
                }
            } // end !isInPipMode

            // Stream View layer
            currentStream?.let { channel ->
                val showStream = isInPipMode || !isKeyboardVisible || isLandscape
                // Delay adding StreamView to composition to prevent WebView flash on first open.
                // If the WebView was already attached (e.g. switching from wide layout), skip the delay.
                var streamComposed by remember { mutableStateOf(streamViewModel.hasWebViewBeenAttached) }
                LaunchedEffect(showStream) {
                    if (showStream) {
                        delay(100)
                        streamComposed = true
                    } else {
                        streamComposed = false
                    }
                }
                if (showStream && streamComposed) {
                    StreamView(
                        channel = channel,
                        streamViewModel = streamViewModel,
                        isInPipMode = isInPipMode,
                        onClose = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            streamViewModel.closeStream()
                        },
                        modifier = if (isInPipMode) {
                            Modifier.fillMaxSize()
                        } else {
                            Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxWidth()
                                .graphicsLayer { alpha = streamState.alpha.value }
                                .onGloballyPositioned { coordinates ->
                                    streamState.heightDp = with(density) { coordinates.size.height.toDp() }
                                }
                        }
                    )
                }
                if (!showStream) {
                    streamState.heightDp = 0.dp
                }
            }

            // Status bar scrim when stream is active — fades with stream/toolbar
            if (currentStream != null && !isFullscreen && !isInPipMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(with(density) { WindowInsets.statusBars.getTop(density).toDp() })
                        .graphicsLayer { alpha = streamState.alpha.value }
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

            // Status bar scrim when toolbar is gesture-hidden — keeps status bar readable
            if (!isInPipMode && !isFullscreen && mainState.gestureToolbarHidden) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(with(density) { WindowInsets.statusBars.getTop(density).toDp() })
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                )
            }

            // Fullscreen Overlay Sheets — after toolbar/scrims so sheets render on top
            if (!isInPipMode) {
                fullScreenSheetOverlay(inputHeightDp + scaffoldBottomPadding)
            }

            // Dismiss scrim for input overflow menu — before input bar so menu items stay clickable
            if (!isInPipMode && inputOverflowExpanded) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                        ) {
                            if (!featureTourState.forceOverflowOpen) {
                                inputOverflowExpanded = false
                            }
                        }
                )
            }

            // Input bar — on top of sheets and dismiss scrim for whisper/reply input
            if (!isInPipMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = scaffoldBottomPadding)
                        .swipeDownToHide(
                            enabled = effectiveShowInput,
                            thresholdPx = swipeDownThresholdPx,
                            onHide = { mainScreenViewModel.setGestureInputHidden(true) },
                        )
                ) {
                    bottomBar()
                }
            }

            // Emote Menu Layer - slides up/down independently of keyboard
            // Fast tween to match system keyboard animation speed
            if (!isInPipMode) emoteMenuLayer(Modifier.align(Alignment.BottomCenter))

            if (!isInPipMode && effectiveShowInput && isKeyboardVisible) {
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

@Stable
private class StreamToolbarState(
    val alpha: Animatable<Float, AnimationVector1D>,
) {
    var heightDp by mutableStateOf(0.dp)
    private var prevHasVisibleStream by mutableStateOf(false)
    private var isKeyboardClosingWithStream by mutableStateOf(false)
    private var wasKeyboardClosingWithStream by mutableStateOf(false)

    val hasVisibleStream: Boolean
        get() = heightDp > 0.dp

    /**
     * Returns the effective toolbar alpha, accounting for the bridge state
     * between keyboard closing and stream becoming visible.
     */
    val effectiveAlpha: Float
        get() = if (hasVisibleStream || isKeyboardClosingWithStream || wasKeyboardClosingWithStream) alpha.value else 1f

    suspend fun updateAnimation(hasVisibleStream: Boolean, keyboardClosingWithStream: Boolean) {
        isKeyboardClosingWithStream = keyboardClosingWithStream
        if (keyboardClosingWithStream) wasKeyboardClosingWithStream = true
        if (hasVisibleStream) wasKeyboardClosingWithStream = false

        when {
            keyboardClosingWithStream                                     -> {
                alpha.animateTo(0f, tween(durationMillis = 150))
            }

            hasVisibleStream && hasVisibleStream != prevHasVisibleStream  -> {
                prevHasVisibleStream = hasVisibleStream
                alpha.snapTo(0f)
                alpha.animateTo(1f, tween(durationMillis = 350))
            }

            !hasVisibleStream && hasVisibleStream != prevHasVisibleStream -> {
                prevHasVisibleStream = hasVisibleStream
                alpha.snapTo(0f)
            }
        }
    }
}

@Composable
private fun rememberStreamToolbarState(
    currentStream: UserName?,
    isKeyboardVisible: Boolean,
    imeTargetBottom: Int,
): StreamToolbarState {
    val state = remember { StreamToolbarState(alpha = Animatable(0f)) }

    val hasVisibleStream = currentStream != null && state.heightDp > 0.dp
    val isKeyboardClosingWithStream = currentStream != null && isKeyboardVisible && imeTargetBottom == 0

    LaunchedEffect(hasVisibleStream, isKeyboardClosingWithStream) {
        state.updateAnimation(hasVisibleStream, isKeyboardClosingWithStream)
    }
    LaunchedEffect(currentStream) {
        if (currentStream == null) state.heightDp = 0.dp
    }

    return state
}

