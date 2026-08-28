package com.flxrs.dankchat.push.server

import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.Headers
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class TwitchProfileImageRepositoryTest {
    private val server = MockWebServer()

    @AfterEach
    fun tearDown() {
        server.close()
    }

    @Test
    fun `fetches profile images and caches them by user ID`() =
        runTest {
            server.start()
            server.enqueue(
                MockResponse(
                    body =
                        """
                        {"data":[
                          {"id":"sender","profile_image_url":"https://example.com/sender.png"},
                          {"id":"channel","profile_image_url":"https://example.com/channel.png"}
                        ]}
                        """.trimIndent(),
                    headers = Headers.Builder().add("Content-Type", "application/json").build(),
                ),
            )
            val repository =
                TwitchProfileImageRepository(
                    clientId = "client-id",
                    usersUrl = server.url("/helix/users").toString(),
                )

            val first = repository.getProfileImageUrls(listOf("sender", "channel"), "access-token")
            val second = repository.getProfileImageUrls(listOf("sender"), "access-token")

            assertEquals("https://example.com/sender.png", first["sender"])
            assertEquals("https://example.com/channel.png", first["channel"])
            assertEquals(first["sender"], second["sender"])
            assertEquals(1, server.requestCount)
            server.takeRequest().let { request ->
                assertEquals("Bearer access-token", request.headers["Authorization"])
                assertEquals("client-id", request.headers["Client-Id"])
                assertEquals(listOf("sender", "channel"), request.url.queryParameterValues("id"))
            }
        }

    @Test
    fun `retries profile images after a failed request`() =
        runTest {
            server.start()
            server.enqueue(MockResponse(code = 500))
            server.enqueue(
                MockResponse(
                    body = """{"data":[{"id":"sender","profile_image_url":"https://example.com/sender.png"}]}""",
                    headers = Headers.Builder().add("Content-Type", "application/json").build(),
                ),
            )
            val repository =
                TwitchProfileImageRepository(
                    clientId = "client-id",
                    usersUrl = server.url("/helix/users").toString(),
                )

            assertEquals(emptyMap(), repository.getProfileImageUrls(listOf("sender"), "access-token"))
            assertEquals(
                "https://example.com/sender.png",
                repository.getProfileImageUrls(listOf("sender"), "access-token")["sender"],
            )
            assertEquals(2, server.requestCount)
        }
}
