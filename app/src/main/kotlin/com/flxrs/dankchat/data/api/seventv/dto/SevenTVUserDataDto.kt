package com.flxrs.dankchat.data.api.seventv.dto

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class SevenTVUserDataDto(
    val id: String,
    val connections: List<SevenTVUserConnection>,
    @SerialName("emote_sets") val emoteSets: List<SevenTVEmoteSetSummaryDto> = emptyList(),
)

@Keep
@Serializable
data class SevenTVEmoteSetSummaryDto(
    val id: String,
    val flags: Int,
) {
    val isPersonal: Boolean get() = flags and PERSONAL_FLAG != 0

    companion object {
        private const val PERSONAL_FLAG = 1 shl 2
    }
}
