package com.flxrs.dankchat.chat.compose.messages.common

import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.flxrs.dankchat.chat.compose.BadgeUi
import com.flxrs.dankchat.chat.compose.EmoteAnimationCoordinator
import com.flxrs.dankchat.chat.compose.EmoteScaling
import com.flxrs.dankchat.chat.compose.EmoteUi
import com.flxrs.dankchat.data.DisplayName
import com.flxrs.dankchat.data.UserId
import com.flxrs.dankchat.data.UserName

/**
 * Appends a formatted timestamp to the AnnotatedString builder.
 */
fun AnnotatedString.Builder.appendTimestamp(
    timestamp: String,
    fontSize: TextUnit,
    color: Color
) {
    if (timestamp.isNotEmpty()) {
        withStyle(
            SpanStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = (fontSize.value * 0.95f).sp,
                color = color,
                letterSpacing = (-0.03).em
            )
        ) {
            append(timestamp)
            append(" ")
        }
    }
}

/**
 * Appends badge inline content markers to the AnnotatedString builder.
 */
fun AnnotatedString.Builder.appendBadges(badges: List<BadgeUi>) {
    badges.forEach { badge ->
        appendInlineContent("BADGE_${badge.position}", "[badge]")
        append(" ")
    }
}

/**
 * Appends message text with emotes, handling emote inline content and spacing.
 */
fun AnnotatedString.Builder.appendMessageWithEmotes(
    message: String,
    emotes: List<EmoteUi>,
    textColor: Color
) {
    withStyle(SpanStyle(color = textColor)) {
        var currentPos = 0
        emotes.sortedBy { it.position.first }.forEach { emote ->
            // Text before emote
            if (currentPos < emote.position.first) {
                append(message.substring(currentPos, emote.position.first))
            }

            // Emote inline content
            appendInlineContent("EMOTE_${emote.code}", emote.code)

            // Cheer amount text
            if (emote.cheerAmount != null) {
                withStyle(
                    SpanStyle(
                        color = emote.cheerColor ?: textColor,
                        fontWeight = FontWeight.Bold,
                    )
                ) {
                    append(emote.cheerAmount.toString())
                }
            }

            // Add space after emote if next character exists and is not whitespace
            val nextPos = emote.position.last + 1
            if (nextPos < message.length && !message[nextPos].isWhitespace()) {
                append(" ")
            }

            currentPos = emote.position.last + 1
        }

        // Remaining text
        if (currentPos < message.length) {
            append(message.substring(currentPos))
        }
    }
}

/**
 * Appends a clickable username with annotation for click handling.
 */
fun AnnotatedString.Builder.appendClickableUsername(
    displayText: String,
    userId: UserId?,
    userName: UserName,
    displayName: DisplayName,
    channel: String = "",
    color: Color
) {
    if (displayText.isNotEmpty()) {
        withStyle(
            SpanStyle(
                fontWeight = FontWeight.Bold,
                color = color
            )
        ) {
            val annotation = if (channel.isNotEmpty()) {
                "${userId?.value ?: ""}|${userName.value}|${displayName.value}|$channel"
            } else {
                "${userId?.value ?: ""}|${userName.value}|${displayName.value}"
            }
            pushStringAnnotation(
                tag = "USER",
                annotation = annotation
            )
            append(displayText)
            pop()
        }
    }
}

/**
 * Builds inline content providers for badges and emotes.
 */
@Composable
fun buildInlineContentProviders(
    badges: List<BadgeUi>,
    emotes: List<EmoteUi>,
    fontSize: Float,
    coordinator: EmoteAnimationCoordinator,
    animateGifs: Boolean,
    onEmoteClick: (List<EmoteUi>) -> Unit
): Map<String, @Composable () -> Unit> {
    val badgeSize = EmoteScaling.getBadgeSize(fontSize)
    
    return buildMap {
        // Badge providers
        badges.forEach { badge ->
            put("BADGE_${badge.position}") {
                BadgeInlineContent(badge = badge, size = badgeSize)
            }
        }

        // Emote providers
        emotes.forEach { emote ->
            put("EMOTE_${emote.code}") {
                EmoteInlineContent(
                    emote = emote,
                    fontSize = fontSize,
                    coordinator = coordinator,
                    animateGifs = animateGifs,
                    onClick = { onEmoteClick(emotes) }
                )
            }
        }
    }
}
