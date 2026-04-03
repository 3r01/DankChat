package com.flxrs.dankchat.ui.chat.messages.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

@Composable
fun SimpleMessageContainer(
    message: String,
    timestamp: String,
    fontSize: TextUnit,
    lightBackgroundColor: Color,
    darkBackgroundColor: Color,
    textAlpha: Float,
    modifier: Modifier = Modifier,
    timestampSpacerWidth: Dp = 6.dp,
) {
    val bgColor = rememberBackgroundColor(lightBackgroundColor, darkBackgroundColor)
    val textColor = rememberAdaptiveTextColor(bgColor)
    val linkColor = rememberAdaptiveLinkColor(bgColor)
    val timestampColor = MaterialTheme.colorScheme.onSurface

    val annotatedString =
        remember(message, timestamp, textColor, linkColor, timestampColor, fontSize, timestampSpacerWidth) {
            buildAnnotatedString {
                withStyle(timestampSpanStyle(fontSize.value, timestampColor)) {
                    append(timestamp)
                }
                appendInlineSpacer(timestampSpacerWidth)
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
                .padding(horizontal = 6.dp, vertical = 3.dp),
    ) {
        LinkableText(
            text = annotatedString,
            style = TextStyle(fontSize = fontSize),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
