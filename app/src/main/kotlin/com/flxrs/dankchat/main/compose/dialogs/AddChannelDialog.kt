package com.flxrs.dankchat.main.compose.dialogs

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.flxrs.dankchat.R
import com.flxrs.dankchat.data.UserName

@Composable
fun AddChannelDialog(
    onDismiss: () -> Unit,
    onAddChannel: (UserName) -> Unit,
) {
    var channelName by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_channel)) },
        text = {
            OutlinedTextField(
                value = channelName,
                onValueChange = { channelName = it },
                label = { Text(stringResource(R.string.add_channel_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    val trimmed = channelName.trim()
                    if (trimmed.isNotBlank()) {
                        onAddChannel(UserName(trimmed))
                        onDismiss()
                    }
                }),
                modifier = Modifier.focusRequester(focusRequester)
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onAddChannel(UserName(channelName.trim()))
                    onDismiss()
                },
                enabled = channelName.isNotBlank()
            ) {
                Text(stringResource(R.string.dialog_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        }
    )

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}
