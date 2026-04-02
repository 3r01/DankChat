package com.flxrs.dankchat.data.api.upload

import android.util.Log
import com.flxrs.dankchat.BuildConfig
import com.flxrs.dankchat.data.api.ApiException
import com.flxrs.dankchat.data.api.upload.dto.UploadDto
import com.flxrs.dankchat.di.UploadOkHttpClient
import com.flxrs.dankchat.preferences.tools.ToolsSettingsDataStore
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.Response
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import java.io.File
import java.net.URLConnection
import java.time.Instant

@Single
class UploadClient(
    @Named(type = UploadOkHttpClient::class) private val httpClient: OkHttpClient,
    private val toolsSettingsDataStore: ToolsSettingsDataStore,
) {
    suspend fun uploadMedia(file: File): Result<UploadDto> = withContext(Dispatchers.IO) {
        val uploader = toolsSettingsDataStore.settings.first().uploaderConfig
        val mimetype = URLConnection.guessContentTypeFromName(file.name)

        val requestBody =
            MultipartBody
                .Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(name = uploader.formField, filename = file.name, body = file.asRequestBody(mimetype.toMediaType()))
                .build()
        val request =
            Request
                .Builder()
                .url(uploader.uploadUrl)
                .header(HttpHeaders.UserAgent, "dankchat/${BuildConfig.VERSION_NAME}")
                .apply {
                    uploader.parsedHeaders.forEach { (name, value) ->
                        header(name, value)
                    }
                }.post(requestBody)
                .build()

        val response =
            runCatching {
                httpClient.newCall(request).execute()
            }.getOrElse {
                return@withContext Result.failure(it)
            }

        when {
            response.isSuccessful -> {
                val imageLinkPattern = uploader.imageLinkPattern
                val deletionLinkPattern = uploader.deletionLinkPattern

                if (imageLinkPattern == null || imageLinkPattern.isBlank()) {
                    return@withContext runCatching {
                        val body = response.body.string()
                        UploadDto(
                            imageLink = body,
                            deleteLink = null,
                            timestamp = Instant.now(),
                        )
                    }
                }

                response
                    .asJson()
                    .mapCatching { json ->
                        val deleteLink = deletionLinkPattern?.takeIf { it.isNotBlank() }?.let { json.extractLink(it) }
                        val imageLink = json.extractLink(imageLinkPattern)
                        UploadDto(
                            imageLink = imageLink,
                            deleteLink = deleteLink,
                            timestamp = Instant.now(),
                        )
                    }
            }

            else -> {
                Log.e(TAG, "Upload failed with ${response.code} ${response.message}")
                val url = URLBuilder(response.request.url.toString()).build()
                Result.failure(ApiException(HttpStatusCode.fromValue(response.code), url, response.message))
            }
        }
    }

    @Suppress("RegExpRedundantEscape")
    private suspend fun JsonElement.extractLink(linkPattern: String): String = withContext(Dispatchers.Default) {
        extractJsonLink(linkPattern)
    }

    private fun Response.asJson(): Result<JsonElement> = runCatching {
        Json.parseToJsonElement(body.string())
    }.onFailure {
        Log.d(TAG, "Error parsing JSON from response: ", it)
    }

    companion object {
        private val TAG = UploadClient::class.java.simpleName
        private val LINK_PATTERN_REGEX = "\\{(.+?)\\}".toRegex()

        @Suppress("RegExpRedundantEscape")
        internal fun JsonElement.extractJsonLink(linkPattern: String): String {
            var result = linkPattern
            LINK_PATTERN_REGEX.findAll(linkPattern).forEach {
                val jsonValue = getJsonValue(it.groupValues[1])
                if (jsonValue != null) {
                    result = result.replace(it.groupValues[0], jsonValue)
                }
            }
            return result
        }

        internal fun JsonElement.getJsonValue(pattern: String): String? {
            return runCatching {
                val result = pattern.split(".").fold(this) { acc: JsonElement, key ->
                    when (acc) {
                        is JsonObject -> acc.jsonObject[key] ?: return@runCatching null
                        is JsonArray -> acc.jsonArray[key.toInt()]
                        is JsonPrimitive -> return@runCatching acc.content
                        else -> return@runCatching null
                    }
                }
                when (result) {
                    is JsonPrimitive -> result.content
                    is JsonObject, is JsonArray -> result.toString()
                }
            }.getOrNull()
        }
    }
}
