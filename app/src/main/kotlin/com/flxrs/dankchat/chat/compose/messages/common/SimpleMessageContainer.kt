package com.flxrs.dankchat.chat.compose.messages.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.flxrs.dankchat.chat.compose.ChatMessageText
import com.flxrs.dankchat.chat.compose.rememberAdaptiveTextColor
import com.flxrs.dankchat.chat.compose.rememberBackgroundColor

/**
 * A simple message container for system messages, notices, and other simple message types.
 * Handles background color, padding, and text rendering consistently.
 */
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

    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(bgColor)
            .padding(vertical = 2.dp)
            .alpha(textAlpha)
    ) {
        ChatMessageText(
            text = message,
            timestamp = timestamp,
            fontSize = fontSize,
            textColor = textColor,
        )
    }
}