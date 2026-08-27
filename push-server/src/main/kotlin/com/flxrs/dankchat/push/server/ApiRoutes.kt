package com.flxrs.dankchat.push.server

import com.flxrs.dankchat.push.ConfigurationResponse
import com.flxrs.dankchat.push.DeviceRegistration
import com.flxrs.dankchat.push.PUSH_PROTOCOL_VERSION
import com.flxrs.dankchat.push.PushConfiguration
import com.flxrs.dankchat.push.PushServerStatus
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import java.security.MessageDigest

fun Route.apiRoutes(
    config: ServerConfig,
    stateStore: StateStore,
) {
    get("/api/v1/status") {
        if (!call.isAuthorized(config.enrollmentToken)) return@get
        val state = stateStore.state.value
        call.respond(
            PushServerStatus(
                protocolVersion = PUSH_PROTOCOL_VERSION,
                configurationRevision = state.configuration?.revision,
                twitchAuthorized = state.twitchTokens != null,
                registeredDevices = state.devices.size,
            ),
        )
    }

    put("/api/v1/config") {
        if (!call.isAuthorized(config.enrollmentToken)) return@put
        val configuration = call.receive<PushConfiguration>()
        val authorizedUserId =
            stateStore.state.value.twitchTokens
                ?.userId
        if (authorizedUserId != null && configuration.twitchUserId != authorizedUserId) {
            call.respond(HttpStatusCode.Conflict, ErrorResponse("Configuration belongs to a different Twitch account"))
            return@put
        }
        val error = configuration.validationError()
        if (error != null) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(error))
            return@put
        }
        val changed = stateStore.updateConfiguration(configuration)
        call.respond(ConfigurationResponse(configuration.revision, changed))
    }

    put("/api/v1/devices") {
        if (!call.isAuthorized(config.enrollmentToken)) return@put
        val registration = call.receive<DeviceRegistration>()
        if (registration.firebaseInstallationId.isBlank() || registration.firebaseInstallationId.length > MAX_DEVICE_ID_LENGTH) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid device registration"))
            return@put
        }
        stateStore.addDevice(registration.firebaseInstallationId)
        call.respond(HttpStatusCode.NoContent)
    }

    delete("/api/v1/devices") {
        if (!call.isAuthorized(config.enrollmentToken)) return@delete
        val registration = call.receive<DeviceRegistration>()
        stateStore.removeDevice(registration.firebaseInstallationId)
        call.respond(HttpStatusCode.NoContent)
    }
}

private suspend fun io.ktor.server.application.ApplicationCall.isAuthorized(expectedToken: String): Boolean {
    val supplied = request.headers[HttpHeaders.Authorization]?.removePrefix("Bearer ")
    val authorized = supplied != null && MessageDigest.isEqual(supplied.toByteArray(), expectedToken.toByteArray())
    if (!authorized) {
        respond(HttpStatusCode.Unauthorized, ErrorResponse("Unauthorized"))
    }
    return authorized
}

private fun PushConfiguration.validationError(): String? =
    when {
        protocolVersion != PUSH_PROTOCOL_VERSION -> "Unsupported protocol version"
        revision < 0 -> "Invalid configuration revision"
        twitchUserId.isBlank() || userName.isBlank() -> "Missing Twitch user"
        channels.any { it.id.isBlank() || it.name.isBlank() } -> "Invalid channel"
        channels.distinctBy { it.id }.size != channels.size -> "Duplicate channel"
        channels.size > MAX_CHAT_CHANNELS -> "Too many channels"
        else -> null
    }

private const val MAX_DEVICE_ID_LENGTH = 4096
private const val MAX_CHAT_CHANNELS = 299
