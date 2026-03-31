package com.flxrs.dankchat.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import com.flxrs.dankchat.preferences.appearance.AppearanceSettingsDataStore
import org.koin.compose.koinInject

// True dark surface tones based on achromatic M3 neutral palette, compressed toward black.
// Standard dark tones: 0,  4,  4,  6,  9, 12, 15, 18
// True dark tones:     0,  0,  0,  2,  4,  6,  8, 10
private val TrueDarkColorScheme =
    darkColorScheme(
        surface = Color.Black,
        surfaceDim = Color.Black,
        surfaceBright = Color(0xFF222222),
        surfaceContainerLowest = Color.Black,
        surfaceContainerLow = Color(0xFF0A0A0A),
        surfaceContainer = Color(0xFF0E0E0E),
        surfaceContainerHigh = Color(0xFF141414),
        surfaceContainerHighest = Color(0xFF1C1C1C),
        background = Color.Black,
        onSurface = Color.White,
        onBackground = Color.White,
    )

data class AdaptiveColors(
    val onSurfaceLight: Color,
    val onSurfaceDark: Color,
)

val LocalAdaptiveColors =
    staticCompositionLocalOf {
        AdaptiveColors(
            onSurfaceLight = lightColorScheme().onSurface,
            onSurfaceDark = darkColorScheme().onSurface,
        )
    }

@Composable
fun DankChatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val inspectionMode = LocalInspectionMode.current
    val appearanceSettings = if (!inspectionMode) koinInject<AppearanceSettingsDataStore>() else null
    val trueDarkTheme = remember { appearanceSettings?.current()?.trueDarkTheme == true }

    // Dynamic color is available on Android 12+
    val dynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val lightColorScheme =
        when {
            dynamicColor -> dynamicLightColorScheme(LocalContext.current)
            else -> expressiveLightColorScheme()
        }
    val darkColorScheme =
        when {
            dynamicColor && trueDarkTheme -> {
                dynamicDarkColorScheme(LocalContext.current).copy(
                    surface = TrueDarkColorScheme.surface,
                    surfaceDim = TrueDarkColorScheme.surfaceDim,
                    surfaceBright = TrueDarkColorScheme.surfaceBright,
                    surfaceContainerLowest = TrueDarkColorScheme.surfaceContainerLowest,
                    surfaceContainerLow = TrueDarkColorScheme.surfaceContainerLow,
                    surfaceContainer = TrueDarkColorScheme.surfaceContainer,
                    surfaceContainerHigh = TrueDarkColorScheme.surfaceContainerHigh,
                    surfaceContainerHighest = TrueDarkColorScheme.surfaceContainerHighest,
                    background = TrueDarkColorScheme.background,
                )
            }

            dynamicColor -> {
                dynamicDarkColorScheme(LocalContext.current)
            }

            else -> {
                darkColorScheme()
            }
        }

    val adaptiveColors =
        AdaptiveColors(
            onSurfaceLight = lightColorScheme.onSurface,
            onSurfaceDark = darkColorScheme.onSurface,
        )
    val colors =
        when {
            darkTheme -> darkColorScheme
            else -> lightColorScheme
        }

    MaterialExpressiveTheme(
        motionScheme = MotionScheme.expressive(),
        colorScheme = colors,
    ) {
        CompositionLocalProvider(LocalAdaptiveColors provides adaptiveColors) {
            content()
        }
    }
}
