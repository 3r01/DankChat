package com.flxrs.dankchat.preferences.whispers

import kotlinx.serialization.Serializable

@Serializable
data class WhisperHistorySettings(
    val webOAuthTokens: Map<String, String> = emptyMap(),
)
