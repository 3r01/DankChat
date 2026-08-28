package com.flxrs.dankchat.data.repo.chat

import com.flxrs.dankchat.data.toDisplayName
import com.flxrs.dankchat.data.toUserId
import com.flxrs.dankchat.data.toUserName
import com.flxrs.dankchat.data.twitch.message.WhisperMessage
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class WhisperHistoryReplyStateTest {
    private val currentUserId = "me".toUserId()

    @Test
    fun `newest received whisper initializes reply target`() {
        val messages = listOf(whisper("first", "one", 100), whisper("second", "two", 200), outgoing(300))

        assertEquals("two".toUserName(), newestReceivedWhisper(messages, currentUserId, afterTimestamp = 0)?.name)
    }

    @Test
    fun `history does not replace newer live reply target`() {
        val messages = listOf(whisper("historical", "qbit", 200), outgoing(300))

        assertNull(newestReceivedWhisper(messages, currentUserId, afterTimestamp = 400))
    }

    private fun whisper(
        id: String,
        sender: String,
        timestamp: Long,
    ) = message(id, sender, "me", timestamp, senderUserId = sender)

    private fun outgoing(timestamp: Long) = message("outgoing", "me", "qbit", timestamp, senderUserId = "me")

    private fun message(
        id: String,
        sender: String,
        recipient: String,
        timestamp: Long,
        senderUserId: String,
    ) = WhisperMessage(
        timestamp = timestamp,
        id = id,
        userId = senderUserId.toUserId(),
        name = sender.toUserName(),
        displayName = sender.toDisplayName(),
        recipientId = recipient.toUserId(),
        recipientName = recipient.toUserName(),
        recipientDisplayName = recipient.toDisplayName(),
        message = "message",
        rawEmotes = "",
        rawBadges = "",
    )
}
