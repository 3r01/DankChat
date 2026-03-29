package com.flxrs.dankchat.ui.chat

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import com.google.android.material.color.MaterialColors

/**
 * Selects the appropriate background color based on current theme.
 * Semi-transparent colors (e.g. checkered backgrounds) are composited over the
 * theme background to produce an opaque result suitable for contrast calculations.
 */
@Composable
fun rememberBackgroundColor(lightColor: Color, darkColor: Color): Color {
    val raw = if (isSystemInDarkTheme()) darkColor else lightColor
    val background = MaterialTheme.colorScheme.background
    return remember(raw, background) {
        when {
            raw == Color.Transparent -> Color.Transparent
            raw.alpha < 1f -> raw.compositeOver(background)
            else -> raw
        }
    }
}

/**
 * Returns the opaque checkered background color for the current theme.
 * Composites [inverseSurface] at low alpha over [background], matching the
 * old adapter's [MaterialColors.layer] behavior.
 */
@Composable
fun rememberCheckeredBackgroundColor(): Color {
    val background = MaterialTheme.colorScheme.background
    val inverseSurface = MaterialTheme.colorScheme.inverseSurface
    return remember(background, inverseSurface) {
        inverseSurface.copy(alpha = MaterialColors.ALPHA_DISABLED_LOW).compositeOver(background)
    }
}
