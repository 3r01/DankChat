package com.flxrs.dankchat.data.api.eventapi.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class EventSubSubscriptionType {
    @SerialName("channel.moderate")
    ChannelModerate,
    @SerialName("automod.message.hold")
    AutomodMessageHold,
    @SerialName("automod.message.update")
    AutomodMessageUpdate,
    Unknown,
}
