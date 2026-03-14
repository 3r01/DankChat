package com.flxrs.dankchat.main.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeAnimationSource
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flxrs.dankchat.chat.compose.BadgeUi
import com.flxrs.dankchat.chat.compose.ChatComposable
import com.flxrs.dankchat.data.twitch.emote.ChatMessageEmote
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    isLoggedIn: Boolean,
    onNavigateToSettings: () -> Unit,
    onUserClick: (userId: String?, userName: String, displayName: String, channel: String?, badges: List<BadgeUi>, isLongPress: Boolean) -> Unit,
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

    // Single state collection per ViewModel
    val tabState by channelTabViewModel.uiState.collectAsStateWithLifecycle()
    val pagerState by channelPagerViewModel.uiState.collectAsStateWithLifecycle()
    val inputState by chatInputViewModel.uiState.collectAsStateWithLifecycle()

    val composePagerState = rememberPagerState(
        initialPage = pagerState.currentPage,
        pageCount = { pagerState.channels.size }
    )
    val coroutineScope = rememberCoroutineScope()
    val density = androidx.compose.ui.platform.LocalDensity.current
    var inputHeight by remember { mutableStateOf(0.dp) }

    // Track keyboard visibility - hide immediately when closing animation starts
    val imeAnimationTarget = WindowInsets.imeAnimationTarget
    val isKeyboardVisible = WindowInsets.isImeVisible &&
        imeAnimationTarget.getBottom(density) > 0  // Target open = keyboard will be visible

    // Sync Compose pager with ViewModel state
    LaunchedEffect(pagerState.currentPage) {
        if (composePagerState.currentPage != pagerState.currentPage &&
            pagerState.currentPage < pagerState.channels.size) {
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
            MainAppBar(
                isLoggedIn = isLoggedIn,
                hasChannels = tabState.tabs.isNotEmpty(),
                totalMentionCount = tabState.tabs.sumOf { it.mentionCount },
                onAddChannel = onAddChannel,
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
            if (tabState.tabs.isEmpty()) {
                EmptyStateContent(
                    isLoggedIn = isLoggedIn,
                    onAddChannel = onAddChannel,
                    onLogin = onLogin,
                    modifier = Modifier.padding(paddingValues)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // Tabs - Single state from ChannelTabViewModel
                    PrimaryScrollableTabRow(
                        selectedTabIndex = tabState.selectedIndex,
                    ) {
                        tabState.tabs.forEachIndexed { index, tab ->
                            Tab(
                                selected = tab.isSelected,
                                onClick = {
                                    channelTabViewModel.selectTab(index)
                                    coroutineScope.launch {
                                        composePagerState.animateScrollToPage(index)
                                    }
                                },
                                text = {
                                    BadgedBox(
                                        badge = {
                                            if (tab.mentionCount > 0) {
                                                Badge { Text("${tab.mentionCount}") }
                                            }
                                        }
                                    ) {
                                        Text(text = tab.displayName)
                                    }
                                }
                            )
                        }
                    }

                    // Chat pager - State from ChannelPagerViewModel
                    HorizontalPager(
                        state = composePagerState,
                        modifier = Modifier.weight(1f)
                    ) { page ->
                        if (page in pagerState.channels.indices) {
                            val channel = pagerState.channels[page]
                            ChatComposable(
                                channel = channel,
                                onUserClick = onUserClick,
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
