package com.flxrs.dankchat.push.server

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

internal class EventSubSessionTest {
    @Test
    fun `welcome uses Twitch keepalive timeout`() {
        val welcome = parseEventSubWelcome(envelope(WELCOME_PAYLOAD))

        assertEquals("session-id", welcome.sessionId)
        assertEquals(23.seconds, welcome.keepaliveTimeout)
    }

    @Test
    fun `reconnect URL is preserved exactly`() {
        val reconnectUrl = "wss://eventsub.wss.twitch.tv/reconnect?session=abc&token=123"

        assertEquals(reconnectUrl, parseEventSubReconnectUrl(envelope(RECONNECT_PAYLOAD.replace("RECONNECT_URL", reconnectUrl))))
    }

    private fun envelope(payload: String) = Json.parseToJsonElement(payload).jsonObject

    private companion object {
        const val WELCOME_PAYLOAD =
            """
            {
              "payload": {
                "session": {
                  "id": "session-id",
                  "keepalive_timeout_seconds": 23
                }
              }
            }
            """
        const val RECONNECT_PAYLOAD =
            """
            {
              "payload": {
                "session": {
                  "reconnect_url": "RECONNECT_URL"
                }
              }
            }
            """
    }
}
