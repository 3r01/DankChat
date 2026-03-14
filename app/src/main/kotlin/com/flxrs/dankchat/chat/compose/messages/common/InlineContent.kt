package com.flxrs.dankchat.chat.compose.messages.common

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import com.flxrs.dankchat.chat.compose.BadgeUi
import com.flxrs.dankchat.chat.compose.EmoteAnimationCoordinator
import com.flxrs.dankchat.chat.compose.EmoteUi
import com.flxrs.dankchat.chat.compose.StackedEmote

/**
 * Renders a badge as inline content in a message.
 */
@Composable
fun BadgeInlineContent(
    badge: BadgeUi,
    size: Dp,
    modifier: Modifier = Modifier
) {
    AsyncImage(
        model = badge.url,
        contentDescription = badge.badge.type.name,
        modifier = modifier.size(size)
    )
}

/**
 * Renders an emote (potentially stacked) as inline content in a message.
 */
@Composable
fun EmoteInlineContent(
    emote: EmoteUi,
    fontSize: Float,
    coordinator: EmoteAnimationCoordinator,
    animateGifs: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    StackedEmote(
        emote = emote,
        fontSize = fontSize,
        emoteCoordinator = coordinator,
        animateGifs = animateGifs,
        modifier = modifier,
        onClick = onClick
    )
}
