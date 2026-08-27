package com.flxrs.dankchat.data.notification

import android.content.Context
import com.flxrs.dankchat.di.DispatchersProvider
import com.flxrs.dankchat.push.PushMessage
import com.flxrs.dankchat.push.PushMessageKind
import com.flxrs.dankchat.utils.datastore.createDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.Serializable
import org.koin.core.annotation.Single

@Single
class RemoteNotificationStore(
    context: Context,
    dispatchersProvider: DispatchersProvider,
) {
    private val dataStore =
        createDataStore(
            fileName = "remote_notification_history",
            context = context,
            defaultValue = RemoteNotificationState(),
            serializer = RemoteNotificationState.serializer(),
            scope = CoroutineScope(dispatchersProvider.io + SupervisorJob()),
        )

    suspend fun add(message: PushMessage): List<PushMessage> = dataStore
        .updateData { state ->
            val messages = (state.messages.filterNot { it.messageId == message.messageId } + message).takeLast(MAX_STORED_MESSAGES)
            RemoteNotificationState(messages)
        }.messages
        .forConversation(message)
        .takeLast(MAX_CONVERSATION_MESSAGES)

    suspend fun remove(messageId: String): RemoteNotificationState = dataStore.updateData { state -> RemoteNotificationState(state.messages.filterNot { it.messageId == messageId }) }

    suspend fun clearChannel(channelName: String): List<PushMessage> {
        var removed = emptyList<PushMessage>()
        dataStore.updateData { state ->
            removed = state.messages.filter { it.kind == PushMessageKind.Mention && it.channelName.equals(channelName, ignoreCase = true) }
            RemoteNotificationState(state.messages - removed.toSet())
        }
        return removed
    }

    suspend fun clearWhispers(senderUserName: String? = null): List<PushMessage> {
        var removed = emptyList<PushMessage>()
        dataStore.updateData { state ->
            removed = state.messages.filter {
                it.kind == PushMessageKind.Whisper && (senderUserName == null || it.senderUserName.equals(senderUserName, ignoreCase = true))
            }
            RemoteNotificationState(state.messages - removed.toSet())
        }
        return removed
    }

    private fun List<PushMessage>.forConversation(message: PushMessage) = filter {
        when (message.kind) {
            PushMessageKind.Mention -> it.kind == message.kind && it.channelName.equals(message.channelName, ignoreCase = true)
            PushMessageKind.Whisper -> it.kind == message.kind && it.senderUserName.equals(message.senderUserName, ignoreCase = true)
        }
    }

    private companion object {
        const val MAX_STORED_MESSAGES = 500
        const val MAX_CONVERSATION_MESSAGES = 25
    }
}

@Serializable
data class RemoteNotificationState(
    val messages: List<PushMessage> = emptyList(),
)
