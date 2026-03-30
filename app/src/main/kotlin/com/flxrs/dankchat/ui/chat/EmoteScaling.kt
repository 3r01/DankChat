package com.flxrs.dankchat.ui.chat

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.flxrs.dankchat.data.twitch.emote.ChatMessageEmote
import kotlin.math.roundToInt

object EmoteScaling {
    private const val BASE_HEIGHT_CONSTANT = 1.173
    private const val SCALE_FACTOR_CONSTANT = 1.5 / 112

    fun getBaseHeight(fontSizeSp: Float): Dp = (fontSizeSp * BASE_HEIGHT_CONSTANT).dp

    fun getScaleFactor(baseHeightPx: Int): Double = baseHeightPx * SCALE_FACTOR_CONSTANT

    fun calculateEmoteDimensionsPx(
        intrinsicWidth: Int,
        intrinsicHeight: Int,
        emote: ChatMessageEmote,
        baseHeightPx: Int,
    ): Pair<Int, Int> {
        val scale = baseHeightPx * SCALE_FACTOR_CONSTANT

        val ratio = intrinsicWidth / intrinsicHeight.toFloat()

        val height =
            when {
                intrinsicHeight < 55 && emote.isTwitch -> (70 * scale).roundToInt()
                intrinsicHeight in 55..111 && emote.isTwitch -> (112 * scale).roundToInt()
                else -> (intrinsicHeight * scale).roundToInt()
            }
        val width = (height * ratio).roundToInt()

        val scaledWidth = (width.toFloat() * emote.scale).roundToInt()
        val scaledHeight = (height.toFloat() * emote.scale).roundToInt()

        return Pair(scaledWidth, scaledHeight)
    }

    fun getBadgeSize(fontSizeSp: Float): Dp = getBaseHeight(fontSizeSp)
}
