package com.flxrs.dankchat.data.notification

import com.flxrs.dankchat.preferences.notifications.RemotePushSettings
import com.flxrs.dankchat.push.ConfigurationResponse
import com.flxrs.dankchat.push.DeviceRegistration
import com.flxrs.dankchat.push.MentionHistoryResponse
import com.flxrs.dankchat.push.PushConfiguration
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import org.koin.core.annotation.Single

@Single
class RemotePushClient(
    private val httpClient: HttpClient,
) {
    suspend fun getMentionHistory(settings: RemotePushSettings): Result<MentionHistoryResponse> = runCatching {
        val response =
            httpClient.get(settings.endpoint("/api/v1/mentions")) {
                authorize(settings)
            }
        check(response.status == HttpStatusCode.OK) { "Push server rejected mention history request: ${response.status}" }
        response.body()
    }

    suspend fun syncConfiguration(
        settings: RemotePushSettings,
        configuration: PushConfiguration,
    ): Result<Long> = runCatching {
        val response =
            httpClient.put(settings.endpoint("/api/v1/config")) {
                authorize(settings)
                contentType(ContentType.Application.Json)
                setBody(configuration)
            }
        check(response.status == HttpStatusCode.OK) { "Push server rejected configuration: ${response.status}" }
        response.body<ConfigurationResponse>().revision
    }

    suspend fun registerDevice(
        settings: RemotePushSettings,
        firebaseInstallationId: String,
    ): Result<Unit> = runCatching {
        val response =
            httpClient.put(settings.endpoint("/api/v1/devices")) {
                authorize(settings)
                contentType(ContentType.Application.Json)
                setBody(DeviceRegistration(firebaseInstallationId))
            }
        check(response.status == HttpStatusCode.NoContent) { "Push server rejected device: ${response.status}" }
    }

    suspend fun unregisterDevice(
        settings: RemotePushSettings,
        firebaseInstallationId: String,
    ): Result<Unit> = runCatching {
        val response =
            httpClient.delete(settings.endpoint("/api/v1/devices")) {
                authorize(settings)
                contentType(ContentType.Application.Json)
                setBody(DeviceRegistration(firebaseInstallationId))
            }
        check(response.status == HttpStatusCode.NoContent) { "Push server rejected device removal: ${response.status}" }
    }

    private fun io.ktor.client.request.HttpRequestBuilder.authorize(settings: RemotePushSettings) {
        header(HttpHeaders.Authorization, "Bearer ${settings.enrollmentToken}")
    }

    private fun RemotePushSettings.endpoint(path: String) = serverUrl.trimEnd('/') + path
}
