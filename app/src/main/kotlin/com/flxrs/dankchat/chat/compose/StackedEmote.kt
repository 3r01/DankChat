package com.flxrs.dankchat.chat.compose

import android.graphics.Rect
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.widget.ImageView
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil3.asDrawable
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.size.Size
import com.flxrs.dankchat.data.twitch.emote.ChatMessageEmote
import com.flxrs.dankchat.utils.extensions.forEachLayer
import com.flxrs.dankchat.utils.extensions.setRunning
import kotlin.math.roundToInt

/**
 * Renders stacked emotes exactly like old ChatAdapter using LayerDrawable.
 * 
 * Key differences from previous approaches:
 * - Creates actual LayerDrawable like ChatAdapter did
 * - Uses LruCache for LayerDrawables (not individual drawables)
 * - Uses AndroidView with ImageView to render the LayerDrawable
 * - NO ContentScale, NO Modifier.size on Image - drawable bounds handle everything
 */
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
    val baseHeight = EmoteScaling.getBaseHeight(fontSize)
    val baseHeightPx = with(density) { baseHeight.toPx().toInt() }
    val scaleFactor = EmoteScaling.getScaleFactor(baseHeightPx)
    
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
            onClick = onClick
        )
        return
    }
    
    // For stacked emotes, create cache key matching old implementation
    val cacheKey = "${emote.emotes.joinToString("-") { it.id }}-$baseHeightPx"
    
    // Load or create LayerDrawable asynchronously
    val layerDrawableState = produceState<LayerDrawable?>(initialValue = null, key1 = cacheKey) {
        // Check cache first
        val cached = emoteCoordinator.getLayerCached(cacheKey)
        if (cached != null) {
            value = cached
            // Control animation
            cached.forEachLayer<Animatable> { it.setRunning(animateGifs) }
        } else {
            // Load all drawables
            val drawables = emote.urls.mapIndexedNotNull { idx, url ->
                val emoteData = emote.emotes.getOrNull(idx) ?: emote.emotes.first()
                try {
                    val request = ImageRequest.Builder(context)
                        .data(url)
                        .size(Size.ORIGINAL)
                        .build()
                    val result = context.imageLoader.execute(request)
                    result.image?.asDrawable(context.resources)?.let { drawable ->
                        transformEmoteDrawable(drawable, scaleFactor, emoteData)
                    }
                } catch (e: Exception) {
                    null
                }
            }.toTypedArray()
            
            if (drawables.isNotEmpty()) {
                // Create LayerDrawable exactly like old implementation
                val layerDrawable = drawables.toLayerDrawable(scaleFactor, emote.emotes)
                emoteCoordinator.putLayerInCache(cacheKey, layerDrawable)
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
    
    // Render LayerDrawable if available using rememberAsyncImagePainter
    layerDrawableState.value?.let { layerDrawable ->
        val widthDp = with(density) { layerDrawable.bounds.width().toDp() }
        val heightDp = with(density) { layerDrawable.bounds.height().toDp() }
        
        // EXPERIMENT: Try rememberAsyncImagePainter with drawable as model
        val painter = rememberAsyncImagePainter(model = layerDrawable)
        
        Image(
            painter = painter,
            contentDescription = null,
            alpha = alpha,
            modifier = modifier
                .size(width = widthDp, height = heightDp)
                .clickable { onClick() }
        )
    }
}

/**
 * Renders a single emote as a Drawable, matching old ChatAdapter behavior.
 */
@Composable
private fun SingleEmoteDrawable(
    url: String,
    chatEmote: ChatMessageEmote,
    scaleFactor: Double,
    emoteCoordinator: EmoteAnimationCoordinator,
    animateGifs: Boolean,
    alpha: Float = 1f,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val context = LocalPlatformContext.current
    val density = LocalDensity.current
    
    // Load drawable asynchronously
    val drawableState = produceState<Drawable?>(initialValue = null, key1 = url) {
        // Fast path: check cache first
        val cached = emoteCoordinator.getCached(url)
        if (cached != null) {
            value = cached
        } else {
            try {
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .size(Size.ORIGINAL)
                    .build()
                val result = context.imageLoader.execute(request)
                result.image?.asDrawable(context.resources)?.let { drawable ->
                    // Transform and cache
                    val transformed = transformEmoteDrawable(drawable, scaleFactor, chatEmote)
                    emoteCoordinator.putInCache(url, transformed)
                    value = transformed
                }
            } catch (e: Exception) {
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
    
    // Render drawable if available
    drawableState.value?.let { drawable ->
        val widthDp = with(density) { drawable.bounds.width().toDp() }
        val heightDp = with(density) { drawable.bounds.height().toDp() }
        
        // EXPERIMENT: Try rememberAsyncImagePainter with drawable as model
        val painter = rememberAsyncImagePainter(model = drawable)
        
        Image(
            painter = painter,
            contentDescription = null,
            alpha = alpha,
            modifier = modifier
                .size(width = widthDp, height = heightDp)
                .clickable { onClick() }
        )
    }
}

/**
 * Transform emote drawable exactly like old ChatAdapter.transformEmoteDrawable().
 * Phase 1: Individual scaling without maxWidth/maxHeight.
 */
private fun transformEmoteDrawable(
    drawable: Drawable,
    scale: Double,
    emote: ChatMessageEmote,
    maxWidth: Int = 0,
    maxHeight: Int = 0
): Drawable {
    val ratio = drawable.intrinsicWidth / drawable.intrinsicHeight.toFloat()
    val height = when {
        drawable.intrinsicHeight < 55 && emote.isTwitch       -> (70 * scale).roundToInt()
        drawable.intrinsicHeight in 55..111 && emote.isTwitch -> (112 * scale).roundToInt()
        else                                                   -> (drawable.intrinsicHeight * scale).roundToInt()
    }
    val width = (height * ratio).roundToInt()

    val scaledWidth = width * emote.scale
    val scaledHeight = height * emote.scale

    val left = if (maxWidth > 0) (maxWidth - scaledWidth).div(2).coerceAtLeast(0) else 0
    val top = (maxHeight - scaledHeight).coerceAtLeast(0)

    drawable.setBounds(left, top, scaledWidth + left, scaledHeight + top)
    return drawable
}

/**
 * Create LayerDrawable from array of drawables exactly like old ChatAdapter.toLayerDrawable().
 */
private fun Array<Drawable>.toLayerDrawable(
    scaleFactor: Double,
    emotes: List<ChatMessageEmote>
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
