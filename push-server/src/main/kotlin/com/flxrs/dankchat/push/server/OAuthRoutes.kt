package com.flxrs.dankchat.push.server

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.atomic.AtomicReference

class OAuthSession {
    private val expectedState = AtomicReference<String?>()

    fun begin(): String = randomState().also(expectedState::set)

    fun consume(candidate: String?): Boolean {
        val expected = expectedState.getAndSet(null) ?: return false
        return candidate != null && java.security.MessageDigest.isEqual(expected.toByteArray(), candidate.toByteArray())
    }

    private fun randomState(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}

fun Route.oauthRoutes(
    oauthClient: TwitchOAuthClient,
    oauthSession: OAuthSession,
    stateStore: StateStore,
) {
    authenticate("setup") {
        get("/oauth/twitch/start") {
            call.respondRedirect(oauthClient.authorizationUrl(oauthSession.begin()))
        }
    }

    get("/oauth/twitch/callback") {
        if (!oauthSession.consume(call.request.queryParameters["state"])) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid OAuth state"))
            return@get
        }
        val error = call.request.queryParameters["error"]
        if (error != null) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("Twitch authorization failed: $error"))
            return@get
        }
        val code = call.request.queryParameters["code"]
        if (code.isNullOrBlank()) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing Twitch authorization code"))
            return@get
        }
        val tokens = oauthClient.exchangeCode(code)
        val configuredUserId =
            stateStore.state.value.configuration
                ?.twitchUserId
        if (configuredUserId != null && tokens.userId != configuredUserId) {
            call.respond(HttpStatusCode.Conflict, ErrorResponse("Authorized Twitch account does not match the configured account"))
            return@get
        }
        stateStore.updateTwitchTokens(tokens)
        call.respondText("Twitch authorization complete. You can close this page.")
    }
}
