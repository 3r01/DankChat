package com.flxrs.dankchat.ui.chat.emote

import android.graphics.drawable.Drawable
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.LayoutDirection

@Stable
class EmoteDrawablePainter(
    val drawable: Drawable,
    private val emoteCoordinator: EmoteAnimationCoordinator,
    private val invalidationsEnabled: Boolean = true,
) : Painter(),
    RememberObserver {
    private var invalidateTick by mutableIntStateOf(0)

    private val invalidationListener: () -> Unit = { invalidateTick++ }

    override val intrinsicSize: Size
        get() {
            val bounds = drawable.bounds
            return if (bounds.width() > 0 && bounds.height() > 0) {
                Size(bounds.width().toFloat(), bounds.height().toFloat())
            } else {
                Size(drawable.intrinsicWidth.toFloat(), drawable.intrinsicHeight.toFloat())
            }
        }

    override fun applyLayoutDirection(layoutDirection: LayoutDirection): Boolean = false

    override fun DrawScope.onDraw() {
        // Read invalidateTick so animation frames invalidate this draw scope
        invalidateTick
        drawIntoCanvas { canvas ->
            drawable.draw(canvas.nativeCanvas)
        }
    }

    override fun onRemembered() {
        // Offscreen pages skip registration, so their animation frames never invalidate a node
        if (invalidationsEnabled) {
            emoteCoordinator.registerInvalidationListener(drawable, invalidationListener)
        }
    }

    override fun onForgotten() {
        if (invalidationsEnabled) {
            emoteCoordinator.unregisterInvalidationListener(drawable, invalidationListener)
        }
    }

    override fun onAbandoned() {
        onForgotten()
    }
}
