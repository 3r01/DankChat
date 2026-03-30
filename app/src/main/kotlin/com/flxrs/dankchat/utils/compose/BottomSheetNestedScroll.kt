package com.flxrs.dankchat.utils.compose

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity

/**
 * Consumes leftover fling velocity from child scrollables, preventing it from propagating
 * to a parent [androidx.compose.material3.ModalBottomSheet] and prematurely dismissing it.
 *
 * Unlike consuming all post-scroll, this only intercepts fling overshoots while still allowing
 * normal drag gestures to propagate — so the sheet can still be dismissed by dragging down
 * when the content is scrolled to the top.
 *
 * Workaround for https://issuetracker.google.com/issues/353304855
 */
object BottomSheetNestedScrollConnection : NestedScrollConnection {
    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset =
        when (source) {
            NestedScrollSource.SideEffect -> available.copy(x = 0f)
            else -> Offset.Zero
        }

    override suspend fun onPostFling(
        consumed: Velocity,
        available: Velocity,
    ): Velocity = available.copy(x = 0f)
}
