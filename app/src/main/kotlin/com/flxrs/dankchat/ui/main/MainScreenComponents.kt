package com.flxrs.dankchat.ui.main

import android.app.Activity
import android.app.PictureInPictureParams
import android.os.Build
import android.util.Rational
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.flxrs.dankchat.ui.chat.emotemenu.EmoteMenu
import com.flxrs.dankchat.ui.main.stream.StreamViewModel
import kotlin.math.abs

@Composable
internal fun observePipMode(streamViewModel: StreamViewModel): Boolean {
    val context = LocalContext.current
    val activity = context as? Activity
    var isInPipMode by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, _ ->
                isInPipMode = activity?.isInPictureInPictureMode == true
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        streamViewModel.shouldEnablePipAutoMode.collect { enabled ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && activity != null) {
                activity.setPictureInPictureParams(
                    PictureInPictureParams
                        .Builder()
                        .setAutoEnterEnabled(enabled)
                        .setAspectRatio(Rational(16, 9))
                        .build(),
                )
            }
        }
    }

    return isInPipMode
}

@Composable
internal fun FullscreenSystemBarsEffect(isFullscreen: Boolean) {
    val context = LocalContext.current
    val window = (context as? Activity)?.window
    val view = LocalView.current

    DisposableEffect(isFullscreen, window, view) {
        if (window == null) return@DisposableEffect onDispose { }
        val controller = WindowCompat.getInsetsController(window, view)
        if (isFullscreen) {
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}

@Composable
internal fun AnimatedStatusBarScrim(
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        StatusBarScrim()
    }
}

@Composable
internal fun StatusBarScrim(
    modifier: Modifier = Modifier,
    colorAlpha: Float = 0.7f,
) {
    val density = LocalDensity.current
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(with(density) { WindowInsets.statusBars.getTop(density).toDp() })
                .background(MaterialTheme.colorScheme.surface.copy(alpha = colorAlpha)),
    )
}

@Composable
internal fun InputDismissScrim(
    forceOpen: Boolean,
    onDismiss: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) {
                    if (!forceOpen) {
                        onDismiss()
                    }
                },
    )
}

/**
 * Modifier that consumes horizontal drags originating from system gesture edge zones
 * to prevent the HorizontalPager from intercepting system back/edge gestures.
 * Uses [PointerEventPass.Initial] so the pager never sees these drags,
 * while taps pass through normally to the content underneath.
 */
@Composable
internal fun Modifier.edgeGestureGuard(): Modifier {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val systemGestureInsets = WindowInsets.systemGestures
    val leftEdgePx = systemGestureInsets.getLeft(density, layoutDirection).toFloat()
    val rightEdgePx = systemGestureInsets.getRight(density, layoutDirection).toFloat()

    return pointerInput(leftEdgePx, rightEdgePx) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val isInEdge = down.position.x < leftEdgePx || down.position.x > (size.width - rightEdgePx)
            if (!isInEdge) return@awaitEachGesture

            var totalDx = 0f
            var claimed = false

            do {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) break

                totalDx += change.positionChange().x
                if (!claimed && abs(totalDx) > viewConfiguration.touchSlop) {
                    claimed = true
                }
                if (claimed) {
                    change.consume()
                }
            } while (true)
        }
    }
}

/**
 * Animated emote menu overlay that slides in from the bottom.
 * Supports predictive back gesture scaling.
 */
@Composable
internal fun EmoteMenuOverlay(
    isVisible: Boolean,
    totalMenuHeight: Dp,
    backProgress: Float,
    onEmoteClick: (code: String, id: String) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(animationSpec = tween(durationMillis = 140), initialOffsetY = { it }),
        exit = slideOutVertically(animationSpec = tween(durationMillis = 140), targetOffsetY = { it }),
        modifier = modifier,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(totalMenuHeight)
                    .graphicsLayer {
                        val scale = 1f - (backProgress * 0.1f)
                        scaleX = scale
                        scaleY = scale
                        alpha = 1f - backProgress
                        translationY = backProgress * 100f
                    }.background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            EmoteMenu(
                onEmoteClick = onEmoteClick,
                onBackspace = onBackspace,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
