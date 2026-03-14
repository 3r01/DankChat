package com.flxrs.dankchat.main.compose.sheets

import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flxrs.dankchat.R
import com.flxrs.dankchat.chat.compose.BadgeUi
import com.flxrs.dankchat.chat.compose.ChatScreen
import com.flxrs.dankchat.chat.replies.RepliesUiState
import com.flxrs.dankchat.chat.replies.compose.RepliesComposeViewModel
import com.flxrs.dankchat.preferences.appearance.AppearanceSettingsDataStore
import kotlinx.coroutines.CancellationException
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepliesSheet(
    rootMessageId: String,
    appearanceSettingsDataStore: AppearanceSettingsDataStore,
    inputHeight: androidx.compose.ui.unit.Dp,
    onDismiss: () -> Unit,
    onUserClick: (userId: String?, userName: String, displayName: String, channel: String?, badges: List<BadgeUi>, isLongPress: Boolean) -> Unit,
    onMessageLongClick: (messageId: String, channel: String?, fullMessage: String) -> Unit,
) {
    val viewModel: RepliesComposeViewModel = koinViewModel(
        key = rootMessageId,
        parameters = { parametersOf(rootMessageId) }
    )
    var backProgress by remember { mutableFloatStateOf(0f) }

    PredictiveBackHandler { progress ->
        try {
            progress.collect { event ->
                backProgress = event.progress
            }
            onDismiss()
        } catch (e: CancellationException) {
            backProgress = 0f
        }
    }

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
        },
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                val scale = 1f - (backProgress * 0.1f)
                scaleX = scale
                scaleY = scale
                alpha = 1f - backProgress
                translationY = backProgress * 100f
            }
    ) { paddingValues ->
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val appearanceSettings by appearanceSettingsDataStore.settings.collectAsStateWithLifecycle(initialValue = appearanceSettingsDataStore.current())

        ChatScreen(
            messages = when (val state = uiState) {
                is RepliesUiState.Found -> state.items
                else -> emptyList()
            },
            fontSize = appearanceSettings.fontSize.toFloat(),
            modifier = Modifier
                .padding(paddingValues)
                .padding(bottom = inputHeight)
                .fillMaxSize(),
            onUserClick = onUserClick,
            onMessageLongClick = onMessageLongClick,
            onEmoteClick = { /* no-op */ }
        )
        
        if (uiState is RepliesUiState.NotFound) {
            androidx.compose.runtime.LaunchedEffect(Unit) {
                onDismiss()
            }
        }
    }


}
