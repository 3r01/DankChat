package com.flxrs.dankchat.push.server

import com.flxrs.dankchat.push.PushConfiguration
import kotlinx.serialization.Serializable

@Serializable
data class ServerState(
    val configuration: PushConfiguration? = null,
    val devices: Set<String> = emptySet(),
    val twitchTokens: TwitchTokens? = null,
)

@Serializable
data class TwitchTokens(
    val userId: String,
    val accessToken: String,
    val refreshToken: String,
)
