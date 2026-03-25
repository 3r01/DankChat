package com.flxrs.dankchat.ui.chat.messages.common

import android.content.Context
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.flxrs.dankchat.data.twitch.emote.ChatMessageEmote
import com.flxrs.dankchat.ui.chat.BadgeUi
import com.flxrs.dankchat.ui.chat.EmoteDimensions
import com.flxrs.dankchat.ui.chat.EmoteScaling
import com.flxrs.dankchat.ui.chat.EmoteUi
import com.flxrs.dankchat.ui.chat.LocalEmoteAnimationCoordinator
import com.flxrs.dankchat.ui.chat.StackedEmote
import com.flxrs.dankchat.ui.chat.TextWithMeasuredInlineContent
import kotlinx.collections.immutable.ImmutableList

@Composable
fun MessageTextWithInlineContent(
    annotatedString: AnnotatedString,
    badges: ImmutableList<BadgeUi>,
    emotes: ImmutableList<EmoteUi>,
    fontSize: Float,
    animateGifs: Boolean,
    onTextClick: (Int) -> Unit,
    onEmoteClick: (List<ChatMessageEmote>) -> Unit,
    modifier: Modifier = Modifier,
    onTextLongClick: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    val emoteCoordinator = LocalEmoteAnimationCoordinator.current
    val density = LocalDensity.current

    val badgeSize = EmoteScaling.getBadgeSize(fontSize)
    val inlineContentProviders: Map<String, @Composable () -> Unit> = remember(badges, emotes, fontSize) {
        buildMap<String, @Composable () -> Unit> {
            badges.forEach { badge ->
                put("BADGE_${badge.position}") {
                    BadgeInlineContent(badge = badge, size = badgeSize)
                }
            }

            emotes.forEach { emote ->
                put("EMOTE_${emote.code}") {
                    StackedEmote(
                        emote = emote,
                        fontSize = fontSize,
                        emoteCoordinator = emoteCoordinator,
                        animateGifs = animateGifs,
                        modifier = Modifier,
                        onClick = { onEmoteClick(emote.emotes) }
                    )
                }
            }
        }
    }

    val knownDimensions = remember(badges, emotes, fontSize, emoteCoordinator) {
        buildMap {
            val badgeSizePx = with(density) { badgeSize.toPx().toInt() }
            badges.forEach { badge ->
                put("BADGE_${badge.position}", EmoteDimensions("BADGE_${badge.position}", badgeSizePx, badgeSizePx))
            }

            val baseHeight = EmoteScaling.getBaseHeight(fontSize)
            val baseHeightPx = with(density) { baseHeight.toPx().toInt() }
            emotes.forEach { emote ->
                val id = "EMOTE_${emote.code}"
                when {
                    emote.urls.size == 1 -> {
                        val dims = emoteCoordinator.dimensionCache.get(emote.urls.first())
                        if (dims != null) {
                            put(id, EmoteDimensions(id, dims.first, dims.second))
                        }
                    }

                    else                 -> {
                        val cacheKey = "${emote.emotes.joinToString("-") { it.id }}-$baseHeightPx"
                        val dims = emoteCoordinator.dimensionCache.get(cacheKey)
                        if (dims != null) {
                            put(id, EmoteDimensions(id, dims.first, dims.second))
                        }
                    }
                }
            }
        }
    }

    TextWithMeasuredInlineContent(
        text = annotatedString,
        inlineContentProviders = inlineContentProviders,
        style = TextStyle(fontSize = fontSize.sp),
        knownDimensions = knownDimensions,
        modifier = modifier.fillMaxWidth(),
        interactionSource = interactionSource,
        onTextClick = onTextClick,
        onTextLongClick = onTextLongClick,
    )
}

fun launchCustomTab(context: Context, url: String) {
    try {
        CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
            .launchUrl(context, url.toUri())
    } catch (e: Exception) {
        Log.e("MessageUrl", "Error launching URL", e)
    }
}

fun timestampSpanStyle(fontSize: Float, color: Color) = SpanStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Bold,
    fontSize = (fontSize * 0.95f).sp,
    color = color,
    letterSpacing = (-0.03).em,
)
