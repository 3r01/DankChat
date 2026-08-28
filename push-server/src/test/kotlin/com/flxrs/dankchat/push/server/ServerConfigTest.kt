package com.flxrs.dankchat.push.server

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class ServerConfigTest {
    @Test
    fun `loads required environment and defaults`() {
        val config = ServerConfig.fromEnvironment(requiredEnvironment())

        assertEquals("0.0.0.0", config.host)
        assertEquals(8080, config.port)
        assertEquals("https://push.example.com", config.publicBaseUrl)
    }

    @Test
    fun `rejects missing secrets`() {
        assertFailsWith<IllegalStateException> {
            ServerConfig.fromEnvironment(requiredEnvironment() - "TWITCH_CLIENT_SECRET")
        }
    }

    private fun requiredEnvironment() =
        mapOf(
            "PUBLIC_BASE_URL" to "https://push.example.com/",
            "ENROLLMENT_TOKEN" to "enroll",
            "TWITCH_CLIENT_ID" to "client",
            "TWITCH_CLIENT_SECRET" to "secret",
            "FIREBASE_CREDENTIALS" to "/run/secrets/firebase.json",
        )
}
