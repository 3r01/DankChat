package com.flxrs.dankchat.main.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.runtime.Composable
import kotlinx.collections.immutable.ImmutableList

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChannelTabRow(
    tabs: ImmutableList<ChannelTabItem>,
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
