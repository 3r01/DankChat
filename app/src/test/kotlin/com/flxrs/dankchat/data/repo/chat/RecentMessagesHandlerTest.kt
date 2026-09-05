package com.flxrs.dankchat.data.repo.chat

import com.flxrs.dankchat.data.api.recentmessages.RecentMessagesApiClient
import com.flxrs.dankchat.data.api.recentmessages.RecentMessagesApiException
import com.flxrs.dankchat.data.api.recentmessages.RecentMessagesError
import com.flxrs.dankchat.data.api.recentmessages.dto.RecentMessagesDto
import com.flxrs.dankchat.data.toUserName
import com.flxrs.dankchat.di.DispatchersProvider
import io.ktor.http.HttpStatusCode
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.io.IOException
import kotlin.test.assertFailsWith

@OptIn(ExperimentalCoroutinesApi::class)
internal class RecentMessagesHandlerTest {
    private val dispatcher = StandardTestDispatcher()
    private val dispatchers: DispatchersProvider = mockk { every { io } returns dispatcher }
    private val api: RecentMessagesApiClient = mockk()
    private val messages: ChatMessageRepository = mockk { every { getMessagesFlow(any()) } returns null }
    private val handler = RecentMessagesHandler(api, mockk(), messages, mockk(), dispatchers)
    private val channel = "forsen".toUserName()

    @Test
    fun `reconnect retries network and server failures then caches successful history`() = runTest(dispatcher) {
        coEvery { api.getRecentMessages(channel, any()) } returnsMany listOf(
            Result.failure(IOException("Network unavailable")),
            Result.failure(RecentMessagesApiException(RecentMessagesError.Unknown, HttpStatusCode.ServiceUnavailable, null)),
            Result.success(RecentMessagesDto(emptyList(), null, null)),
        )

        handler.load(channel, isReconnect = true)
        handler.load(channel)

        coVerify(exactly = 3) { api.getRecentMessages(channel, 100) }
        coVerify(exactly = 0) { api.getRecentMessages(channel, null) }
    }

    @Test
    fun `persistent network failure has bounded retries and can recover on next foreground`() = runTest(dispatcher) {
        coEvery { api.getRecentMessages(channel, any()) } returns Result.failure(IOException("Network unavailable"))
        handler.load(channel, isReconnect = true)
        coVerify(exactly = 3) { api.getRecentMessages(channel, 100) }

        coEvery { api.getRecentMessages(channel, any()) } returns Result.success(RecentMessagesDto(emptyList(), null, null))
        handler.load(channel, isReconnect = true)
        coVerify(exactly = 4) { api.getRecentMessages(channel, 100) }
    }

    @Test
    fun `ignored channel is not retried`() = runTest(dispatcher) {
        coEvery { api.getRecentMessages(channel, any()) } returns Result.failure(
            RecentMessagesApiException(RecentMessagesError.ChannelIgnored, HttpStatusCode.Forbidden, null),
        )

        handler.load(channel, isReconnect = true)

        coVerify(exactly = 1) { api.getRecentMessages(channel, 100) }
    }

    @Test
    fun `cancelled history load propagates cancellation without retry`() = runTest(dispatcher) {
        coEvery { api.getRecentMessages(channel, any()) } returns Result.failure(CancellationException())

        assertFailsWith<CancellationException> { handler.load(channel, isReconnect = true) }

        coVerify(exactly = 1) { api.getRecentMessages(channel, 100) }
    }
}
