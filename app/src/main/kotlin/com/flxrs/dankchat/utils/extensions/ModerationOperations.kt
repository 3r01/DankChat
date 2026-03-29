package com.flxrs.dankchat.utils.extensions

import com.flxrs.dankchat.data.chat.ChatImportance
import com.flxrs.dankchat.data.chat.ChatItem
import com.flxrs.dankchat.data.twitch.message.ModerationMessage
import com.flxrs.dankchat.data.twitch.message.PrivMessage
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

fun MutableList<ChatItem>.replaceOrAddHistoryModerationMessage(moderationMessage: ModerationMessage) {
    if (!moderationMessage.canClearMessages) {
        return
    }

    if (checkForStackedTimeouts(moderationMessage)) {
        add(ChatItem(moderationMessage, importance = ChatImportance.SYSTEM))
    }
}

fun List<ChatItem>.replaceOrAddModerationMessage(moderationMessage: ModerationMessage, scrollBackLength: Int, onMessageRemoved: (ChatItem) -> Unit): List<ChatItem> = toMutableList().apply {
    if (!moderationMessage.canClearMessages) {
        addAndTrimInline(ChatItem(moderationMessage, importance = ChatImportance.SYSTEM), scrollBackLength, onMessageRemoved)
        return this
    }

    val addSystemMessage = checkForStackedTimeouts(moderationMessage)
    for (idx in indices) {
        val item = this[idx]
        when (moderationMessage.action) {
            ModerationMessage.Action.Clear -> {
                this[idx] =
                    when (item.message) {
                        is PrivMessage -> item.copy(tag = item.tag + 1, message = item.message.copy(timedOut = true), importance = ChatImportance.DELETED)
                        else -> item.copy(tag = item.tag + 1, importance = ChatImportance.DELETED)
                    }
            }

            ModerationMessage.Action.Timeout,
            ModerationMessage.Action.Ban,
            ModerationMessage.Action.SharedTimeout,
            ModerationMessage.Action.SharedBan,
            -> {
                item.message as? PrivMessage ?: continue
                if (moderationMessage.targetUser != item.message.name) {
                    continue
                }

                this[idx] = item.copy(tag = item.tag + 1, message = item.message.copy(timedOut = true), importance = ChatImportance.DELETED)
            }

            else -> {
                continue
            }
        }
    }

    if (addSystemMessage) {
        addAndTrimInline(ChatItem(moderationMessage, importance = ChatImportance.SYSTEM), scrollBackLength, onMessageRemoved)
    }
}

fun List<ChatItem>.replaceWithTimeout(moderationMessage: ModerationMessage, scrollBackLength: Int, onMessageRemoved: (ChatItem) -> Unit): List<ChatItem> = toMutableList().apply {
    val targetMsgId = moderationMessage.targetMsgId ?: return@apply
    if (moderationMessage.fromEventSource) {
        val end = (lastIndex - 20).coerceAtLeast(0)
        for (idx in lastIndex downTo end) {
            val item = this[idx]
            val message = item.message as? ModerationMessage ?: continue
            if ((message.action == ModerationMessage.Action.Delete || message.action == ModerationMessage.Action.SharedDelete) && message.targetMsgId == targetMsgId && !message.fromEventSource) {
                this[idx] = item.copy(tag = item.tag + 1, message = moderationMessage)
                return@apply
            }
        }
    }

    for (idx in indices) {
        val item = this[idx]
        if (item.message is PrivMessage && item.message.id == targetMsgId) {
            this[idx] = item.copy(tag = item.tag + 1, message = item.message.copy(timedOut = true), importance = ChatImportance.DELETED)
            break
        }
    }

    addAndTrimInline(ChatItem(moderationMessage, importance = ChatImportance.SYSTEM), scrollBackLength, onMessageRemoved)
}

private fun MutableList<ChatItem>.checkForStackedTimeouts(moderationMessage: ModerationMessage): Boolean {
    if (moderationMessage.canStack) {
        val end = (lastIndex - 20).coerceAtLeast(0)
        for (idx in lastIndex downTo end) {
            val item = this[idx]
            val message = item.message as? ModerationMessage ?: continue
            if (message.targetUser != moderationMessage.targetUser || message.action != moderationMessage.action) {
                continue
            }

            if ((moderationMessage.timestamp - message.timestamp).milliseconds >= 5.seconds) {
                return true
            }

            when {
                !moderationMessage.fromEventSource && message.fromEventSource -> {
                    Unit
                }

                moderationMessage.fromEventSource && !message.fromEventSource -> {
                    this[idx] = item.copy(tag = item.tag + 1, message = moderationMessage)
                }

                moderationMessage.action == ModerationMessage.Action.Timeout || moderationMessage.action == ModerationMessage.Action.SharedTimeout -> {
                    val stackedMessage = moderationMessage.copy(stackCount = message.stackCount + 1)
                    this[idx] = item.copy(tag = item.tag + 1, message = stackedMessage)
                }
            }
            return false
        }
    }

    return true
}
