package com.flxrs.dankchat.main.compose.sheets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flxrs.dankchat.R
import com.flxrs.dankchat.chat.compose.BadgeUi
import com.flxrs.dankchat.chat.replies.compose.RepliesComposable
import com.flxrs.dankchat.chat.replies.compose.RepliesComposeViewModel
import com.flxrs.dankchat.preferences.appearance.AppearanceSettingsDataStore
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepliesSheet(
    rootMessageId: String,
    appearanceSettingsDataStore: AppearanceSettingsDataStore,
    onDismiss: () -> Unit,
    onUserClick: (userId: String?, userName: String, displayName: String, channel: String?, badges: List<BadgeUi>, isLongPress: Boolean) -> Unit,
    onMessageLongClick: (messageId: String, channel: String?, fullMessage: String) -> Unit,
) {
    val viewModel: RepliesComposeViewModel = koinViewModel(
        key = rootMessageId,
        parameters = { parametersOf(rootMessageId) }
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxSize()
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.replies_title)) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    }
                )
            }
        ) { paddingValues ->
            // Use local ViewModel version of RepliesComposable or similar
            // For simplicity, I'll call RepliesComposable but I might need to adjust it to take the new VM
            // Or just inline its logic here since we have the new VM.
            
            val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
            val appearanceSettings = appearanceSettingsDataStore.settings.collectAsStateWithLifecycle(initialValue = appearanceSettingsDataStore.current()).value

            com.flxrs.dankchat.chat.compose.ChatScreen(
                messages = when (uiState) {
                    is com.flxrs.dankchat.chat.replies.RepliesUiState.Found -> uiState.items
                    else -> emptyList()
                },
                fontSize = appearanceSettings.fontSize.toFloat(),
                modifier = Modifier.padding(paddingValues).fillMaxSize(),
                onUserClick = onUserClick,
                onMessageLongClick = onMessageLongClick,
                onEmoteClick = { /* no-op */ }
            )
            
            if (uiState is com.flxrs.dankchat.chat.replies.RepliesUiState.NotFound) {
                androidx.compose.runtime.LaunchedEffect(Unit) {
                    onDismiss()
                }
            }
        }
    }
}
