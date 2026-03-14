package com.flxrs.dankchat.main.compose.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.flxrs.dankchat.R
import com.flxrs.dankchat.chat.emotemenu.EmoteItem
import com.flxrs.dankchat.chat.emotemenu.EmoteMenuTab
import com.flxrs.dankchat.main.compose.EmoteMenuViewModel
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Surface
import com.flxrs.dankchat.preferences.components.DankBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmoteMenu(
    onEmoteClick: (String, String) -> Unit,
    viewModel: EmoteMenuViewModel = koinViewModel(),
    modifier: Modifier = Modifier
) {
    val tabItems by viewModel.emoteTabItems.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { tabItems.size }
    )
    val subsGridState = rememberLazyGridState()
    val subsFirstHeader = tabItems.getOrNull(EmoteMenuTab.SUBS.ordinal)
        ?.items?.firstOrNull()?.let { (it as? EmoteItem.Header)?.title }

    LaunchedEffect(subsFirstHeader) {
        subsGridState.scrollToItem(0)
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PrimaryTabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
            ) {
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
                                }
                            )
                        }
                    )
                }
            }

            val navBarBottom = WindowInsets.navigationBars.getBottom(LocalDensity.current)
            val navBarBottomDp = with(LocalDensity.current) { navBarBottom.toDp() }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1
            ) { page ->
                val tab = tabItems[page]
                val items = tab.items

                if (tab.type == EmoteMenuTab.RECENT && items.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        DankBackground(visible = true)
                        Text(
                            text = stringResource(R.string.no_recent_emotes),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 160.dp) // Offset below logo
                        )
                    }
                } else {
                    val gridState = if (tab.type == EmoteMenuTab.SUBS) subsGridState else rememberLazyGridState()
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 40.dp),
                        state = gridState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 8.dp + navBarBottomDp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                            }
                        ) { item ->
                            when (item) {
                                is EmoteItem.Header -> {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp)
                                    )
                                }

                                is EmoteItem.Emote -> {
                                    AsyncImage(
                                        model = item.emote.url,
                                        contentDescription = item.emote.code,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(1f)
                                            .clickable { onEmoteClick(item.emote.code, item.emote.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
