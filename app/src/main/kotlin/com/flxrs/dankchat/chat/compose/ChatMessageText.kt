package com.flxrs.dankchat.chat.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em

/**
 * Renders a chat message text with support for:
 * - Timestamps (monospace, bold)
 * - Username colors
 * - Emotes and badges (via InlineTextContent)
 * - Clickable spans (usernames, links, emotes)
 * 
 * NOTE: fontSize should come from appearanceSettings.fontSize, not be hardcoded
 * NOTE: nameColor should come from the message's nameColor, not be hardcoded
 */
@Composable
fun ChatMessageText(
    text: String,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    textColor: Color? = null,
    timestamp: String? = null,
    nameText: String? = null,
    nameColor: Color? = null,
    isAction: Boolean = false,
    inlineContent: Map<String, InlineTextContent> = emptyMap(),
) {
    val timestampColor = MaterialTheme.colorScheme.onSurface
    val defaultTextColor = textColor ?: MaterialTheme.colorScheme.onSurface
    val defaultNameColor = nameColor ?: MaterialTheme.colorScheme.onSurface

    val annotatedString = remember(text, timestamp, nameText, defaultNameColor, isAction, defaultTextColor, timestampColor, fontSize) {
        buildAnnotatedString {
            // Add timestamp if present
            if (timestamp != null) {
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
            }

            // Add username if present
            if (nameText != null) {
                withStyle(
                    SpanStyle(
                        color = defaultNameColor,
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
                    color = if (isAction) defaultNameColor else defaultTextColor
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
