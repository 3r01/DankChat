package com.flxrs.dankchat.preferences.whispers

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedSecureTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flxrs.dankchat.R
import com.flxrs.dankchat.domain.WhisperHistoryStatus
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhisperHistorySettingsSheet(onDismissRequest: () -> Unit) {
    val viewModel = koinViewModel<WhisperHistorySettingsViewModel>()
    val state = viewModel.state.collectAsStateWithLifecycle().value
    val token = rememberTextFieldState()
    var showPassword by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = stringResource(R.string.preference_whisper_history_title),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            )
            Text(
                text = stringResource(R.string.preference_whisper_history_description),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            )
            Text(
                text = whisperHistoryStatusText(state),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            )
            OutlinedSecureTextField(
                state = token,
                enabled = state.userName != null,
                textObfuscationMode = if (showPassword) TextObfuscationMode.Visible else TextObfuscationMode.Hidden,
                label = {
                    Text(stringResource(R.string.preference_whisper_history_token_label))
                },
                isError = state.invalidToken,
                supportingText = {
                    if (state.invalidToken) {
                        Text(stringResource(R.string.preference_whisper_history_token_empty))
                    }
                },
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null)
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, autoCorrectEnabled = false, keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                TextButton(
                    onClick = viewModel::clearToken,
                    enabled = state.hasSavedToken,
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text(stringResource(R.string.clear))
                }
                TextButton(
                    onClick = {
                        if (viewModel.saveToken(token.text.toString())) token.clearText()
                    },
                    enabled = state.userName != null,
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text(stringResource(R.string.save))
                }
            }
            AnimatedVisibility(state.status is WhisperHistoryStatus.Loading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(64.dp))
        }
    }
}

@Composable
private fun whisperHistoryStatusText(state: WhisperHistorySettingsState): String = when {
    state.userName == null ->
        stringResource(R.string.preference_whisper_history_login_required)

    !state.hasSavedToken ->
        stringResource(R.string.preference_whisper_history_no_token, state.userName)

    state.status is WhisperHistoryStatus.Loading ->
        stringResource(R.string.preference_whisper_history_loading)

    state.status is WhisperHistoryStatus.Loaded ->
        stringResource(R.string.preference_whisper_history_loaded, state.status.messageCount)

    state.status is WhisperHistoryStatus.Error -> state.status.message

    else ->
        stringResource(R.string.preference_whisper_history_token_saved, state.userName)
}
