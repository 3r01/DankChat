package com.flxrs.dankchat.data.notification

import com.flxrs.dankchat.data.chat.ChatItem
import com.flxrs.dankchat.data.chat.toMentionTabItems
import com.flxrs.dankchat.data.irc.IrcMessage
import com.flxrs.dankchat.data.repo.chat.ChatNotificationRepository
import com.flxrs.dankchat.data.repo.chat.MessageProcessor
import com.flxrs.dankchat.data.toUserName
import com.flxrs.dankchat.data.twitch.message.Highlight
import com.flxrs.dankchat.data.twitch.message.HighlightType
import com.flxrs.dankchat.data.twitch.message.PrivMessage
import com.flxrs.dankchat.data.twitch.message.hasMention
import com.flxrs.dankchat.preferences.notifications.RemotePushSettingsDataStore
import com.flxrs.dankchat.push.MentionHistoryMessage
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.annotation.Single

@Single
class RemoteMentionHistoryRepository(
    private val remotePushSettingsDataStore: RemotePushSettingsDataStore,
    private val remotePushClient: RemotePushClient,
    private val messageProcessor: MessageProcessor,
    private val chatNotificationRepository: ChatNotificationRepository,
) {
    private val mutex = Mutex()

    suspend fun restore(): Result<Int> = mutex.withLock {
        val settings = remotePushSettingsDataStore.current()
        if (!settings.isConfigured) return Result.success(0)

        remotePushClient.getMentionHistory(settings).mapCatching { response ->
            val items =
                response.messages
                    .mapNotNull { stored ->
                        val message = messageProcessor.processIrcMessage(stored.toIrcMessage()) as? PrivMessage ?: return@mapNotNull null
                        val highlighted =
                            if (message.highlights.hasMention()) {
                                message
                            } else {
                                message.copy(highlights = message.highlights + Highlight(HighlightType.Custom))
                            }
                        ChatItem(highlighted)
                    }.toMentionTabItems()
            chatNotificationRepository.addMentionsDeduped(items)
            items.size
        }
    }
}

internal fun MentionHistoryMessage.toIrcMessage(): IrcMessage {
    val badgeTag = badges.joinToString(",") { "${it.setId}/${it.id}" }
    val badgeInfoTag = badges.filter { !it.info.isNullOrBlank() }.joinToString(",") { "${it.setId}/${it.info}" }
    val emoteTag =
        emotes
            .groupBy { it.id }
            .entries
            .joinToString("/") { (id, positions) -> "$id:${positions.joinToString(",") { "${it.start}-${it.end}" }}" }
    val tags =
        buildMap {
            put("id", messageId)
            put("tmi-sent-ts", timestamp.toString())
            put("room-id", channelId)
            put("user-id", senderUserId)
            put("display-name", senderDisplayName)
            put("color", color.orEmpty())
            put("badges", badgeTag)
            put("badge-info", badgeInfoTag)
            put("emotes", emoteTag)
            reply?.let { value ->
                put("reply-parent-msg-id", value.parentMessageId)
                put("reply-parent-msg-body", value.parentMessageBody)
                put("reply-parent-user-id", value.parentUserId)
                put("reply-parent-user-login", value.parentUserName)
                put("reply-parent-display-name", value.parentDisplayName)
                put("reply-thread-parent-msg-id", value.threadMessageId)
                put("reply-thread-parent-user-login", value.threadUserName)
                put("reply-thread-parent-display-name", value.threadDisplayName)
            }
        }
    val body = if (isAction) "\u0001ACTION $text\u0001" else text
    return IrcMessage(
        raw = "",
        prefix = "$senderUserName!$senderUserName@$senderUserName.tmi.twitch.tv",
        command = "PRIVMSG",
        params = listOf("#${channelName.toUserName()}", body),
        tags = tags,
    )
}
