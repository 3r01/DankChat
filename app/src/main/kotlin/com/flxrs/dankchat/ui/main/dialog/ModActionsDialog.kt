package com.flxrs.dankchat.ui.main.dialog

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
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.flxrs.dankchat.R
import com.flxrs.dankchat.data.twitch.message.RoomState
import com.flxrs.dankchat.utils.DateTimeUtils

private data class ParameterDialogConfig(val titleRes: Int, val hintRes: Int, val defaultValue: String, val commandPrefix: String)

private sealed interface SubView {
    data object SlowMode : SubView
    data object FollowerMode : SubView
    data object CommercialPresets : SubView
    data object RaidInput : SubView
    data object ShoutoutInput : SubView
}

private val SLOW_MODE_PRESETS = listOf(3, 5, 10, 20, 30, 60, 120)
private val COMMERCIAL_PRESETS = listOf(30, 60, 90, 120, 150, 180)

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
    0               -> stringResource(R.string.room_state_follower_any)
    in 1..59        -> stringResource(R.string.room_state_duration_minutes, minutes)
    in 60..1439     -> stringResource(R.string.room_state_duration_hours, minutes / 60)
    in 1440..10079  -> stringResource(R.string.room_state_duration_days, minutes / 1440)
    in 10080..43199 -> stringResource(R.string.room_state_duration_weeks, minutes / 10080)
    else            -> stringResource(R.string.room_state_duration_months, minutes / 43200)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ModActionsDialog(
    roomState: RoomState?,
    isBroadcaster: Boolean,
    isStreamActive: Boolean,
    shieldModeActive: Boolean?,
    onSendCommand: (String) -> Unit,
    onAnnounce: () -> Unit,
    onDismiss: () -> Unit,
) {
    var subView by remember { mutableStateOf<SubView?>(null) }
    var parameterDialog by remember { mutableStateOf<SubView?>(null) }
    var showSheet by remember { mutableStateOf(true) }
    var showClearChatConfirmation by remember { mutableStateOf(false) }

    if (showClearChatConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearChatConfirmation = false },
            title = { Text(stringResource(R.string.mod_actions_clear_chat)) },
            text = { Text(stringResource(R.string.mod_actions_confirm_clear_chat)) },
            confirmButton = {
                TextButton(onClick = {
                    onSendCommand("/clear")
                    showClearChatConfirmation = false
                    onDismiss()
                }) {
                    Text(stringResource(R.string.dialog_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearChatConfirmation = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }

    parameterDialog?.let { type ->
        val (titleRes, hintRes, defaultValue, commandPrefix) = when (type) {
            SubView.SlowMode     -> ParameterDialogConfig(
                titleRes = R.string.room_state_slow_mode,
                hintRes = R.string.seconds,
                defaultValue = "30",
                commandPrefix = "/slow"
            )

            SubView.FollowerMode -> ParameterDialogConfig(
                titleRes = R.string.room_state_follower_only,
                hintRes = R.string.minutes,
                defaultValue = "10",
                commandPrefix = "/followers"
            )

            else                 -> return@let
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
                targetState = subView,
                transitionSpec = {
                    when {
                        targetState != null -> slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                        else                -> slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                    }
                },
                label = "ModActionsContent"
            ) { currentView ->
                when (currentView) {
                    null                       -> ModActionsMainView(
                        roomState = roomState,
                        isBroadcaster = isBroadcaster,
                        isStreamActive = isStreamActive,
                        shieldModeActive = shieldModeActive,
                        onSendCommand = onSendCommand,
                        onShowSubView = { subView = it },
                        onClearChat = { showClearChatConfirmation = true },
                        onAnnounce = onAnnounce,
                        onDismiss = onDismiss,
                    )

                    SubView.SlowMode           -> PresetChips(
                        titleRes = R.string.room_state_slow_mode,
                        presets = SLOW_MODE_PRESETS,
                        formatLabel = { stringResource(R.string.room_state_duration_seconds, it) },
                        onPresetClick = { value ->
                            onSendCommand("/slow $value")
                            onDismiss()
                        },
                        onCustomClick = {
                            parameterDialog = SubView.SlowMode
                            showSheet = false
                        },
                    )

                    SubView.FollowerMode       -> FollowerPresetChips(
                        onPresetClick = { preset ->
                            onSendCommand("/followers ${preset.commandArg}")
                            onDismiss()
                        },
                        onCustomClick = {
                            parameterDialog = SubView.FollowerMode
                            showSheet = false
                        },
                    )

                    SubView.CommercialPresets   -> PresetChips(
                        titleRes = R.string.mod_actions_commercial,
                        presets = COMMERCIAL_PRESETS,
                        formatLabel = { stringResource(R.string.room_state_duration_seconds, it) },
                        onPresetClick = { value ->
                            onSendCommand("/commercial $value")
                            onDismiss()
                        },
                        onCustomClick = null,
                    )

                    SubView.RaidInput          -> UserInputSubView(
                        titleRes = R.string.mod_actions_raid,
                        hintRes = R.string.mod_actions_channel_hint,
                        onConfirm = { target ->
                            onSendCommand("/raid $target")
                            onDismiss()
                        },
                    )

                    SubView.ShoutoutInput      -> UserInputSubView(
                        titleRes = R.string.mod_actions_shoutout,
                        hintRes = R.string.mod_actions_username_hint,
                        onConfirm = { target ->
                            onSendCommand("/shoutout $target")
                            onDismiss()
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ModActionsMainView(
    roomState: RoomState?,
    isBroadcaster: Boolean,
    isStreamActive: Boolean,
    shieldModeActive: Boolean?,
    onSendCommand: (String) -> Unit,
    onShowSubView: (SubView) -> Unit,
    onClearChat: () -> Unit,
    onAnnounce: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
            .navigationBarsPadding(),
    ) {
        // Room state section
        SectionHeader(stringResource(R.string.mod_actions_room_state_section))
        RoomStateModeChips(
            roomState = roomState,
            onSendCommand = onSendCommand,
            onShowSubView = onShowSubView,
            onDismiss = onDismiss,
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Mod actions section
        SectionHeader(stringResource(R.string.mod_actions_section))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val isShieldActive = shieldModeActive == true
            FilterChip(
                selected = isShieldActive,
                onClick = {
                    onSendCommand(if (isShieldActive) "/shieldoff" else "/shield")
                    onDismiss()
                },
                label = { Text(stringResource(R.string.mod_actions_shield_mode)) },
                leadingIcon = if (isShieldActive) {
                    { Icon(Icons.Default.Check, contentDescription = null) }
                } else {
                    null
                },
            )
            AssistChip(
                onClick = onClearChat,
                label = { Text(stringResource(R.string.mod_actions_clear_chat)) },
            )
            AssistChip(
                onClick = {
                    onAnnounce()
                    onDismiss()
                },
                label = { Text(stringResource(R.string.mod_actions_announce)) },
            )
            if (isStreamActive) {
                AssistChip(
                    onClick = { onShowSubView(SubView.ShoutoutInput) },
                    label = { Text(stringResource(R.string.mod_actions_shoutout)) },
                )
            }
        }

        // Broadcaster section
        if (isBroadcaster && isStreamActive) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SectionHeader(stringResource(R.string.mod_actions_broadcaster_section))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AssistChip(
                    onClick = { onShowSubView(SubView.CommercialPresets) },
                    label = { Text(stringResource(R.string.mod_actions_commercial)) },
                )
                AssistChip(
                    onClick = { onShowSubView(SubView.RaidInput) },
                    label = { Text(stringResource(R.string.mod_actions_raid)) },
                )
                AssistChip(
                    onClick = {
                        onSendCommand("/unraid")
                        onDismiss()
                    },
                    label = { Text(stringResource(R.string.mod_actions_cancel_raid)) },
                )
                AssistChip(
                    onClick = {
                        onSendCommand("/marker")
                        onDismiss()
                    },
                    label = { Text(stringResource(R.string.mod_actions_stream_marker)) },
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RoomStateModeChips(
    roomState: RoomState?,
    onSendCommand: (String) -> Unit,
    onShowSubView: (SubView) -> Unit,
    onDismiss: () -> Unit,
) {
    FlowRow(
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
            } else {
                null
            },
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
            } else {
                null
            },
        )

        val isSlowMode = roomState?.isSlowMode == true
        val slowModeWaitTime = roomState?.slowModeWaitTime
        FilterChip(
            selected = isSlowMode,
            onClick = {
                when {
                    isSlowMode -> {
                        onSendCommand("/slowoff")
                        onDismiss()
                    }

                    else       -> onShowSubView(SubView.SlowMode)
                }
            },
            label = {
                val label = stringResource(R.string.room_state_slow_mode)
                Text(if (isSlowMode && slowModeWaitTime != null) "$label (${DateTimeUtils.formatSeconds(slowModeWaitTime)})" else label)
            },
            leadingIcon = if (isSlowMode) {
                { Icon(Icons.Default.Check, contentDescription = null) }
            } else {
                null
            },
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
            } else {
                null
            },
        )

        val isFollowerOnly = roomState?.isFollowMode == true
        val followerDuration = roomState?.followerModeDuration
        FilterChip(
            selected = isFollowerOnly,
            onClick = {
                when {
                    isFollowerOnly -> {
                        onSendCommand("/followersoff")
                        onDismiss()
                    }

                    else           -> onShowSubView(SubView.FollowerMode)
                }
            },
            label = {
                val label = stringResource(R.string.room_state_follower_only)
                Text(if (isFollowerOnly && followerDuration != null && followerDuration > 0) "$label (${DateTimeUtils.formatSeconds(followerDuration * 60)})" else label)
            },
            leadingIcon = if (isFollowerOnly) {
                { Icon(Icons.Default.Check, contentDescription = null) }
            } else {
                null
            },
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
    onCustomClick: (() -> Unit)?,
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

            if (onCustomClick != null) {
                AssistChip(
                    onClick = onCustomClick,
                    label = { Text(stringResource(R.string.room_state_preset_custom)) },
                )
            }
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

@Composable
private fun UserInputSubView(
    titleRes: Int,
    hintRes: Int,
    onConfirm: (String) -> Unit,
) {
    var inputValue by remember { mutableStateOf("") }

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

        OutlinedTextField(
            value = inputValue,
            onValueChange = { inputValue = it },
            label = { Text(stringResource(hintRes)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                if (inputValue.isNotBlank()) {
                    onConfirm(inputValue.trim())
                }
            }),
            modifier = Modifier.fillMaxWidth(),
        )

        TextButton(
            onClick = { onConfirm(inputValue.trim()) },
            enabled = inputValue.isNotBlank(),
            modifier = Modifier
                .align(Alignment.End)
                .padding(top = 8.dp),
        ) {
            Text(stringResource(R.string.dialog_ok))
        }
    }
}
