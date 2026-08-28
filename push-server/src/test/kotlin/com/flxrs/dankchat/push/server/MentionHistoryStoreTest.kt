package com.flxrs.dankchat.push.server

import com.flxrs.dankchat.push.MentionHistoryMessage
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertEquals

internal class MentionHistoryStoreTest {
    @TempDir
    lateinit var directory: Path

    @Test
    fun `persists deduplicates orders and bounds mention history`() =
        runTest {
            val path = directory.resolve("mentions.json")
            val store = MentionHistoryStore(path, maxMessages = 2)

            store.add("user", message("newer", timestamp = 30))
            store.add("user", message("older", timestamp = 10))
            store.add("user", message("middle", timestamp = 20))
            store.add("user", message("middle", timestamp = 25))

            assertEquals(listOf("middle", "newer"), store.getAll("user").map { it.messageId })
            assertEquals(25, store.getAll("user").first().timestamp)
            assertEquals(store.getAll("user"), MentionHistoryStore(path, maxMessages = 2).getAll("user"))
        }

    @Test
    fun `history is isolated when the authorized Twitch account changes`() =
        runTest {
            val store = MentionHistoryStore(directory.resolve("mentions.json"))
            store.add("first-user", message("first", timestamp = 10))

            assertEquals(emptyList(), store.getAll("second-user"))

            store.add("second-user", message("second", timestamp = 20))

            assertEquals(emptyList(), store.getAll("first-user"))
            assertEquals(listOf("second"), store.getAll("second-user").map { it.messageId })
        }

    private fun message(
        id: String,
        timestamp: Long,
    ) = MentionHistoryMessage(
        messageId = id,
        timestamp = timestamp,
        channelId = "1",
        channelName = "channel",
        senderUserId = "2",
        senderUserName = "sender",
        senderDisplayName = "Sender",
        text = "message",
    )
}
