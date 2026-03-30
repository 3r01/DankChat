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

    fun getBadgeSize(fontSizeSp: Float): Dp = getBaseHeight(fontSizeSp)
}
