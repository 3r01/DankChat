package com.flxrs.dankchat.data.api.whisperhistory

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant

internal class WhisperHistoryApiClientTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `parses both directions in chronological order`() {
        val response = json.decodeFromString<WhisperHistoryResponse>(RESPONSE)

        val messages = parseWhisperHistoryResponse(response, expectedUserId = "me", cutoff = Instant.parse("2026-08-27T10:00:00Z"))

        assertEquals(listOf("incoming", "outgoing"), messages.map { it.id })
        assertEquals("hello", messages[0].text)
        assertEquals("other", messages[0].sender.id)
        assertEquals("me", messages[0].recipient.id)
        assertEquals("waves", messages[1].text)
        assertEquals("me", messages[1].sender.id)
        assertEquals("other", messages[1].recipient.id)
        assertEquals(listOf(WhisperHistoryEmote("25", 0, 4)), messages[1].emotes)
    }

    @Test
    fun `rejects a token for another account`() {
        val response = json.decodeFromString<WhisperHistoryResponse>(RESPONSE)

        assertFailsWith<WhisperHistoryException.WrongAccount> {
            parseWhisperHistoryResponse(response, expectedUserId = "someone-else", cutoff = Instant.DISTANT_PAST)
        }
    }

    private companion object {
        const val RESPONSE =
            """
            {
              "data": {
                "currentUser": {
                  "id": "me",
                  "whisperThreads": {
                    "edges": [{
                      "node": {
                        "participants": [
                          { "id": "me", "login": "my_login", "displayName": "My_Login", "chatColor": "#123456" },
                          { "id": "other", "login": "qbit", "displayName": "Qbit", "chatColor": "#654321" }
                        ],
                        "messages": {
                          "edges": [
                            {
                              "node": {
                                "id": "outgoing",
                                "nonce": "nonce-2",
                                "sentAt": "2026-08-27T12:00:00Z",
                                "from": { "id": "me" },
                                "content": {
                                  "content": "\u0001ACTION waves\u0001",
                                  "emotes": [{ "id": "fallback", "emoteID": "25", "from": 0, "to": 4 }]
                                }
                              }
                            },
                            {
                              "node": {
                                "id": "incoming",
                                "nonce": "nonce-1",
                                "sentAt": "2026-08-27T11:00:00Z",
                                "from": { "id": "other" },
                                "content": { "content": "hello", "emotes": [] }
                              }
                            }
                          ]
                        }
                      }
                    }]
                  }
                }
              }
            }
            """
    }
}
