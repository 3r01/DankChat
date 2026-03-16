package com.flxrs.dankchat.chat.compose.messages

import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.flxrs.dankchat.chat.compose.ChatMessageUiState
import com.flxrs.dankchat.chat.compose.appendWithLinks
import com.flxrs.dankchat.chat.compose.messages.common.SimpleMessageContainer
import com.flxrs.dankchat.chat.compose.rememberAdaptiveTextColor
import com.flxrs.dankchat.chat.compose.rememberBackgroundColor
import com.flxrs.dankchat.chat.compose.rememberNormalizedColor
import com.flxrs.dankchat.chat.compose.resolve

/**
 * Renders a system message (connected, disconnected, emote loading failures, etc.)
 */
@Composable
fun SystemMessageComposable(
    message: ChatMessageUiState.SystemMessageUi,
    fontSize: Float,
    modifier: Modifier = Modifier,
) {
    SimpleMessageContainer(
        message = message.message.resolve(),
        timestamp = message.timestamp,
        fontSize = fontSize.sp,
        lightBackgroundColor = message.lightBackgroundColor,
        darkBackgroundColor = message.darkBackgroundColor,
        textAlpha = message.textAlpha,
        modifier = modifier,
    )
}

/**
 * Renders a notice message from Twitch
 */
@Composable
fun NoticeMessageComposable(
    message: ChatMessageUiState.NoticeMessageUi,
    fontSize: Float,
    modifier: Modifier = Modifier,
) {
    SimpleMessageContainer(
        message = message.message,
        timestamp = message.timestamp,
        fontSize = fontSize.sp,
        lightBackgroundColor = message.lightBackgroundColor,
        darkBackgroundColor = message.darkBackgroundColor,
        textAlpha = message.textAlpha,
        modifier = modifier,
    )
}

/**
 * Renders a user notice message (subscriptions, announcements, etc.)
 * The display name is highlighted with the user's color.
 */
