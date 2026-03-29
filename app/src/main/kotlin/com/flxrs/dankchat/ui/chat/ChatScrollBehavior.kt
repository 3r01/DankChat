package com.flxrs.dankchat.ui.chat

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange

/**
 * Observes scroll direction and fires [onHide]/[onShow] when the accumulated
 * scroll delta exceeds [thresholdPx].
 *
 * With `reverseLayout = true` the nested scroll deltas are inverted:
 * `available.y > 0` = finger up = reading old messages = hide toolbar;
 * `available.y < 0` = finger down = toward new messages = show toolbar.
 *
 * Returns [Offset.Zero] — scroll is observed, never consumed.
 */
class ScrollDirectionTracker(private val hideThresholdPx: Float, private val showThresholdPx: Float, private val onHide: () -> Unit, private val onShow: () -> Unit) : NestedScrollConnection {
    private var accumulated = 0f

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        if (source != NestedScrollSource.UserInput) return Offset.Zero
        val delta = consumed.y
        if (delta == 0f) return Offset.Zero
        // Reset accumulator on direction change to avoid stale buildup
        when {
            accumulated > 0f && delta < 0f -> accumulated = 0f
            accumulated < 0f && delta > 0f -> accumulated = 0f
        }
        accumulated += delta
        when {
            accumulated > hideThresholdPx -> {
                onHide()
                accumulated = 0f
            }

            accumulated < -showThresholdPx -> {
                onShow()
                accumulated = 0f
            }
        }
        return Offset.Zero
    }
}

/**
 * Detects a cumulative downward drag exceeding [thresholdPx] and calls [onHide].
 * Uses [PointerEventPass.Initial] to observe events before children (text fields,
 * buttons) consume them. Events are never consumed so children still work normally.
 */
fun Modifier.swipeDownToHide(enabled: Boolean, thresholdPx: Float, onHide: () -> Unit): Modifier {
    if (!enabled) return this
    return this.pointerInput(enabled) {
        awaitEachGesture {
            awaitFirstDown(pass = PointerEventPass.Initial, requireUnconsumed = false)
            var totalDragY = 0f
            var fired = false
            while (true) {
                val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                val change = event.changes.firstOrNull() ?: break
                if (!change.pressed) break
                totalDragY += change.positionChange().y
                if (totalDragY > thresholdPx && !fired) {
                    fired = true
                    onHide()
                }
            }
        }
    }
}
