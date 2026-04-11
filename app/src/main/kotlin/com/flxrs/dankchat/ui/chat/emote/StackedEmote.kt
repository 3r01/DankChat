package com.flxrs.dankchat.ui.chat.emote

import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.asDrawable
import coil3.compose.LocalPlatformContext
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.size.Size
import com.flxrs.dankchat.data.twitch.emote.ChatMessageEmote
import com.flxrs.dankchat.ui.chat.EmoteUi
import com.flxrs.dankchat.utils.extensions.forEachLayer
import com.flxrs.dankchat.utils.extensions.setRunning
import kotlin.math.roundToInt

private const val BASE_HEIGHT_CONSTANT = 1.173
private const val SCALE_FACTOR_CONSTANT = 1.5 / 112

fun emoteBaseHeight(fontSizeSp: Float): Dp = (fontSizeSp * BASE_HEIGHT_CONSTANT).dp

internal fun emoteScaleFactor(baseHeightPx: Int): Double = baseHeightPx * SCALE_FACTOR_CONSTANT

@Composable
fun StackedEmote(
    emote: EmoteUi,
    fontSize: Float,
    emoteCoordinator: EmoteAnimationCoordinator,
    modifier: Modifier = Modifier,
    animateGifs: Boolean = true,
    alpha: Float = 1f,
    onClick: () -> Unit = {},
) {
    val context = LocalPlatformContext.current
    val density = LocalDensity.current
    val baseHeight = emoteBaseHeight(fontSize)
    val baseHeightPx = with(density) { baseHeight.toPx().toInt() }
    val scaleFactor = emoteScaleFactor(baseHeightPx)

    // For single emote, render directly without LayerDrawable
    if (emote.urls.size == 1 && emote.emotes.isNotEmpty()) {
        SingleEmoteDrawable(
            url = emote.urls.first(),
            chatEmote = emote.emotes.first(),
            scaleFactor = scaleFactor,
            emoteCoordinator = emoteCoordinator,
            animateGifs = animateGifs,
            alpha = alpha,
            modifier = modifier,
            onClick = onClick,
        )
        return
    }

    // For stacked emotes, create cache key matching old implementation
    val cacheKey = "${emote.emotes.joinToString("-") { it.id }}-$baseHeightPx"

    // Estimate placeholder size from dimension cache or from base height
    val cachedDims = emoteCoordinator.dimensionCache.get(cacheKey)
    val estimatedHeightPx = cachedDims?.second ?: (baseHeightPx * (emote.emotes.firstOrNull()?.scale ?: 1))
    val estimatedWidthPx = cachedDims?.first ?: estimatedHeightPx

    // Load or create LayerDrawable asynchronously
    val layerDrawableState =
        produceState<LayerDrawable?>(initialValue = null, key1 = cacheKey) {
            // Check cache first
            val cached = emoteCoordinator.getLayerCached(cacheKey)
            if (cached != null) {
                value = cached
                // Control animation
                cached.forEachLayer<Animatable> { it.setRunning(animateGifs) }
            } else {
                // Load all drawables
                val drawables =
                    emote.urls
                        .mapIndexedNotNull { idx, url ->
                            val emoteData = emote.emotes.getOrNull(idx) ?: emote.emotes.first()
                            try {
                                val request =
                                    ImageRequest
                                        .Builder(context)
                                        .data(url)
                                        .size(Size.ORIGINAL)
                                        .build()
                                val result = context.imageLoader.execute(request)
                                result.image?.asDrawable(context.resources)?.let { drawable ->
                                    transformEmoteDrawable(drawable, scaleFactor, emoteData)
                                }
                            } catch (_: Exception) {
                                null
                            }
                        }.toTypedArray()

                if (drawables.isNotEmpty()) {
                    val layerDrawable = drawables.toLayerDrawable(scaleFactor, emote.emotes)
                    emoteCoordinator.putLayerInCache(cacheKey, layerDrawable)
                    // Store dimensions for future placeholder sizing
                    emoteCoordinator.dimensionCache.put(
                        cacheKey,
                        layerDrawable.bounds.width() to layerDrawable.bounds.height(),
                    )
                    value = layerDrawable
                    // Control animation
                    layerDrawable.forEachLayer<Animatable> { it.setRunning(animateGifs) }
                }
            }
        }

    // Update animation state when setting changes
    LaunchedEffect(animateGifs, layerDrawableState.value) {
        layerDrawableState.value?.forEachLayer<Animatable> { it.setRunning(animateGifs) }
    }

    val layerDrawable = layerDrawableState.value
    if (layerDrawable != null) {
        // Render with actual dimensions
        val widthDp = with(density) { layerDrawable.bounds.width().toDp() }
        val heightDp = with(density) { layerDrawable.bounds.height().toDp() }
        val painter = remember(layerDrawable) { EmoteDrawablePainter(layerDrawable) }

        Image(
            painter = painter,
            contentDescription = null,
            alpha = alpha,
            modifier =
                modifier
                    .size(width = widthDp, height = heightDp)
                    .clickable { onClick() },
        )
    } else {
        // Placeholder with estimated size to prevent layout shift
        val widthDp = with(density) { estimatedWidthPx.toDp() }
        val heightDp = with(density) { estimatedHeightPx.toDp() }
        Box(
            modifier =
                modifier
                    .size(width = widthDp, height = heightDp)
                    .clickable { onClick() },
        )
    }
}

