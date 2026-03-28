package com.flxrs.dankchat.ui.main

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.core.content.getSystemService
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flxrs.dankchat.R
import com.flxrs.dankchat.data.auth.AuthEvent
import com.flxrs.dankchat.data.auth.AuthStateCoordinator
import com.flxrs.dankchat.data.auth.StartupValidation
import com.flxrs.dankchat.data.auth.StartupValidationHolder
import com.flxrs.dankchat.data.repo.chat.toDisplayStrings
import com.flxrs.dankchat.data.repo.data.toDisplayStrings
import com.flxrs.dankchat.data.state.GlobalLoadingState
import com.flxrs.dankchat.preferences.DankChatPreferenceStore
import com.flxrs.dankchat.ui.main.channel.ChannelTabViewModel
import com.flxrs.dankchat.ui.main.dialog.DialogStateViewModel
import com.flxrs.dankchat.ui.main.input.ChatInputViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun MainScreenEventHandler(
    snackbarHostState: SnackbarHostState,
    mainEventBus: MainEventBus,
    dialogViewModel: DialogStateViewModel,
    chatInputViewModel: ChatInputViewModel,
    channelTabViewModel: ChannelTabViewModel,
    mainScreenViewModel: MainScreenViewModel,
    preferenceStore: DankChatPreferenceStore,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
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

    // Collect auth events from AuthStateCoordinator (snackbar-only events like LoggedIn, ValidationFailed).
    // Startup validation dialogs (ScopesOutdated, TokenInvalid) are driven by startupValidation state directly.
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(Unit) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            authStateCoordinator.events.collect { event ->
                when (event) {
                    is AuthEvent.LoggedIn      -> {
                        launch {
                            delay(2000)
                            snackbarHostState.currentSnackbarData?.dismiss()
                        }
                        snackbarHostState.showSnackbar(
                            message = resources.getString(R.string.snackbar_login, event.userName),
                            duration = SnackbarDuration.Short,
                        )
                    }

                    AuthEvent.ValidationFailed -> {
                        snackbarHostState.showSnackbar(
                            message = resources.getString(R.string.oauth_verify_failed),
                            duration = SnackbarDuration.Short,
                        )
                    }

                    else                       -> Unit
                }
            }
        }
    }

    val startupValidationHolder: StartupValidationHolder = koinInject()
    val startupValidation by startupValidationHolder.state.collectAsStateWithLifecycle()
    val loadingState by mainScreenViewModel.globalLoadingState.collectAsStateWithLifecycle()
    LaunchedEffect(loadingState, startupValidation) {
        if (startupValidation !is StartupValidation.Validated) return@LaunchedEffect
        val state = loadingState as? GlobalLoadingState.Failed ?: return@LaunchedEffect
        val dataSteps = state.failures.map { it.step }.toDisplayStrings(resources)
        val chatSteps = state.chatFailures.map { it.step }.toDisplayStrings(resources)
        val allSteps = dataSteps + chatSteps
        val stepsText = allSteps.joinToString(", ")
        val message = when {
            allSteps.size == 1 -> resources.getString(R.string.snackbar_data_load_failed_cause, stepsText)
            else               -> resources.getString(R.string.snackbar_data_load_failed_multiple_causes, stepsText)
        }
        val result = snackbarHostState.showSnackbar(
            message = message,
            actionLabel = resources.getString(R.string.snackbar_retry),
            duration = SnackbarDuration.Long
        )
        if (result == SnackbarResult.ActionPerformed) {
            mainScreenViewModel.retryDataLoading(state)
        }
    }
}
