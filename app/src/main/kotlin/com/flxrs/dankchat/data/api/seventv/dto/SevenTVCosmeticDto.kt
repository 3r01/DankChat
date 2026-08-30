package com.flxrs.dankchat.data.api.seventv.dto

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class SevenTVBadgeDto(
    val id: String,
    val name: String,
    val tooltip: String? = null,
    val host: SevenTVEmoteHostDto,
)

@Keep
@Serializable
data class SevenTVPaintDto(
    val id: String,
    val name: String,
    val function: String,
    val color: Long? = null,
    val repeat: Boolean = false,
    val angle: Float = 0f,
    val stops: List<SevenTVPaintStopDto> = emptyList(),
    @SerialName("image_url") val imageUrl: String? = null,
    val shadows: List<SevenTVPaintShadowDto> = emptyList(),
)

@Keep
@Serializable
data class SevenTVPaintStopDto(
    val at: Float,
    val color: Long,
)

@Keep
@Serializable
data class SevenTVPaintShadowDto(
    @SerialName("x_offset") val xOffset: Float,
    @SerialName("y_offset") val yOffset: Float,
    val radius: Float,
    val color: Long,
)