@Composable
private fun SingleEmoteDrawable(
    url: String,
    chatEmote: ChatMessageEmote,
    scaleFactor: Double,
    emoteCoordinator: EmoteAnimationCoordinator,
    animateGifs: Boolean,
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
    onClick: () -> Unit = {},
) {
    val context = LocalPlatformContext.current
    val density = LocalDensity.current

    // Use dimension cache for instant placeholder sizing on repeat views
    val cachedDims = emoteCoordinator.dimensionCache.get(url)

    // Load drawable asynchronously
    val drawableState =
        produceState<Drawable?>(initialValue = null, key1 = url) {
            // Fast path: check cache first
            val cached = emoteCoordinator.getCached(url)
            if (cached != null) {
                value = cached
            } else {
                try {
                    val request =
                        ImageRequest
                            .Builder(context)
                            .data(url)
                            .size(Size.ORIGINAL)
                            .build()
                    val result = context.imageLoader.execute(request)
                    result.image?.asDrawable(context.resources)?.let { drawable ->
                        // Transform and cache
                        val transformed = transformEmoteDrawable(drawable, scaleFactor, chatEmote)
                        emoteCoordinator.putInCache(url, transformed)
                        // Store dimensions for future placeholder sizing
                        emoteCoordinator.dimensionCache.put(
                            url,
                            transformed.bounds.width() to transformed.bounds.height(),
                        )
                        value = transformed
                    }
                } catch (_: Exception) {
                    // Ignore errors
                }
            }
        }

    // Update animation state when setting changes
    LaunchedEffect(animateGifs, drawableState.value) {
        if (drawableState.value is Animatable) {
            (drawableState.value as Animatable).setRunning(animateGifs)
        }
    }

    val drawable = drawableState.value
    if (drawable != null) {
        // Render with actual dimensions
        val widthDp = with(density) { drawable.bounds.width().toDp() }
        val heightDp = with(density) { drawable.bounds.height().toDp() }
        val painter = remember(drawable) { EmoteDrawablePainter(drawable) }

        Image(
            painter = painter,
            contentDescription = null,
            alpha = alpha,
            modifier =
                modifier
                    .size(width = widthDp, height = heightDp)
                    .clickable { onClick() },
        )
    } else if (cachedDims != null) {
        // Placeholder with cached size to prevent layout shift
        val widthDp = with(density) { cachedDims.first.toDp() }
        val heightDp = with(density) { cachedDims.second.toDp() }
        Box(
            modifier =
                modifier
                    .size(width = widthDp, height = heightDp)
                    .clickable { onClick() },
        )
    }
}

private fun transformEmoteDrawable(
    drawable: Drawable,
    scale: Double,
    emote: ChatMessageEmote,
    maxWidth: Int = 0,
    maxHeight: Int = 0,
): Drawable {
    val ratio = drawable.intrinsicWidth / drawable.intrinsicHeight.toFloat()
    val height =
        when {
            drawable.intrinsicHeight < 55 && emote.isTwitch -> (70 * scale).roundToInt()
            drawable.intrinsicHeight in 55..111 && emote.isTwitch -> (112 * scale).roundToInt()
            else -> (drawable.intrinsicHeight * scale).roundToInt()
        }
    val width = (height * ratio).roundToInt()

    val scaledWidth = width * emote.scale
    val scaledHeight = height * emote.scale

    val left = if (maxWidth > 0) (maxWidth - scaledWidth).div(2).coerceAtLeast(0) else 0
    val top = (maxHeight - scaledHeight).coerceAtLeast(0)

    drawable.setBounds(left, top, scaledWidth + left, scaledHeight + top)
    return drawable
}

private fun Array<Drawable>.toLayerDrawable(
    scaleFactor: Double,
    emotes: List<ChatMessageEmote>,
): LayerDrawable = LayerDrawable(this).apply {
    val bounds = this@toLayerDrawable.map { it.bounds }
    val maxWidth = bounds.maxOf { it.width() }
    val maxHeight = bounds.maxOf { it.height() }
    setBounds(0, 0, maxWidth, maxHeight)

    // Phase 2: Re-adjust bounds with maxWidth/maxHeight
    forEachIndexed { idx, dr ->
        transformEmoteDrawable(dr, scaleFactor, emotes[idx], maxWidth, maxHeight)
    }
}
