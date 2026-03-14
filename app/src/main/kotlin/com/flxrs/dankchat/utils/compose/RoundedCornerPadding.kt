package com.flxrs.dankchat.utils.compose

import android.os.Build
import android.view.RoundedCorner
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import kotlin.math.max
import kotlin.math.sin

/**
 * Adds padding to avoid content being clipped by rounded display corners.
 * 
 * This modifier:
 * 1. Gets the component's position in window coordinates
 * 2. Checks if the component intersects with any rounded corner boundaries
 * 3. Adds padding only where needed to push content into the safe area
 * 
 * Uses the 45-degree boundary method from Android documentation.
 */
fun Modifier.avoidRoundedCorners(fallback: PaddingValues): Modifier = composed {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        return@composed this.padding(fallback)
    }

    val view = LocalView.current
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current

    var paddingStart by remember { mutableStateOf(fallback.calculateStartPadding(direction)) }
    var paddingTop by remember { mutableStateOf(0.dp) }
    var paddingEnd by remember { mutableStateOf(fallback.calculateEndPadding(direction)) }
    var paddingBottom by remember { mutableStateOf(0.dp) }

    this
        .onGloballyPositioned { coordinates ->
            val compatInsets = ViewCompat.getRootWindowInsets(view) ?: return@onGloballyPositioned
            val windowInsets = compatInsets.toWindowInsets() ?: return@onGloballyPositioned

            // Get component position and size in window coordinates
            val position = coordinates.positionInWindow()
            val componentLeft = position.x.toInt()
            val componentTop = position.y.toInt()
            val componentRight = componentLeft + coordinates.size.width
            val componentBottom = componentTop + coordinates.size.height

            // Check all four corners
            val topLeft = windowInsets.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)
            val topRight = windowInsets.getRoundedCorner(RoundedCorner.POSITION_TOP_RIGHT)
            val bottomLeft = windowInsets.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_LEFT)
            val bottomRight = windowInsets.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_RIGHT)

            // Calculate padding for each side
            paddingTop = with(density) {
                maxOf(
                    topLeft?.calculateTopPaddingForComponent(componentLeft, componentTop) ?: 0,
                    topRight?.calculateTopPaddingForComponent(componentRight, componentTop) ?: 0
                ).toDp()
            }

            paddingBottom = with(density) {
                maxOf(
                    bottomLeft?.calculateBottomPaddingForComponent(componentLeft, componentBottom) ?: 0,
                    bottomRight?.calculateBottomPaddingForComponent(componentRight, componentBottom) ?: 0
                ).toDp()
            }

            paddingStart = with(density) {
                maxOf(
                    topLeft?.calculateStartPaddingForComponent(componentLeft, componentTop) ?: 0,
                    bottomLeft?.calculateStartPaddingForComponent(componentLeft, componentBottom) ?: 0
                ).toDp()
            }

            paddingEnd = with(density) {
                maxOf(
                    topRight?.calculateEndPaddingForComponent(componentRight, componentTop) ?: 0,
                    bottomRight?.calculateEndPaddingForComponent(componentRight, componentBottom) ?: 0
                ).toDp()
            }
        }
        .padding(
            start = paddingStart,
            top = paddingTop,
            end = paddingEnd,
            bottom = paddingBottom
        )
}

@RequiresApi(api = 31)
private fun RoundedCorner.calculateTopPaddingForComponent(
    componentX: Int,
    componentTop: Int
): Int {
    val offset = (radius * sin(Math.toRadians(45.0))).toInt()
    val topBoundary = center.y - offset
    val leftBoundary = center.x - offset
    val rightBoundary = center.x + offset

    if (componentX !in leftBoundary..rightBoundary) {
        return 0
    }

    return max(0, topBoundary - componentTop)
}

@RequiresApi(api = 31)
private fun RoundedCorner.calculateBottomPaddingForComponent(
    componentX: Int,
    componentBottom: Int
): Int {
    val offset = (radius * sin(Math.toRadians(45.0))).toInt()
    val bottomBoundary = center.y + offset
    val leftBoundary = center.x - offset
    val rightBoundary = center.x + offset

    if (componentX !in leftBoundary..rightBoundary) {
        return 0
    }

    return max(0, componentBottom - bottomBoundary)
}

@RequiresApi(api = 31)
private fun RoundedCorner.calculateStartPaddingForComponent(
    componentLeft: Int,
    componentY: Int
): Int {
    val offset = (radius * sin(Math.toRadians(45.0))).toInt()
    val leftBoundary = center.x - offset
    val topBoundary = center.y - offset
    val bottomBoundary = center.y + offset

    if (componentY !in topBoundary..bottomBoundary) {
        return 0
    }

    return max(0, leftBoundary - componentLeft)
}

@RequiresApi(api = 31)
private fun RoundedCorner.calculateEndPaddingForComponent(
    componentRight: Int,
    componentY: Int
): Int {
    val offset = (radius * sin(Math.toRadians(45.0))).toInt()
    val rightBoundary = center.x + offset
    val topBoundary = center.y - offset
    val bottomBoundary = center.y + offset

    if (componentY !in topBoundary..bottomBoundary) {
        return 0
    }

    return max(0, componentRight - rightBoundary)
}
