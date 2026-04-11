package com.flxrs.dankchat.ui.main

import com.flxrs.dankchat.data.UserName
import java.io.File

sealed interface MainEvent {
    data class Error(
        val throwable: Throwable,
    ) : MainEvent

    data object LogOutRequested : MainEvent

    data object UploadLoading : MainEvent

    data class UploadSuccess(
        val url: String,
    ) : MainEvent

    data class UploadFailed(
        val errorMessage: String?,
        val mediaFile: File,
        val imageCapture: Boolean,
    ) : MainEvent

    data class OpenChannel(
        val channel: UserName,
    ) : MainEvent

    data class MessageCopied(
        val text: String,
    ) : MainEvent

    data object MessageIdCopied : MainEvent
}
