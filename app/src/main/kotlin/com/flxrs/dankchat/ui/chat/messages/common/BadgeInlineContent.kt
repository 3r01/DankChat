package com.flxrs.dankchat.ui.chat.messages.common

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import com.flxrs.dankchat.data.twitch.badge.Badge
import com.flxrs.dankchat.ui.chat.BadgeUi

private val FfzVipShape = RoundedCornerShape(percent = 15)

@Composable
fun BadgeInlineContent(
    badge: BadgeUi,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    when (badge.badge) {
        is Badge.SharedChatBadge -> {
            AsyncImage(
                model = badge.drawableResId ?: badge.url,
                contentDescription = badge.badge.type.name,
                modifier =
                    modifier
                        .size(size)
                        .clip(CircleShape),
            )
        }

        is Badge.FFZVipBadge -> {
            AsyncImage(
                model = badge.url,
                contentDescription = badge.badge.type.name,
                modifier =
                    modifier
                        .size(size)
                        .clip(FfzVipShape),
            )
        }

        else -> {
            AsyncImage(
                model = badge.drawableResId ?: badge.url,
                contentDescription = badge.badge.type.name,
                modifier = modifier.size(size),
            )
        }
    }
}
