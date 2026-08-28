package com.flxrs.dankchat.preferences.notifications

import kotlinx.serialization.Serializable

@Serializable
data class RemotePushSettings(
    val enabled: Boolean = false,
    val serverUrl: String = "",
    val enrollmentToken: String = "",
) {
    val isConfigured: Boolean
        get() = enabled && serverUrl.isNotBlank() && enrollmentToken.isNotBlank()
}
