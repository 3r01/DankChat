package com.flxrs.dankchat.data.repo.chat

import com.flxrs.dankchat.data.chat.ChatItem
import com.flxrs.dankchat.data.toDisplayName
import com.flxrs.dankchat.data.toUserId
import com.flxrs.dankchat.data.toUserName
import com.flxrs.dankchat.data.twitch.message.Highlight
import com.flxrs.dankchat.data.twitch.message.HighlightType
import com.flxrs.dankchat.data.twitch.message.WhisperMessage
import com.flxrs.dankchat.di.DispatchersProvider
import com.flxrs.dankchat.preferences.chat.ChatSettings
import com.flxrs.dankchat.preferences.chat.ChatSettingsDataStore
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
internal class ChatMessageRepositoryTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val dispatchersProvider =
        object : DispatchersProvider {
            override val default: CoroutineDispatcher = testDispatcher
            override val io: CoroutineDispatcher = testDispatcher
            override val main: CoroutineDispatcher = testDispatcher
            override val immediate: CoroutineDispatcher = testDispatcher
        }

    private val messageProcessor: MessageProcessor = mockk(relaxed = true)
    private val chatNotificationRepository: ChatNotificationRepository = mockk(relaxed = true)
    private val chatSettingsDataStore: ChatSettingsDataStore = mockk()
    private lateinit var repository: ChatMessageRepository

    @BeforeEach
    fun setup() {
        every { chatSettingsDataStore.debouncedScrollBack } returns flowOf(500)
        every { messageProcessor.processInlineWhisper(any()) } answers { firstArg() }
        repository =
            ChatMessageRepository(
                messageProcessor = messageProcessor,
                chatNotificationRepository = chatNotificationRepository,
                dispatchersProvider = dispatchersProvider,
                chatSettingsDataStore = chatSettingsDataStore,
            )
        repository.createMessageFlows("channel-one".toUserName())
        repository.createMessageFlows("channel-two".toUserName())
    }

    @Test
    fun `disabled inline whispers are not broadcast`() {
        every { chatSettingsDataStore.current() } returns ChatSettings(showWhispersInline = false)

        repository.broadcastWhisperIfEnabled(whisperItem())

        assertTrue(repository.getChat("channel-one".toUserName()).value.isEmpty())
        assertTrue(repository.getChat("channel-two".toUserName()).value.isEmpty())
    }

    @Test
    fun `enabled inline whispers are highlighted and broadcast to every channel`() {
        every { chatSettingsDataStore.current() } returns ChatSettings(showWhispersInline = true)
        every { messageProcessor.processInlineWhisper(any()) } answers {
            firstArg<WhisperMessage>().copy(highlights = setOf(Highlight(HighlightType.InlineWhisper)))
        }

        repository.broadcastWhisperIfEnabled(whisperItem())

        listOf("channel-one", "channel-two").forEach { channel ->
            val inlineItem = repository.getChat(channel.toUserName()).value.single()
            assertFalse(inlineItem.isMentionTab)
            assertEquals(
                setOf(HighlightType.InlineWhisper),
                inlineItem.message.highlights
                    .map { it.type }
                    .toSet(),
            )
        }
    }

    @Test
    fun `historical inline whispers merge with channel history by timestamp`() {
        every { chatSettingsDataStore.current() } returns ChatSettings(showWhispersInline = true)
        repository.replaceHistoricalWhispersInline(listOf(whisperItem(id = "whisper", timestamp = 200)))

        val merged =
            repository.mergeHistoricalInlineWhispers(
                listOf(
                    whisperItem(id = "before", timestamp = 100),
                    whisperItem(id = "after", timestamp = 300),
                ),
            )

        assertEquals(listOf("before", "whisper", "after"), merged.map { it.message.id })
    }

    @Test
    fun `channels created after history loading include historical inline whispers`() {
        every { chatSettingsDataStore.current() } returns ChatSettings(showWhispersInline = true)
        repository.replaceHistoricalWhispersInline(listOf(whisperItem(id = "whisper", timestamp = 200)))

        repository.createMessageFlows("channel-three".toUserName())

        assertEquals(
            "whisper",
            repository
                .getChat("channel-three".toUserName())
                .value
                .single()
                .message.id,
        )
    }

    private fun whisperItem(
        id: String = "whisper",
        timestamp: Long = 0,
    ) = ChatItem(
        WhisperMessage(
            timestamp = timestamp,
            id = id,
            userId = "sender-id".toUserId(),
            name = "sender".toUserName(),
            displayName = "Sender".toDisplayName(),
            recipientId = "recipient-id".toUserId(),
            recipientName = "recipient".toUserName(),
            recipientDisplayName = "Recipient".toDisplayName(),
            message = "secret",
            rawEmotes = "",
            rawBadges = "",
        ),
    )
}
