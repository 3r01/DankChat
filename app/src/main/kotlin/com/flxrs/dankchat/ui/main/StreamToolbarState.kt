package com.flxrs.dankchat.ui.main

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.flxrs.dankchat.data.UserName

@Stable
internal class StreamToolbarState(val alpha: Animatable<Float, AnimationVector1D>) {
    var heightDp by mutableStateOf(0.dp)
    private var prevHasVisibleStream by mutableStateOf(false)
    private var isKeyboardClosingWithStream by mutableStateOf(false)
    private var wasKeyboardClosingWithStream by mutableStateOf(false)

    val hasVisibleStream: Boolean
        get() = heightDp > 0.dp

    val effectiveAlpha: Float
        get() = if (hasVisibleStream || isKeyboardClosingWithStream || wasKeyboardClosingWithStream) alpha.value else 1f

    suspend fun updateAnimation(hasVisibleStream: Boolean, keyboardClosingWithStream: Boolean) {
        isKeyboardClosingWithStream = keyboardClosingWithStream
        if (keyboardClosingWithStream) wasKeyboardClosingWithStream = true
        if (hasVisibleStream) wasKeyboardClosingWithStream = false

        when {
            keyboardClosingWithStream -> {
                alpha.animateTo(0f, tween(durationMillis = 150))
            }

            hasVisibleStream && !prevHasVisibleStream -> {
                prevHasVisibleStream = true
                alpha.snapTo(0f)
                alpha.animateTo(1f, tween(durationMillis = 350))
            }

            !hasVisibleStream && prevHasVisibleStream -> {
                prevHasVisibleStream = false
                alpha.snapTo(0f)
            }
        }
    }
}

@Composable
internal fun rememberStreamToolbarState(currentStream: UserName?, isKeyboardVisible: Boolean, imeTargetBottom: Int): StreamToolbarState {
    val state = remember { StreamToolbarState(alpha = Animatable(0f)) }

    val hasVisibleStream = currentStream != null && state.heightDp > 0.dp
    val isKeyboardClosingWithStream = currentStream != null && isKeyboardVisible && imeTargetBottom == 0

    LaunchedEffect(hasVisibleStream, isKeyboardClosingWithStream) {
        state.updateAnimation(hasVisibleStream, isKeyboardClosingWithStream)
    }
    LaunchedEffect(currentStream) {
        if (currentStream == null) state.heightDp = 0.dp
    }

    return state
}
