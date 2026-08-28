package com.flxrs.dankchat.data.repo.chat

import com.flxrs.dankchat.data.api.eventapi.EventSubManager
import com.flxrs.dankchat.data.toUserName
import com.flxrs.dankchat.data.twitch.chat.ChatConnection
import com.flxrs.dankchat.data.twitch.chat.ConnectionState
import com.flxrs.dankchat.data.twitch.pubsub.PubSubManager
import com.flxrs.dankchat.di.DispatchersProvider
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
internal class ChatConnectorRemotePushTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val dispatchersProvider =
        object : DispatchersProvider {
            override val default: CoroutineDispatcher = testDispatcher
            override val io: CoroutineDispatcher = testDispatcher
            override val main: CoroutineDispatcher = testDispatcher
            override val immediate: CoroutineDispatcher = testDispatcher
        }
    private val readConnection: ChatConnection = mockk(relaxed = true) { every { connected } returns MutableStateFlow(true) }
    private val writeConnection: ChatConnection = mockk(relaxed = true) { every { connected } returns MutableStateFlow(true) }
    private val connector =
        ChatConnector(
            readConnection = readConnection,
            writeConnection = writeConnection,
            pubSubManager = mockk<PubSubManager>(relaxed = true),
            eventSubManager = mockk<EventSubManager>(relaxed = true),
            dispatchersProvider = dispatchersProvider,
        )

    @Test
    fun `remote push transition tracks intentionally disconnected channels until reconnect`() = runTest(testDispatcher) {
        val connected = "connected".toUserName()
        connector.createConnectionState(connected)
        connector.setAllConnectionStates(ConnectionState.CONNECTED)

        connector.pauseForRemotePush()

        assertTrue(connector.isRemotePushTransition())
        assertEquals(setOf(connected), connector.completeRemotePushTransition())
        assertFalse(connector.isRemotePushTransition())
    }
}
