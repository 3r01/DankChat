package com.flxrs.dankchat.push.server

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.hours

class TwitchProfileImageRepository(
    private val clientId: String,
    private val client: HttpClient = defaultClient(),
    private val usersUrl: String = TWITCH_USERS_URL,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val mutex = Mutex()
    private val cache = mutableMapOf<String, CacheEntry>()

    suspend fun getProfileImageUrls(
        userIds: Collection<String>,
        accessToken: String,
    ): Map<String, String> =
        mutex.withLock {
            val requestedIds = userIds.filter { it.isNotBlank() }.distinct()
            val currentTime = now()
            val missingIds = requestedIds.filter { cache[it]?.expiresAt?.let { expiry -> expiry > currentTime } != true }
            if (missingIds.isNotEmpty()) {
                val fetched = fetchProfileImageUrls(missingIds, accessToken)
                fetched.forEach { (userId, url) ->
                    cache[userId] = CacheEntry(url, currentTime + CACHE_DURATION.inWholeMilliseconds)
                }
            }
            requestedIds.mapNotNull { userId -> cache[userId]?.let { userId to it.url } }.toMap()
        }

    private suspend fun fetchProfileImageUrls(
        userIds: List<String>,
        accessToken: String,
    ): Map<String, String> =
        runCatching {
            val response =
                client.get(usersUrl) {
                    bearerAuth(accessToken)
                    header("Client-Id", clientId)
                    userIds.forEach { parameter("id", it) }
                }
            if (response.status != HttpStatusCode.OK) {
                error("Twitch users request failed: ${response.status} ${response.bodyAsText()}")
            }
            response.body<TwitchUsersResponse>().data.associate { it.id to it.profileImageUrl }
        }.onFailure { logger.warn("Failed to resolve Twitch profile images", it) }
            .getOrDefault(emptyMap())

    private data class CacheEntry(
        val url: String,
        val expiresAt: Long,
    )

    companion object {
        private const val TWITCH_USERS_URL = "https://api.twitch.tv/helix/users"
        private val CACHE_DURATION = 24.hours

        private fun defaultClient() =
            HttpClient(OkHttp) {
                install(HttpTimeout) {
                    requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
                }
                install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true })
                }
            }

        private const val REQUEST_TIMEOUT_MILLIS = 5_000L
    }
}

@Serializable
private data class TwitchUsersResponse(
    val data: List<TwitchUser>,
)

@Serializable
private data class TwitchUser(
    val id: String,
    @SerialName("profile_image_url")
    val profileImageUrl: String,
)
