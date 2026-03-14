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

import com.flxrs.dankchat.main.compose.dialogs.AddChannelDialog

import com.flxrs.dankchat.chat.user.compose.UserPopupDialog
import com.flxrs.dankchat.chat.user.UserPopupStateParams
import com.flxrs.dankchat.data.UserId
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.DisplayName

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    isLoggedIn: Boolean,
    onNavigateToSettings: () -> Unit,
    onMessageLongClick: (messageId: String, channel: String?, fullMessage: String) -> Unit,
    onEmoteClick: (List<ChatMessageEmote>) -> Unit,
    onLogin: () -> Unit,
    onRelogin: () -> Unit,
    onLogout: () -> Unit,
    onManageChannels: () -> Unit,
    onOpenChannel: () -> Unit,
    onRemoveChannel: () -> Unit,
    onReportChannel: () -> Unit,
    onBlockChannel: () -> Unit,
    onReloadEmotes: () -> Unit,
    onReconnect: () -> Unit,
    onClearChat: () -> Unit,
    onCaptureImage: () -> Unit,
    onCaptureVideo: () -> Unit,
    onChooseMedia: () -> Unit,
    onAddChannel: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Scoped ViewModels - each handles one concern
    val mainScreenViewModel: MainScreenViewModel = koinViewModel()
    val channelManagementViewModel: ChannelManagementViewModel = koinViewModel()
    val channelTabViewModel: ChannelTabViewModel = koinViewModel()
    val channelPagerViewModel: ChannelPagerViewModel = koinViewModel()
    val chatInputViewModel: ChatInputViewModel = koinViewModel()
    val sheetNavigationViewModel: SheetNavigationViewModel = koinViewModel()
    val scope = rememberCoroutineScope()

    var showAddChannelDialog by remember { mutableStateOf(false) }
    var userPopupParams by remember { mutableStateOf<UserPopupStateParams?>(null) }

    if (showAddChannelDialog) {
        AddChannelDialog(
            onDismiss = { showAddChannelDialog = false },
            onAddChannel = { 
                channelManagementViewModel.addChannel(it)
                showAddChannelDialog = false
            }
        )
    }

    if (userPopupParams != null) {
        UserPopupDialog(
            params = userPopupParams!!,
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

    val tabState = channelTabViewModel.uiState.collectAsStateWithLifecycle().value
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
            imeAnimationTarget.getBottom(density) > 0  // Target open = keyboard will be visible

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
                onLogout = onLogout,
                onManageChannels = onManageChannels,
                onOpenChannel = onOpenChannel,
                onRemoveChannel = onRemoveChannel,
                onReportChannel = onReportChannel,
                onBlockChannel = onBlockChannel,
                onReloadEmotes = onReloadEmotes,
                onReconnect = onReconnect,
                onClearChat = onClearChat,
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
                                    // Always open popup for now (long press handled same as click)
                                    userPopupParams = UserPopupStateParams(
                                        targetUserId = userId?.let { UserId(it) } ?: UserId(""),
                                        targetUserName = UserName(userName),
                                        targetDisplayName = DisplayName(displayName),
                                        channel = channel?.let { UserName(it) },
                                        badges = badges.map { it.badge }
                                    )
                                },
                                onMessageLongClick = onMessageLongClick,
                                onEmoteClick = onEmoteClick,
                                onReplyClick = { replyMessageId ->
                                    sheetNavigationViewModel.openReplies(replyMessageId)
                                }
                            )
                        }
                    }

                    // Input - State from ChatInputViewModel
                    ChatInputLayout(
                        textFieldState = chatInputViewModel.textFieldState,
                        canSend = inputState.canSend,
                        onSend = chatInputViewModel::sendMessage,
                        onEmoteClick = { sheetNavigationViewModel.openEmoteSheet() },
                        modifier = Modifier.onGloballyPositioned { coordinates ->
                            inputHeight = with(density) { coordinates.size.height.toDp() }
                        }
                    )
                }
            }

            // Suggestion dropdown floats above input field (only when keyboard visible)
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
