package com.flxrs.dankchat.utils.extensions

import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils

/**
 * Adjusts this color to ensure readable contrast against [background].
 *
 * Uses WCAG contrast ratio (target 4.5:1 for normal text) and shifts
 * the color's lightness in HSL space until the target is met,
 * preserving hue and saturation as much as possible.
 */
@ColorInt
fun Int.normalizeColor(@ColorInt background: Int): Int {
    // calculateContrast requires opaque colors; force full alpha on both
    val opaqueColor = this or 0xFF000000.toInt()
    val opaqueBackground = background or 0xFF000000.toInt()
    val contrast = ColorUtils.calculateContrast(opaqueColor, opaqueBackground)
    if (contrast >= MIN_CONTRAST_RATIO) return this

    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(opaqueColor, hsl)

    val bgLuminance = ColorUtils.calculateLuminance(opaqueBackground)
    // On dark backgrounds, increase lightness; on light backgrounds, decrease it
    val shouldLighten = bgLuminance < 0.5

    // Binary search for the minimum lightness adjustment that meets contrast.
    var low: Float
    var high: Float
    if (shouldLighten) {
        low = hsl[2] // original lightness
        high = 1f // max lightness
    } else {
        low = 0f // min lightness
        high = hsl[2] // original lightness
    }

    var bestL = hsl[2]
    var bestContrast = contrast

    repeat(MAX_ITERATIONS) {
        val mid = (low + high) / 2f
        hsl[2] = mid
        val candidate = ColorUtils.HSLToColor(hsl)
        val candidateContrast = ColorUtils.calculateContrast(candidate, opaqueBackground)

        if (candidateContrast >= MIN_CONTRAST_RATIO) {
            bestL = mid
            bestContrast = candidateContrast
            // Try closer to original (less adjustment)
            if (shouldLighten) high = mid else low = mid
        } else {
            // Need more adjustment (further from original)
            if (shouldLighten) low = mid else high = mid
        }
    }

    if (bestContrast >= MIN_CONTRAST_RATIO) {
        hsl[2] = bestL
        return ColorUtils.HSLToColor(hsl)
    }

    // Fallback: push to extreme lightness
    hsl[2] = if (shouldLighten) 0.9f else 0.1f
    return ColorUtils.HSLToColor(hsl)
}

private const val MIN_CONTRAST_RATIO = 4.5
private const val MAX_ITERATIONS = 16

/** convert int to RGB with zero pad */
val Int.hexCode: String
    get() = Integer.toHexString(rgb).padStart(6, '0')

/** helper to extract only RGB part (i.e. drop the alpha part) */
private val Int.rgb: Int
    get() = this and 0xffffff
