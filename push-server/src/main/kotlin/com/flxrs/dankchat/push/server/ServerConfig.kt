package com.flxrs.dankchat.push.server

import java.nio.file.Path

data class ServerConfig(
    val host: String,
    val port: Int,
    val publicBaseUrl: String,
    val enrollmentToken: String,
    val twitchClientId: String,
    val twitchClientSecret: String,
    val firebaseCredentials: Path,
    val dataDirectory: Path,
) {
    companion object {
        fun fromEnvironment(environment: Map<String, String> = System.getenv()) =
            ServerConfig(
                host = environment["HOST"] ?: "0.0.0.0",
                port = environment["PORT"]?.toIntOrNull() ?: 8080,
                publicBaseUrl = environment.required("PUBLIC_BASE_URL").trimEnd('/'),
                enrollmentToken = environment.required("ENROLLMENT_TOKEN"),
                twitchClientId = environment.required("TWITCH_CLIENT_ID"),
                twitchClientSecret = environment.required("TWITCH_CLIENT_SECRET"),
                firebaseCredentials = Path.of(environment.required("FIREBASE_CREDENTIALS")),
                dataDirectory = Path.of(environment["DATA_DIRECTORY"] ?: "data"),
            )

        private fun Map<String, String>.required(name: String): String = get(name)?.takeIf(String::isNotBlank) ?: error("Missing required environment variable $name")
    }
}
