package com.flxrs.dankchat.main.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.flxrs.dankchat.R
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.main.InputState
import com.flxrs.dankchat.preferences.appearance.InputAction

private const val MAX_INPUT_ACTIONS = 4

@Composable
fun ChatInputLayout(
    textFieldState: TextFieldState,
    inputState: InputState,
    enabled: Boolean,
    canSend: Boolean,
    showReplyOverlay: Boolean,
    replyName: UserName?,
    isEmoteMenuOpen: Boolean,
    helperText: String?,
    isUploading: Boolean,
    isLoading: Boolean,
    isFullscreen: Boolean,
    isModerator: Boolean,
    isStreamActive: Boolean,
    hasStreamData: Boolean,
    inputActions: Set<InputAction>,
    onSend: () -> Unit,
    onLastMessageClick: () -> Unit,
    onEmoteClick: () -> Unit,
    onReplyDismiss: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onToggleInput: () -> Unit,
    onToggleStream: () -> Unit,
    showWhisperOverlay: Boolean,
    whisperTarget: UserName?,
    onWhisperDismiss: () -> Unit,
    onChangeRoomState: () -> Unit,
    onInputActionsChanged: (Set<InputAction>) -> Unit,
    onSearchClick: () -> Unit = {},
    onNewWhisper: (() -> Unit)? = null,
    showQuickActions: Boolean = true,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    var maxTextFieldHeight by remember { mutableIntStateOf(0) }
    val hint = when (inputState) {
        InputState.Default -> stringResource(R.string.hint_connected)
        InputState.Replying -> stringResource(R.string.hint_replying)
        InputState.Whispering -> stringResource(R.string.hint_whispering)
        InputState.NotLoggedIn -> stringResource(R.string.hint_not_logged_int)
        InputState.Disconnected -> stringResource(R.string.hint_disconnected)
    }

    val textFieldColors = TextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        disabledContainerColor = Color.Transparent,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent,
        errorIndicatorColor = Color.Transparent
    )
    val defaultColors = TextFieldDefaults.colors()
    val surfaceColor = if (enabled) {
        defaultColors.unfocusedContainerColor
    } else {
        defaultColors.disabledContainerColor
    }

    var quickActionsExpanded by remember { mutableStateOf(false) }
    var showConfigSheet by remember { mutableStateOf(false) }
    val topEndRadius by animateDpAsState(
        targetValue = if (quickActionsExpanded) 0.dp else 24.dp,
        label = "topEndCornerRadius"
    )

    Box(modifier = modifier.fillMaxWidth()) {
        Surface(
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = topEndRadius),
            color = surfaceColor,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                // Reply Header
                AnimatedVisibility(
                    visible = showReplyOverlay && replyName != null,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.reply_header, replyName?.value.orEmpty()),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = onReplyDismiss,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.dialog_dismiss),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                // Whisper Header
                AnimatedVisibility(
                    visible = showWhisperOverlay && whisperTarget != null,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.whisper_header, whisperTarget?.value.orEmpty()),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = onWhisperDismiss,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.dialog_dismiss),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                // Text Field
                TextField(
                    state = textFieldState,
                    enabled = enabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .layout { measurable, constraints ->
                            val placeable = measurable.measure(constraints)
                            val height = maxOf(placeable.height, maxTextFieldHeight)
                            maxTextFieldHeight = height
                            layout(placeable.width, height) {
                                placeable.placeRelative(0, 0)
                            }
                        }
                        .padding(bottom = 0.dp), // Reduce bottom padding as actions are below
                    label = { Text(hint) },
                    colors = textFieldColors,
                    shape = RoundedCornerShape(0.dp),
                    lineLimits = TextFieldLineLimits.MultiLine(
                        minHeightInLines = 1,
                        maxHeightInLines = 5
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    onKeyboardAction = { if (canSend) onSend() }
                )

                // Helper text (roomstate + live info)
                AnimatedVisibility(
                    visible = !helperText.isNullOrEmpty(),
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Text(
                        text = helperText.orEmpty(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 4.dp)
                            .basicMarquee(),
                        textAlign = TextAlign.Start
                    )
                }

                // Progress indicator for uploads and data loading
                AnimatedVisibility(
                    visible = isUploading || isLoading,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                // Actions Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
                ) {
                    // Configurable action icons (enum order)
                    for (action in InputAction.entries) {
                        if (action !in inputActions) continue
                        when (action) {
                            InputAction.Emotes -> {
                                IconButton(
                                    onClick = {
                                        if (isEmoteMenuOpen) {
                                            focusRequester.requestFocus()
                                        }
                                        onEmoteClick()
                                    },
                                    enabled = enabled,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isEmoteMenuOpen) Icons.Default.Keyboard else Icons.Default.EmojiEmotions,
                                        contentDescription = stringResource(
                                            if (isEmoteMenuOpen) R.string.dialog_dismiss else R.string.emote_menu_hint
                                        ),
                                    )
                                }
                            }

                            InputAction.Search -> {
                                IconButton(
                                    onClick = onSearchClick,
                                    enabled = enabled,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = stringResource(R.string.message_history),
                                    )
                                }
                            }

                            InputAction.History -> {
                                IconButton(
                                    onClick = onLastMessageClick,
                                    enabled = enabled,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = stringResource(R.string.resume_scroll),
                                    )
                                }
                            }

                            InputAction.Stream -> {
                                if (hasStreamData || isStreamActive) {
                                    IconButton(
                                        onClick = onToggleStream,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isStreamActive) Icons.Default.VideocamOff else Icons.Default.Videocam,
                                            contentDescription = stringResource(R.string.toggle_stream),
                                        )
                                    }
                                }
                            }

                            InputAction.RoomState -> {
                                if (isModerator) {
                                    IconButton(
                                        onClick = onChangeRoomState,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Shield,
                                            contentDescription = stringResource(R.string.menu_room_state),
                                        )
                                    }
                                }
                            }

                            InputAction.Fullscreen -> {
                                IconButton(
                                    onClick = onToggleFullscreen,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                        contentDescription = stringResource(R.string.toggle_fullscreen),
                                    )
                                }
                            }

                            InputAction.HideInput -> {
                                IconButton(
                                    onClick = onToggleInput,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VisibilityOff,
                                        contentDescription = stringResource(R.string.menu_hide_input),
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Quick Actions / Overflow Button
                    if (showQuickActions) {
                        IconButton(
                            onClick = { quickActionsExpanded = !quickActionsExpanded },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.more),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // New Whisper Button (only on whisper tab)
                    if (onNewWhisper != null) {
                        IconButton(
                            onClick = onNewWhisper,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.whisper_new),
                            )
                        }
                    }

                    // Send Button (Right)
                    SendButton(
                        enabled = canSend,
                        onSend = onSend,
                        modifier = Modifier
                    )
                }
            }
        }

        // Quick actions menu — Popup with custom positioning and slide animation
        val menuVisibleState = remember { MutableTransitionState(false) }
        menuVisibleState.targetState = quickActionsExpanded

        if (menuVisibleState.currentState || menuVisibleState.targetState) {
            val positionProvider = remember {
                object : PopupPositionProvider {
                    override fun calculatePosition(
                        anchorBounds: IntRect,
                        windowSize: IntSize,
                        layoutDirection: LayoutDirection,
                        popupContentSize: IntSize
                    ): IntOffset = IntOffset(
                        x = anchorBounds.right - popupContentSize.width,
                        y = anchorBounds.top - popupContentSize.height
                    )
                }
            }

            Popup(
                popupPositionProvider = positionProvider,
                onDismissRequest = { quickActionsExpanded = false },
                properties = PopupProperties(focusable = true),
            ) {
                AnimatedVisibility(
                    visibleState = menuVisibleState,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(durationMillis = 150)
                    ) + fadeIn(animationSpec = tween(durationMillis = 100)),
                    exit = shrinkVertically(
                        shrinkTowards = Alignment.Bottom,
                        animationSpec = tween(durationMillis = 120)
                    ) + fadeOut(animationSpec = tween(durationMillis = 80)),
                ) {
                    Surface(
                        shape = RoundedCornerShape(topStart = 12.dp),
                        color = surfaceColor,
                    ) {
                        Column(modifier = Modifier.width(IntrinsicSize.Max)) {
                            // Overflow items: actions NOT in inputActions (with conditions)
                            for (action in InputAction.entries) {
                                if (action in inputActions) continue
                                val overflowItem = getOverflowItem(
                                    action = action,
                                    isStreamActive = isStreamActive,
                                    hasStreamData = hasStreamData,
                                    isFullscreen = isFullscreen,
                                    isModerator = isModerator,
                                )
                                if (overflowItem != null) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(overflowItem.labelRes)) },
                                        onClick = {
                                            when (action) {
                                                InputAction.Emotes -> onEmoteClick()
                                                InputAction.Search -> onSearchClick()
                                                InputAction.History -> onLastMessageClick()
                                                InputAction.Stream -> onToggleStream()
                                                InputAction.RoomState -> onChangeRoomState()
                                                InputAction.Fullscreen -> onToggleFullscreen()
                                                InputAction.HideInput -> onToggleInput()
                                            }
                                            quickActionsExpanded = false
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = overflowItem.icon,
                                                contentDescription = null
                                            )
                                        }
                                    )
                                }
                            }

                            HorizontalDivider()

                            // Configure actions item
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.input_action_configure)) },
                                onClick = {
                                    quickActionsExpanded = false
                                    showConfigSheet = true
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = null
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showConfigSheet) {
        InputActionConfigSheet(
            inputActions = inputActions,
            onInputActionsChanged = onInputActionsChanged,
            onDismiss = { showConfigSheet = false },
        )
    }
}

