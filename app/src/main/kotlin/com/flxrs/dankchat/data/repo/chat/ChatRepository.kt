package com.flxrs.dankchat.data.repo.chat

import android.graphics.Color
import com.flxrs.dankchat.auth.AuthDataStore
import com.flxrs.dankchat.chat.ChatItem
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.repo.channel.ChannelRepository
import com.flxrs.dankchat.data.repo.emote.EmoteRepository
import com.flxrs.dankchat.data.toDisplayName
import com.flxrs.dankchat.data.toUserName
import com.flxrs.dankchat.data.twitch.message.Message
import com.flxrs.dankchat.data.twitch.message.SystemMessageType
import com.flxrs.dankchat.data.twitch.message.WhisperMessage
import com.flxrs.dankchat.data.twitch.message.toChatItem
import com.flxrs.dankchat.preferences.chat.ChatSettingsDataStore
import com.flxrs.dankchat.utils.extensions.INVISIBLE_CHAR
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.Single

@Single
class ChatRepository(
    private val chatChannelProvider: ChatChannelProvider,
    private val chatMessageRepository: ChatMessageRepository,
    private val chatConnector: ChatConnector,
    private val chatNotificationRepository: ChatNotificationRepository,
    private val chatEventProcessor: ChatEventProcessor,
    private val userStateRepository: UserStateRepository,
    private val usersRepository: UsersRepository,
    private val emoteRepository: EmoteRepository,
    private val channelRepository: ChannelRepository,
    private val messageProcessor: MessageProcessor,
    private val authDataStore: AuthDataStore,
    private val chatSettingsDataStore: ChatSettingsDataStore,
) {

    val activeChannel get() = chatChannelProvider.activeChannel
    val channels get() = chatChannelProvider.channels

    fun setActiveChannel(channel: UserName?) = chatChannelProvider.setActiveChannel(channel)

    fun joinChannel(channel: UserName, listenToPubSub: Boolean = true): List<UserName> {
        val currentChannels = channels.value.orEmpty()
        if (channel in currentChannels) {
            return currentChannels
        }

        val updatedChannels = currentChannels + channel
        chatChannelProvider.setChannels(updatedChannels)

        createFlowsIfNecessary(channel)
        chatMessageRepository.clearMessages(channel)

        chatConnector.joinIrcChannel(channel)
        if (listenToPubSub) {
            chatConnector.addPubSubChannel(channel)
        }

        return updatedChannels
    }

    fun updateChannels(updatedChannels: List<UserName>): List<UserName> {
        val currentChannels = channels.value.orEmpty()
        val removedChannels = currentChannels - updatedChannels.toSet()

        removedChannels.forEach { partChannel(it) }

        chatChannelProvider.setChannels(updatedChannels)
        return removedChannels
    }

    fun createFlowsIfNecessary(channel: UserName) {
        chatMessageRepository.createMessageFlows(channel)
        chatConnector.createConnectionState(channel)
        chatNotificationRepository.createMentionFlows(channel)
        usersRepository.initChannel(channel)
        channelRepository.initRoomState(channel)
    }

    fun sendMessage(input: String, replyId: String? = null) {
        val channel = chatChannelProvider.activeChannel.value ?: return
        val preparedMessage = prepareMessage(channel, input, replyId) ?: return
        chatConnector.sendRaw(preparedMessage)
    }

    fun fakeWhisperIfNecessary(input: String) {
        if (chatConnector.connectedAndHasWhisperTopic) {
            return
        }

        val split = input.split(" ")
        if (split.size > 2 && (split[0] == "/w" || split[0] == ".w") && split[1].isNotBlank()) {
            val message = input.substring(4 + split[1].length)
            val emotes = emoteRepository.parse3rdPartyEmotes(message, WhisperMessage.WHISPER_CHANNEL, withTwitch = true)
            val userState = userStateRepository.userState.value
            val name = authDataStore.userName ?: return
            val displayName = userState.displayName ?: return
            val fakeMessage = WhisperMessage(
                userId = userState.userId,
                name = name,
                displayName = displayName,
                color = userState.color?.let(Color::parseColor) ?: Message.DEFAULT_COLOR,
                recipientId = null,
                recipientColor = Message.DEFAULT_COLOR,
                recipientName = split[1].toUserName(),
                recipientDisplayName = split[1].toDisplayName(),
                message = message,
                rawEmotes = "",
                rawBadges = "",
                emotes = emotes,
            )
            val fakeItem = ChatItem(fakeMessage, isMentionTab = true)
            chatNotificationRepository.addWhisper(fakeItem)
        }
    }

    fun getLastMessage(): String? = chatEventProcessor.getLastMessageForDisplay(chatChannelProvider.activeChannel.value)

    fun appendLastMessage(channel: UserName, message: String) = chatEventProcessor.setLastMessage(channel, message)

    suspend fun loadRecentMessagesIfEnabled(channel: UserName) {
        when {
            chatSettingsDataStore.settings.first().loadMessageHistory -> chatEventProcessor.loadRecentMessages(channel)
            else                                                      -> {
                chatMessageRepository.getMessagesFlow(channel)?.update { current ->
                    current + SystemMessageType.NoHistoryLoaded.toChatItem()
                }
            }
        }
    }

    fun makeAndPostSystemMessage(type: SystemMessageType, channel: UserName) {
        chatMessageRepository.addSystemMessage(channel, type)
    }

    fun makeAndPostCustomSystemMessage(msg: String, channel: UserName) {
        chatMessageRepository.addSystemMessage(channel, SystemMessageType.Custom(msg))
    }

    private fun partChannel(channel: UserName) {
        chatMessageRepository.removeMessageFlows(channel)
        chatConnector.removeConnectionState(channel)
        chatConnector.partChannel(channel)
        chatNotificationRepository.removeMentionFlows(channel)
        chatEventProcessor.removeLastMessage(channel)
        usersRepository.removeChannel(channel)
        userStateRepository.removeChannel(channel)
        channelRepository.removeRoomState(channel)
        emoteRepository.removeChannel(channel)
        messageProcessor.cleanupMessageThreadsInChannel(channel)
    }

    private fun prepareMessage(channel: UserName, message: String, replyId: String?): String? {
        if (message.isBlank()) {
            return null
        }

        val trimmedMessage = message.trimEnd()
        val replyIdOrBlank = replyId?.let { "@reply-parent-msg-id=$it " }.orEmpty()
        val currentLastMessage = chatEventProcessor.getLastMessage(channel).orEmpty()

        val messageWithSuffix = if (currentLastMessage == trimmedMessage) {
            val startIndex = if (trimmedMessage.startsWith('/') || trimmedMessage.startsWith('.')) {
                trimmedMessage.indexOf(' ').let { if (it == -1) 0 else it + 1 }
            } else {
                0
            }
            val spaceIndex = trimmedMessage.indexOf(' ', startIndex)

            when {
                spaceIndex != -1 -> trimmedMessage.replaceRange(spaceIndex, spaceIndex + 1, "  ")
                else             -> "$trimmedMessage $INVISIBLE_CHAR"
            }
        } else {
            trimmedMessage
        }

        chatEventProcessor.setLastMessage(channel, messageWithSuffix)
        return "${replyIdOrBlank}PRIVMSG #$channel :$messageWithSuffix"
    }
}
