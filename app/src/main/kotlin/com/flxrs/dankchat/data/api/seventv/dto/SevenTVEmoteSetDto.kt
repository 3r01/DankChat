package com.flxrs.dankchat.data.api.seventv.dto

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class SevenTVEmoteSetDto(
    val id: String,
    val name: String,
    val emotes: List<SevenTVEmoteDto>?,
    val flags: Int = 0,
) {
    val isPersonalOrCommercial: Boolean get() = flags and (PERSONAL_FLAG or COMMERCIAL_FLAG) != 0

    companion object {
        private const val PERSONAL_FLAG = 1 shl 2
        private const val COMMERCIAL_FLAG = 1 shl 3
    }
}
