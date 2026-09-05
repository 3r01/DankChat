package com.flxrs.dankchat.data.api.whisperhistory

import com.flxrs.dankchat.data.api.twitchgql.TwitchWebGqlClient
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import org.koin.core.annotation.Single
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

@Single
class WhisperHistoryApiClient(
    private val gqlClient: TwitchWebGqlClient,
    private val json: Json,
) {
    suspend fun getRecentWhispers(
        userId: String,
        webOAuthToken: String,
    ): Result<List<WhisperHistoryEntry>> = try {
        Result.success(
            gqlClient
                .execute(
                    webOAuthToken = webOAuthToken,
                    body =
                        json.encodeToJsonElement(
                            WhisperHistoryRequest(
                                operationName = "DankChatWhisperThreads",
                                query = WHISPER_THREADS_QUERY,
                                variables = WhisperHistoryVariables(THREADS_PER_REQUEST, MESSAGES_PER_THREAD),
                            ),
                        ) as JsonObject,
                ).let { response ->
                    if (response.status == 401) {
                        throw WhisperHistoryException.TokenExpired
                    }
                    if (response.status !in 200..299) {
                        throw WhisperHistoryException.RequestFailed("Twitch returned ${response.status}")
                    }
                    parseWhisperHistoryResponse(
                        response = json.decodeFromString(response.body),
                        expectedUserId = userId,
                        cutoff = Clock.System.now() - HISTORY_WINDOW,
                    )
                },
        )
    } catch (error: CancellationException) {
        throw error
    } catch (error: Throwable) {
        Result.failure(error)
    }

    private companion object {
        const val THREADS_PER_REQUEST = 100
        const val MESSAGES_PER_THREAD = 100
        val HISTORY_WINDOW = 24.hours
    }
}

sealed class WhisperHistoryException(
    message: String,
) : Exception(message) {
    data object TokenExpired : WhisperHistoryException("Twitch web token expired")

    data object WrongAccount : WhisperHistoryException("Twitch web token belongs to another account")

    class RequestFailed(
        message: String,
    ) : WhisperHistoryException(message)
}

internal fun parseWhisperHistoryResponse(
    response: WhisperHistoryResponse,
    expectedUserId: String,
    cutoff: Instant,
): List<WhisperHistoryEntry> {
    response.errors
        .firstOrNull()
        ?.message
        ?.let { throw WhisperHistoryException.RequestFailed(it) }
    val currentUser = response.requireCurrentUser(expectedUserId)

    val seenIds = mutableSetOf<String>()
    return currentUser.whisperThreads.edges
        .asSequence()
        .flatMap { edge ->
            val participants = edge.node.participants.associateBy(WhisperParticipantDto::id)
            edge.node.messages.edges.asSequence().mapNotNull { messageEdge ->
                val message = messageEdge.node
                val sentAt = runCatching { Instant.parse(message.sentAt) }.getOrNull() ?: return@mapNotNull null
                if (message.id.isBlank() || sentAt < cutoff || !seenIds.add(message.id)) return@mapNotNull null
                val sender = participants[message.from.id] ?: return@mapNotNull null
                val recipient = edge.node.participants.firstOrNull { it.id != sender.id } ?: return@mapNotNull null
                WhisperHistoryEntry(
                    id = message.id,
                    nonce = message.nonce,
                    timestamp = sentAt.toEpochMilliseconds(),
                    sender = sender.toParticipant(),
                    recipient = recipient.toParticipant(),
                    text = normalizeWhisperText(message.content.content),
                    emotes =
                        message.content.emotes.mapNotNull { emote ->
                            val id = emote.emoteId.ifBlank { emote.id }
                            id.takeIf(String::isNotBlank)?.let { WhisperHistoryEmote(it, emote.from, emote.to) }
                        },
                )
            }
        }.toList()
        .sortedBy(WhisperHistoryEntry::timestamp)
        .takeLast(MAX_HISTORY_MESSAGES)
}

