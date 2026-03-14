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
        // Unread or Mentioned -> High visibility (OnSurface)
        tab.mentionCount > 0 || tab.hasUnread -> MaterialTheme.colorScheme.onSurface
        // Idle -> Lower visibility
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
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
