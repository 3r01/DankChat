package com.flxrs.dankchat.chat.compose

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.flxrs.dankchat.theme.LocalAdaptiveColors
import com.google.android.material.color.MaterialColors

/**
 * Returns appropriate text color (light or dark) based on background brightness.
 * Uses MaterialColors.isColorLight() to determine if background is light,
 * then selects dark text for light backgrounds and vice versa.
 * 
 * For transparent backgrounds, uses the surface color for brightness calculation
 * since that's what will be visible behind the text.
 */
@Composable
fun rememberAdaptiveTextColor(backgroundColor: Color): Color {
    val adaptiveColors = LocalAdaptiveColors.current
    val surfaceColor = MaterialTheme.colorScheme.surface
    
    // For transparent backgrounds, use surface color for calculation
    val effectiveBackground = if (backgroundColor == Color.Transparent) {
        surfaceColor
    } else {
        backgroundColor
    }
    
    val isLightBackground = MaterialColors.isColorLight(effectiveBackground.toArgb())
    
    return if (isLightBackground) {
        adaptiveColors.onSurfaceLight
    } else {
        adaptiveColors.onSurfaceDark
    }
}
