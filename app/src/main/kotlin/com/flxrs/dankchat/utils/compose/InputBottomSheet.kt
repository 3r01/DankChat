package com.flxrs.dankchat.utils.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.flxrs.dankchat.R

@Composable
fun InputBottomSheet(
    title: String,
    hint: String,
    confirmText: String = stringResource(R.string.dialog_ok),
    defaultValue: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    showClearButton: Boolean = false,
    validate: ((String) -> String?)? = null,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var inputValue by remember { mutableStateOf(defaultValue) }
    val focusRequester = remember { FocusRequester() }
    val trimmed = inputValue.trim()
    val errorText = validate?.invoke(trimmed)
    val isValid = trimmed.isNotBlank() && errorText == null

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    StyledBottomSheet(onDismiss = onDismiss) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        OutlinedTextField(
            value = inputValue,
            onValueChange = { inputValue = it },
            label = { Text(hint) },
            singleLine = true,
            isError = errorText != null,
            trailingIcon = if (showClearButton && inputValue.isNotEmpty()) {
                {
                    IconButton(onClick = { inputValue = "" }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = stringResource(R.string.clear),
                        )
                    }
                }
            } else {
                null
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = {
                if (isValid) {
                    onConfirm(trimmed)
                }
            }),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
        )

        AnimatedVisibility(
            visible = errorText != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Text(
                text = errorText.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        TextButton(
            onClick = { onConfirm(trimmed) },
            enabled = isValid,
            modifier = Modifier
                .align(Alignment.End)
                .padding(top = 8.dp),
        ) {
            Text(confirmText)
        }
    }
}
