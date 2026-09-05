package com.flxrs.dankchat.data.api.twitchgql

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import org.koin.core.annotation.Single
import java.util.UUID

@Single
class TwitchGifApiClient(
    private val httpClient: HttpClient,
    private val gqlClient: TwitchWebGqlClient,
    private val json: Json,
) {
    private val giphyPingbackId = UUID.randomUUID().toString()

    suspend fun getConfig(
        channelId: String,
        webOAuthToken: String,
    ): Result<TwitchGifConfig> = gql(
        webOAuthToken,
        GifConfigRequest(
            operationName = GIF_CONFIG_OPERATION,
            variables = GifConfigVariables(channelId),
            query = GIF_CONFIG_QUERY,
        ),
    ) { response ->
        val root = json.decodeFromString<GifConfigResponse>(response)
        root.errorMessage()?.let(::error)
        val data = root.data
            ?: error("GIFs are unavailable in this channel")
        val config = data.gifPickerConfig ?: error("GIFs are unavailable in this channel")
        val resolvedConfig =
            config.copy(
                canSend = data.user
                    ?.self
                    ?.subscriptionBenefit
                    ?.tier
                    ?.toIntOrNull()
                    ?.let { it >= MINIMUM_SUBSCRIPTION_TIER } == true,
            )
        logger.info {
            "GIF picker permissions (enabled=${resolvedConfig.isEnabled}, " +
                "allowlisted=${resolvedConfig.isAllowlisted}, canSend=${resolvedConfig.canSend})"
        }
        if (resolvedConfig.isAvailable()) check(resolvedConfig.apiKey.isNotBlank()) { "Twitch returned no Giphy API key" }
        resolvedConfig
    }

    suspend fun getGifs(
        config: TwitchGifConfig,
        query: String,
        offset: Int = 0,
    ): Result<TwitchGifPickerPage> = runCatchingApi {
        val searchTerm = query.trim()
        val response =
            httpClient.get(
                if (searchTerm.isBlank()) "$GIPHY_API_URL/trending" else "$GIPHY_API_URL/search",
            ) {
                parameter("api_key", config.apiKey)
                parameter("pingback_id", giphyPingbackId)
                parameter("limit", GIF_LIMIT)
                parameter("offset", offset)
                parameter("rating", config.contentRating.toGiphyRating())
                if (searchTerm.isNotBlank()) parameter("q", searchTerm)
            }
        check(response.status.isSuccess()) { "Giphy returned ${response.status.value}" }
        val body = response.body<GiphyResponse>()
        if (body.data.isEmpty() && !body.message.isNullOrBlank()) error(body.message)
        var includedAd = false
        val gifs = body.data
            .asSequence()
            .filter { it.rating.isPickerRating() }
            .filter { gif ->
                if (!gif.isAd) return@filter true
                if (searchTerm.isNotBlank() || includedAd) return@filter false
                includedAd = true
                true
            }.mapNotNull { gif ->
                val original = gif.images?.original ?: return@mapNotNull null
                val preview = gif.images.fixedHeight ?: gif.images.fixedWidth ?: return@mapNotNull null
                val url = original.url.takeIf(String::isNotBlank) ?: return@mapNotNull null
                val previewUrl = preview.url.takeIf(String::isNotBlank) ?: return@mapNotNull null
                val title = gif.title?.trim().takeUnless { it.isNullOrEmpty() } ?: return@mapNotNull null
                TwitchGifPickerItem(
                    id = gif.id,
                    title = title,
                    url = url,
                    previewUrl = previewUrl,
                    width = preview.width.toIntOrNull() ?: 1,
                    height = preview.height.toIntOrNull() ?: 1,
                    searchTerm = searchTerm.takeIf(String::isNotBlank),
                )
            }.filter { it.id.isNotBlank() }
            .distinctBy { it.id }
            .toList()
        val nextOffset = (offset.toLong() + body.data.size)
            .takeIf { body.data.isNotEmpty() && it < (body.pagination?.totalCount ?: Int.MAX_VALUE) && it <= Int.MAX_VALUE }
            ?.toInt()
        TwitchGifPickerPage(gifs, nextOffset)
    }

    suspend fun sendGif(
        channelId: String,
        gif: TwitchGifPickerItem,
        webOAuthToken: String,
    ): Result<TwitchGifSendResult> = gql(
        webOAuthToken,
        SendGifRequest(
            operationName = SEND_GIF_OPERATION,
            variables =
                SendGifVariables(
                    SendGifInput(
                        channelId = channelId,
                        gifId = gif.id,
                        gifUrl = gif.url,
                        searchTerm = gif.searchTerm,
                    ),
                ),
            query = SEND_GIF_QUERY,
        ),
    ) { response ->
        val root = json.decodeFromString<SendGifResponse>(response)
        root.errorMessage()?.let(::error)
        val result = root.data?.sendGifMessage
            ?: error("Twitch did not return a GIF result")
        if (!result.error.isNullOrBlank()) {
            throw TwitchGifSendException(result.error, result.secondsUntilCanSend.coerceAtLeast(0))
        }
        check(!result.message?.id.isNullOrBlank()) { "Twitch did not send the GIF" }
        TwitchGifSendResult(result.secondsUntilCanSend.coerceAtLeast(0))
    }

    private suspend inline fun <reified Request : Any, ResultType> gql(
        webOAuthToken: String,
        request: Request,
        crossinline parse: (String) -> ResultType,
    ): Result<ResultType> = runCatchingApi {
        val response =
            gqlClient.execute(
                webOAuthToken = webOAuthToken,
                body = json.encodeToJsonElement(request) as JsonObject,
            )
        check(response.status in 200..299) { "Twitch returned ${response.status}" }
        parse(response.body)
    }

    private companion object {
        const val GIPHY_API_URL = "https://api.giphy.com/v1/gifs"
        const val GIF_LIMIT = 24
        const val MINIMUM_SUBSCRIPTION_TIER = 2000
    }
}

