package com.flxrs.dankchat.data.repo.chat

import android.util.Log
import com.flxrs.dankchat.chat.ChatImportance
import com.flxrs.dankchat.chat.ChatItem
import com.flxrs.dankchat.chat.toMentionTabItems
import com.flxrs.dankchat.data.DisplayName
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.api.recentmessages.RecentMessagesApiClient
import com.flxrs.dankchat.data.api.recentmessages.RecentMessagesApiException
import com.flxrs.dankchat.data.api.recentmessages.RecentMessagesError
import com.flxrs.dankchat.data.api.recentmessages.dto.RecentMessagesDto
import com.flxrs.dankchat.data.irc.IrcMessage
import com.flxrs.dankchat.data.toDisplayName
import com.flxrs.dankchat.data.toUserId
import com.flxrs.dankchat.data.toUserName
import com.flxrs.dankchat.data.twitch.message.Message
import com.flxrs.dankchat.data.twitch.message.ModerationMessage
import com.flxrs.dankchat.data.twitch.message.PrivMessage
import com.flxrs.dankchat.data.twitch.message.SystemMessageType
import com.flxrs.dankchat.data.twitch.message.UserNoticeMessage
import com.flxrs.dankchat.data.twitch.message.hasMention
import com.flxrs.dankchat.data.twitch.message.toChatItem
import com.flxrs.dankchat.utils.extensions.addAndLimit
import com.flxrs.dankchat.utils.extensions.replaceOrAddHistoryModerationMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single
import kotlin.system.measureTimeMillis

/**
 * Handles loading and merging of recent message history from the RecentMessages API.
 * Used for both initial channel load and reconnect.
 */
@Single
class RecentMessagesHandler(
    private val recentMessagesApiClient: RecentMessagesApiClient,
    private val messageProcessor: MessageProcessor,
) {

    private val loadedChannels = mutableSetOf<UserName>()

    data class Result(
        val mentionItems: List<ChatItem>,
        val userSuggestions: List<Pair<UserName, DisplayName>>,
    )

    /**
     * Loads recent messages for a channel, processes them, and merges into the provided message flow.
     * Returns mention items and user suggestions for the caller to handle.
     */
    suspend fun load(
        channel: UserName,
        isReconnect: Boolean = false,
        messagesFlow: MutableStateFlow<List<ChatItem>>,
        scrollBackLength: Int,
        onMessageRemoved: (ChatItem) -> Unit,
        onLoadingFailure: (ChatLoadingStep, Throwable) -> Unit,
        postSystemMessage: (SystemMessageType, Set<UserName>) -> Unit,
    ): Result = withContext(Dispatchers.IO) {
        if (!isReconnect && channel in loadedChannels) {
            return@withContext Result(emptyList(), emptyList())
        }

        val limit = if (isReconnect) RECENT_MESSAGES_LIMIT_AFTER_RECONNECT else null
        val result = recentMessagesApiClient.getRecentMessages(channel, limit).getOrElse { throwable ->
            if (!isReconnect) {
                handleFailure(throwable, channel, onLoadingFailure, postSystemMessage)
            }
            return@withContext Result(emptyList(), emptyList())
        }

        loadedChannels += channel
        val recentMessages = result.messages.orEmpty()
        val items = mutableListOf<ChatItem>()
        val messageIndex = HashMap<String, Message>(recentMessages.size)
        val userSuggestions = mutableListOf<Pair<UserName, DisplayName>>()

        measureTimeMillis {
            for (recentMessage in recentMessages) {
                val parsedIrc = IrcMessage.parse(recentMessage)
                val isDeleted = parsedIrc.tags["rm-deleted"] == "1"
                if (messageProcessor.isUserBlocked(parsedIrc.tags["user-id"]?.toUserId())) {
                    continue
                }

                when (parsedIrc.command) {
                    "CLEARCHAT" -> {
                        val parsed = runCatching {
                            ModerationMessage.parseClearChat(parsedIrc)
                        }.getOrNull() ?: continue

                        items.replaceOrAddHistoryModerationMessage(parsed)
                    }

                    "CLEARMSG"  -> {
                        val parsed = runCatching {
                            ModerationMessage.parseClearMessage(parsedIrc)
                        }.getOrNull() ?: continue

                        items += ChatItem(parsed, importance = ChatImportance.SYSTEM)
                    }

                    else        -> {
                        val message = runCatching {
                            messageProcessor.processIrcMessage(parsedIrc) { _, id -> messageIndex[id] }
                        }.getOrNull() ?: continue

                        messageIndex[message.id] = message
                        if (message is PrivMessage) {
                            val userForSuggestion = message.name.valueOrDisplayName(message.displayName).toDisplayName()
                            userSuggestions += message.name.lowercase() to userForSuggestion
                        }

                        val importance = when {
                            isDeleted   -> ChatImportance.DELETED
                            isReconnect -> ChatImportance.SYSTEM
                            else        -> ChatImportance.REGULAR
                        }
                        if (message is UserNoticeMessage && message.childMessage != null) {
                            items += ChatItem(message.childMessage, importance = importance)
                        }
                        items += ChatItem(message, importance = importance)
                    }
                }
            }
        }.let { Log.i(TAG, "Parsing message history for #$channel took $it ms") }

        messagesFlow.update { current ->
            val withIncompleteWarning = when {
                !isReconnect && recentMessages.isNotEmpty() && result.errorCode == RecentMessagesDto.ERROR_CHANNEL_NOT_JOINED -> {
                    current + SystemMessageType.MessageHistoryIncomplete.toChatItem()
                }

                else                                                                                                          -> current
            }

            withIncompleteWarning.addAndLimit(items, scrollBackLength, onMessageRemoved, checkForDuplications = true)
        }

        val mentionItems = items.filter { it.message.highlights.hasMention() }.toMentionTabItems()
        Result(mentionItems, userSuggestions)
    }

    private fun handleFailure(
        throwable: Throwable,
        channel: UserName,
        onLoadingFailure: (ChatLoadingStep, Throwable) -> Unit,
        postSystemMessage: (SystemMessageType, Set<UserName>) -> Unit,
    ) {
        val type = when (throwable) {
            !is RecentMessagesApiException -> {
                onLoadingFailure(ChatLoadingStep.RecentMessages(channel), throwable)
                SystemMessageType.MessageHistoryUnavailable(status = null)
            }

            else                           -> when (throwable.error) {
                RecentMessagesError.ChannelNotJoined -> return
                RecentMessagesError.ChannelIgnored   -> SystemMessageType.MessageHistoryIgnored
                else                                 -> {
                    onLoadingFailure(ChatLoadingStep.RecentMessages(channel), throwable)
                    SystemMessageType.MessageHistoryUnavailable(status = throwable.status.value.toString())
                }
            }
        }
        postSystemMessage(type, setOf(channel))
    }

    companion object {
        private val TAG = RecentMessagesHandler::class.java.simpleName
        private const val RECENT_MESSAGES_LIMIT_AFTER_RECONNECT = 100
    }
}
