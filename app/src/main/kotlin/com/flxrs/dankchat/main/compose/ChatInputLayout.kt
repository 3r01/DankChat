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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
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
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TooltipScope
import androidx.compose.material3.TooltipState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalDensity
import com.flxrs.dankchat.R
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.main.InputState
import com.flxrs.dankchat.preferences.appearance.InputAction
import sh.calvin.reorderable.ReorderableColumn

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
    inputActions: List<InputAction>,
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
    onInputActionsChanged: (List<InputAction>) -> Unit,
    onSearchClick: () -> Unit = {},
    onNewWhisper: (() -> Unit)? = null,
    showQuickActions: Boolean = true,
    inputActionsTooltipState: TooltipState? = null,
    overflowMenuTooltipState: TooltipState? = null,
    configureActionsTooltipState: TooltipState? = null,
    swipeGestureTooltipState: TooltipState? = null,
    forceOverflowOpen: Boolean = false,
    onTourAdvance: (() -> Unit)? = null,
    onTourSkip: (() -> Unit)? = null,
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

    // Filter to actions that would actually render based on current state
    val effectiveActions = remember(inputActions, isModerator, hasStreamData, isStreamActive) {
        inputActions.filter { action ->
            when (action) {
                InputAction.Stream -> hasStreamData || isStreamActive
                InputAction.RoomState -> isModerator
                else -> true
            }
        }
    }

    var visibleActions by remember { mutableStateOf(effectiveActions) }
    var userExpandedMenu by remember { mutableStateOf(false) }
    val quickActionsExpanded = userExpandedMenu || forceOverflowOpen
    var showConfigSheet by remember { mutableStateOf(false) }
    val topEndRadius by animateDpAsState(
        targetValue = if (quickActionsExpanded) 0.dp else 24.dp,
        label = "topEndCornerRadius"
    )

    val inputContent: @Composable () -> Unit = {
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

                // Actions Row — uses BoxWithConstraints to hide actions that don't fit
                val actionsRowContent: @Composable () -> Unit = {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
                    ) {
                        val iconSize = 40.dp
                        // Fixed slots: emote + overflow + send (+ whisper if present)
                        val fixedSlots = 1 + (if (showQuickActions) 1 else 0) + (if (onNewWhisper != null) 1 else 0) + 1
                        val availableForActions = maxWidth - iconSize * fixedSlots
                        val maxVisibleActions = (availableForActions / iconSize).toInt().coerceAtLeast(0)
                        visibleActions = effectiveActions.take(maxVisibleActions)

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Emote/Keyboard Button (start-aligned, always visible)
                            IconButton(
                                onClick = {
                                    if (isEmoteMenuOpen) {
                                        focusRequester.requestFocus()
                                    }
                                    onEmoteClick()
                                },
                                enabled = enabled,
                                modifier = Modifier.size(iconSize)
                            ) {
                                Icon(
                                    imageVector = if (isEmoteMenuOpen) Icons.Default.Keyboard else Icons.Default.EmojiEmotions,
                                    contentDescription = stringResource(
                                        if (isEmoteMenuOpen) R.string.dialog_dismiss else R.string.emote_menu_hint
                                    ),
                                )
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            // End-aligned group: overflow + actions + whisper + send
                            val endAlignedContent: @Composable () -> Unit = {
                                // Overflow Button (leading the end-aligned group)
                                if (showQuickActions) {
                                    val overflowButton: @Composable () -> Unit = {
                                        IconButton(
                                            onClick = {
                                                if (overflowMenuTooltipState != null) {
                                                    onTourAdvance?.invoke()
                                                } else {
                                                    userExpandedMenu = !quickActionsExpanded
                                                }
                                            },
                                            modifier = Modifier.size(iconSize)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.MoreVert,
                                                contentDescription = stringResource(R.string.more),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    if (overflowMenuTooltipState != null) {
                                        TooltipBox(
                                            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                                            tooltip = {
                                                TourTooltip(
                                                    text = stringResource(R.string.tour_overflow_menu),
                                                    onAction = { onTourAdvance?.invoke() },
                                                    onSkip = { onTourSkip?.invoke() },
                                                )
                                            },
                                            state = overflowMenuTooltipState,
                                            hasAction = true,
                                        ) {
                                            overflowButton()
                                        }
                                    } else {
                                        overflowButton()
                                    }
                                }

                                // Configurable action icons (only those that fit)
                                for (action in visibleActions) {
                                    InputActionButton(
                                        action = action,
                                        enabled = enabled,
                                        isStreamActive = isStreamActive,
                                        isFullscreen = isFullscreen,
                                        onSearchClick = onSearchClick,
                                        onLastMessageClick = onLastMessageClick,
                                        onToggleStream = onToggleStream,
                                        onChangeRoomState = onChangeRoomState,
                                        onToggleFullscreen = onToggleFullscreen,
                                        onToggleInput = onToggleInput,
                                        modifier = Modifier.size(iconSize),
                                    )
                                }

                                // New Whisper Button (only on whisper tab)
                                if (onNewWhisper != null) {
                                    IconButton(
                                        onClick = onNewWhisper,
                                        modifier = Modifier.size(iconSize)
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
                                )
                            }

                            if (inputActionsTooltipState != null) {
                                TooltipBox(
                                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                                    tooltip = {
                                        TourTooltip(
                                            text = stringResource(R.string.tour_input_actions),
                                            onAction = { onTourAdvance?.invoke() },
                                            onSkip = { onTourSkip?.invoke() },
                                        )
                                    },
                                    state = inputActionsTooltipState,
                                    onDismissRequest = {},
                                    focusable = true,
                                    hasAction = true,
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        endAlignedContent()
                                    }
                                }
                            } else {
                                endAlignedContent()
                            }
                    }
                }
                }

                actionsRowContent()
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
                onDismissRequest = { if (!forceOverflowOpen) userExpandedMenu = false },
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
                            // Overflow items: actions NOT visible in the action bar
                            for (action in InputAction.entries) {
                                if (action in visibleActions) continue
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
                                                InputAction.Search -> onSearchClick()
                                                InputAction.LastMessage -> onLastMessageClick()
                                                InputAction.Stream -> onToggleStream()
                                                InputAction.RoomState -> onChangeRoomState()
                                                InputAction.Fullscreen -> onToggleFullscreen()
                                                InputAction.HideInput -> onToggleInput()
                                            }
                                            userExpandedMenu = false
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
                            val configureItem: @Composable () -> Unit = {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.input_action_configure)) },
                                    onClick = {
                                        if (configureActionsTooltipState != null) {
                                            onTourAdvance?.invoke()
                                        } else {
                                            userExpandedMenu = false
                                            showConfigSheet = true
                                        }
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Settings,
                                            contentDescription = null
                                        )
                                    }
                                )
                            }
                            if (configureActionsTooltipState != null) {
                                TooltipBox(
                                    positionProvider = rememberStartAlignedTooltipPositionProvider(),
                                    tooltip = {
                                        EndCaretTourTooltip(
                                            text = stringResource(R.string.tour_configure_actions),
                                            onAction = { onTourAdvance?.invoke() },
                                            onSkip = { onTourSkip?.invoke() },
                                        )
                                    },
                                    state = configureActionsTooltipState,
                                    onDismissRequest = {},
                                    focusable = true,
                                    hasAction = true,
                                ) {
                                    configureItem()
                                }
                            } else {
                                configureItem()
                            }
                        }
                    }
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxWidth()) {
        if (swipeGestureTooltipState != null) {
            TooltipBox(
                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                tooltip = {
                    TourTooltip(
                        text = stringResource(R.string.tour_swipe_gesture),
                        onAction = { onTourAdvance?.invoke() },
                        onSkip = { onTourSkip?.invoke() },
                    )
                },
                state = swipeGestureTooltipState,
                hasAction = true,
            ) {
                inputContent()
            }
        } else {
            inputContent()
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
    InputAction.Search -> OverflowItem(
        labelRes = R.string.input_action_search,
        icon = Icons.Default.Search,
    )

    InputAction.LastMessage -> OverflowItem(
        labelRes = R.string.input_action_last_message,
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
    inputActions: List<InputAction>,
    onInputActionsChanged: (List<InputAction>) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val localEnabled = remember { mutableStateListOf(*inputActions.toTypedArray()) }

    val disabledActions = InputAction.entries.filter { it !in localEnabled }
    val atLimit = localEnabled.size >= MAX_INPUT_ACTIONS

    ModalBottomSheet(
        onDismissRequest = {
            onInputActionsChanged(localEnabled.toList())
            onDismiss()
        },
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = if (atLimit) stringResource(R.string.input_actions_max, MAX_INPUT_ACTIONS) else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // Enabled actions (reorderable, drag constrained to this section)
            ReorderableColumn(
                list = localEnabled.toList(),
                onSettle = { from, to ->
                    localEnabled.apply { add(to, removeAt(from)) }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { _, action, isDragging ->
                val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp)

                Surface(
                    shadowElevation = elevation,
                    color = if (isDragging) MaterialTheme.colorScheme.surfaceContainerHighest else Color.Transparent,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .longPressDraggableHandle()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .height(40.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.DragHandle,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(Modifier.width(16.dp))
                        Icon(
                            imageVector = action.icon,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = stringResource(action.labelRes),
                            modifier = Modifier.weight(1f),
                        )
                        Checkbox(
                            checked = true,
                            onCheckedChange = { localEnabled.remove(action) },
                        )
                    }
                }
            }

            // Divider between enabled and disabled
            if (disabledActions.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            }

            // Disabled actions (not reorderable)
            for (action in disabledActions) {
                val actionEnabled = !atLimit

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (actionEnabled) {
                                Modifier.clickable { localEnabled.add(action) }
                            } else {
                                Modifier
                            }
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .height(40.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Spacer(Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Icon(
                        imageVector = action.icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = if (actionEnabled) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                        },
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = stringResource(action.labelRes),
                        modifier = Modifier.weight(1f),
                        color = if (actionEnabled) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        },
                    )
                    Checkbox(
                        checked = false,
                        onCheckedChange = { localEnabled.add(action) },
                        enabled = actionEnabled,
                    )
                }
            }
        }
    }
}

private val InputAction.labelRes: Int
    get() = when (this) {
        InputAction.Search -> R.string.input_action_search
        InputAction.LastMessage -> R.string.input_action_last_message
        InputAction.Stream -> R.string.input_action_stream
        InputAction.RoomState -> R.string.input_action_room_state
        InputAction.Fullscreen -> R.string.input_action_fullscreen
        InputAction.HideInput -> R.string.input_action_hide_input
    }

private val InputAction.icon: ImageVector
    get() = when (this) {
        InputAction.Search -> Icons.Default.Search
        InputAction.LastMessage -> Icons.Default.History
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

@Composable
private fun InputActionButton(
    action: InputAction,
    enabled: Boolean,
    isStreamActive: Boolean,
    isFullscreen: Boolean,
    onSearchClick: () -> Unit,
    onLastMessageClick: () -> Unit,
    onToggleStream: () -> Unit,
    onChangeRoomState: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onToggleInput: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (icon, contentDescription, onClick) = when (action) {
        InputAction.Search -> Triple(Icons.Default.Search, R.string.message_history, onSearchClick)
        InputAction.LastMessage -> Triple(Icons.Default.History, R.string.resume_scroll, onLastMessageClick)
        InputAction.Stream -> Triple(
            if (isStreamActive) Icons.Default.VideocamOff else Icons.Default.Videocam,
            R.string.toggle_stream,
            onToggleStream,
        )
        InputAction.RoomState -> Triple(Icons.Default.Shield, R.string.menu_room_state, onChangeRoomState)
        InputAction.Fullscreen -> Triple(
            if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
            R.string.toggle_fullscreen,
            onToggleFullscreen,
        )
        InputAction.HideInput -> Triple(Icons.Default.VisibilityOff, R.string.menu_hide_input, onToggleInput)
    }

    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = stringResource(contentDescription),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TooltipScope.TourTooltip(
    text: String,
    onAction: () -> Unit,
    onSkip: () -> Unit,
    isLast: Boolean = false,
) {
    RichTooltip(
        caretShape = TooltipDefaults.caretShape(caretSize = DpSize(24.dp, 12.dp)),
        action = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onSkip) {
                    Text(stringResource(R.string.tour_skip))
                }
                TextButton(onClick = onAction) {
                    Text(stringResource(if (isLast) R.string.tour_got_it else R.string.tour_next))
                }
            }
        }
    ) {
        Text(text)
    }
}

