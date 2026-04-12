package com.flxrs.dankchat.ui.main.dialog

import androidx.compose.runtime.Immutable
import com.flxrs.dankchat.data.repo.crash.CrashEntry
import com.flxrs.dankchat.ui.chat.message.MessageOptionsParams
import com.flxrs.dankchat.ui.chat.user.UserPopupStateParams

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
    val crashEntry: CrashEntry? = null,
)
