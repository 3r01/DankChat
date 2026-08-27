package com.flxrs.dankchat.preferences.notifications

import kotlinx.serialization.Serializable

@Serializable
data class RemotePushDevice(
    val firebaseInstallationId: String = "",
)
