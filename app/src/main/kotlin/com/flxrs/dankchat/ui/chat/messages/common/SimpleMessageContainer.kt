package com.flxrs.dankchat.ui.chat.messages.common

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.flxrs.dankchat.ui.chat.appendWithLinks
import com.flxrs.dankchat.ui.chat.rememberAdaptiveTextColor
import com.flxrs.dankchat.ui.chat.rememberBackgroundColor

/**
 * A simple message container for system messages, notices, and other simple message types.
 * Handles background color, padding, and text rendering consistently.
 * Supports clickable URLs in the message text.
 */
@Suppress("DEPRECATION")
@Composable
fun SimpleMessageContainer(
    message: String,
    timestamp: String,
    fontSize: TextUnit,
    lightBackgroundColor: Color,
    darkBackgroundColor: Color,
    textAlpha: Float,
    modifier: Modifier = Modifier,
) {
    val bgColor = rememberBackgroundColor(lightBackgroundColor, darkBackgroundColor)
    val textColor = rememberAdaptiveTextColor(bgColor)
    val linkColor = MaterialTheme.colorScheme.primary
    val timestampColor = MaterialTheme.colorScheme.onSurface
    val context = LocalContext.current

    val annotatedString =
        remember(message, timestamp, textColor, linkColor, timestampColor, fontSize) {
            buildAnnotatedString {
                withStyle(timestampSpanStyle(fontSize.value, timestampColor)) {
                    append(timestamp)
                }
                append(" ")
                withStyle(SpanStyle(color = textColor)) {
                    appendWithLinks(message, linkColor)
                }
            }
        }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .alpha(textAlpha)
                .background(bgColor)
                .padding(horizontal = 2.dp, vertical = 2.dp),
    ) {
        ClickableText(
            text = annotatedString,
            style = TextStyle(fontSize = fontSize),
            modifier = Modifier.fillMaxWidth(),
            onClick = { offset ->
                annotatedString
                    .getStringAnnotations("URL", offset, offset)
                    .firstOrNull()
                    ?.let { annotation ->
                        launchCustomTab(context, annotation.item)
                    }
            },
        )
    }
}
