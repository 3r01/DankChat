package com.flxrs.dankchat.main.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.ui.Modifier
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
    val mainScreenViewModel: MainScreenViewModel = koinViewModel()
    val sheetNavigationViewModel: SheetNavigationViewModel = koinViewModel()

    val channels by mainScreenViewModel.channels.collectAsStateWithLifecycle()
    val activeChannel by mainScreenViewModel.activeChannel.collectAsStateWithLifecycle()
    val activeChannelIndex by mainScreenViewModel.activeChannelIndex.collectAsStateWithLifecycle()
    val unreadMessagesMap by mainScreenViewModel.unreadMessagesMap.collectAsStateWithLifecycle()
    val channelMentionCount by mainScreenViewModel.channelMentionCount.collectAsStateWithLifecycle()
    val totalMentionCount by mainScreenViewModel.totalMentionCount.collectAsStateWithLifecycle()

    // Simple local input state (will be replaced with ChatInputViewModel later)
    var inputText by remember { mutableStateOf("") }

    val pagerState = rememberPagerState(
        initialPage = activeChannelIndex,
        pageCount = { channels.size }
    )
    val coroutineScope = rememberCoroutineScope()

    // Sync pager with active channel
    LaunchedEffect(activeChannelIndex) {
        if (pagerState.currentPage != activeChannelIndex && activeChannelIndex < channels.size) {
            pagerState.animateScrollToPage(activeChannelIndex)
        }
    }

    // Update active channel when user swipes
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage < channels.size) {
            val channel = channels[pagerState.currentPage].channel
            mainScreenViewModel.setActiveChannel(channel)
        }
    }

    Scaffold(
        topBar = {
            MainAppBar(
                isLoggedIn = isLoggedIn,
                hasChannels = channels.isNotEmpty(),
                totalMentionCount = totalMentionCount,
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
        if (channels.isEmpty()) {
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
                // Tabs
                PrimaryScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage,
                ) {
                    channels.forEachIndexed { index, channelWithRename ->
                        val channel = channelWithRename.channel
                        val displayName = channelWithRename.rename?.value ?: channel.value
                        val hasUnread = unreadMessagesMap[channel] == true
                        val mentionCount = channelMentionCount[channel] ?: 0

                        Tab(
                            selected = index == pagerState.currentPage,
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            text = {
                                BadgedBox(
                                    badge = {
                                        // TODO proper viewmodel state for this
                                        if (mentionCount > 0) {
                                            Badge()
                                        }
                                    },
                                    content = {
                                        Text(text = channel.value)
                                    }
                                )
                            },
                        )
                    }
                }

                // Chat pager
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f)
                ) { page ->
                    if (page in channels.indices) {
                        val channel = channels[page].channel
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

                // Input at bottom
                ChatInputLayout(
                    inputText = inputText,
                    onInputChange = { inputText = it },
                    onSend = {
                        // TODO: Implement send via ChatRepository
                        inputText = ""
                    },
                    onEmoteClick = { sheetNavigationViewModel.openEmoteSheet() },
                    modifier = Modifier
                )
            }
        }
    }
}
