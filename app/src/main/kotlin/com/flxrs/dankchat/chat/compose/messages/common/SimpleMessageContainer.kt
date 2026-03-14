package com.flxrs.dankchat.chat.compose.messages.common

import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
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
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.core.net.toUri
import com.flxrs.dankchat.chat.compose.appendWithLinks
import com.flxrs.dankchat.chat.compose.rememberAdaptiveTextColor
import com.flxrs.dankchat.chat.compose.rememberBackgroundColor

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

    val annotatedString = remember(message, timestamp, textColor, linkColor, timestampColor, fontSize) {
        buildAnnotatedString {
            withStyle(
                SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = fontSize * 0.95f,
                    color = timestampColor,
                    letterSpacing = (-0.03).em,
                )
            ) {
                append(timestamp)
            }
            append(" ")
            withStyle(SpanStyle(color = textColor)) {
                appendWithLinks(message, linkColor)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(bgColor)
            .padding(vertical = 2.dp)
            .alpha(textAlpha)
    ) {
        ClickableText(
            text = annotatedString,
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
                            Log.e("SimpleMessageContainer", "Error launching URL", e)
                        }
                    }
            }
        )
    }
}
