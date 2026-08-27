package com.flxrs.dankchat.push.server

import com.flxrs.dankchat.push.ChatMessageCandidate
import com.flxrs.dankchat.push.PushConfiguration
import com.flxrs.dankchat.push.PushMessage
import com.flxrs.dankchat.push.PushMessageKind
import com.flxrs.dankchat.push.PushRuleEvaluator
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.slf4j.LoggerFactory
import java.time.Instant
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

class EventSubSupervisor(
    private val serverConfig: ServerConfig,
    private val stateStore: StateStore,
    private val oauthClient: TwitchOAuthClient,
    private val pushSender: PushSender,
    private val client: HttpClient = defaultClient(),
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val participationTracker = ParticipationTracker()

    fun start(scope: CoroutineScope) =
        scope.launch {
            stateStore.state.collectLatest { state ->
                val configuration = state.configuration ?: return@collectLatest
                val tokens = state.twitchTokens ?: return@collectLatest
                runWithReconnect(configuration, tokens)
            }
        }

    private suspend fun runWithReconnect(
        configuration: PushConfiguration,
        initialTokens: TwitchTokens,
    ) {
        var tokens = initialTokens
        var backoff = 1.seconds
        while (true) {
            try {
                runSession(configuration, tokens)
                backoff = 1.seconds
            } catch (e: TwitchTokenRejectedException) {
                tokens = oauthClient.refresh(tokens.refreshToken)
                stateStore.updateTwitchTokens(tokens)
                backoff = 1.seconds
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.warn("EventSub connection failed; reconnecting in {}", backoff, e)
                delay(backoff)
                backoff = (backoff * 2).coerceAtMost(60.seconds)
            }
        }
    }

    private suspend fun runSession(
        configuration: PushConfiguration,
        tokens: TwitchTokens,
    ) {
        client.webSocket(EVENTSUB_WEBSOCKET_URL) {
            launch {
                while (true) {
                    delay(1.hours)
                    oauthClient.validate(tokens.accessToken, tokens.userId)
                }
            }
            var sessionId: String? = null
            for (frame in incoming) {
                if (frame !is Frame.Text) continue
                val envelope = json.parseToJsonElement(frame.readText()).jsonObject
                when (envelope.metadataString("message_type")) {
                    "session_welcome" -> {
                        sessionId = envelope.payload().objectAt("session").string("id")
                        createSubscriptions(sessionId, configuration, tokens.accessToken)
                    }

                    "notification" -> {
                        handleNotification(envelope, configuration)
                    }

                    "session_reconnect" -> {
                        error("EventSub requested reconnect")
                    }

                    "revocation" -> {
                        logger.warn("EventSub subscription revoked: {}", envelope)
                    }

                    "session_keepalive" -> {}
                }
            }
            checkNotNull(sessionId) { "EventSub closed before welcome" }
        }
    }

    private suspend fun createSubscriptions(
        sessionId: String,
        configuration: PushConfiguration,
        accessToken: String,
    ) = coroutineScope {
        val subscriptions =
            configuration.channels.map { channel ->
                SubscriptionRequest(
                    type = "channel.chat.message",
                    condition = mapOf("broadcaster_user_id" to channel.id, "user_id" to configuration.twitchUserId),
                )
            } +
                if (configuration.notifyWhispers) {
                    listOf(SubscriptionRequest("user.whisper.message", mapOf("user_id" to configuration.twitchUserId)))
                } else {
                    emptyList()
                }
        subscriptions
            .map { request ->
                async { createSubscription(sessionId, request, accessToken) }
            }.awaitAll()
    }

    private suspend fun createSubscription(
        sessionId: String,
        request: SubscriptionRequest,
        accessToken: String,
    ) {
        val response =
            client.post(EVENTSUB_SUBSCRIPTIONS_URL) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                header("Client-Id", serverConfig.twitchClientId)
                contentType(ContentType.Application.Json)
                setBody(
                    buildJsonObject {
                        put("type", request.type)
                        put("version", "1")
                        put("condition", request.condition.toJsonObject())
                        put(
                            "transport",
                            buildJsonObject {
                                put("method", "websocket")
                                put("session_id", sessionId)
                            },
                        )
                    },
                )
            }
        if (response.status == HttpStatusCode.Unauthorized) throw TwitchTokenRejectedException()
        check(response.status.value in 200..299) { "Failed to create ${request.type} subscription: ${response.status} ${response.body<String>()}" }
    }

    private suspend fun handleNotification(
        envelope: JsonObject,
        configuration: PushConfiguration,
    ) {
        val event = envelope.payload().objectAt("event")
        val timestamp = Instant.parse(envelope.metadataString("message_timestamp")).toEpochMilli()
        when (envelope.payload().objectAt("subscription").string("type")) {
            "channel.chat.message" -> handleChatMessage(event, timestamp, configuration)
            "user.whisper.message" -> handleWhisper(event, timestamp, configuration)
        }
    }

    private suspend fun handleChatMessage(
        event: JsonObject,
        timestamp: Long,
        configuration: PushConfiguration,
    ) {
        val senderId = event.string("chatter_user_id")
        val messageId = event.string("message_id")
        val reply = event["reply"]?.takeUnless { it is JsonNull }?.jsonObject
        val rootId = reply?.optionalString("root_message_id") ?: messageId
        val isCurrentUser = senderId == configuration.twitchUserId
        val replyTargetsCurrentUser =
            reply?.optionalString("parent_user_id") == configuration.twitchUserId ||
                reply?.optionalString("root_user_id") == configuration.twitchUserId
        val participated = participationTracker.participated(rootId, isCurrentUser, replyTargetsCurrentUser)
        val badges =
            event["badges"]
                ?.jsonArray
                ?.map { badge ->
                    val value = badge.jsonObject
                    "${value.string("set_id")}/${value.string("id")}"
                }.orEmpty()
        val candidate =
            ChatMessageCandidate(
                senderUserName = event.string("chatter_user_login"),
                text = event.objectAt("message").string("text"),
                badges = badges,
                participatedReply = participated,
                isSharedChatDuplicate = event.optionalString("source_broadcaster_user_id") != null,
            )
        if (!PushRuleEvaluator(configuration).shouldNotify(candidate)) return

        pushSender.send(
            PushMessage(
                messageId = messageId,
                timestamp = timestamp,
                channelId = event.string("broadcaster_user_id"),
                channelName = event.string("broadcaster_user_login"),
                senderUserId = senderId,
                senderUserName = candidate.senderUserName,
                senderDisplayName = event.string("chatter_user_name"),
                text = candidate.text,
                kind = PushMessageKind.Mention,
            ),
        )
    }

    private suspend fun handleWhisper(
        event: JsonObject,
        timestamp: Long,
        configuration: PushConfiguration,
    ) {
        if (!configuration.notifyWhispers) return
        pushSender.send(
            PushMessage(
                messageId = event.string("whisper_id"),
                timestamp = timestamp,
                senderUserId = event.string("from_user_id"),
                senderUserName = event.string("from_user_login"),
                senderDisplayName = event.string("from_user_name"),
                text = event.objectAt("whisper").string("text"),
                kind = PushMessageKind.Whisper,
            ),
        )
    }

    private fun JsonObject.metadataString(key: String) = objectAt("metadata").string(key)

    private fun JsonObject.payload() = objectAt("payload")

    private fun JsonObject.objectAt(key: String) = getValue(key).jsonObject

    private fun JsonObject.string(key: String) = getValue(key).jsonPrimitive.content

    private fun JsonObject.optionalString(key: String) = get(key)?.jsonPrimitive?.contentOrNull

    private fun Map<String, String>.toJsonObject() = JsonObject(mapValues { JsonPrimitive(it.value) })

    private data class SubscriptionRequest(
        val type: String,
        val condition: Map<String, String>,
    )

    companion object {
        private const val EVENTSUB_WEBSOCKET_URL = "wss://eventsub.wss.twitch.tv/ws"
        private const val EVENTSUB_SUBSCRIPTIONS_URL = "https://api.twitch.tv/helix/eventsub/subscriptions"

        private fun defaultClient() =
            HttpClient(OkHttp) {
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
                install(WebSockets)
            }
    }
}
