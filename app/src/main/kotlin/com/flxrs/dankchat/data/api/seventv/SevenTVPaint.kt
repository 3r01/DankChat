package com.flxrs.dankchat.data.api.seventv

data class SevenTVPaint(
    val id: String,
    val name: String,
    val function: String,
    val color: Long?,
    val repeat: Boolean,
    val angle: Float,
    val stops: List<Stop>,
    val imageUrl: String?,
    val shadows: List<Shadow>,
) {
    data class Stop(
        val position: Float,
        val rgba: Long,
    )

    data class Shadow(
        val xOffset: Float,
        val yOffset: Float,
        val radius: Float,
        val rgba: Long,
    )
}
