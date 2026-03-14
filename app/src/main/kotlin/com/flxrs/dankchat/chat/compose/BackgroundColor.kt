package com.flxrs.dankchat.chat.compose

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Selects the appropriate background color based on current theme.
 */
@Composable
fun rememberBackgroundColor(lightColor: Color, darkColor: Color): Color {
    return if (isSystemInDarkTheme()) darkColor else lightColor
}