private suspend inline fun <T> runCatchingApi(crossinline block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (error: CancellationException) {
    throw error
} catch (error: Throwable) {
    Result.failure(error)
}

@Serializable
data class TwitchGifPickerItem(
    val id: String,
    val title: String,
    val url: String,
    val previewUrl: String,
    val width: Int,
    val height: Int,
    val searchTerm: String?,
)

data class TwitchGifSendResult(
    val secondsUntilCanSend: Int,
)

data class TwitchGifPickerPage(
    val gifs: List<TwitchGifPickerItem>,
    val nextOffset: Int?,
)

class TwitchGifSendException(
    message: String,
    val secondsUntilCanSend: Int,
) : Exception(message)

@Serializable
data class TwitchGifConfig(
    val isEnabled: Boolean,
    val isAllowlisted: Boolean,
    val canSend: Boolean = false,
    val apiKey: String = "",
    val contentRating: String = "g",
)

internal fun TwitchGifConfig.isAvailable(): Boolean = isEnabled && isAllowlisted && canSend

private val logger = KotlinLogging.logger("TwitchGifApiClient")

@Serializable
private data class GifConfigRequest(
    val operationName: String,
    val variables: GifConfigVariables,
    val query: String,
)

@Serializable
private data class GifConfigVariables(
    @SerialName("channelID") val channelId: String,
)

@Serializable
private data class GifConfigResponse(
    val data: GifConfigData? = null,
    val errors: List<TwitchGifGraphQlError> = emptyList(),
)

@Serializable
private data class GifConfigData(
    val gifPickerConfig: TwitchGifConfig? = null,
    val user: GifConfigUser? = null,
)

@Serializable
private data class GifConfigUser(
    val self: GifConfigSelf? = null,
)

@Serializable
private data class GifConfigSelf(
    val subscriptionBenefit: GifSubscriptionBenefit? = null,
)

@Serializable
private data class GifSubscriptionBenefit(
    val tier: String = "",
)

@Serializable
private data class GiphyResponse(
    val data: List<GiphyGif> = emptyList(),
    val message: String? = null,
    val pagination: GiphyPagination? = null,
)

@Serializable
private data class GiphyPagination(
    @SerialName("total_count") val totalCount: Int = Int.MAX_VALUE,
)

@Serializable
private data class GiphyGif(
    val id: String,
    val title: String? = null,
    val rating: String = "",
    @SerialName("is_ad") val isAd: Boolean = false,
    val images: GiphyImages? = null,
)

@Serializable
private data class GiphyImages(
    val original: GiphyImage? = null,
    @SerialName("fixed_height") val fixedHeight: GiphyImage? = null,
    @SerialName("fixed_width") val fixedWidth: GiphyImage? = null,
)

@Serializable
private data class GiphyImage(
    val url: String = "",
    val width: String = "1",
    val height: String = "1",
)

@Serializable
private data class SendGifRequest(
    val operationName: String,
    val variables: SendGifVariables,
    val query: String,
)

@Serializable
private data class SendGifVariables(
    val input: SendGifInput,
)

@Serializable
private data class SendGifInput(
    @SerialName("channelID") val channelId: String,
    @SerialName("gifID") val gifId: String,
    @SerialName("gifURL") val gifUrl: String,
    val searchTerm: String? = null,
)

@Serializable
private data class SendGifResponse(
    val data: SendGifData? = null,
    val errors: List<TwitchGifGraphQlError> = emptyList(),
)

@Serializable
private data class TwitchGifGraphQlError(
    val message: String = "Twitch rejected the request",
)

@Serializable
private data class SendGifData(
    val sendGifMessage: SendGifResult? = null,
)

@Serializable
private data class SendGifResult(
    val error: String? = null,
    val secondsUntilCanSend: Int = 0,
    val message: SendGifMessage? = null,
)

@Serializable
private data class SendGifMessage(
    val id: String = "",
)

internal fun String.toGiphyRating(): String = when (this) {
    "PG_13" -> "pg-13"
    "G_PG" -> "pg"
    else -> "g"
}

private fun String.isPickerRating(): Boolean = lowercase() in PICKER_RATINGS

private val PICKER_RATINGS = setOf("y", "g", "pg", "pg-13")

private fun GifConfigResponse.errorMessage(): String? = errors.takeIf(List<*>::isNotEmpty)?.joinToString("; ") { it.message }

private fun SendGifResponse.errorMessage(): String? = errors.takeIf(List<*>::isNotEmpty)?.joinToString("; ") { it.message }

private const val GIF_CONFIG_OPERATION = "getGifPickerConfig"
private const val GIF_CONFIG_QUERY =
    "query $GIF_CONFIG_OPERATION(\$channelID: ID!) { gifPickerConfig(channelID: \$channelID) { isEnabled isAllowlisted apiKey contentRating } user(id: \$channelID) { self { subscriptionBenefit { tier } } } }"

private const val SEND_GIF_OPERATION = "sendGifMessage"
private const val SEND_GIF_QUERY =
    "mutation $SEND_GIF_OPERATION(\$input: SendGifMessageInput!) { sendGifMessage(input: \$input) { error secondsUntilCanSend message { id } } }"
