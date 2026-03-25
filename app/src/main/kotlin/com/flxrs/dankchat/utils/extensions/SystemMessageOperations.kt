package com.flxrs.dankchat.utils.extensions

import com.flxrs.dankchat.data.chat.ChatItem
import com.flxrs.dankchat.data.twitch.message.SystemMessage
import com.flxrs.dankchat.data.twitch.message.SystemMessageType
import com.flxrs.dankchat.data.twitch.message.toChatItem

fun List<ChatItem>.addSystemMessage(type: SystemMessageType, scrollBackLength: Int, onMessageRemoved: (ChatItem) -> Unit, onReconnect: () -> Unit = {}): List<ChatItem> {
    return when {
        type != SystemMessageType.Connected -> addAndLimit(type.toChatItem(), scrollBackLength, onMessageRemoved)
        else                                -> replaceLastSystemMessageIfNecessary(scrollBackLength, onMessageRemoved, onReconnect)
    }
}

private fun List<ChatItem>.replaceLastSystemMessageIfNecessary(scrollBackLength: Int, onMessageRemoved: (ChatItem) -> Unit, onReconnect: () -> Unit): List<ChatItem> {
    val item = lastOrNull()
    val message = item?.message
    return when ((message as? SystemMessage)?.type) {
        SystemMessageType.Disconnected          -> {
            onReconnect()
            dropLast(1) + item.copy(message = SystemMessage(SystemMessageType.Reconnected))
        }

        is SystemMessageType.ChannelNonExistent -> dropLast(1) + SystemMessageType.Connected.toChatItem()
        else                                    -> addAndLimit(SystemMessageType.Connected.toChatItem(), scrollBackLength, onMessageRemoved)
    }
}
