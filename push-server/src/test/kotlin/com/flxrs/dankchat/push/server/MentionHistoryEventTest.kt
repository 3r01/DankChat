package com.flxrs.dankchat.push.server

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

internal class MentionHistoryEventTest {
    @Test
    fun `EventSub mention preserves rendering and reply metadata`() {
        val event = Json.parseToJsonElement(EVENT).jsonObject

        val mention = parseMentionHistoryMessage(event, timestamp = 1234, normalizedText = "🙂Kappa")

        assertEquals("message-id", mention.messageId)
        assertEquals("#123456", mention.color)
        assertFalse(mention.isAction)
        assertEquals("moderator", mention.badges.single().setId)
        assertEquals("1", mention.badges.single().id)
        assertEquals("info", mention.badges.single().info)
        assertEquals(1, mention.emotes.single().start)
        assertEquals(5, mention.emotes.single().end)
        assertEquals("parent-id", mention.reply?.parentMessageId)
        assertEquals("thread-id", mention.reply?.threadMessageId)
        assertEquals("thread", mention.reply?.threadUserName)
    }

    private companion object {
        const val EVENT =
            """
            {
              "broadcaster_user_id": "channel-id",
              "broadcaster_user_login": "channel",
              "chatter_user_id": "sender-id",
              "chatter_user_login": "sender",
              "chatter_user_name": "Sender",
              "message_id": "message-id",
              "color": "#123456",
              "badges": [{"set_id":"moderator","id":"1","info":"info"}],
              "message": {
                "text": "🙂Kappa",
                "fragments": [
                  {"type":"text","text":"🙂","emote":null},
                  {"type":"emote","text":"Kappa","emote":{"id":"25"}}
                ]
              },
              "reply": {
                "parent_message_id": "parent-id",
                "parent_message_body": "parent body",
                "parent_user_id": "parent-user-id",
                "parent_user_login": "parent",
                "parent_user_name": "Parent",
                "thread_message_id": "thread-id",
                "thread_message_body": "thread body",
                "thread_user_id": "thread-user-id",
                "thread_user_login": "thread",
                "thread_user_name": "Thread"
              }
            }
            """
    }
}
