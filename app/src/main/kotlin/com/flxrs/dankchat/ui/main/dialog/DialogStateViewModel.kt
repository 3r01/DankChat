package com.flxrs.dankchat.ui.main.dialog

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.twitch.emote.ChatMessageEmote
import com.flxrs.dankchat.preferences.DankChatPreferenceStore
import com.flxrs.dankchat.ui.chat.message.MessageOptionsParams
import com.flxrs.dankchat.ui.chat.user.UserPopupStateParams
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class DialogStateViewModel(
    private val preferenceStore: DankChatPreferenceStore,
) : ViewModel() {

    private val _state = MutableStateFlow(DialogState())
    val state: StateFlow<DialogState> = _state.asStateFlow()

    // Channel dialogs
    fun showAddChannel() {
        update { copy(showAddChannel = true) }
    }

    fun dismissAddChannel() {
        update { copy(showAddChannel = false) }
    }

    fun showManageChannels() {
        update { copy(showManageChannels = true) }
    }

    fun dismissManageChannels() {
        update { copy(showManageChannels = false) }
    }

    fun showRemoveChannel() {
        update { copy(showRemoveChannel = true) }
    }

    fun dismissRemoveChannel() {
        update { copy(showRemoveChannel = false) }
    }

    fun showBlockChannel() {
        update { copy(showBlockChannel = true) }
    }

    fun dismissBlockChannel() {
        update { copy(showBlockChannel = false) }
    }

    fun showClearChat() {
        update { copy(showClearChat = true) }
    }

    fun dismissClearChat() {
        update { copy(showClearChat = false) }
    }

    fun showRoomState() {
        update { copy(showRoomState = true) }
    }

    fun dismissRoomState() {
        update { copy(showRoomState = false) }
    }

    // Auth dialogs
    fun showLogout() {
        update { copy(showLogout = true) }
    }

    fun dismissLogout() {
        update { copy(showLogout = false) }
    }

    fun showLoginOutdated(username: UserName) {
        update { copy(loginOutdated = username) }
    }

    fun dismissLoginOutdated() {
        update { copy(loginOutdated = null) }
    }

    fun showLoginExpired() {
        update { copy(showLoginExpired = true) }
    }

    fun dismissLoginExpired() {
        update { copy(showLoginExpired = false) }
    }

    // Whisper dialog
    fun showNewWhisper() {
        update { copy(showNewWhisper = true) }
    }

    fun dismissNewWhisper() {
        update { copy(showNewWhisper = false) }
    }

    // Upload
    fun setPendingUploadAction(action: (() -> Unit)?) {
        update { copy(pendingUploadAction = action) }
    }

    fun setUploading(uploading: Boolean) {
        update { copy(isUploading = uploading) }
    }

    // Message interactions
    fun showUserPopup(params: UserPopupStateParams) {
        if (!preferenceStore.isLoggedIn) return
        update { copy(userPopupParams = params) }
    }

    fun dismissUserPopup() {
        update { copy(userPopupParams = null) }
    }

    fun showMessageOptions(params: MessageOptionsParams) {
        update { copy(messageOptionsParams = params) }
    }

    fun dismissMessageOptions() {
        update { copy(messageOptionsParams = null) }
    }

    fun showEmoteInfo(emotes: List<ChatMessageEmote>) {
        update { copy(emoteInfoEmotes = emotes.toImmutableList()) }
    }

    fun dismissEmoteInfo() {
        update { copy(emoteInfoEmotes = null) }
    }

    private inline fun update(crossinline transform: DialogState.() -> DialogState) {
        _state.value = _state.value.transform()
    }
}

@Immutable
data class DialogState(
    val showAddChannel: Boolean = false,
    val showManageChannels: Boolean = false,
    val showRemoveChannel: Boolean = false,
    val showBlockChannel: Boolean = false,
    val showClearChat: Boolean = false,
    val showRoomState: Boolean = false,
    val showLogout: Boolean = false,
    val loginOutdated: UserName? = null,
    val showLoginExpired: Boolean = false,
    val showNewWhisper: Boolean = false,
    val pendingUploadAction: (() -> Unit)? = null,
    val isUploading: Boolean = false,
    val userPopupParams: UserPopupStateParams? = null,
    val messageOptionsParams: MessageOptionsParams? = null,
    val emoteInfoEmotes: ImmutableList<ChatMessageEmote>? = null,
)
