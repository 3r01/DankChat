package com.flxrs.dankchat.ui.chat.emotemenu

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.flxrs.dankchat.R
import com.flxrs.dankchat.data.repo.emote.GifPickerLibrary
import com.flxrs.dankchat.data.twitch.emote.GenericEmote
import com.flxrs.dankchat.preferences.components.DankBackground
import com.flxrs.dankchat.ui.chat.emote.EmoteInfoViewModel
import com.flxrs.dankchat.ui.chat.emote.toEmoteSheetData
import com.flxrs.dankchat.ui.main.sheet.EmoteMenuViewModel
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmoteMenu(
    onEmoteClick: (String, String) -> Unit,
    allowGifs: Boolean,
    onMenuInputFocusChange: (Boolean) -> Unit,
    onGifPickerVisibleChange: (Boolean) -> Unit,
    onGifSendSuccess: () -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EmoteMenuViewModel = koinViewModel(),
    emoteInfoViewModel: EmoteInfoViewModel = koinViewModel(),
    gifPickerViewModel: GifPickerViewModel = koinViewModel(),
) {
    val allTabItems by viewModel.emoteTabItems.collectAsStateWithLifecycle()
    val gifsAvailable by gifPickerViewModel.isAvailable.collectAsStateWithLifecycle()
    val tabItems = remember(allTabItems, gifsAvailable, allowGifs) {
        if (gifsAvailable && allowGifs) allTabItems else allTabItems.filterNot { it.type == EmoteMenuTab.GIFS }
    }
    val selectedTabIndex by viewModel.selectedTabIndex.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val pagerState =
        rememberPagerState(
            initialPage = selectedTabIndex.coerceIn(0, tabItems.lastIndex.coerceAtLeast(0)),
            pageCount = { tabItems.size },
        )

    val currentTabIndex = pagerState.currentPage.coerceIn(0, tabItems.lastIndex.coerceAtLeast(0))
    val isGifPickerVisible = tabItems.getOrNull(currentTabIndex)?.type == EmoteMenuTab.GIFS
    DisposableEffect(isGifPickerVisible) {
        onGifPickerVisibleChange(isGifPickerVisible)
        onDispose { onGifPickerVisibleChange(false) }
    }
    LaunchedEffect(currentTabIndex) {
        viewModel.selectTab(currentTabIndex)
    }
    LaunchedEffect(tabItems.size) {
        if (pagerState.currentPage > tabItems.lastIndex) {
            pagerState.scrollToPage(tabItems.lastIndex.coerceAtLeast(0))
        }
    }
    val subsGridState = rememberLazyGridState()
    val subsFirstHeader =
        tabItems
            .getOrNull(EmoteMenuTab.SUBS.ordinal)
            ?.items
            ?.firstOrNull()
            ?.let { (it as? EmoteItem.Header)?.title }

    LaunchedEffect(subsFirstHeader) {
        subsGridState.scrollToItem(0)
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PrimaryTabRow(
                selectedTabIndex = currentTabIndex,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            ) {
                tabItems.forEachIndexed { index, tabItem ->
                    Tab(
                        selected = currentTabIndex == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = {
                            Text(
                                text =
                                    when (tabItem.type) {
                                        EmoteMenuTab.RECENT -> stringResource(R.string.emote_menu_tab_recent)
                                        EmoteMenuTab.SUBS -> stringResource(R.string.emote_menu_tab_subs)
                                        EmoteMenuTab.CHANNEL -> stringResource(R.string.emote_menu_tab_channel)
                                        EmoteMenuTab.GLOBAL -> stringResource(R.string.emote_menu_tab_global)
                                        EmoteMenuTab.GIFS -> stringResource(R.string.emote_menu_tab_gifs)
                                    },
                            )
                        },
                    )
                }
            }

            val navBarBottom = WindowInsets.navigationBars.getBottom(LocalDensity.current)
            val navBarBottomDp = with(LocalDensity.current) { navBarBottom.toDp() }

            Box(modifier = Modifier.fillMaxSize()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 1,
                ) { page ->
                    val tab = tabItems.getOrNull(page) ?: return@HorizontalPager
                    if (tab.type == EmoteMenuTab.GIFS) {
                        val gifState by gifPickerViewModel.state.collectAsStateWithLifecycle()
                        val library by gifPickerViewModel.library.collectAsStateWithLifecycle(GifPickerLibrary())
                        GifPickerPage(
                            isVisible = isGifPickerVisible,
                            state = gifState,
                            library = library,
                            navBarBottomDp = navBarBottomDp,
                            onSearchFocusChange = onMenuInputFocusChange,
                            onSearch = gifPickerViewModel::search,
                            onLoadMore = gifPickerViewModel::loadMore,
                            onGifClick = { gifPickerViewModel.send(it, onGifSendSuccess) },
                        )
                    } else {
                        EmoteGridPage(
                            tab = tab,
                            subsGridState = subsGridState,
                            navBarBottomDp = navBarBottomDp,
                            onEmoteClick = onEmoteClick,
                            onEmoteLongClick = { emote -> emoteInfoViewModel.show(listOf(emote.toEmoteSheetData())) },
                        )
                    }
                }

                if (tabItems.getOrNull(pagerState.currentPage)?.type != EmoteMenuTab.GIFS) {
                    // Floating backspace button at bottom-end, matching keyboard position
                    IconButton(
                        onClick = onBackspace,
                        colors =
                            IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            ),
                        modifier =
                            Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 8.dp, bottom = 8.dp + navBarBottomDp)
                                .size(48.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                            contentDescription = stringResource(R.string.backspace),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GifPickerPage(
    isVisible: Boolean,
    state: GifPickerState,
    library: GifPickerLibrary,
    navBarBottomDp: Dp,
    onSearchFocusChange: (Boolean) -> Unit,
    onSearch: (String) -> Unit,
    onLoadMore: () -> Unit,
    onGifClick: (com.flxrs.dankchat.data.api.twitchgql.TwitchGifPickerItem) -> Unit,
) {
    val favoriteIds = remember(library.favorites) { library.favorites.map { it.id }.toSet() }
    var section by remember { mutableStateOf(GifPickerSection.Browse) }
    var query by remember { mutableStateOf("") }
    DisposableEffect(Unit) {
        onDispose { onSearchFocusChange(false) }
    }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GifPickerSection.entries.forEach { item ->
                FilterChip(
                    selected = section == item,
                    onClick = { section = item },
                    label = { Text(stringResource(item.title)) },
                )
            }
        }
        OutlinedTextField(
            enabled = state !is GifPickerState.Sending,
            value = query,
            onValueChange = {
                query = it
                onSearch(it)
            },
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            placeholder = { Text(stringResource(R.string.gif_search)) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .onFocusChanged { onSearchFocusChange(it.isFocused) }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
        )
        Text(
            text = stringResource(R.string.gif_powered_by_giphy),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.End).padding(horizontal = 12.dp),
        )
        if (section != GifPickerSection.Browse) {
            val savedGifs = when (section) {
                GifPickerSection.Favorites -> library.favorites
                else -> library.recent
            }.filter { query.isBlank() || it.title.contains(query.trim(), ignoreCase = true) }
            if (savedGifs.isEmpty()) {
                GifPickerMessage(stringResource(R.string.gif_no_saved_results))
            } else {
                GifGrid(
                    gifs = savedGifs,
                    navBarBottomDp = navBarBottomDp,
                    enabled = state !is GifPickerState.Sending,
                    nextOffset = null,
                    isLoadingMore = false,
                    pageError = null,
                    isVisible = isVisible,
                    onLoadMore = {},
                    favoriteIds = favoriteIds,
                ) { onGifClick(it.copy(searchTerm = null)) }
            }
        } else {
            when (val current = state) {
                GifPickerState.Loading ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        androidx.compose.material3.CircularProgressIndicator()
                    }

                GifPickerState.Unavailable ->
                    GifPickerMessage(stringResource(R.string.gif_unavailable))

                is GifPickerState.Error -> GifPickerMessage(current.message)

                is GifPickerState.Ready, is GifPickerState.Sending -> {
                    val ready = current as? GifPickerState.Ready
                    GifGrid(
                        gifs = ready?.gifs ?: (current as GifPickerState.Sending).gifs,
                        navBarBottomDp = navBarBottomDp,
                        enabled = ready != null,
                        nextOffset = ready?.nextOffset,
                        isLoadingMore = ready?.isLoadingMore == true,
                        pageError = ready?.pageError,
                        isVisible = isVisible,
                        onLoadMore = onLoadMore,
                        favoriteIds = favoriteIds,
                    ) { onGifClick(it) }
                }
            }
        }
    }
}

