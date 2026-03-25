package com.flxrs.dankchat.main.compose.dialogs

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.flxrs.dankchat.utils.DateTimeUtils

private data class ParameterDialogConfig(val titleRes: Int, val hintRes: Int, val defaultValue: String, val commandPrefix: String)

private enum class ParameterDialogType {
    SLOW_MODE,
    FOLLOWER_MODE
}

private val SLOW_MODE_PRESETS = listOf(3, 5, 10, 20, 30, 60, 120)
private data class FollowerPreset(val minutes: Int, val commandArg: String)

private val FOLLOWER_MODE_PRESETS = listOf(
    FollowerPreset(0, "0"),
    FollowerPreset(10, "10m"),
    FollowerPreset(30, "30m"),
    FollowerPreset(60, "1h"),
    FollowerPreset(1440, "1d"),
    FollowerPreset(10080, "1w"),
    FollowerPreset(43200, "30d"),
    FollowerPreset(129600, "90d"),
)

@Composable
private fun formatFollowerPreset(minutes: Int): String = when (minutes) {
    0           -> stringResource(R.string.room_state_follower_any)
    in 1..59    -> stringResource(R.string.room_state_duration_minutes, minutes)
    in 60..1439 -> stringResource(R.string.room_state_duration_hours, minutes / 60)
    in 1440..10079  -> stringResource(R.string.room_state_duration_days, minutes / 1440)
    in 10080..43199 -> stringResource(R.string.room_state_duration_weeks, minutes / 10080)
    else        -> stringResource(R.string.room_state_duration_months, minutes / 43200)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RoomStateDialog(
    roomState: RoomState?,
    onSendCommand: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var presetsView by remember { mutableStateOf<ParameterDialogType?>(null) }
    var parameterDialog by remember { mutableStateOf<ParameterDialogType?>(null) }
    var showSheet by remember { mutableStateOf(true) }

    parameterDialog?.let { type ->
        val (titleRes, hintRes, defaultValue, commandPrefix) = when (type) {
            ParameterDialogType.SLOW_MODE -> ParameterDialogConfig(
                titleRes = R.string.room_state_slow_mode,
                hintRes = R.string.seconds,
                defaultValue = "30",
                commandPrefix = "/slow"
            )

            ParameterDialogType.FOLLOWER_MODE -> ParameterDialogConfig(
                titleRes = R.string.room_state_follower_only,
                hintRes = R.string.minutes,
                defaultValue = "10",
                commandPrefix = "/followers"
            )
        }

        var inputValue by remember(type) { mutableStateOf(defaultValue) }

        AlertDialog(
            onDismissRequest = {
                parameterDialog = null
                onDismiss()
            },
            title = { Text(stringResource(titleRes)) },
            text = {
                OutlinedTextField(
                    value = inputValue,
                    onValueChange = { inputValue = it },
                    label = { Text(stringResource(hintRes)) },
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
                TextButton(onClick = {
                    parameterDialog = null
                    onDismiss()
                }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            AnimatedContent(
                targetState = presetsView,
                transitionSpec = {
                    when {
                        targetState != null -> slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                        else                -> slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                    }
                },
                label = "RoomStateContent"
            ) { currentView ->
                when (currentView) {
                    null -> RoomStateModeChips(
                        roomState = roomState,
                        onSendCommand = onSendCommand,
                        onShowPresets = { presetsView = it },
                        onDismiss = onDismiss,
                    )

                    ParameterDialogType.SLOW_MODE -> PresetChips(
                        titleRes = R.string.room_state_slow_mode,
                        presets = SLOW_MODE_PRESETS,
                        formatLabel = { stringResource(R.string.room_state_duration_seconds, it) },
                        onPresetClick = { value ->
                            onSendCommand("/slow $value")
                            onDismiss()
                        },
                        onCustomClick = {
                            parameterDialog = ParameterDialogType.SLOW_MODE
                            showSheet = false
                        },
                    )

                    ParameterDialogType.FOLLOWER_MODE -> FollowerPresetChips(
                        onPresetClick = { preset ->
                            onSendCommand("/followers ${preset.commandArg}")
                            onDismiss()
                        },
                        onCustomClick = {
                            parameterDialog = ParameterDialogType.FOLLOWER_MODE
                            showSheet = false
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RoomStateModeChips(
    roomState: RoomState?,
    onSendCommand: (String) -> Unit,
    onShowPresets: (ParameterDialogType) -> Unit,
    onDismiss: () -> Unit,
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
                    onShowPresets(ParameterDialogType.SLOW_MODE)
                }
            },
            label = {
                val label = stringResource(R.string.room_state_slow_mode)
                Text(if (isSlowMode && slowModeWaitTime != null) "$label (${DateTimeUtils.formatSeconds(slowModeWaitTime)})" else label)
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
                    onShowPresets(ParameterDialogType.FOLLOWER_MODE)
                }
            },
            label = {
                val label = stringResource(R.string.room_state_follower_only)
                Text(if (isFollowerOnly && followerDuration != null && followerDuration > 0) "$label (${DateTimeUtils.formatSeconds(followerDuration * 60)})" else label)
            },
            leadingIcon = if (isFollowerOnly) {
                { Icon(Icons.Default.Check, contentDescription = null) }
            } else null,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PresetChips(
    titleRes: Int,
    presets: List<Int>,
    formatLabel: @Composable (Int) -> String,
    onPresetClick: (Int) -> Unit,
    onCustomClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
            .navigationBarsPadding(),
    ) {
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            presets.forEach { value ->
                AssistChip(
                    onClick = { onPresetClick(value) },
                    label = { Text(formatLabel(value)) },
                )
            }

            AssistChip(
                onClick = onCustomClick,
                label = { Text(stringResource(R.string.room_state_preset_custom)) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FollowerPresetChips(
    onPresetClick: (FollowerPreset) -> Unit,
    onCustomClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
            .navigationBarsPadding(),
    ) {
        Text(
            text = stringResource(R.string.room_state_follower_only),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FOLLOWER_MODE_PRESETS.forEach { preset ->
                AssistChip(
                    onClick = { onPresetClick(preset) },
                    label = { Text(formatFollowerPreset(preset.minutes)) },
                )
            }

            AssistChip(
                onClick = onCustomClick,
                label = { Text(stringResource(R.string.room_state_preset_custom)) },
            )
        }
    }
}
