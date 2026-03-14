package com.flxrs.dankchat.main.compose

import android.content.ClipData
import android.content.ClipboardManager
import android.content.res.Resources
import androidx.core.content.getSystemService
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.flxrs.dankchat.R
import com.flxrs.dankchat.data.state.GlobalLoadingState
import com.flxrs.dankchat.main.MainActivity
import com.flxrs.dankchat.main.MainEvent
import com.flxrs.dankchat.preferences.DankChatPreferenceStore
import kotlinx.coroutines.launch

@Composable
fun MainScreenEventHandler(
    navController: NavController,
    resources: Resources,
    snackbarHostState: SnackbarHostState,
    mainEventBus: MainEventBus,
    dialogViewModel: DialogStateViewModel,
    chatInputViewModel: ChatInputViewModel,
    channelTabViewModel: ChannelTabViewModel,
    channelManagementViewModel: ChannelManagementViewModel,
    mainScreenViewModel: MainScreenViewModel,
    preferenceStore: DankChatPreferenceStore,
) {
    val context = LocalContext.current

    // MainEventBus event collection
    LaunchedEffect(Unit) {
        mainEventBus.events.collect { event ->
            when (event) {
                is MainEvent.LogOutRequested -> dialogViewModel.showLogout()
                is MainEvent.UploadLoading -> dialogViewModel.setUploading(true)
                is MainEvent.UploadSuccess -> {
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
                is MainEvent.UploadFailed -> {
                    dialogViewModel.setUploading(false)
                    val message = event.errorMessage?.let { resources.getString(R.string.snackbar_upload_failed_cause, it) }
                        ?: resources.getString(R.string.snackbar_upload_failed)
                    snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Long)
                }
                is MainEvent.LoginValidated -> {
                    snackbarHostState.showSnackbar(
                        message = resources.getString(R.string.snackbar_login, event.username),
                        duration = SnackbarDuration.Short
                    )
                }
                is MainEvent.LoginOutdated -> {
                    dialogViewModel.showLoginOutdated(event.username)
                }
                MainEvent.LoginTokenInvalid -> {
                    dialogViewModel.showLoginExpired()
                }
                MainEvent.LoginValidationFailed -> {
                    snackbarHostState.showSnackbar(
                        message = resources.getString(R.string.oauth_verify_failed),
                        duration = SnackbarDuration.Short
                    )
                }
                is MainEvent.OpenChannel -> {
                    channelTabViewModel.selectTab(
                        preferenceStore.channels.indexOf(event.channel)
                    )
                    (context as? MainActivity)?.clearNotificationsOfChannel(event.channel)
                }
                else -> Unit
            }
        }
    }

    // Handle Login Result
    val navBackStackEntry = navController.currentBackStackEntryAsState().value
    val loginSuccess by navBackStackEntry?.savedStateHandle?.getStateFlow<Boolean?>("login_success", null)?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(null) }
    LaunchedEffect(loginSuccess) {
        if (loginSuccess == true) {
            channelManagementViewModel.reconnect()
            mainScreenViewModel.reloadGlobalData()
            navBackStackEntry?.savedStateHandle?.remove<Boolean>("login_success")
            launch {
                val name = preferenceStore.userName
                val message = if (name != null) {
                    resources.getString(R.string.snackbar_login, name)
                } else {
                    resources.getString(R.string.login)
                }
                snackbarHostState.showSnackbar(message)
            }
        }
    }

    // Handle data loading errors
    val loadingState by mainScreenViewModel.globalLoadingState.collectAsStateWithLifecycle()
    LaunchedEffect(loadingState) {
        if (loadingState is GlobalLoadingState.Failed) {
            val state = loadingState as GlobalLoadingState.Failed
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
