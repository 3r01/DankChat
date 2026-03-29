package com.flxrs.dankchat.ui.main.input

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.twitch.chat.ConnectionState
import com.flxrs.dankchat.preferences.chat.UserLongClickBehavior
import com.flxrs.dankchat.ui.chat.suggestion.Suggestion
import com.flxrs.dankchat.ui.main.InputState
import com.flxrs.dankchat.utils.TextResource
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class ChatInputUiState(
    val text: String = "",
    val canSend: Boolean = false,
    val enabled: Boolean = false,
    val hasLastMessage: Boolean = false,
    val suggestions: ImmutableList<Suggestion> = persistentListOf(),
    val activeChannel: UserName? = null,
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val isLoggedIn: Boolean = false,
    val inputState: InputState = InputState.Disconnected,
    val overlay: InputOverlay = InputOverlay.None,
    val replyMessageId: String? = null,
    val isEmoteMenuOpen: Boolean = false,
    val helperText: HelperText = HelperText(),
    val isWhisperTabActive: Boolean = false,
    val characterCounter: CharacterCounterState = CharacterCounterState.Hidden,
    val userLongClickBehavior: UserLongClickBehavior = UserLongClickBehavior.MentionsUser,
)

@Stable
sealed interface InputOverlay {
    data object None : InputOverlay
    data class Reply(val name: UserName) : InputOverlay
    data class Whisper(val target: UserName) : InputOverlay
    data object Announce : InputOverlay
}

@Stable
sealed interface CharacterCounterState {
    data object Hidden : CharacterCounterState

    @Immutable
    data class Visible(val text: String, val isOverLimit: Boolean) : CharacterCounterState
}

@Immutable
data class HelperText(
    val roomStateParts: ImmutableList<TextResource> = persistentListOf(),
    val streamInfo: String? = null,
) {
    val isEmpty: Boolean get() = roomStateParts.isEmpty() && streamInfo == null
}
