package com.flxrs.dankchat.data.notification

import com.flxrs.dankchat.data.UserName

internal class NotificationConversationStore(
    private val historyLimit: Int,
) {
    private val lock = Any()
    private val channelNotifications = mutableMapOf<UserName, LinkedHashMap<Int, NotificationData>>()
    private val whisperNotifications = mutableMapOf<UserName, WhisperNotificationState>()

    fun addChannelMessage(
        channel: UserName,
        notificationId: Int,
        data: NotificationData,
    ) {
        synchronized(lock) {
            channelNotifications.getOrPut(channel) { linkedMapOf() }[notificationId] = data
        }
    }

    fun channelSummary(channel: UserName): List<NotificationData> = synchronized(lock) {
        channelNotifications[channel]
            ?.values
            ?.toList()
            ?.takeLast(historyLimit)
            .orEmpty()
    }

    fun removeChannelMessage(
        channel: UserName,
        notificationId: Int,
    ) {
        synchronized(lock) {
            channelNotifications[channel]?.remove(notificationId)
            if (channelNotifications[channel].isNullOrEmpty()) {
                channelNotifications.remove(channel)
            }
        }
    }

    fun clearChannel(channel: UserName): List<Int> = synchronized(lock) {
        channelNotifications
            .remove(channel)
            ?.keys
            ?.toList()
            .orEmpty()
    }

    fun channelKeys(): List<UserName> = synchronized(lock) { channelNotifications.keys.toList() }

    fun addWhisperMessage(
        key: UserName,
        target: NotificationData,
        message: ConversationMessage,
    ) {
        synchronized(lock) {
            val state = whisperNotifications.getOrPut(key) { WhisperNotificationState(target) }
            state.target = target
            state.messages.add(message)
            state.messages.trimHistory()
        }
    }

    fun whisper(key: UserName): WhisperNotificationSnapshot? = synchronized(lock) {
        whisperNotifications[key]?.let { WhisperNotificationSnapshot(it.target, it.messages.toList()) }
    }

    fun clearWhisper(key: UserName) {
        synchronized(lock) {
            whisperNotifications.remove(key)
        }
    }

    fun whisperKeys(): List<UserName> = synchronized(lock) { whisperNotifications.keys.toList() }

    private fun MutableList<ConversationMessage>.trimHistory() {
        while (size > historyLimit) {
            removeAt(0)
        }
    }

    private data class WhisperNotificationState(
        var target: NotificationData,
        val messages: MutableList<ConversationMessage> = mutableListOf(),
    )
}

internal data class WhisperNotificationSnapshot(
    val target: NotificationData,
    val messages: List<ConversationMessage>,
)

internal data class ConversationMessage(
    val text: String,
    val timestamp: Long,
    val sender: ConversationSender,
)

internal data class ConversationSender(
    val name: String,
    val key: String,
)
