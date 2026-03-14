package com.flxrs.dankchat.chat.message.compose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.repo.RepliesRepository
import com.flxrs.dankchat.data.repo.channel.ChannelRepository
import com.flxrs.dankchat.data.repo.chat.ChatRepository
import com.flxrs.dankchat.data.repo.chat.UserStateRepository
import com.flxrs.dankchat.data.repo.command.CommandRepository
import com.flxrs.dankchat.data.repo.command.CommandResult
import com.flxrs.dankchat.data.twitch.chat.ConnectionState
import com.flxrs.dankchat.data.twitch.message.PrivMessage
import com.flxrs.dankchat.data.twitch.message.WhisperMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam
import kotlin.time.Duration.Companion.seconds

@KoinViewModel
class MessageOptionsComposeViewModel(
    @InjectedParam private val messageId: String,
    @InjectedParam private val channel: UserName?,
    @InjectedParam private val canModerateParam: Boolean,
    @InjectedParam private val canReplyParam: Boolean,
    private val chatRepository: ChatRepository,
    private val channelRepository: ChannelRepository,
    private val userStateRepository: UserStateRepository,
    private val commandRepository: CommandRepository,
    private val repliesRepository: RepliesRepository,
) : ViewModel() {

    private val messageFlow = flowOf(chatRepository.findMessage(messageId, channel))
    private val connectionStateFlow = chatRepository.getConnectionState(channel ?: WhisperMessage.WHISPER_CHANNEL)

    val state: StateFlow<MessageOptionsState> = combine(
        userStateRepository.userState,
        connectionStateFlow,
        messageFlow
    ) { userState, connectionState, message ->
        when (message) {
            null -> MessageOptionsState.NotFound
            else -> {
                val asPrivMessage = message as? PrivMessage
                val asWhisperMessage = message as? WhisperMessage
                val rootId = asPrivMessage?.thread?.rootId
                val name = asPrivMessage?.name ?: asWhisperMessage?.name ?: return@combine MessageOptionsState.NotFound
                val replyName = asPrivMessage?.thread?.name ?: name
                val originalMessage = asPrivMessage?.originalMessage ?: asWhisperMessage?.originalMessage
                MessageOptionsState.Found(
                    messageId = message.id,
                    rootThreadId = rootId ?: message.id,
                    replyName = replyName,
                    name = name,
                    originalMessage = originalMessage.orEmpty(),
                    canModerate = canModerateParam && channel != null && channel in userState.moderationChannels,
                    hasReplyThread = canReplyParam && rootId != null && repliesRepository.hasMessageThread(rootId),
                    canReply = connectionState == ConnectionState.CONNECTED && canReplyParam
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MessageOptionsState.Loading)

    fun timeoutUser(index: Int) = viewModelScope.launch {
        val duration = TIMEOUT_MAP[index] ?: return@launch
        val name = (state.value as? MessageOptionsState.Found)?.name ?: return@launch
        sendCommand(".timeout $name $duration")
    }

    fun banUser() = viewModelScope.launch {
        val name = (state.value as? MessageOptionsState.Found)?.name ?: return@launch
        sendCommand(".ban $name")
    }

    fun unbanUser() = viewModelScope.launch {
        val name = (state.value as? MessageOptionsState.Found)?.name ?: return@launch
        sendCommand(".unban $name")
    }

    fun deleteMessage() = viewModelScope.launch {
        sendCommand(".delete $messageId")
    }

    private suspend fun sendCommand(message: String) {
        val activeChannel = channel ?: return
        val roomState = channelRepository.getRoomState(activeChannel) ?: return
        val userState = userStateRepository.userState.value
        val result = runCatching {
            commandRepository.checkForCommands(message, activeChannel, roomState, userState)
        }.getOrNull() ?: return

        when (result) {
            is CommandResult.IrcCommand            -> chatRepository.sendMessage(message)
            is CommandResult.AcceptedTwitchCommand -> result.response?.let { chatRepository.makeAndPostCustomSystemMessage(it, activeChannel) }
            else                                   -> Unit
        }
    }

    companion object {
        private val TIMEOUT_MAP = mapOf(
            0 to "1",
            1 to "30",
            2 to "60",
            3 to "300",
            4 to "600",
            5 to "1800",
            6 to "3600",
            7 to "86400",
            8 to "604800",
        )
    }
}

sealed interface MessageOptionsState {
    data object Loading : MessageOptionsState
    data object NotFound : MessageOptionsState
    data class Found(
        val messageId: String,
        val rootThreadId: String,
        val replyName: UserName,
        val name: UserName,
        val originalMessage: String,
        val canModerate: Boolean,
        val hasReplyThread: Boolean,
        val canReply: Boolean,
    ) : MessageOptionsState
}
