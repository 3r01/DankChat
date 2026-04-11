package com.flxrs.dankchat.ui.chat.emote

import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
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
