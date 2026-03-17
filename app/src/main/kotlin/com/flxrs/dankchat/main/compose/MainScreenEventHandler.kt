package com.flxrs.dankchat.main.compose

import android.content.ClipData
import android.content.ClipboardManager
import android.content.res.Resources
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.getSystemService
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flxrs.dankchat.R
import com.flxrs.dankchat.auth.AuthEvent
import com.flxrs.dankchat.auth.AuthStateCoordinator
import com.flxrs.dankchat.data.state.GlobalLoadingState
import com.flxrs.dankchat.main.MainActivity
import com.flxrs.dankchat.main.MainEvent
import com.flxrs.dankchat.preferences.DankChatPreferenceStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun MainScreenEventHandler(
    resources: Resources,
    snackbarHostState: SnackbarHostState,
    mainEventBus: MainEventBus,
    dialogViewModel: DialogStateViewModel,
    chatInputViewModel: ChatInputViewModel,
    channelTabViewModel: ChannelTabViewModel,
    mainScreenViewModel: MainScreenViewModel,
    preferenceStore: DankChatPreferenceStore,
) {
    val context = LocalContext.current
    val authStateCoordinator: AuthStateCoordinator = koinInject()

    // MainEventBus event collection
    LaunchedEffect(Unit) {
        mainEventBus.events.collect { event ->
            when (event) {
                is MainEvent.LogOutRequested -> dialogViewModel.showLogout()
                is MainEvent.UploadLoading   -> dialogViewModel.setUploading(true)
                is MainEvent.UploadSuccess   -> {
                    dialogViewModel.setUploading(false)
                    context.getSystemService<ClipboardManager>()
                        ?.setPrimaryClip(ClipData.newPlainText("dankchat_media_url", event.url))
                    chatInputViewModel.postSystemMessage(resources.getString(R.string.system_message_upload_complete, event.url))
                    val result = snackbarHostState.showSnackbar(
                        message = resources.getString(R.string.snackbar_image_uploaded, event.url),
                        actionLabel = resources.getString(R.string.snackbar_paste),
                        duration = SnackbarDuration.Long
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        chatInputViewModel.insertText(event.url)
                    }
                }

                is MainEvent.UploadFailed    -> {
                    dialogViewModel.setUploading(false)
                    val message = event.errorMessage?.let { resources.getString(R.string.snackbar_upload_failed_cause, it) }
                        ?: resources.getString(R.string.snackbar_upload_failed)
                    snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Long)
                }

                is MainEvent.OpenChannel     -> {
                    channelTabViewModel.selectTab(
                        preferenceStore.channels.indexOf(event.channel)
                    )
                    (context as? MainActivity)?.clearNotificationsOfChannel(event.channel)
                }

                else                         -> Unit
            }
        }
    }

    // Collect auth events from AuthStateCoordinator
    LaunchedEffect(Unit) {
        authStateCoordinator.events.collect { event ->
            when (event) {
                is AuthEvent.LoggedIn       -> {
                    launch {
                        delay(2000)
                        snackbarHostState.currentSnackbarData?.dismiss()
                    }
                    snackbarHostState.showSnackbar(
                        message = resources.getString(R.string.snackbar_login, event.userName),
                        duration = SnackbarDuration.Short,
                    )
                }

                is AuthEvent.ScopesOutdated -> {
                    dialogViewModel.showLoginOutdated(event.userName)
                }

                AuthEvent.TokenInvalid      -> {
                    dialogViewModel.showLoginExpired()
                }

                AuthEvent.ValidationFailed  -> {
                    snackbarHostState.showSnackbar(
                        message = resources.getString(R.string.oauth_verify_failed),
                        duration = SnackbarDuration.Short,
                    )
                }
            }
        }
    }

    // Handle data loading errors
    val loadingState by mainScreenViewModel.globalLoadingState.collectAsStateWithLifecycle()
    LaunchedEffect(loadingState) {
        val state = loadingState as? GlobalLoadingState.Failed
        if (state != null) {
            launch {
                snackbarHostState.showSnackbar(
                    message = state.message,
                    actionLabel = resources.getString(R.string.snackbar_retry),
                    duration = SnackbarDuration.Long
                )
            }
        }
    }
}