private enum class GifPickerSection(
    val title: Int,
) {
    Browse(R.string.gif_browse),
    Favorites(R.string.gif_favorites),
    Recent(R.string.emote_menu_tab_recent),
}

@Composable
private fun GifPickerMessage(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(24.dp))
    }
}

@Composable
private fun GifGrid(
    gifs: List<com.flxrs.dankchat.data.api.twitchgql.TwitchGifPickerItem>,
    navBarBottomDp: Dp,
    nextOffset: Int?,
    isLoadingMore: Boolean,
    pageError: String?,
    isVisible: Boolean,
    onLoadMore: () -> Unit,
    favoriteIds: Set<String>,
    enabled: Boolean = true,
    onClick: (com.flxrs.dankchat.data.api.twitchgql.TwitchGifPickerItem) -> Unit,
) {
    var selectedGif by remember { mutableStateOf<com.flxrs.dankchat.data.api.twitchgql.TwitchGifPickerItem?>(null) }
    val gridState = rememberLazyGridState()
    LaunchedEffect(gridState, nextOffset, isLoadingMore, pageError, isVisible) {
        if (!isVisible || nextOffset == null || isLoadingMore || pageError != null) return@LaunchedEffect
        snapshotFlow {
            val layout = gridState.layoutInfo
            layout.viewportEndOffset > 0 && (gifs.isEmpty() || (layout.visibleItemsInfo.lastOrNull()?.index ?: -1) >= gifs.lastIndex)
        }.collect { reachedEnd ->
            if (reachedEnd) onLoadMore()
        }
    }
    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Adaptive(112.dp),
        contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = 56.dp + navBarBottomDp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(gifs, key = { it.id }) { gif ->
            Box {
                DropdownMenu(
                    expanded = selectedGif?.id == gif.id,
                    onDismissRequest = { selectedGif = null },
                    properties = PopupProperties(focusable = false),
                ) {
                    GifFavoriteAction(gif, onDone = { selectedGif = null })
                }
                AsyncImage(
                    model = gif.previewUrl,
                    contentDescription = gif.title,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .widthIn(min = 80.dp)
                            .aspectRatio((gif.width.toFloat() / gif.height.coerceAtLeast(1)).coerceIn(0.5f, 2f))
                            .pointerInput(gif, enabled) {
                                if (enabled) detectTapGestures(onTap = { onClick(gif) }, onLongPress = { selectedGif = gif })
                            },
                )
                if (gif.id in favoriteIds) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = stringResource(R.string.gif_favorites),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(18.dp),
                    )
                }
            }
        }
        if (isLoadingMore || pageError != null) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                    if (isLoadingMore) {
                        androidx.compose.material3.CircularProgressIndicator(Modifier.size(24.dp))
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(pageError.orEmpty(), style = MaterialTheme.typography.bodySmall)
                            TextButton(onClick = onLoadMore) { Text(stringResource(R.string.snackbar_retry)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmoteGridPage(
    tab: EmoteMenuTabItem,
    subsGridState: LazyGridState,
    navBarBottomDp: Dp,
    onEmoteClick: (code: String, id: String) -> Unit,
    onEmoteLongClick: (GenericEmote) -> Unit,
) {
    val items = tab.items

    if (tab.type == EmoteMenuTab.RECENT && items.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            DankBackground(visible = true)
            Text(
                text = stringResource(R.string.no_recent_emotes),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 160.dp),
            )
        }
    } else {
        val gridState =
            when (tab.type) {
                EmoteMenuTab.SUBS -> subsGridState
                else -> rememberLazyGridState()
            }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 40.dp),
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 56.dp + navBarBottomDp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(
                count = items.size,
                // Sections are unique by title and emote ids are deduplicated per section, so keys
                // stay stable across list shifts and items can be reused
                key = { index ->
                    when (val item = items[index]) {
                        is EmoteItem.Emote -> "emote-${item.emote.emoteType.title}-${item.emote.id}"
                        is EmoteItem.Header -> "header-${item.title}"
                    }
                },
                span = { index ->
                    when (items[index]) {
                        is EmoteItem.Header -> GridItemSpan(maxLineSpan)
                        is EmoteItem.Emote -> GridItemSpan(1)
                    }
                },
                contentType = { index ->
                    when (items[index]) {
                        is EmoteItem.Header -> "header"
                        is EmoteItem.Emote -> "emote"
                    }
                },
            ) { index ->
                val item = items[index]
                when (item) {
                    is EmoteItem.Header -> {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                        )
                    }

                    is EmoteItem.Emote -> {
                        AsyncImage(
                            model = item.emote.url,
                            contentDescription = item.emote.code,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .pointerInput(item.emote) {
                                        detectTapGestures(
                                            onTap = { onEmoteClick(item.emote.code, item.emote.id) },
                                            onLongPress = { onEmoteLongClick(item.emote) },
                                        )
                                    },
                        )
                    }
                }
            }
        }
    }
}
