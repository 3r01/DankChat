package com.flxrs.dankchat.data.api.seventv.dto

import androidx.annotation.Keep
import com.flxrs.dankchat.data.UserId
import com.flxrs.dankchat.data.toUserId
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class SevenTVUserConnection(
    val id: String? = null,
    val platform: String,
) {
    val twitchUserId: UserId? get() = id?.toUserId()?.takeIf { platform == twitch }

    companion object {
        const val twitch = "TWITCH"
    }
}
