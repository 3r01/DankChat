package com.flxrs.dankchat.data.notification

import com.flxrs.dankchat.push.MentionHistoryBadge
import com.flxrs.dankchat.push.MentionHistoryEmote
import com.flxrs.dankchat.push.MentionHistoryMessage
import com.flxrs.dankchat.push.MentionHistoryReply
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class RemoteMentionHistoryTest {
    @Test
    fun `stored mention maps to a processable IRC message`() {
        val message =
            MentionHistoryMessage(
                messageId = "message-id",
                timestamp = 1234,
                channelId = "channel-id",
                channelName = "forsen",
                senderUserId = "sender-id",
                senderUserName = "sender",
                senderDisplayName = "Sender",
                text = "Kappa hello",
                color = "#123456",
                isAction = true,
                badges = listOf(MentionHistoryBadge("moderator", "1", "badge-info")),
                emotes = listOf(MentionHistoryEmote("25", 0, 4)),
                reply =
                    MentionHistoryReply(
                        parentMessageId = "parent-id",
                        parentMessageBody = "parent body",
                        parentUserId = "parent-user-id",
                        parentUserName = "parent",
                        parentDisplayName = "Parent",
                        threadMessageId = "thread-id",
                        threadMessageBody = "thread body",
                        threadUserId = "thread-user-id",
                        threadUserName = "thread",
                        threadDisplayName = "Thread",
                    ),
            )

        val irc = message.toIrcMessage()

        assertEquals("PRIVMSG", irc.command)
        assertEquals(listOf("#forsen", "\u0001ACTION Kappa hello\u0001"), irc.params)
        assertEquals("message-id", irc.tags["id"])
        assertEquals("moderator/1", irc.tags["badges"])
        assertEquals("moderator/badge-info", irc.tags["badge-info"])
        assertEquals("25:0-4", irc.tags["emotes"])
        assertEquals("parent-id", irc.tags["reply-parent-msg-id"])
        assertEquals("thread-id", irc.tags["reply-thread-parent-msg-id"])
    }
}
