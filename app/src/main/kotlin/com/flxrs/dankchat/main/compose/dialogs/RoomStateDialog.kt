package com.flxrs.dankchat.main.compose.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.flxrs.dankchat.R
import com.flxrs.dankchat.data.twitch.message.RoomState

private enum class ParameterDialogType {
    SLOW_MODE,
    FOLLOWER_MODE
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RoomStateDialog(
    roomState: RoomState?,
    onSendCommand: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var parameterDialog by remember { mutableStateOf<ParameterDialogType?>(null) }

    parameterDialog?.let { type ->
        val (title, hint, defaultValue, commandPrefix) = when (type) {
            ParameterDialogType.SLOW_MODE -> listOf(
                R.string.room_state_slow_mode,
                R.string.seconds,
                "30",
                "/slow"
            )

            ParameterDialogType.FOLLOWER_MODE -> listOf(
                R.string.room_state_follower_only,
                R.string.minutes,
                "10",
                "/followers"
            )
        }

        var inputValue by remember(type) { mutableStateOf(defaultValue as String) }

        AlertDialog(
            onDismissRequest = { parameterDialog = null },
            title = { Text(stringResource(title as Int)) },
            text = {
                OutlinedTextField(
                    value = inputValue,
                    onValueChange = { inputValue = it },
                    label = { Text(stringResource(hint as Int)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onSendCommand("$commandPrefix $inputValue")
                    parameterDialog = null
                    onDismiss()
                }) {
                    Text(stringResource(R.string.dialog_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { parameterDialog = null }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val isEmoteOnly = roomState?.isEmoteMode == true
            FilterChip(
                selected = isEmoteOnly,
                onClick = {
                    onSendCommand(if (isEmoteOnly) "/emoteonlyoff" else "/emoteonly")
                    onDismiss()
                },
                label = { Text(stringResource(R.string.room_state_emote_only)) },
                leadingIcon = if (isEmoteOnly) {
                    { Icon(Icons.Default.Check, contentDescription = null) }
                } else null,
            )

            val isSubOnly = roomState?.isSubscriberMode == true
            FilterChip(
                selected = isSubOnly,
                onClick = {
                    onSendCommand(if (isSubOnly) "/subscribersoff" else "/subscribers")
                    onDismiss()
                },
                label = { Text(stringResource(R.string.room_state_subscriber_only)) },
                leadingIcon = if (isSubOnly) {
                    { Icon(Icons.Default.Check, contentDescription = null) }
                } else null,
            )

            val isSlowMode = roomState?.isSlowMode == true
            val slowModeWaitTime = roomState?.slowModeWaitTime
            FilterChip(
                selected = isSlowMode,
                onClick = {
                    if (isSlowMode) {
                        onSendCommand("/slowoff")
                        onDismiss()
                    } else {
                        parameterDialog = ParameterDialogType.SLOW_MODE
                    }
                },
                label = {
                    val label = stringResource(R.string.room_state_slow_mode)
                    Text(if (isSlowMode && slowModeWaitTime != null) "$label (${slowModeWaitTime}s)" else label)
                },
                leadingIcon = if (isSlowMode) {
                    { Icon(Icons.Default.Check, contentDescription = null) }
                } else null,
            )

            val isUniqueChat = roomState?.isUniqueChatMode == true
            FilterChip(
                selected = isUniqueChat,
                onClick = {
                    onSendCommand(if (isUniqueChat) "/uniquechatoff" else "/uniquechat")
                    onDismiss()
                },
                label = { Text(stringResource(R.string.room_state_unique_chat)) },
                leadingIcon = if (isUniqueChat) {
                    { Icon(Icons.Default.Check, contentDescription = null) }
                } else null,
            )

            val isFollowerOnly = roomState?.isFollowMode == true
            val followerDuration = roomState?.followerModeDuration
            FilterChip(
                selected = isFollowerOnly,
                onClick = {
                    if (isFollowerOnly) {
                        onSendCommand("/followersoff")
                        onDismiss()
                    } else {
                        parameterDialog = ParameterDialogType.FOLLOWER_MODE
                    }
                },
                label = {
                    val label = stringResource(R.string.room_state_follower_only)
                    Text(if (isFollowerOnly && followerDuration != null && followerDuration > 0) "$label (${followerDuration}m)" else label)
                },
                leadingIcon = if (isFollowerOnly) {
                    { Icon(Icons.Default.Check, contentDescription = null) }
                } else null,
            )
        }
    }
}
