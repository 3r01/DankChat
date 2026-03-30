package com.flxrs.dankchat.ui.chat.emote

import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
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
) : Painter(),
    androidx.compose.runtime.RememberObserver {
    private var invalidateTick by mutableIntStateOf(0)

    private val mainHandler = Handler(Looper.getMainLooper())

    private val callback =
        object : Drawable.Callback {
            override fun invalidateDrawable(d: Drawable) {
                invalidateTick++
            }

            override fun scheduleDrawable(
                d: Drawable,
                what: Runnable,
                time: Long,
            ) {
                mainHandler.postAtTime(what, time)
            }

            override fun unscheduleDrawable(
                d: Drawable,
                what: Runnable,
            ) {
                mainHandler.removeCallbacks(what)
            }
        }

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
        // Read invalidateTick to trigger recomposition on animation frames
        invalidateTick
        drawIntoCanvas { canvas ->
            drawable.draw(canvas.nativeCanvas)
        }
    }

    override fun onRemembered() {
        drawable.callback = callback
        drawable.setVisible(true, true)
    }

    override fun onForgotten() {
        drawable.setVisible(false, false)
        drawable.callback = null
    }

    override fun onAbandoned() {
        onForgotten()
    }
}
