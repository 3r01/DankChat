package com.flxrs.dankchat.push.server

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.basic
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.security.MessageDigest

fun Application.pushServer(config: ServerConfig) {
    val logger = environment.log
    val stateStore = StateStore(config.dataDirectory.resolve("state.json"))
    val oauthClient = TwitchOAuthClient(config)
    val oauthSession = OAuthSession()
    val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val eventSubMonitor = EventSubMonitor()
    EventSubSupervisor(config, stateStore, oauthClient, FirebasePushSender(config, stateStore), eventSubMonitor).start(backgroundScope)
    monitor.subscribe(ApplicationStopped) { backgroundScope.cancel() }
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
    install(Authentication) {
        basic("setup") {
            realm = "DankChat Push"
            validate { credentials ->
                if (MessageDigest.isEqual(credentials.password.toByteArray(), config.enrollmentToken.toByteArray())) {
                    UserIdPrincipal(credentials.name)
                } else {
                    null
                }
            }
        }
    }
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            logger.error("Unhandled request failure", cause)
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Internal server error"))
        }
    }
    routing {
        get("/health") {
            val state = stateStore.state.value
            val eventSub = eventSubMonitor.snapshot()
            val setupComplete = state.configuration != null && state.twitchTokens != null && state.devices.isNotEmpty()
            call.respond(
                HealthResponse(
                    status =
                        if (!setupComplete) {
                            "setup"
                        } else if (eventSub.connected) {
                            "ready"
                        } else {
                            "degraded"
                        },
                    publicBaseUrl = config.publicBaseUrl,
                    eventSubConnected = eventSub.connected,
                    eventSubSubscriptions = eventSub.subscriptionCount,
                    eventSubLastConnectedAt = eventSub.lastConnectedAt,
                    eventSubLastActivityAt = eventSub.lastActivityAt,
                    eventSubLastFailureAt = eventSub.lastFailureAt,
                    eventSubLastFailure = eventSub.lastFailure,
                ),
            )
        }
        apiRoutes(config, stateStore)
        oauthRoutes(oauthClient, oauthSession, stateStore)
    }
}

@Serializable
data class HealthResponse(
    val status: String,
    val publicBaseUrl: String,
    val eventSubConnected: Boolean,
    val eventSubSubscriptions: Int,
    val eventSubLastConnectedAt: Long?,
    val eventSubLastActivityAt: Long?,
    val eventSubLastFailureAt: Long?,
    val eventSubLastFailure: String?,
)

@Serializable
data class ErrorResponse(
    val error: String,
)