/**
 * Tour tooltip positioned to the start of its anchor, with a right-pointing caret on the end side.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EndCaretTourTooltip(
    text: String,
    onAction: () -> Unit,
    onSkip: () -> Unit,
) {
    val containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = containerColor,
            shadowElevation = 2.dp,
            tonalElevation = 2.dp,
            modifier = Modifier.widthIn(max = 220.dp),
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 12.dp, bottom = 8.dp)
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    TextButton(onClick = onSkip) {
                        Text(stringResource(R.string.tour_skip))
                    }
                    TextButton(onClick = onAction) {
                        Text(stringResource(R.string.tour_next))
                    }
                }
            }
        }
        Canvas(modifier = Modifier.size(width = 12.dp, height = 24.dp)) {
            val path = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, size.height / 2f)
                lineTo(0f, size.height)
                close()
            }
            drawPath(path, containerColor)
        }
    }
}

/**
 * Positions the tooltip to the start (left in LTR) of the anchor, vertically centered.
 * Falls back to above-positioning if there's not enough horizontal space.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun rememberStartAlignedTooltipPositionProvider(
    spacingBetweenTooltipAndAnchor: Dp = 4.dp,
): PopupPositionProvider {
    val spacingPx = with(LocalDensity.current) { spacingBetweenTooltipAndAnchor.roundToPx() }
    return remember(spacingPx) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ): IntOffset {
                val startX = anchorBounds.left - popupContentSize.width - spacingPx
                return if (startX >= 0) {
                    // Fits to the start — vertically center on anchor
                    val y = anchorBounds.top + (anchorBounds.height - popupContentSize.height) / 2
                    IntOffset(
                        startX,
                        y.coerceIn(0, (windowSize.height - popupContentSize.height).coerceAtLeast(0)),
                    )
                } else {
                    // Not enough space — fall back to above, horizontally end-aligned with anchor
                    val x = (anchorBounds.right - popupContentSize.width)
                        .coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))
                    val y = (anchorBounds.top - popupContentSize.height - spacingPx)
                        .coerceIn(0, (windowSize.height - popupContentSize.height).coerceAtLeast(0))
                    IntOffset(x, y)
                }
            }
        }
    }
}
