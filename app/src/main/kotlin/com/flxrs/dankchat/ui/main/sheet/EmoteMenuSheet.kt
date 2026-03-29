package com.flxrs.dankchat.ui.main.sheet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SheetState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.flxrs.dankchat.R
import com.flxrs.dankchat.ui.chat.emotemenu.EmoteItem
import com.flxrs.dankchat.ui.chat.emotemenu.EmoteMenuTab
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmoteMenuSheet(onDismiss: () -> Unit, onEmoteClick: (String, String) -> Unit, sheetState: SheetState, viewModel: EmoteMenuViewModel = koinViewModel()) {
    val tabItems by viewModel.emoteTabItems.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { tabItems.size },
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.height(400.dp), // Fixed height for emote menu
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
                tabItems.forEachIndexed { index, tabItem ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = {
                            Text(
                                text = when (tabItem.type) {
                                    EmoteMenuTab.RECENT -> stringResource(R.string.emote_menu_tab_recent)
                                    EmoteMenuTab.SUBS -> stringResource(R.string.emote_menu_tab_subs)
                                    EmoteMenuTab.CHANNEL -> stringResource(R.string.emote_menu_tab_channel)
                                    EmoteMenuTab.GLOBAL -> stringResource(R.string.emote_menu_tab_global)
                                },
                            )
                        },
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1,
            ) { page ->
                val items = tabItems[page].items
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 40.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(
                        items = items,
                        key = { item ->
                            when (item) {
                                is EmoteItem.Emote -> "emote-${item.emote.id}-${item.emote.code}"
                                is EmoteItem.Header -> "header-${item.title}"
                            }
                        },
                        span = { item ->
                            when (item) {
                                is EmoteItem.Header -> GridItemSpan(maxLineSpan)
                                is EmoteItem.Emote -> GridItemSpan(1)
                            }
                        },
                        contentType = { item ->
                            when (item) {
                                is EmoteItem.Header -> "header"
                                is EmoteItem.Emote -> "emote"
                            }
                        },
                    ) { item ->
                        when (item) {
                            is EmoteItem.Header -> {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                )
                            }

                            is EmoteItem.Emote -> {
                                AsyncImage(
                                    model = item.emote.url,
                                    contentDescription = item.emote.code,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .clickable { onEmoteClick(item.emote.code, item.emote.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
