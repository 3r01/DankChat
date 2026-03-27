package com.flxrs.dankchat.data.repo.chat

import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.api.eventapi.EventSubManager
import com.flxrs.dankchat.data.twitch.chat.ChatConnection
import com.flxrs.dankchat.data.twitch.chat.ConnectionState
import com.flxrs.dankchat.data.twitch.pubsub.PubSubManager
import com.flxrs.dankchat.di.DispatchersProvider
import com.flxrs.dankchat.di.ReadConnection
import com.flxrs.dankchat.di.WriteConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import java.util.concurrent.ConcurrentHashMap

@Single
class ChatConnector(
    @Named(type = ReadConnection::class) private val readConnection: ChatConnection,
    @Named(type = WriteConnection::class) private val writeConnection: ChatConnection,
    private val pubSubManager: PubSubManager,
    private val eventSubManager: EventSubManager,
    dispatchersProvider: DispatchersProvider,
) {

    private val scope = CoroutineScope(SupervisorJob() + dispatchersProvider.default)
    private val connectionState = ConcurrentHashMap<UserName, MutableStateFlow<ConnectionState>>()

    val readEvents get() = readConnection.messages
    val writeEvents get() = writeConnection.messages
    val pubSubEvents get() = pubSubManager.messages
    val eventSubEvents get() = eventSubManager.events

    fun getConnectionState(channel: UserName): StateFlow<ConnectionState> =
        connectionState.getOrPut(channel) { MutableStateFlow(ConnectionState.DISCONNECTED) }

    fun setConnectionState(channel: UserName, state: ConnectionState) {
        connectionState.getOrPut(channel) { MutableStateFlow(state) }.value = state
    }

    fun setAllConnectionStates(state: ConnectionState) {
        connectionState.forEach { (_, flow) ->
            flow.value = state
        }
    }

    fun createConnectionState(channel: UserName) {
        connectionState.putIfAbsent(channel, MutableStateFlow(ConnectionState.DISCONNECTED))
    }

    fun removeConnectionState(channel: UserName) {
        connectionState.remove(channel)
    }

    fun connectAndJoin(channels: List<UserName>) {
        if (!pubSubManager.connected) {
            pubSubManager.start()
        }

        if (!readConnection.connected) {
            readConnection.connect()
            writeConnection.connect()

            if (channels.isNotEmpty()) {
                readConnection.joinChannels(channels)
            }
        }
    }

    fun closeAndReconnect(channels: List<UserName>) = scope.launch {
        readConnection.close()
        writeConnection.close()
        pubSubManager.close()
        eventSubManager.close()
        connectAndJoin(channels)
    }

    fun reconnect(reconnectPubsub: Boolean = true) {
        readConnection.reconnect()
        writeConnection.reconnect()

        if (reconnectPubsub) {
            pubSubManager.reconnect()
            eventSubManager.reconnect()
        }
    }

    fun reconnectIfNecessary() {
        readConnection.reconnectIfNecessary()
        writeConnection.reconnectIfNecessary()
        pubSubManager.reconnectIfNecessary()
        eventSubManager.reconnectIfNecessary()
    }

    fun joinIrcChannel(channel: UserName) {
        readConnection.joinChannel(channel)
    }

    fun addPubSubChannel(channel: UserName) {
        pubSubManager.addChannel(channel)
    }

    fun partChannel(channel: UserName) {
        readConnection.partChannel(channel)
        pubSubManager.removeChannel(channel)
        eventSubManager.removeChannel(channel)
    }

    fun sendRaw(message: String) {
        writeConnection.sendMessage(message)
    }

    val connectedAndHasWhisperTopic: Boolean get() = pubSubManager.connectedAndHasWhisperTopic

    fun connectedAndHasModerateTopic(channel: UserName): Boolean = eventSubManager.connectedAndHasModerateTopic(channel)

    val connectedAndHasUserMessageTopic: Boolean get() = eventSubManager.connectedAndHasUserMessageTopic
}