private data class OverflowItem(
    val labelRes: Int,
    val icon: ImageVector,
)

private fun getOverflowItem(
    action: InputAction,
    isStreamActive: Boolean,
    hasStreamData: Boolean,
    isFullscreen: Boolean,
    isModerator: Boolean,
): OverflowItem? = when (action) {
    InputAction.Emotes -> OverflowItem(
        labelRes = R.string.input_action_emotes,
        icon = Icons.Default.EmojiEmotions,
    )

    InputAction.Search -> OverflowItem(
        labelRes = R.string.input_action_search,
        icon = Icons.Default.Search,
    )

    InputAction.History -> OverflowItem(
        labelRes = R.string.input_action_history,
        icon = Icons.Default.History,
    )

    InputAction.Stream -> when {
        hasStreamData || isStreamActive -> OverflowItem(
            labelRes = if (isStreamActive) R.string.menu_hide_stream else R.string.menu_show_stream,
            icon = if (isStreamActive) Icons.Default.VideocamOff else Icons.Default.Videocam,
        )

        else -> null
    }

    InputAction.RoomState -> when {
        isModerator -> OverflowItem(
            labelRes = R.string.menu_room_state,
            icon = Icons.Default.Shield,
        )

        else -> null
    }

    InputAction.Fullscreen -> OverflowItem(
        labelRes = if (isFullscreen) R.string.menu_exit_fullscreen else R.string.menu_fullscreen,
        icon = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
    )

    InputAction.HideInput -> OverflowItem(
        labelRes = R.string.menu_hide_input,
        icon = Icons.Default.VisibilityOff,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InputActionConfigSheet(
    inputActions: Set<InputAction>,
    onInputActionsChanged: (Set<InputAction>) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val atLimit = inputActions.size >= MAX_INPUT_ACTIONS

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.input_actions_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (atLimit) {
                Text(
                    text = stringResource(R.string.input_actions_max, MAX_INPUT_ACTIONS),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            for (action in InputAction.entries) {
                val checked = action in inputActions
                val actionEnabled = checked || !atLimit

                ListItem(
                    headlineContent = { Text(stringResource(action.labelRes)) },
                    leadingContent = {
                        Icon(
                            imageVector = action.icon,
                            contentDescription = null,
                        )
                    },
                    trailingContent = {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = null, // handled by row click
                            enabled = actionEnabled,
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (actionEnabled) {
                                Modifier.clickable {
                                    val newSet = if (checked) {
                                        inputActions - action
                                    } else {
                                        inputActions + action
                                    }
                                    onInputActionsChanged(newSet)
                                }
                            } else {
                                Modifier
                            }
                        ),
                )
            }
        }
    }
}

private val InputAction.labelRes: Int
    get() = when (this) {
        InputAction.Emotes -> R.string.input_action_emotes
        InputAction.Search -> R.string.input_action_search
        InputAction.History -> R.string.input_action_history
        InputAction.Stream -> R.string.input_action_stream
        InputAction.RoomState -> R.string.input_action_room_state
        InputAction.Fullscreen -> R.string.input_action_fullscreen
        InputAction.HideInput -> R.string.input_action_hide_input
    }

private val InputAction.icon: ImageVector
    get() = when (this) {
        InputAction.Emotes -> Icons.Default.EmojiEmotions
        InputAction.Search -> Icons.Default.Search
        InputAction.History -> Icons.Default.History
        InputAction.Stream -> Icons.Default.Videocam
        InputAction.RoomState -> Icons.Default.Shield
        InputAction.Fullscreen -> Icons.Default.Fullscreen
        InputAction.HideInput -> Icons.Default.VisibilityOff
    }

@Composable
private fun SendButton(
    enabled: Boolean,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor = if (enabled) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
    }

    IconButton(
        onClick = onSend,
        enabled = enabled,
        modifier = modifier.size(40.dp)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Send,
            contentDescription = stringResource(R.string.send_hint),
            tint = contentColor
        )
    }
}
