package com.flxrs.dankchat.main.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChannelTabRow(
    tabs: List<ChannelTabItem>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    PrimaryScrollableTabRow(
        selectedTabIndex = selectedIndex,
    ) {
        tabs.forEachIndexed { index, tab ->
            ChannelTab(
                tab = tab,
                onClick = {
                    onTabSelected(index)
                }
            )
        }
    }
}
