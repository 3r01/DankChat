package com.flxrs.dankchat.data.api.seventv.dto

import kotlinx.serialization.Serializable

@Serializable
data class SevenTVPresenceDto(
    val kind: Int = CHANNEL_PRESENCE_KIND,
    val data: Data,
) {
    @Serializable
    data class Data(
        val id: String,
        val platform: String = TWITCH_PLATFORM,
    )

    companion object {
        private const val CHANNEL_PRESENCE_KIND = 1
        private const val TWITCH_PLATFORM = "TWITCH"
    }
}
