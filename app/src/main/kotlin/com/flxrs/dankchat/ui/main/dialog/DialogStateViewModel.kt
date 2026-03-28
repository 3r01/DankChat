package com.flxrs.dankchat.ui.main.dialog

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
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

    fun showModActions() {
        update { copy(showModActions = true) }
    }

    fun dismissModActions() {
        update { copy(showModActions = false) }
    }

    // Auth dialogs
    fun showLogout() {
        update { copy(showLogout = true) }
    }

    fun dismissLogout() {
        update { copy(showLogout = false) }
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
    val showModActions: Boolean = false,
    val showLogout: Boolean = false,
    val showNewWhisper: Boolean = false,
    val pendingUploadAction: (() -> Unit)? = null,
    val isUploading: Boolean = false,
    val userPopupParams: UserPopupStateParams? = null,
    val messageOptionsParams: MessageOptionsParams? = null,
    val emoteInfoEmotes: ImmutableList<ChatMessageEmote>? = null,
)
