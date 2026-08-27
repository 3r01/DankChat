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
import io.ktor.client.plugins.websocket.ClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.onTimeout
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withTimeout
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
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

class EventSubSupervisor(
    private val serverConfig: ServerConfig,
    private val stateStore: StateStore,
    private val oauthClient: TwitchOAuthClient,
    private val pushSender: PushSender,
    private val monitor: EventSubMonitor = EventSubMonitor(),
    private val client: HttpClient = defaultClient(),
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val participationTracker = ParticipationTracker()

    fun start(scope: CoroutineScope) =
        scope.launch {
            runRestartingWorker(
                restartDelay = SUPERVISOR_RESTART_DELAY,
                onFailure = { cause ->
                    monitor.markDisconnected(cause)
                    logger.error("EventSub supervisor failed unexpectedly; restarting", cause)
                },
            ) {
                stateStore.state.collectLatest { state ->
                    val configuration = state.configuration ?: return@collectLatest
                    val tokens = state.twitchTokens ?: return@collectLatest
                    runWithReconnect(configuration, tokens)
                }
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
                monitor.markConnecting()
                runSession(configuration, tokens)
                backoff = 1.seconds
            } catch (e: TwitchTokenRejectedException) {
                tokens = oauthClient.refresh(tokens.refreshToken)
                stateStore.updateTwitchTokens(tokens)
                backoff = 1.seconds
            } catch (e: CancellationException) {
                currentCoroutineContext().ensureActive()
                reconnectAfterFailure(e, backoff)
                backoff = (backoff * 2).coerceAtMost(60.seconds)
            } catch (e: Exception) {
                reconnectAfterFailure(e, backoff)
                backoff = (backoff * 2).coerceAtMost(60.seconds)
            }
        }
    }

    private suspend fun reconnectAfterFailure(
        cause: Throwable,
        backoff: Duration,
    ) {
        monitor.markDisconnected(cause)
        logger.warn("EventSub connection failed; reconnecting in {}", backoff, cause)
        delay(backoff)
    }

    private suspend fun runSession(
        configuration: PushConfiguration,
        tokens: TwitchTokens,
    ) = coroutineScope {
        val validationJob =
            launch {
                while (true) {
                    delay(1.hours)
                    oauthClient.validate(tokens.accessToken, tokens.userId)
                }
            }
        var session = client.webSocketSession(EVENTSUB_WEBSOCKET_URL)
        try {
            var welcome = session.awaitWelcome()
            createSubscriptions(welcome.sessionId, configuration, tokens.accessToken)
            val subscriptionCount = configuration.channels.size + if (configuration.notifyWhispers) 1 else 0
            monitor.markConnected(subscriptionCount)
            logger.info("EventSub connected with {} subscriptions", subscriptionCount)

            while (true) {
                val reconnectUrl = consumeSession(session, welcome.keepaliveTimeout, configuration)
                val oldSession = session
                val newSession = client.webSocketSession(reconnectUrl)
                try {
                    welcome = awaitReconnectWelcome(oldSession, newSession, welcome.keepaliveTimeout, configuration)
                    session = newSession
                } catch (e: Exception) {
                    newSession.close()
                    throw e
                }
                oldSession.close()
            }
        } finally {
            monitor.markDisconnected(null)
            validationJob.cancel()
            session.close()
        }
    }

    private suspend fun consumeSession(
        session: ClientWebSocketSession,
        keepaliveTimeout: Duration,
        configuration: PushConfiguration,
    ): String {
        while (true) {
            val envelope =
                withTimeout(keepaliveTimeout) {
                    session.receiveEnvelope()
                }
            handleEnvelope(envelope, configuration)?.let { return it }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun awaitReconnectWelcome(
        oldSession: ClientWebSocketSession,
        newSession: ClientWebSocketSession,
        keepaliveTimeout: Duration,
        configuration: PushConfiguration,
    ): SessionWelcome =
        coroutineScope {
            val welcome = async { newSession.awaitWelcome() }
            var connected: SessionWelcome? = null
            while (connected == null) {
                connected =
                    select<SessionWelcome?> {
                        welcome.onAwait { it }
                        oldSession.incoming.onReceiveCatching { result ->
                            val frame =
                                result.getOrNull()
                                    ?: throw IllegalStateException("EventSub closed during reconnect", result.exceptionOrNull())
                            if (frame is Frame.Text) {
                                handleEnvelope(json.parseToJsonElement(frame.readText()).jsonObject, configuration)
                            }
                            null
                        }
                        onTimeout(keepaliveTimeout) {
                            error("EventSub keepalive timed out during reconnect")
                        }
                    }
            }
            connected
        }

    private suspend fun ClientWebSocketSession.awaitWelcome(): SessionWelcome {
        val envelope =
            withTimeout(WELCOME_TIMEOUT) {
                receiveEnvelope()
            }
        check(envelope.metadataString("message_type") == "session_welcome") { "EventSub did not send a welcome message" }
        return parseEventSubWelcome(envelope)
    }

    private suspend fun ClientWebSocketSession.receiveEnvelope(): JsonObject {
        while (true) {
            val result = incoming.receiveCatching()
            val frame =
                result.getOrNull()
                    ?: throw IllegalStateException("EventSub connection closed", result.exceptionOrNull())
            if (frame is Frame.Text) return json.parseToJsonElement(frame.readText()).jsonObject
        }
    }

    private suspend fun handleEnvelope(
        envelope: JsonObject,
        configuration: PushConfiguration,
    ): String? =
        when (envelope.metadataString("message_type")) {
            "notification" -> {
                monitor.markActivity()
                handleNotification(envelope, configuration)
                null
            }

            "session_reconnect" -> {
                monitor.markActivity()
                parseEventSubReconnectUrl(envelope)
            }

            "revocation" -> {
                monitor.markActivity()
                logger.warn("EventSub subscription revoked: {}", envelope)
                null
            }

            "session_keepalive" -> {
                monitor.markActivity()
                null
            }

            else -> {
                null
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
                text = normalizeEventSubMessageText(event.objectAt("message").string("text")),
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
        private val WELCOME_TIMEOUT = 10.seconds
        private val SUPERVISOR_RESTART_DELAY = 1.seconds

        private fun defaultClient() =
            HttpClient(OkHttp) {
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
                install(WebSockets)
            }
    }
}

internal suspend fun runRestartingWorker(
    restartDelay: Duration,
    onFailure: (Throwable) -> Unit,
    worker: suspend () -> Unit,
) {
    while (true) {
        try {
            worker()
            error("EventSub worker completed unexpectedly")
        } catch (e: CancellationException) {
            currentCoroutineContext().ensureActive()
            onFailure(e)
        } catch (e: Exception) {
            onFailure(e)
        }
        delay(restartDelay)
    }
}

internal fun normalizeEventSubMessageText(text: String): String =
    if (text.startsWith(ACTION_PREFIX) && text.endsWith(ACTION_SUFFIX)) {
        text.substring(ACTION_PREFIX.length, text.length - ACTION_SUFFIX.length)
    } else {
        text
    }

internal fun parseEventSubWelcome(envelope: JsonObject): SessionWelcome {
    val session =
        envelope
            .getValue("payload")
            .jsonObject
            .getValue("session")
            .jsonObject
    return SessionWelcome(
        sessionId = session.getValue("id").jsonPrimitive.content,
        keepaliveTimeout =
            session
                .getValue("keepalive_timeout_seconds")
                .jsonPrimitive.content
                .toLong()
                .seconds,
    )
}

internal fun parseEventSubReconnectUrl(envelope: JsonObject): String =
    envelope
        .getValue("payload")
        .jsonObject
        .getValue("session")
        .jsonObject
        .getValue("reconnect_url")
        .jsonPrimitive
        .content

internal data class SessionWelcome(
    val sessionId: String,
    val keepaliveTimeout: Duration,
)

private const val ACTION_PREFIX = "\u0001ACTION "
private const val ACTION_SUFFIX = "\u0001"
