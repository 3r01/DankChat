package com.flxrs.dankchat.data.notification

import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.toDisplayName
import com.flxrs.dankchat.data.toUserName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class NotificationConversationStoreTest {
    @Test
    fun `channel summary is capped without discarding child notifications`() {
        val store = NotificationConversationStore(historyLimit = 2)
        val channel = "channel".toUserName()

        (1..3).forEach { id -> store.addChannelMessage(channel, id, notificationData(id)) }

        assertEquals(listOf("2", "3"), store.channelSummary(channel).map(NotificationData::id))
        assertEquals(listOf(1, 2, 3), store.clearChannel(channel))
    }

    @Test
    fun `dismissing one channel notification preserves the others`() {
        val store = NotificationConversationStore(historyLimit = 25)
        val channel = "channel".toUserName()
        (1..3).forEach { id -> store.addChannelMessage(channel, id, notificationData(id)) }

        store.removeChannelMessage(channel, notificationId = 2)

        assertEquals(listOf("1", "3"), store.channelSummary(channel).map(NotificationData::id))
        assertEquals(listOf(1, 3), store.clearChannel(channel))
    }

    @Test
    fun `clearing one channel preserves other channels`() {
        val store = NotificationConversationStore(historyLimit = 25)
        val first = "first".toUserName()
        val second = "second".toUserName()
        store.addChannelMessage(first, 1, notificationData(1, first))
        store.addChannelMessage(second, 2, notificationData(2, second))

        assertEquals(listOf(1), store.clearChannel(first))
        assertEquals(listOf("2"), store.channelSummary(second).map(NotificationData::id))
    }

    @Test
    fun `whisper conversations retain only the latest messages`() {
        val store = NotificationConversationStore(historyLimit = 2)
        val sender = "sender".toUserName()
        val target = notificationData(1)

        store.addWhisperMessage(sender, target, conversationMessage("one"))
        store.addWhisperMessage(sender, target, conversationMessage("two"))
        store.addWhisperMessage(sender, target, conversationMessage("three"))

        assertEquals(listOf("two", "three"), store.whisper(sender)?.messages?.map(ConversationMessage::text))
    }

    @Test
    fun `clearing one whisper preserves other senders`() {
        val store = NotificationConversationStore(historyLimit = 25)
        val first = "first".toUserName()
        val second = "second".toUserName()
        store.addWhisperMessage(first, notificationData(1), conversationMessage("one"))
        store.addWhisperMessage(second, notificationData(2), conversationMessage("two"))

        store.clearWhisper(first)

        assertNull(store.whisper(first))
        assertEquals(listOf("two"), store.whisper(second)?.messages?.map(ConversationMessage::text))
    }

    private fun notificationData(
        id: Int,
        channel: UserName = "channel".toUserName(),
    ) = NotificationData(
        id = id.toString(),
        timestamp = id.toLong(),
        channel = channel,
        userId = null,
        name = "sender".toUserName(),
        displayName = "Sender".toDisplayName(),
        message = "message $id",
    )

    private fun conversationMessage(text: String) = ConversationMessage(
        text = text,
        timestamp = 0L,
        sender = ConversationSender(name = "Sender", key = "sender"),
    )
}
