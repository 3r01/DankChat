package com.flxrs.dankchat.ui.chat.emote

import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

@Stable
class EmoteAnimationCoordinator {
    private val emoteCache = LruCache<String, Drawable>(512)
    private val layerCache = LruCache<String, LayerDrawable>(256)
    val dimensionCache = LruCache<String, Pair<Int, Int>>(1024)

    private val mainHandler = Handler(Looper.getMainLooper())
    private val invalidationListeners = HashMap<Drawable, MutableSet<() -> Unit>>()

    // Drawables are shared between all visible occurrences of an emote, but a Drawable only has
    // a single callback slot, so invalidations are fanned out to every registered listener
    private val fanOutCallback =
        object : Drawable.Callback {
            override fun invalidateDrawable(who: Drawable) {
                invalidationListeners[who]?.forEach { it() }
            }

            override fun scheduleDrawable(
                who: Drawable,
                what: Runnable,
                `when`: Long,
            ) {
                mainHandler.postAtTime(what, `when`)
            }

            override fun unscheduleDrawable(
                who: Drawable,
                what: Runnable,
            ) {
                mainHandler.removeCallbacks(what)
            }
        }

    fun registerInvalidationListener(
        drawable: Drawable,
        listener: () -> Unit,
    ) {
        val listeners = invalidationListeners.getOrPut(drawable) { mutableSetOf() }
        listeners += listener
        drawable.callback = fanOutCallback
        if (listeners.size == 1) {
            // Restarting on the first visible occurrence phase-aligns emotes that enter the
            // viewport together, additional occurrences must not reset the running animation
            drawable.setVisible(true, true)
        }
    }

    fun unregisterInvalidationListener(
        drawable: Drawable,
        listener: () -> Unit,
    ) {
        val listeners = invalidationListeners[drawable] ?: return
        listeners -= listener
        if (listeners.isEmpty()) {
            invalidationListeners.remove(drawable)
            drawable.setVisible(false, false)
            drawable.callback = null
        }
    }

    fun getCached(url: String): Drawable? = emoteCache.get(url)

    fun putInCache(
        url: String,
        drawable: Drawable,
    ) {
        emoteCache.put(url, drawable)
    }

    fun getLayerCached(cacheKey: String): LayerDrawable? = layerCache.get(cacheKey)

    fun putLayerInCache(
        cacheKey: String,
        layerDrawable: LayerDrawable,
    ) {
        layerCache.put(cacheKey, layerDrawable)
    }

    fun clear() {
        emoteCache.evictAll()
        layerCache.evictAll()
        dimensionCache.evictAll()
    }
}

val LocalEmoteAnimationCoordinator =
    staticCompositionLocalOf<EmoteAnimationCoordinator> {
        error("No EmoteAnimationCoordinator provided. Wrap your chat composables with CompositionLocalProvider.")
    }

@Composable
fun rememberEmoteAnimationCoordinator(): EmoteAnimationCoordinator = remember { EmoteAnimationCoordinator() }
