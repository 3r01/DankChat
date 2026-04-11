package com.flxrs.dankchat.data.twitch.pubsub

import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.auth.AuthDataStore
import com.flxrs.dankchat.data.auth.StartupValidationHolder
import com.flxrs.dankchat.data.repo.channel.Channel
import com.flxrs.dankchat.data.repo.channel.ChannelRepository
import com.flxrs.dankchat.data.repo.chat.ChatChannelProvider
import com.flxrs.dankchat.data.toUserId
import com.flxrs.dankchat.di.DispatchersProvider
import com.flxrs.dankchat.preferences.developer.DeveloperSettingsDataStore
import com.flxrs.dankchat.utils.extensions.withoutOAuthPrefix
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSockets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single
import kotlinx.coroutines.channels.Channel as CoroutineChannel

private val logger = KotlinLogging.logger("PubSubManager")

@Single
class PubSubManager(
    private val channelRepository: ChannelRepository,
    private val chatChannelProvider: ChatChannelProvider,
    private val developerSettingsDataStore: DeveloperSettingsDataStore,
    private val authDataStore: AuthDataStore,
    private val startupValidationHolder: StartupValidationHolder,
    httpClient: HttpClient,
    private val json: Json,
    dispatchersProvider: DispatchersProvider,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatchersProvider.default)
    private val client =
        httpClient.config {
            install(WebSockets)
        }
    private val connections = mutableListOf<PubSubConnection>()
    private val collectJobs = mutableListOf<Job>()
    private val receiveChannel = CoroutineChannel<PubSubMessage>(capacity = CoroutineChannel.BUFFERED)

    val messages = receiveChannel.receiveAsFlow().shareIn(scope, started = SharingStarted.Eagerly)
    val connected: Boolean
        get() = connections.any { it.connected }

    init {
        scope.launch {
            startupValidationHolder.awaitResolved()
            combine(
                authDataStore.settings.map { it.isLoggedIn to it.userId }.distinctUntilChanged(),
                chatChannelProvider.channels.filterNotNull(),
                developerSettingsDataStore.settings.map { it.shouldUsePubSub }.distinctUntilChanged(),
            ) { (isLoggedIn, userId), channels, shouldUsePubSub ->
                Triple(if (isLoggedIn) userId else null, channels, shouldUsePubSub)
            }.collect { (userId, channels, shouldUsePubSub) ->
                closeAll()
                if (userId == null) {
                    logger.debug { "[PubSub] skipping connection, not logged in" }
                    return@collect
                }
                val resolved = channelRepository.getChannels(channels)
                val topics = buildTopics(userId, resolved, shouldUsePubSub)
                logger.info { "[PubSub] rebuilding connections for ${resolved.size} channels, ${topics.size} topics (pubsub=$shouldUsePubSub)" }
                listen(topics)
            }
        }
    }

    fun reconnect() = resetCollectionWith { reconnect() }

    fun reconnectIfNecessary() = resetCollectionWith { reconnectIfNecessary() }

    fun removeChannel(channel: UserName) {
        val emptyConnections =
            connections
                .onEach { it.unlistenByChannel(channel) }
                .filterNot { it.hasTopics }
                .onEach { it.close() }

        if (emptyConnections.isEmpty()) {
            return
        }

        connections.removeAll { conn -> emptyConnections.any { it.tag == conn.tag } }
        resetCollectionWith()
    }

    private fun buildTopics(
        userId: String,
        channels: List<Channel>,
        shouldUsePubSub: Boolean,
    ): Set<PubSubTopic> = buildSet {
        val uid = userId.toUserId()
        for (channel in channels) {
            add(PubSubTopic.PointRedemptions(channelId = channel.id, channelName = channel.name))
            if (shouldUsePubSub) {
                add(PubSubTopic.ModeratorActions(userId = uid, channelId = channel.id, channelName = channel.name))
            }
        }
    }

    private fun listen(topics: Set<PubSubTopic>) {
        val oAuth = authDataStore.oAuthKey?.withoutOAuthPrefix ?: return
        val remainingTopics =
            connections.fold(topics) { acc, conn ->
                conn.listen(acc)
            }

        if (remainingTopics.isEmpty() || connections.size >= PubSubConnection.MAX_CONNECTIONS) {
            return
        }

        scope.launch {
            remainingTopics
                .chunked(PubSubConnection.MAX_TOPICS)
                .withIndex()
                .takeWhile { (idx, _) -> connections.size + idx + 1 <= PubSubConnection.MAX_CONNECTIONS }
                .forEach { (_, chunk) ->
                    val connection =
                        PubSubConnection(
                            tag = "#${connections.size}",
                            client = client,
                            scope = this,
                            oAuth = oAuth,
                            jsonFormat = json,
                        )
                    connection.connect(initialTopics = chunk.toSet())
                    connections += connection
                    collectJobs += launch { connection.collectEvents() }
                }
        }
    }

    private fun closeAll() {
        collectJobs.forEach { it.cancel() }
        collectJobs.clear()
        connections.forEach { it.close() }
        connections.clear()
    }

    private fun resetCollectionWith(action: PubSubConnection.() -> Unit = {}) = scope.launch {
        collectJobs.forEach { it.cancel() }
        collectJobs.clear()
        collectJobs.addAll(
            elements =
                connections
                    .map { conn ->
                        conn.action()
                        launch { conn.collectEvents() }
                    },
        )
    }

    private suspend fun PubSubConnection.collectEvents() {
        events.collect {
            when (it) {
                is PubSubEvent.Message -> receiveChannel.send(it.message)
                else -> Unit
            }
        }
    }
}
