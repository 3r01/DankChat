package com.flxrs.dankchat.ui.chat.message

import androidx.compose.runtime.Immutable
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.twitch.message.TwitchGif
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
sealed interface MessageOptionsState {
    data object Loading : MessageOptionsState

    data object NotFound : MessageOptionsState

    sealed interface Found : MessageOptionsState {
        val name: UserName
        val originalMessage: String
        val canModerate: Boolean
        val urls: ImmutableList<String>

        data class RegularMessage(
            override val name: UserName,
            override val originalMessage: String,
            override val canModerate: Boolean,
            override val urls: ImmutableList<String>,
            val messageId: String,
            val rootThreadId: String,
            val rootThreadName: UserName?,
            val rootThreadMessage: String?,
            val replyName: UserName,
            val hasReplyThread: Boolean,
            val replyAction: MessageReplyAction?,
            val gifs: ImmutableList<TwitchGif> = persistentListOf(),
        ) : Found

        data class AutomodMessage(
            override val name: UserName,
            override val originalMessage: String,
            override val canModerate: Boolean,
            override val urls: ImmutableList<String>,
        ) : Found
    }
}
