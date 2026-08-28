package com.flxrs.dankchat.push.server

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.Parameters
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class TwitchOAuthClient(
    private val config: ServerConfig,
    private val client: HttpClient = defaultClient(),
) {
    val callbackUrl = "${config.publicBaseUrl}/oauth/twitch/callback"

    fun authorizationUrl(state: String): String {
        val parameters =
            mapOf(
                "client_id" to config.twitchClientId,
                "redirect_uri" to callbackUrl,
                "response_type" to "code",
                "scope" to REQUIRED_SCOPES.joinToString(" "),
                "state" to state,
            )
        return "https://id.twitch.tv/oauth2/authorize?" +
            parameters.entries.joinToString("&") { (key, value) -> "${key.encode()}=${value.encode()}" }
    }

    suspend fun exchangeCode(code: String): TwitchTokens {
        val response: TwitchTokenResponse =
            client
                .submitForm(
                    url = "https://id.twitch.tv/oauth2/token",
                    formParameters =
                        Parameters.build {
                            append("client_id", config.twitchClientId)
                            append("client_secret", config.twitchClientSecret)
                            append("code", code)
                            append("grant_type", "authorization_code")
                            append("redirect_uri", callbackUrl)
                        },
                ).body()
        check(REQUIRED_SCOPES.all(response.scope::contains)) { "Twitch authorization did not grant all required scopes" }
        return response.toTokens()
    }

    suspend fun refresh(refreshToken: String): TwitchTokens {
        val response: TwitchTokenResponse =
            client
                .submitForm(
                    url = "https://id.twitch.tv/oauth2/token",
                    formParameters =
                        Parameters.build {
                            append("client_id", config.twitchClientId)
                            append("client_secret", config.twitchClientSecret)
                            append("grant_type", "refresh_token")
                            append("refresh_token", refreshToken)
                        },
                ).body()
        check(REQUIRED_SCOPES.all(response.scope::contains)) { "Refreshed Twitch authorization is missing required scopes" }
        return response.toTokens()
    }

    suspend fun validate(
        accessToken: String,
        expectedUserId: String,
    ) {
        val validation =
            try {
                validation(accessToken)
            } catch (e: ClientRequestException) {
                if (e.response.status.value == 401) throw TwitchTokenRejectedException()
                throw e
            }
        if (validation.userId != expectedUserId || !REQUIRED_SCOPES.all(validation.scopes::contains)) {
            throw TwitchTokenRejectedException()
        }
    }

    private suspend fun TwitchTokenResponse.toTokens(): TwitchTokens {
        val validation = validation(accessToken)
        check(REQUIRED_SCOPES.all(validation.scopes::contains)) { "Twitch authorization validation is missing required scopes" }
        return TwitchTokens(validation.userId, accessToken, refreshToken)
    }

    private suspend fun validation(accessToken: String): TwitchValidationResponse =
        client
            .get("https://id.twitch.tv/oauth2/validate") {
                header(HttpHeaders.Authorization, "OAuth $accessToken")
            }.body()

    private fun String.encode() = URLEncoder.encode(this, StandardCharsets.UTF_8)

    companion object {
        val REQUIRED_SCOPES = listOf("user:read:chat", "user:read:whispers")

        private fun defaultClient() =
            HttpClient(OkHttp) {
                install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true })
                }
                expectSuccess = true
            }
    }
}

class TwitchTokenRejectedException : Exception()

@Serializable
private data class TwitchTokenResponse(
    @SerialName("access_token")
    val accessToken: String,
    @SerialName("refresh_token")
    val refreshToken: String,
    val scope: List<String>,
)

@Serializable
private data class TwitchValidationResponse(
    @SerialName("user_id")
    val userId: String,
    val scopes: List<String>,
)
