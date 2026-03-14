package com.flxrs.dankchat.auth

import kotlinx.serialization.Serializable

@Serializable
data class AuthSettings(
    val oAuthKey: String? = null,
    val userName: String? = null,
    val displayName: String? = null,
    val userId: String? = null,
    val clientId: String = DEFAULT_CLIENT_ID,
    val isLoggedIn: Boolean = false,
) {
    companion object {
        const val DEFAULT_CLIENT_ID = "xu7vd1i6tlr0ak45q1li2wdc0lrma8"
    }
}