private fun WhisperHistoryResponse.requireCurrentUser(expectedUserId: String): WhisperCurrentUser {
    val currentUser = data?.currentUser ?: throw WhisperHistoryException.RequestFailed("Twitch returned no account")
    if (currentUser.id != expectedUserId) throw WhisperHistoryException.WrongAccount
    return currentUser
}

private fun WhisperParticipantDto.toParticipant() = WhisperHistoryParticipant(
    id = id,
    login = login,
    displayName = displayName.ifBlank { login },
    color = chatColor,
)

private fun normalizeWhisperText(text: String): String = if (text.startsWith(ACTION_PREFIX) && text.endsWith(ACTION_SUFFIX)) {
    text.substring(ACTION_PREFIX.length, text.length - ACTION_SUFFIX.length)
} else {
    text
}

data class WhisperHistoryEntry(
    val id: String,
    val nonce: String,
    val timestamp: Long,
    val sender: WhisperHistoryParticipant,
    val recipient: WhisperHistoryParticipant,
    val text: String,
    val emotes: List<WhisperHistoryEmote>,
)

data class WhisperHistoryParticipant(
    val id: String,
    val login: String,
    val displayName: String,
    val color: String?,
)

data class WhisperHistoryEmote(
    val id: String,
    val from: Int,
    val to: Int,
)

@Serializable
internal data class WhisperHistoryRequest(
    val operationName: String,
    val query: String,
    val variables: WhisperHistoryVariables,
)

@Serializable
internal data class WhisperHistoryVariables(
    val threadsFirst: Int,
    val messagesFirst: Int,
)

@Serializable
internal data class WhisperHistoryResponse(
    val data: WhisperHistoryData? = null,
    val errors: List<WhisperHistoryError> = emptyList(),
)

@Serializable
internal data class WhisperHistoryData(
    val currentUser: WhisperCurrentUser? = null,
)

@Serializable
internal data class WhisperCurrentUser(
    val id: String,
    val whisperThreads: WhisperThreadConnection,
)

@Serializable
internal data class WhisperThreadConnection(
    val edges: List<WhisperThreadEdge> = emptyList(),
)

@Serializable
internal data class WhisperThreadEdge(
    val node: WhisperThread,
)

@Serializable
internal data class WhisperThread(
    val participants: List<WhisperParticipantDto> = emptyList(),
    val messages: WhisperMessageConnection,
)

@Serializable
internal data class WhisperParticipantDto(
    val id: String,
    val login: String = "",
    val displayName: String = "",
    val chatColor: String? = null,
)

@Serializable
internal data class WhisperMessageConnection(
    val edges: List<WhisperMessageEdge> = emptyList(),
)

@Serializable
internal data class WhisperMessageEdge(
    val node: WhisperMessageDto,
)

@Serializable
internal data class WhisperMessageDto(
    val id: String,
    val nonce: String = "",
    val sentAt: String,
    val from: WhisperMessageSender,
    val content: WhisperContent,
)

@Serializable
internal data class WhisperMessageSender(
    val id: String,
)

@Serializable
internal data class WhisperContent(
    val content: String,
    val emotes: List<WhisperEmoteDto> = emptyList(),
)

@Serializable
internal data class WhisperEmoteDto(
    val id: String = "",
    @SerialName("emoteID") val emoteId: String = "",
    val from: Int,
    val to: Int,
)

@Serializable
internal data class WhisperHistoryError(
    val message: String = "Twitch rejected the request",
)

private const val MAX_HISTORY_MESSAGES = 1000
private const val ACTION_PREFIX = "\u0001ACTION "
private const val ACTION_SUFFIX = "\u0001"

private val WHISPER_THREADS_QUERY =
    """
    query DankChatWhisperThreads(${'$'}threadsFirst: Int!, ${'$'}messagesFirst: Int!) {
      currentUser {
        id
        whisperThreads(first: ${'$'}threadsFirst) {
          edges {
            node {
              participants { id login displayName chatColor }
              messages(first: ${'$'}messagesFirst) {
                edges {
                  node {
                    id nonce sentAt
                    from { id }
                    content {
                      content
                      emotes { id emoteID from to }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
    """.trimIndent()