@Suppress("DEPRECATION")
@Composable
fun UserNoticeMessageComposable(
    message: ChatMessageUiState.UserNoticeMessageUi,
    highlightShape: Shape = RectangleShape,
    fontSize: Float,
    modifier: Modifier = Modifier,
) {
    val bgColor = rememberBackgroundColor(message.lightBackgroundColor, message.darkBackgroundColor)
    val textColor = MaterialTheme.colorScheme.onSurface
    val linkColor = MaterialTheme.colorScheme.primary
    val timestampColor = MaterialTheme.colorScheme.onSurface
    val nameColor = rememberNormalizedColor(message.rawNameColor, bgColor)
    val textSize = fontSize.sp
    val context = LocalContext.current

    val annotatedString = remember(message, textColor, nameColor, linkColor, timestampColor, textSize) {
        buildAnnotatedString {
            // Timestamp
            if (message.timestamp.isNotEmpty()) {
                withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = textSize * 0.95f,
                        color = timestampColor,
                        letterSpacing = (-0.03).em,
                    )
                ) {
                    append(message.timestamp)
                }
                append(" ")
            }

            // Message text with colored display name
            val displayName = message.displayName
            val msgText = message.message
            val nameIndex = when {
                displayName.isNotEmpty() -> msgText.indexOf(displayName, ignoreCase = true)
                else                     -> -1
            }

            when {
                nameIndex >= 0 -> {
                    // Text before name
                    if (nameIndex > 0) {
                        withStyle(SpanStyle(color = textColor)) {
                            appendWithLinks(msgText.substring(0, nameIndex), linkColor)
                        }
                    }

                    // Colored username
                    withStyle(SpanStyle(color = nameColor, fontWeight = FontWeight.Bold)) {
                        append(msgText.substring(nameIndex, nameIndex + displayName.length))
                    }

                    // Text after name
                    val afterIndex = nameIndex + displayName.length
                    if (afterIndex < msgText.length) {
                        withStyle(SpanStyle(color = textColor)) {
                            appendWithLinks(msgText.substring(afterIndex), linkColor)
                        }
                    }
                }

                else           -> {
                    // No display name found, render as plain text
                    withStyle(SpanStyle(color = textColor)) {
                        appendWithLinks(msgText, linkColor)
                    }
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .alpha(message.textAlpha)
            .background(bgColor, highlightShape)
            .padding(horizontal = 2.dp, vertical = 2.dp)
    ) {
        ClickableText(
            text = annotatedString,
            style = TextStyle(fontSize = textSize),
            modifier = Modifier.fillMaxWidth(),
            onClick = { offset ->
                annotatedString.getStringAnnotations("URL", offset, offset)
                    .firstOrNull()?.let { annotation ->
                        try {
                            CustomTabsIntent.Builder()
                                .setShowTitle(true)
                                .build()
                                .launchUrl(context, annotation.item.toUri())
                        } catch (e: Exception) {
                            Log.e("UserNoticeMessage", "Error launching URL", e)
                        }
                    }
            }
        )
    }
}

/**
 * Renders a date separator between messages from different days
 */
@Composable
fun DateSeparatorComposable(
    message: ChatMessageUiState.DateSeparatorUi,
    fontSize: Float,
    modifier: Modifier = Modifier,
) {
    SimpleMessageContainer(
        message = message.dateText,
        timestamp = message.timestamp,
        fontSize = fontSize.sp,
        lightBackgroundColor = message.lightBackgroundColor,
        darkBackgroundColor = message.darkBackgroundColor,
        textAlpha = message.textAlpha,
        modifier = modifier,
    )
}

/**
 * Renders a moderation message (timeouts, bans, deletions) with colored usernames.
 */
@Suppress("DEPRECATION")
@Composable
fun ModerationMessageComposable(
    message: ChatMessageUiState.ModerationMessageUi,
    fontSize: Float,
    modifier: Modifier = Modifier,
) {
    val bgColor = rememberBackgroundColor(message.lightBackgroundColor, message.darkBackgroundColor)
    val textColor = rememberAdaptiveTextColor(bgColor)
    val timestampColor = MaterialTheme.colorScheme.onSurface
    val creatorColor = rememberNormalizedColor(message.creatorColor, bgColor)
    val targetColor = rememberNormalizedColor(message.targetColor, bgColor)
    val textSize = fontSize.sp
    val resolvedMessage = message.message.resolve()
    val context = LocalContext.current
    val linkColor = MaterialTheme.colorScheme.primary

    val annotatedString = remember(
        message, resolvedMessage, textColor, creatorColor, targetColor, linkColor, timestampColor, textSize
    ) {
        buildAnnotatedString {
            // Timestamp
            if (message.timestamp.isNotEmpty()) {
                withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = textSize * 0.95f,
                        color = timestampColor,
                        letterSpacing = (-0.03).em,
                    )
                ) {
                    append(message.timestamp)
                }
                append(" ")
            }

            // Build list of name ranges to color
            val nameRanges = buildList {
                var searchFrom = 0
                message.creatorName?.let { name ->
                    val idx = resolvedMessage.indexOf(name, startIndex = searchFrom, ignoreCase = true)
                    if (idx >= 0) {
                        add(Triple(idx, name.length, creatorColor))
                        searchFrom = idx + name.length
                    }
                }
                message.targetName?.let { name ->
                    val idx = resolvedMessage.indexOf(name, startIndex = searchFrom, ignoreCase = true)
                    if (idx >= 0) add(Triple(idx, name.length, targetColor))
                }
            }.sortedBy { it.first }

            // Render message with colored name segments
            var cursor = 0
            for ((start, length, color) in nameRanges) {
                if (start < cursor) continue // skip overlapping
                if (start > cursor) {
                    withStyle(SpanStyle(color = textColor)) {
                        appendWithLinks(resolvedMessage.substring(cursor, start), linkColor)
                    }
                }
                withStyle(SpanStyle(color = color, fontWeight = FontWeight.Bold)) {
                    append(resolvedMessage.substring(start, start + length))
                }
                cursor = start + length
            }
            if (cursor < resolvedMessage.length) {
                withStyle(SpanStyle(color = textColor)) {
                    appendWithLinks(resolvedMessage.substring(cursor), linkColor)
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .alpha(message.textAlpha)
            .background(bgColor)
            .padding(horizontal = 2.dp, vertical = 2.dp)
    ) {
        ClickableText(
            text = annotatedString,
            style = TextStyle(fontSize = textSize),
            modifier = Modifier.fillMaxWidth(),
            onClick = { offset ->
                annotatedString.getStringAnnotations("URL", offset, offset)
                    .firstOrNull()?.let { annotation ->
                        try {
                            CustomTabsIntent.Builder()
                                .setShowTitle(true)
                                .build()
                                .launchUrl(context, annotation.item.toUri())
                        } catch (e: Exception) {
                            Log.e("ModerationMessage", "Error launching URL", e)
                        }
                    }
            }
        )
    }
}
