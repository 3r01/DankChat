package com.flxrs.dankchat.chat.compose

import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import coil3.DrawableImage
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import com.flxrs.dankchat.utils.extensions.setRunning

/**
 * Coordinates emote loading and animation synchronization across the entire chat.
 * 
 * Based on the old ChatAdapter/EmoteRepository approach:
 * - Uses LruCache to cache drawables (bounded memory, unlike ConcurrentHashMap)
 * - Shares Drawable instances across all usages of the same emote
 * - This keeps animated GIF frame counters synchronized naturally
 * - No mutex needed - Coil handles concurrent requests internally
 * - Emote animation controlled via setRunning() based on animateGifs setting
 * 
 * Same pattern as:
 * - EmoteRepository.badgeCache: LruCache<String, Drawable>(64)
 * - EmoteRepository.layerCache: LruCache<String, LayerDrawable>(256)
 */
@Stable
class EmoteAnimationCoordinator(
    val imageLoader: ImageLoader,
    private val platformContext: PlatformContext,
) {
    // LruCache for single emote drawables (like badgeCache in EmoteRepository)
    private val emoteCache = LruCache<String, Drawable>(256)

    // LruCache for stacked emote drawables (like layerCache in EmoteRepository)
    private val layerCache = LruCache<String, LayerDrawable>(128)

    // Cache of known emote dimensions (width, height in px) to avoid layout shifts
    val dimensionCache = LruCache<String, Pair<Int, Int>>(512)

    /**
     * Get or load an emote drawable.
     * 
     * Returns cached drawable if available, otherwise loads and caches it.
     * Sharing the same Drawable instance keeps animations synchronized.
     */
    suspend fun getOrLoadEmote(url: String, animateGifs: Boolean): Drawable? {
        // Fast path: already cached
        emoteCache.get(url)?.let { cached ->
            // Control animation based on setting
            if (cached is Animatable) {
                cached.setRunning(animateGifs)
            }
            return cached
        }

        // Load the emote via Coil (Coil handles concurrent requests internally)
        return try {
            val request = ImageRequest.Builder(platformContext)
                .data(url)
                .build()

            val result = imageLoader.execute(request)
            if (result is SuccessResult) {
                val image = result.image
                if (image is DrawableImage) {
                    val drawable = image.drawable
                    // Cache it for reuse
                    emoteCache.put(url, drawable)
                    // Control animation
                    if (drawable is Animatable) {
                        drawable.setRunning(animateGifs)
                    }
                    drawable
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Check if an emote is already cached.
     */
    fun getCached(url: String): Drawable? = emoteCache.get(url)

    /**
     * Put a drawable in the cache (used by AsyncImage onSuccess callback).
     */
    fun putInCache(url: String, drawable: Drawable) {
        emoteCache.put(url, drawable)
    }

    /**
     * Get a cached LayerDrawable for stacked emotes.
     */
    fun getLayerCached(cacheKey: String): LayerDrawable? = layerCache.get(cacheKey)

    /**
     * Put a LayerDrawable in the cache for stacked emotes.
     */
    fun putLayerInCache(cacheKey: String, layerDrawable: LayerDrawable) {
        layerCache.put(cacheKey, layerDrawable)
    }

    /**
     * Clear all caches.
     */
    fun clear() {
        emoteCache.evictAll()
        layerCache.evictAll()
        dimensionCache.evictAll()
    }
}

/**
 * CompositionLocal providing a shared EmoteAnimationCoordinator.
 * Must be provided at the chat root (e.g., ChatComposable) so all messages
 * share the same coordinator and its LruCache.
 */
val LocalEmoteAnimationCoordinator = staticCompositionLocalOf<EmoteAnimationCoordinator> {
    error("No EmoteAnimationCoordinator provided. Wrap your chat composables with CompositionLocalProvider.")
}

/**
 * Creates and remembers a singleton EmoteAnimationCoordinator using the given ImageLoader.
 * Call this once at the chat root, then provide via [LocalEmoteAnimationCoordinator].
 */
@Composable
fun rememberEmoteAnimationCoordinator(imageLoader: ImageLoader): EmoteAnimationCoordinator {
    val context = LocalPlatformContext.current
    return remember(imageLoader) {
        EmoteAnimationCoordinator(imageLoader, context)
    }
}
