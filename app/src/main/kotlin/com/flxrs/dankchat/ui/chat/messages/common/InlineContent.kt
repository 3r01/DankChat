package com.flxrs.dankchat.ui.chat.messages.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import com.flxrs.dankchat.data.twitch.badge.Badge
import com.flxrs.dankchat.ui.chat.BadgeUi
import com.flxrs.dankchat.ui.chat.EmoteAnimationCoordinator
import com.flxrs.dankchat.ui.chat.EmoteUi
import com.flxrs.dankchat.ui.chat.StackedEmote

private val FfzModGreen = Color(0xFF34AE0A)

/**
 * Renders a badge as inline content in a message.
 * FFZ mod badges get a green background fill since the badge image is foreground-only.
 */
@Composable
fun BadgeInlineContent(badge: BadgeUi, size: Dp, modifier: Modifier = Modifier) {
    when (badge.badge) {
        is Badge.FFZModBadge -> {
            Box(
                modifier = modifier
                    .size(size)
                    .background(FfzModGreen),
            ) {
                AsyncImage(
                    model = badge.url,
                    contentDescription = badge.badge.type.name,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        is Badge.SharedChatBadge -> {
            AsyncImage(
                model = badge.drawableResId ?: badge.url,
                contentDescription = badge.badge.type.name,
                modifier = modifier
                    .size(size)
                    .clip(CircleShape),
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

/**
 * Renders an emote (potentially stacked) as inline content in a message.
 */
@Composable
fun EmoteInlineContent(emote: EmoteUi, fontSize: Float, coordinator: EmoteAnimationCoordinator, animateGifs: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    StackedEmote(
        emote = emote,
        fontSize = fontSize,
        emoteCoordinator = coordinator,
        animateGifs = animateGifs,
        modifier = modifier,
        onClick = onClick,
    )
}
