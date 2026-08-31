package com.flxrs.dankchat.data.api.seventv

import com.flxrs.dankchat.data.UserId

data class SevenTVUserDetails(
    val id: String,
    val activeEmoteSetId: String,
    val connectionIndex: Int,
    val twitchUserId: UserId?,
)
