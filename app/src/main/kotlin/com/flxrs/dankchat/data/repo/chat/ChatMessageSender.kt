package com.flxrs.dankchat.data.repo.chat

import android.util.Log
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.api.helix.HelixApiClient
import com.flxrs.dankchat.data.api.helix.HelixApiException
import com.flxrs.dankchat.data.api.helix.HelixError
import com.flxrs.dankchat.data.api.helix.dto.SendChatMessageRequestDto
import com.flxrs.dankchat.data.auth.AuthDataStore
import com.flxrs.dankchat.data.repo.channel.ChannelRepository
import com.flxrs.dankchat.data.twitch.message.SystemMessageType
import com.flxrs.dankchat.preferences.developer.ChatSendProtocol
import com.flxrs.dankchat.preferences.developer.DeveloperSettingsDataStore
import com.flxrs.dankchat.utils.extensions.INVISIBLE_CHAR
import org.koin.core.annotation.Single

@Single
class ChatMessageSender(
    private val chatConnector: ChatConnector,
    private val helixApiClient: HelixApiClient,
    private val channelRepository: ChannelRepository,
    private val authDataStore: AuthDataStore,
    private val chatMessageRepository: ChatMessageRepository,
    private val chatEventProcessor: ChatEventProcessor,
    private val developerSettingsDataStore: DeveloperSettingsDataStore,
) {

    suspend fun send(channel: UserName, message: String, replyId: String? = null, forceIrc: Boolean = false) {
        if (message.isBlank()) {
            return
        }

        val protocol = developerSettingsDataStore.current().chatSendProtocol
        when {
            forceIrc || protocol == ChatSendProtocol.IRC -> sendViaIrc(channel, message, replyId)
            else                                         -> sendViaHelix(channel, message, replyId)
        }
    }

    private suspend fun sendViaIrc(channel: UserName, message: String, replyId: String?) {
        val trimmedMessage = message.trimEnd()
        val replyIdOrBlank = replyId?.let { "@reply-parent-msg-id=$it " }.orEmpty()
        val currentLastMessage = chatEventProcessor.getLastMessage(channel).orEmpty()

        val messageWithSuffix = when {
            currentLastMessage == trimmedMessage -> applyAntiDuplicate(trimmedMessage)
            else                                 -> trimmedMessage
        }

        chatEventProcessor.setLastMessage(channel, messageWithSuffix)
        chatConnector.sendRaw("${replyIdOrBlank}PRIVMSG #$channel :$messageWithSuffix")
        chatMessageRepository.incrementSentMessageCount(ChatSendProtocol.IRC)
    }

    private suspend fun sendViaHelix(channel: UserName, message: String, replyId: String?) {
        val trimmedMessage = message.trimEnd()
        val senderId = authDataStore.userIdString ?: run {
            postError(channel, "Not logged in.")
            return
        }
        val broadcasterId = channelRepository.getChannel(channel)?.id ?: run {
            postError(channel, "Could not resolve channel ID for $channel.")
            return
        }

        val request = SendChatMessageRequestDto(
            broadcasterId = broadcasterId,
            senderId = senderId,
            message = trimmedMessage,
            replyParentMessageId = replyId,
        )

        helixApiClient.postChatMessage(request).fold(
            onSuccess = { response ->
                when {
                    response.isSent -> {
                        chatEventProcessor.setLastMessage(channel, trimmedMessage)
                        chatMessageRepository.incrementSentMessageCount(ChatSendProtocol.Helix)
                    }

                    else            -> {
                        val reason = response.dropReason
                        val msg = when (reason) {
                            null -> "Message was not sent."
                            else -> "Message dropped: ${reason.message} (${reason.code})"
                        }
                        postError(channel, msg)
                    }
                }
            },
            onFailure = { throwable ->
                Log.e(TAG, "Helix send failed", throwable)
                postError(channel, throwable.toSendErrorMessage())
            },
        )
    }

    private fun postError(channel: UserName, message: String) {
        chatMessageRepository.addSystemMessage(channel, SystemMessageType.Custom(message))
        chatMessageRepository.incrementSendFailureCount()
    }

    private fun applyAntiDuplicate(message: String): String {
        val startIndex = when {
            message.startsWith('/') || message.startsWith('.') -> message.indexOf(' ').let { if (it == -1) 0 else it + 1 }
            else                                               -> 0
        }
        val spaceIndex = message.indexOf(' ', startIndex)

        return when {
            spaceIndex != -1 -> message.replaceRange(spaceIndex, spaceIndex + 1, "  ")
            else             -> "$message $INVISIBLE_CHAR"
        }
    }

    private fun Throwable.toSendErrorMessage(): String = when (this) {
        is HelixApiException -> when (error) {
            HelixError.NotLoggedIn            -> "Not logged in."
            HelixError.MissingScopes          -> "Missing user:write:chat scope. Please re-login."
            HelixError.UserNotAuthorized       -> "Not authorized to send messages in this channel."
            HelixError.MessageTooLarge         -> "Message is too large."
            HelixError.ChatMessageRateLimited  -> "Rate limited. Try again in a moment."
            HelixError.Forwarded               -> message ?: "Unknown error."
            else                               -> message ?: "Unknown error."
        }

        else                -> message ?: "Unknown error."
    }

    companion object {
        private val TAG = ChatMessageSender::class.java.simpleName
    }
}
