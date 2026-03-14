package com.flxrs.dankchat.chat.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Renders a chat message text with support for:
 * - Timestamps (monospace, bold)
 * - Username colors
 * - Emotes and badges (via InlineTextContent)
 * - Clickable spans (usernames, links, emotes)
 */
@Composable
fun ChatMessageText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 14.sp,
    textColor: Color = Color.White,
    timestamp: String? = null,
    nameText: String? = null,
    nameColor: Color = Color.Gray,
    isAction: Boolean = false,
    inlineContent: Map<String, InlineTextContent> = emptyMap(),
) {
    val annotatedString = remember(text, timestamp, nameText, nameColor, isAction, textColor) {
        buildAnnotatedString {
            // Add timestamp if present
            if (timestamp != null) {
                withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = fontSize * 0.95f,
                        letterSpacing = 0.05.sp
                    )
                ) {
                    append(timestamp)
                }
                append(" ")
            }

            // Add username if present
            if (nameText != null) {
                withStyle(
                    SpanStyle(
                        color = nameColor,
                        fontWeight = FontWeight.Bold
                    )
                ) {
                    append(nameText)
                }
                if (!isAction) {
                    append(": ")
                } else {
                    append(" ")
                }
            }

            // Add message text
            withStyle(
                SpanStyle(
                    color = if (isAction) nameColor else textColor
                )
            ) {
                append(text)
            }
        }
    }

    Box(modifier = modifier.padding(horizontal = 8.dp)) {
        BasicText(
            text = annotatedString,
            modifier = Modifier.fillMaxWidth(),
            inlineContent = inlineContent
        )
    }
}
