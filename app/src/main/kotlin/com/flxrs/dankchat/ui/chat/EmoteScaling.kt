package com.flxrs.dankchat.ui.chat

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.flxrs.dankchat.data.twitch.emote.ChatMessageEmote
import kotlin.math.roundToInt

/**
 * Emote scaling utilities that match the original ChatAdapter logic EXACTLY.
 *
 * Old ChatAdapter constants:
 * - BASE_HEIGHT_CONSTANT = 1.173
 * - SCALE_FACTOR_CONSTANT = 1.5 / 112
 * - baseHeight = textSize * BASE_HEIGHT_CONSTANT
 * - scaleFactor = baseHeight * SCALE_FACTOR_CONSTANT
 *
 * This ensures 100% visual parity with the old TextView-based rendering.
 */
object EmoteScaling {
    private const val BASE_HEIGHT_CONSTANT = 1.173
    private const val SCALE_FACTOR_CONSTANT = 1.5 / 112

    /**
     * Calculate base emote height from font size.
     * This matches the line height of text.
     */
    fun getBaseHeight(fontSizeSp: Float): Dp = (fontSizeSp * BASE_HEIGHT_CONSTANT).dp

    /**
     * Calculate scale factor from base height in pixels.
     */
    fun getScaleFactor(baseHeightPx: Int): Double = baseHeightPx * SCALE_FACTOR_CONSTANT

    /**
     * Calculate scaled emote dimensions matching old ChatAdapter.transformEmoteDrawable() EXACTLY.
     *
     * Old logic:
     * 1. ratio = intrinsicWidth / intrinsicHeight
     * 2. height = special handling for Twitch emotes, else intrinsicHeight * scale
     * 3. width = height * ratio
     * 4. scaledWidth = width * emote.scale
     * 5. scaledHeight = height * emote.scale
     *
     * Returns pixel dimensions.
     *
     * @param intrinsicWidth Original emote width in pixels
     * @param intrinsicHeight Original emote height in pixels
     * @param emote The emote with scale factor and type info
     * @param baseHeightPx Base height in pixels (line height)
     * @return Pair of (widthPx, heightPx) in pixels
     */
    fun calculateEmoteDimensionsPx(intrinsicWidth: Int, intrinsicHeight: Int, emote: ChatMessageEmote, baseHeightPx: Int): Pair<Int, Int> {
        val scale = baseHeightPx * SCALE_FACTOR_CONSTANT

        val ratio = intrinsicWidth / intrinsicHeight.toFloat()

        // Match ChatAdapter height calculation exactly
        val height =
            when {
                intrinsicHeight < 55 && emote.isTwitch -> (70 * scale).roundToInt()
                intrinsicHeight in 55..111 && emote.isTwitch -> (112 * scale).roundToInt()
                else -> (intrinsicHeight * scale).roundToInt()
            }
        val width = (height * ratio).roundToInt()

        // Apply individual emote scale
        val scaledWidth = (width.toFloat() * emote.scale).roundToInt()
        val scaledHeight = (height.toFloat() * emote.scale).roundToInt()

        return Pair(scaledWidth, scaledHeight)
    }

    /**
     * Calculate badge dimensions.
     * Badges are always square at the base height.
     */
    fun getBadgeSize(fontSizeSp: Float): Dp = getBaseHeight(fontSizeSp)
}
