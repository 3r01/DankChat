package com.flxrs.dankchat.main.compose.sheets

import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import com.flxrs.dankchat.R
import com.flxrs.dankchat.chat.compose.BadgeUi
import com.flxrs.dankchat.chat.mention.compose.MentionComposable
import com.flxrs.dankchat.chat.mention.compose.MentionComposeViewModel
import com.flxrs.dankchat.data.twitch.emote.ChatMessageEmote
import com.flxrs.dankchat.preferences.appearance.AppearanceSettingsDataStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MentionSheet(
    mentionViewModel: MentionComposeViewModel,
    initialisWhisperTab: Boolean,
    appearanceSettingsDataStore: AppearanceSettingsDataStore,
    inputHeight: androidx.compose.ui.unit.Dp,
    onDismiss: () -> Unit,
    onUserClick: (userId: String?, userName: String, displayName: String, channel: String?, badges: List<BadgeUi>, isLongPress: Boolean) -> Unit,
    onMessageLongClick: (messageId: String, channel: String?, fullMessage: String) -> Unit,
    onEmoteClick: (List<ChatMessageEmote>) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = if (initialisWhisperTab) 1 else 0,
        pageCount = { 2 }
    )
    var backProgress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(pagerState.currentPage) {
        mentionViewModel.setCurrentTab(pagerState.currentPage)
    }

    PredictiveBackHandler { progress ->
        try {
            progress.collect { event ->
                backProgress = event.progress
            }
            onDismiss()
        } catch (e: CancellationException) {
            backProgress = 0f
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                val scale = 1f - (backProgress * 0.1f)
                scaleX = scale
                scaleY = scale
                alpha = 1f - backProgress
                translationY = backProgress * 100f
            },
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(R.string.mentions_title)) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    }
                )
                PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
                    Tab(
                        selected = pagerState.currentPage == 0,
                        onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                        text = { Text(stringResource(R.string.mentions)) }
                    )
                    Tab(
                        selected = pagerState.currentPage == 1,
                        onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                        text = { Text(stringResource(R.string.whispers)) }
                    )
                }
            }
        },
    ) { paddingValues ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .padding(paddingValues)
                .padding(bottom = inputHeight)
                .fillMaxSize()
        ) { page ->
            MentionComposable(
                mentionViewModel = mentionViewModel,
                appearanceSettingsDataStore = appearanceSettingsDataStore,
                isWhisperTab = page == 1,
                onUserClick = onUserClick,
                onMessageLongClick = onMessageLongClick,
                onEmoteClick = onEmoteClick
            )
        }
    }


}