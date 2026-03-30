package com.flxrs.dankchat.ui.main.dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.InsertEmoticon
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.flxrs.dankchat.R
import com.flxrs.dankchat.ui.chat.emote.EmoteSheetItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmoteInfoDialog(
    items: List<EmoteSheetItem>,
    isLoggedIn: Boolean,
    onUseEmote: (String) -> Unit,
    onCopyEmote: (String) -> Unit,
    onOpenLink: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { items.size })

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (items.size > 1) {
                PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
                    items.forEachIndexed { index, item ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                scope.launch { pagerState.animateScrollToPage(index) }
                            },
                            text = { Text(item.name) },
                        )
                    }
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth(),
            ) { page ->
                val item = items[page]
                EmoteInfoContent(
                    item = item,
                    showUseEmote = isLoggedIn,
                    onUseEmote = {
                        onUseEmote(item.name)
                        onDismiss()
                    },
                    onCopyEmote = {
                        onCopyEmote(item.name)
                        onDismiss()
                    },
                    onOpenLink = {
                        onOpenLink(item.providerUrl)
                        onDismiss()
                    },
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun EmoteInfoContent(
    item: EmoteSheetItem,
    showUseEmote: Boolean,
    onUseEmote: () -> Unit,
    onCopyEmote: () -> Unit,
    onOpenLink: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = stringResource(R.string.emote_sheet_image_description),
                modifier = Modifier.size(96.dp),
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(item.emoteType),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
                item.baseName?.let {
                    Text(
                        text = stringResource(R.string.emote_sheet_alias_of, it),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                }
                item.creatorName?.let {
                    Text(
                        text = stringResource(R.string.emote_sheet_created_by, it),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                }
                Text(
                    text = if (item.isZeroWidth) stringResource(R.string.emote_sheet_zero_width_emote) else "",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                )
            }
        }

        if (showUseEmote) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.emote_sheet_use)) },
                leadingContent = { Icon(Icons.Default.InsertEmoticon, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onUseEmote),
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
        }
        ListItem(
            headlineContent = { Text(stringResource(R.string.emote_sheet_copy)) },
            leadingContent = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
            modifier = Modifier.clickable(onClick = onCopyEmote),
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.emote_sheet_open_link)) },
            leadingContent = { Icon(Icons.Default.OpenInBrowser, contentDescription = null) },
            modifier = Modifier.clickable(onClick = onOpenLink),
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
    }
}
