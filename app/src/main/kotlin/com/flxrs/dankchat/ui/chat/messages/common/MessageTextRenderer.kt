package com.flxrs.dankchat.ui.chat.messages.common

import android.content.Context
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.flxrs.dankchat.ui.chat.BadgeUi
import com.flxrs.dankchat.ui.chat.EmoteUi
import com.flxrs.dankchat.ui.chat.emote.EmoteSheetData
import com.flxrs.dankchat.ui.chat.emote.LocalEmoteAnimationCoordinator
import com.flxrs.dankchat.ui.chat.emote.StackedEmote
import com.flxrs.dankchat.ui.chat.emote.emoteBaseHeight
import com.flxrs.dankchat.ui.chat.emote.toEmoteSheetData
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toImmutableMap

private val logger = KotlinLogging.logger("MessageTextRenderer")

@Composable
fun MessageTextWithInlineContent(
    annotatedString: AnnotatedString,
    badges: ImmutableList<BadgeUi>,
    emotes: ImmutableList<EmoteUi>,
    fontSize: Float,
    animateGifs: Boolean,
    onTextClick: (Int) -> Unit,
    onEmoteClick: (List<EmoteSheetData>) -> Unit,
    modifier: Modifier = Modifier,
    onTextLongClick: ((Int) -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    val emoteCoordinator = LocalEmoteAnimationCoordinator.current
    val density = LocalDensity.current

    val badgeSize = emoteBaseHeight(fontSize)
    val inlineContentProviders: ImmutableMap<String, @Composable () -> Unit> =
        remember(badges, emotes, fontSize, animateGifs) {
            buildMap<String, @Composable () -> Unit> {
                badges.forEach { badge ->
                    put("BADGE_${badge.position}") {
                        BadgeInlineContent(badge = badge, size = badgeSize)
                    }
                }

                emotes.forEach { emote ->
                    put("EMOTE_${emote.position}") {
                        StackedEmote(
                            emote = emote,
                            fontSize = fontSize,
                            emoteCoordinator = emoteCoordinator,
                            animateGifs = animateGifs,
                            modifier = Modifier,
                            onClick = { onEmoteClick(emote.emotes.map { it.toEmoteSheetData() }) },
                        )
                    }
                }
            }.toImmutableMap()
        }

    val baseHeightPx = with(density) { badgeSize.toPx().toInt() }
    var dimensionRefresh by remember(badges, emotes, fontSize) { mutableIntStateOf(0) }
    val knownDimensions =
        remember(badges, emotes, fontSize, emoteCoordinator, dimensionRefresh) {
            buildMap {
                badges.forEach { badge ->
                    put("BADGE_${badge.position}", EmoteDimensions("BADGE_${badge.position}", baseHeightPx, baseHeightPx))
                }

                emotes.forEach { emote ->
                    val id = "EMOTE_${emote.position}"
                    val dims = emoteCoordinator.getDimensions(emote.dimensionKey(baseHeightPx))
                    if (dims != null) {
                        put(id, EmoteDimensions(id, dims.first, dims.second))
                    }
                }
            }.toImmutableMap()
        }

    // Rows composed before their emotes loaded wait for the dimensions to land, then upgrade
    // to the measured text fast path
    val hasMissingDimensions = emotes.any { "EMOTE_${it.position}" !in knownDimensions }
    if (hasMissingDimensions) {
        LaunchedEffect(badges, emotes, fontSize) {
            emoteCoordinator.dimensionUpdates.collect {
                val allKnown = emotes.all { emote -> emoteCoordinator.getDimensions(emote.dimensionKey(baseHeightPx)) != null }
                if (allKnown) {
                    dimensionRefresh++
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

private fun EmoteUi.dimensionKey(baseHeightPx: Int): String = when {
    urls.size == 1 -> urls.first()
    else -> "${emotes.joinToString("-") { it.id }}-$baseHeightPx"
}

fun launchCustomTab(
    context: Context,
    url: String,
) {
    try {
        CustomTabsIntent
            .Builder()
            .setShowTitle(true)
            .build()
            .launchUrl(context, url.toUri())
    } catch (e: Exception) {
        logger.error(e) { "Error launching URL" }
    }
}

fun timestampSpanStyle(
    fontSize: Float,
    color: Color,
) = SpanStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Bold,
    fontSize = (fontSize * 0.95f).sp,
    color = color,
    letterSpacing = (-0.03).em,
)

private const val SPACER_PREFIX = "spacer_"

fun AnnotatedString.Builder.appendInlineSpacer(width: Dp) {
    appendInlineContent("$SPACER_PREFIX${width.value}", " ")
}

fun isSpacerId(id: String): Boolean = id.startsWith(SPACER_PREFIX)

fun spacerWidthDp(id: String): Float = id.removePrefix(SPACER_PREFIX).toFloatOrNull() ?: 0f
