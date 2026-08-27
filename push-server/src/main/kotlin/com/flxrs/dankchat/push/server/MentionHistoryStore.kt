package com.flxrs.dankchat.push.server

import com.flxrs.dankchat.push.MentionHistoryMessage
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

class MentionHistoryStore(
    private val path: Path,
    private val maxMessages: Int = MAX_MESSAGES,
    private val json: Json = Json { prettyPrint = true },
) {
    private val mutex = Mutex()
    private var state = load()

    suspend fun add(
        twitchUserId: String,
        message: MentionHistoryMessage,
    ) = mutex.withLock {
        val existing = state.takeIf { it.twitchUserId == twitchUserId }?.messages.orEmpty()
        val messages =
            (existing.filterNot { it.messageId == message.messageId } + message)
                .sortedBy { it.timestamp }
                .takeLast(maxMessages)
        persist(MentionHistoryState(twitchUserId, messages))
    }

    suspend fun getAll(twitchUserId: String?): List<MentionHistoryMessage> =
        mutex.withLock {
            state.messages.takeIf { twitchUserId != null && twitchUserId == state.twitchUserId }.orEmpty()
        }

    private fun load(): MentionHistoryState {
        if (!Files.exists(path)) return MentionHistoryState()
        return json.decodeFromString(Files.readString(path))
    }

    private fun persist(value: MentionHistoryState) {
        Files.createDirectories(path.parent)
        val temporary = path.resolveSibling("${path.fileName}.tmp")
        Files.writeString(temporary, json.encodeToString(MentionHistoryState.serializer(), value))
        try {
            Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING)
        }
        state = value
    }

    private companion object {
        const val MAX_MESSAGES = 1000
    }
}

@Serializable
private data class MentionHistoryState(
    val twitchUserId: String? = null,
    val messages: List<MentionHistoryMessage> = emptyList(),
)
