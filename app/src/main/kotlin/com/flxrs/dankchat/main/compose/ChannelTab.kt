package com.flxrs.dankchat.main.compose

import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun ChannelTab(
    tab: ChannelTabItem,
    onClick: () -> Unit
) {
    val tabColor = when {
        tab.isSelected -> MaterialTheme.colorScheme.primary
        tab.mentionCount > 0 -> MaterialTheme.colorScheme.error
        tab.hasUnread -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Tab(
        selected = tab.isSelected,
        onClick = onClick,
        selectedContentColor = tabColor,
        unselectedContentColor = tabColor,
        text = {
            BadgedBox(
                badge = {
                    if (tab.mentionCount > 0) {
                        // TODO could add mention count as text
                        Badge()
                    }
                }
            ) {
                Text(
                    text = tab.displayName,
                    color = tabColor
                )
            }
        }
    )
}
