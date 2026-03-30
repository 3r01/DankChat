package com.flxrs.dankchat.ui.main.input

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TooltipScope
import androidx.compose.material3.TooltipState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.flxrs.dankchat.R
import com.flxrs.dankchat.preferences.appearance.InputAction
import com.flxrs.dankchat.ui.main.InputState
import com.flxrs.dankchat.ui.main.QuickActionsMenu
import com.flxrs.dankchat.utils.resolve
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import sh.calvin.reorderable.ReorderableColumn

@Composable
fun ChatInputLayout(
    textFieldState: TextFieldState,
    uiState: ChatInputUiState,
    callbacks: ChatInputCallbacks,
    isSheetOpen: Boolean,
    isUploading: Boolean,
    isLoading: Boolean,
    isFullscreen: Boolean,
    isModerator: Boolean,
    isStreamActive: Boolean,
    hasStreamData: Boolean,
    inputActions: ImmutableList<InputAction>,
    modifier: Modifier = Modifier,
    debugMode: Boolean = false,
    overflowExpanded: Boolean = false,
    onOverflowExpandedChange: (Boolean) -> Unit = {},
    tourState: TourOverlayState = TourOverlayState(),
    isRepeatedSendEnabled: Boolean = false,
) {
    val inputState = uiState.inputState
    val enabled = uiState.enabled
    val hasLastMessage = uiState.hasLastMessage
    val canSend = uiState.canSend
    val isEmoteMenuOpen = uiState.isEmoteMenuOpen
    val helperText = if (isSheetOpen) HelperText() else uiState.helperText
    val overlay = uiState.overlay
    val characterCounter = uiState.characterCounter
    val showQuickActions = !isSheetOpen
    val onSend = callbacks.onSend
    val onLastMessageClick = callbacks.onLastMessageClick
    val onEmoteClick = callbacks.onEmoteClick
    val onOverlayDismiss = callbacks.onOverlayDismiss
    val onToggleFullscreen = callbacks.onToggleFullscreen
    val onToggleInput = callbacks.onToggleInput
    val onToggleStream = callbacks.onToggleStream
    val onModActions = callbacks.onModActions
    val onInputActionsChange = callbacks.onInputActionsChange
    val onSearchClick = callbacks.onSearchClick
    val onDebugInfoClick = callbacks.onDebugInfoClick
    val onNewWhisper = callbacks.onNewWhisper
    val onRepeatedSendChange = callbacks.onRepeatedSendChange

    val focusRequester = remember { FocusRequester() }
    val hint =
        when (inputState) {
            InputState.Default -> stringResource(R.string.hint_connected)
            InputState.Replying -> stringResource(R.string.hint_replying)
            InputState.Announcing -> stringResource(R.string.hint_announcing)
            InputState.Whispering -> stringResource(R.string.hint_whispering)
            InputState.NotLoggedIn -> stringResource(R.string.hint_not_logged_int)
            InputState.Disconnected -> stringResource(R.string.hint_disconnected)
        }

    val textFieldColors =
        TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            errorIndicatorColor = Color.Transparent,
        )
    val defaultColors = TextFieldDefaults.colors()
    val surfaceColor =
        if (enabled) {
            defaultColors.unfocusedContainerColor
        } else {
            defaultColors.disabledContainerColor
        }

    // Filter to actions that would actually render based on current state
    val effectiveActions =
        remember(inputActions, isModerator, hasStreamData, isStreamActive, debugMode) {
            inputActions
                .filter { action ->
                    when (action) {
                        InputAction.Stream -> hasStreamData || isStreamActive
                        InputAction.ModActions -> isModerator
                        InputAction.Debug -> debugMode
                        else -> true
                    }
                }.toImmutableList()
        }

    var visibleActions by remember { mutableStateOf(effectiveActions) }
    val quickActionsExpanded = overflowExpanded || tourState.forceOverflowOpen
    var showConfigSheet by remember { mutableStateOf(false) }
    val topEndRadius by animateDpAsState(
        targetValue = if (quickActionsExpanded) 0.dp else 24.dp,
        label = "topEndCornerRadius",
    )

    val inputContent: @Composable () -> Unit = {
        Surface(
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = topEndRadius),
            color = surfaceColor,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
            ) {
                // Input mode overlay header
                AnimatedVisibility(
                    visible = overlay != InputOverlay.None,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    val headerText =
                        when (overlay) {
                            is InputOverlay.Reply -> stringResource(R.string.reply_header, overlay.name.value)
                            is InputOverlay.Whisper -> stringResource(R.string.whisper_header, overlay.target.value)
                            is InputOverlay.Announce -> stringResource(R.string.mod_actions_announce_header)
                            InputOverlay.None -> ""
                        }
                    InputOverlayHeader(
                        text = headerText,
                        onDismiss = onOverlayDismiss,
                    )
                }

                // Text Field
                TextField(
                    state = textFieldState,
                    enabled = enabled && !tourState.isTourActive,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .padding(bottom = 0.dp),
                    // Reduce bottom padding as actions are below
                    label = { Text(hint) },
                    suffix = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.height(IntrinsicSize.Min),
                        ) {
                            when (characterCounter) {
                                is CharacterCounterState.Hidden -> {
                                    Unit
                                }

                                is CharacterCounterState.Visible -> {
                                    Text(
                                        text = characterCounter.text,
                                        color =
                                            when {
                                                characterCounter.isOverLimit -> MaterialTheme.colorScheme.error
                                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                            }
                            AnimatedVisibility(
                                visible = enabled && textFieldState.text.isNotEmpty(),
                                enter = fadeIn() + expandHorizontally(expandFrom = Alignment.Start),
                                exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.Start),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = stringResource(R.string.dialog_dismiss),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier =
                                        Modifier
                                            .padding(start = 4.dp)
                                            .size(20.dp)
                                            .clickable { textFieldState.clearText() },
                                )
                            }
                        }
                    },
                    colors = textFieldColors,
                    shape = RoundedCornerShape(0.dp),
                    lineLimits =
                        TextFieldLineLimits.MultiLine(
                            minHeightInLines = 1,
                            maxHeightInLines = 5,
                        ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    onKeyboardAction = { if (canSend) onSend() },
                )

                // Helper text (roomstate + live info)
                val resolvedRoomState = helperText.roomStateParts.map { it.resolve() }
                val roomStateText = resolvedRoomState.joinToString(separator = ", ")
                val streamInfoText = helperText.streamInfo
                AnimatedVisibility(
                    visible = !helperText.isEmpty,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    val combinedText = listOfNotNull(roomStateText.ifEmpty { null }, streamInfoText).joinToString(separator = " - ")
                    val textMeasurer = rememberTextMeasurer()
                    val style = MaterialTheme.typography.labelSmall
                    val density = LocalDensity.current
                    BoxWithConstraints(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 4.dp)
                                .animateContentSize(),
                    ) {
                        val maxWidthPx = with(density) { maxWidth.roundToPx() }
                        val fitsOnOneLine =
                            remember(combinedText, style, maxWidthPx) {
                                textMeasurer.measure(combinedText, style).size.width <= maxWidthPx
                            }
                        when {
                            fitsOnOneLine || streamInfoText == null || roomStateText.isEmpty() -> {
                                Text(
                                    text = combinedText,
                                    style = style,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    modifier = Modifier.fillMaxWidth().basicMarquee(),
                                )
                            }

                            else -> {
                                Column {
                                    Text(
                                        text = roomStateText,
                                        style = style,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        modifier = Modifier.fillMaxWidth().basicMarquee(),
                                    )
                                    Text(
                                        text = streamInfoText,
                                        style = style,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        modifier = Modifier.fillMaxWidth().basicMarquee(),
                                    )
                                }
                            }
                        }
                    }
                }

                // Progress indicator for uploads and data loading
                AnimatedVisibility(
                    visible = isUploading || isLoading,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    LinearProgressIndicator(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }

                // Actions Row — uses BoxWithConstraints to hide actions that don't fit
                InputActionsRow(
                    inputActions = inputActions,
                    effectiveActions = effectiveActions,
                    isEmoteMenuOpen = isEmoteMenuOpen,
                    enabled = enabled,
                    showQuickActions = showQuickActions,
                    tourState = tourState,
                    quickActionsExpanded = quickActionsExpanded,
                    canSend = canSend,
                    hasLastMessage = hasLastMessage,
                    isStreamActive = isStreamActive,
                    isFullscreen = isFullscreen,
                    focusRequester = focusRequester,
                    onEmoteClick = onEmoteClick,
                    onOverflowExpandedChange = onOverflowExpandedChange,
                    onNewWhisper = onNewWhisper,
                    onSearchClick = onSearchClick,
                    onLastMessageClick = onLastMessageClick,
                    onToggleStream = onToggleStream,
                    onModActions = onModActions,
                    onToggleFullscreen = onToggleFullscreen,
                    onToggleInput = onToggleInput,
                    onDebugInfoClick = onDebugInfoClick,
                    onSend = onSend,
                    isRepeatedSendEnabled = isRepeatedSendEnabled,
                    onRepeatedSendChange = onRepeatedSendChange,
                    onVisibleActionsChange = { visibleActions = it },
                )
            }
        }
    }

    Box(modifier = modifier.fillMaxWidth()) {
        OptionalTourTooltip(
            tooltipState = tourState.swipeGestureTooltipState,
            text = stringResource(R.string.tour_swipe_gesture),
            onAdvance = tourState.onAdvance,
            onSkip = tourState.onSkip,
        ) {
            inputContent()
        }

        // Overflow menu — overlays above input, end-aligned
        AnimatedVisibility(
            visible = quickActionsExpanded,
            enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
            exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut(),
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        layout(placeable.width, 0) {
                            placeable.placeRelative(0, -placeable.height)
                        }
                    },
        ) {
            var backProgress by remember { mutableFloatStateOf(0f) }
            PredictiveBackHandler { progress ->
                try {
                    progress.collect { event ->
                        backProgress = event.progress
                    }
                    onOverflowExpandedChange(false)
                } catch (_: CancellationException) {
                    backProgress = 0f
                }
            }
            QuickActionsMenu(
                modifier =
                    Modifier.graphicsLayer {
                        val scale = 1f - (backProgress * 0.1f)
                        scaleX = scale
                        scaleY = scale
                        alpha = 1f - backProgress
                    },
                surfaceColor = surfaceColor,
                visibleActions = visibleActions,
                enabled = enabled,
                hasLastMessage = hasLastMessage,
                isStreamActive = isStreamActive,
                hasStreamData = hasStreamData,
                isFullscreen = isFullscreen,
                isModerator = isModerator,
                tourState = tourState,
                onActionClick = { action ->
                    when (action) {
                        InputAction.Search -> onSearchClick()
                        InputAction.LastMessage -> onLastMessageClick()
                        InputAction.Stream -> onToggleStream()
                        InputAction.ModActions -> onModActions()
                        InputAction.Fullscreen -> onToggleFullscreen()
                        InputAction.HideInput -> onToggleInput()
                        InputAction.Debug -> onDebugInfoClick()
                    }
                    onOverflowExpandedChange(false)
                },
                onConfigureClick = {
                    onOverflowExpandedChange(false)
                    showConfigSheet = true
                },
            )
        }
    }

    if (showConfigSheet) {
        InputActionConfigSheet(
            inputActions = inputActions,
            debugMode = debugMode,
            onInputActionsChange = onInputActionsChange,
            onDismiss = { showConfigSheet = false },
        )
    }
}

@Composable
private fun SendButton(
    enabled: Boolean,
    isRepeatedSendEnabled: Boolean,
    onSend: () -> Unit,
    onRepeatedSendChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor =
        when {
            !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            else -> MaterialTheme.colorScheme.primary
        }

    val gestureModifier =
        when {
            enabled && isRepeatedSendEnabled -> {
                Modifier.pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onSend() },
                        onLongPress = { onRepeatedSendChange(true) },
                        onPress = {
                            tryAwaitRelease()
                            onRepeatedSendChange(false)
                        },
                    )
                }
            }

            enabled -> {
                Modifier.clickable(
                    interactionSource = null,
                    indication = null,
                    onClick = onSend,
                )
            }

            else -> {
                Modifier
            }
        }

    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .then(gestureModifier)
                .padding(4.dp),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Send,
            contentDescription = stringResource(R.string.send_hint),
            modifier = Modifier.size(28.dp),
            tint = contentColor,
        )
    }
}

@Composable
private fun InputActionButton(
    action: InputAction,
    enabled: Boolean,
    hasLastMessage: Boolean,
    isStreamActive: Boolean,
    isFullscreen: Boolean,
    onSearchClick: () -> Unit,
    onLastMessageClick: () -> Unit,
    onToggleStream: () -> Unit,
    onModActions: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onToggleInput: () -> Unit,
    modifier: Modifier = Modifier,
    onDebugInfoClick: () -> Unit = {},
) {
    val (icon, contentDescription, onClick) =
        when (action) {
            InputAction.Search -> {
                Triple(Icons.Default.Search, R.string.message_history, onSearchClick)
            }

            InputAction.LastMessage -> {
                Triple(Icons.Default.History, R.string.resume_scroll, onLastMessageClick)
            }

            InputAction.Stream -> {
                Triple(
                    if (isStreamActive) Icons.Default.VideocamOff else Icons.Default.Videocam,
                    R.string.toggle_stream,
                    onToggleStream,
                )
            }

            InputAction.ModActions -> {
                Triple(Icons.Default.Shield, R.string.menu_mod_actions, onModActions)
            }

            InputAction.Fullscreen -> {
                Triple(
                    if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                    R.string.toggle_fullscreen,
                    onToggleFullscreen,
                )
            }

            InputAction.HideInput -> {
                Triple(Icons.Default.VisibilityOff, R.string.menu_hide_input, onToggleInput)
            }

            InputAction.Debug -> {
                Triple(Icons.Default.BugReport, R.string.input_action_debug, onDebugInfoClick)
            }
        }

    val actionEnabled =
        when (action) {
            InputAction.Search, InputAction.Fullscreen, InputAction.HideInput, InputAction.Debug -> true
            InputAction.LastMessage -> enabled && hasLastMessage
            InputAction.Stream, InputAction.ModActions -> enabled
        }

    IconButton(
        onClick = onClick,
        enabled = actionEnabled,
        modifier = modifier,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = stringResource(contentDescription),
        )
    }
}

@Composable
private fun InputOverlayHeader(
    text: String,
    onDismiss: () -> Unit,
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.dialog_dismiss),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        HorizontalDivider(
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InputActionsRow(
    inputActions: ImmutableList<InputAction>,
    effectiveActions: ImmutableList<InputAction>,
    isEmoteMenuOpen: Boolean,
    enabled: Boolean,
    showQuickActions: Boolean,
    tourState: TourOverlayState,
    quickActionsExpanded: Boolean,
    canSend: Boolean,
    hasLastMessage: Boolean,
    isStreamActive: Boolean,
    isFullscreen: Boolean,
    focusRequester: FocusRequester,
    onEmoteClick: () -> Unit,
    onOverflowExpandedChange: (Boolean) -> Unit,
    onNewWhisper: (() -> Unit)?,
    onSearchClick: () -> Unit,
    onLastMessageClick: () -> Unit,
    onToggleStream: () -> Unit,
    onModActions: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onToggleInput: () -> Unit,
    onSend: () -> Unit,
    onVisibleActionsChange: (ImmutableList<InputAction>) -> Unit,
    onDebugInfoClick: () -> Unit = {},
    isRepeatedSendEnabled: Boolean = false,
    onRepeatedSendChange: (Boolean) -> Unit = {},
) {
    BoxWithConstraints(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
    ) {
        val iconSize = 40.dp
        // Fixed slots: emote + overflow + send (+ whisper if present)
        val fixedSlots = 1 + (if (showQuickActions) 1 else 0) + (if (onNewWhisper != null) 1 else 0) + 1
        val availableForActions = maxWidth - iconSize * fixedSlots
        val maxVisibleActions = (availableForActions / iconSize).toInt().coerceAtLeast(0)
        val allActions = inputActions.take(maxVisibleActions).toImmutableList()
        val visibleActions = effectiveActions.take(maxVisibleActions).toImmutableList()
        onVisibleActionsChange(visibleActions)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Emote/Keyboard Button (start-aligned, always visible)
            IconButton(
                onClick = {
                    if (isEmoteMenuOpen) {
                        focusRequester.requestFocus()
                    }
                    onEmoteClick()
                },
                enabled = enabled && !tourState.isTourActive,
                modifier = Modifier.size(iconSize),
            ) {
                Icon(
                    imageVector = if (isEmoteMenuOpen) Icons.Default.Keyboard else Icons.Default.EmojiEmotions,
                    contentDescription =
                        stringResource(
                            if (isEmoteMenuOpen) R.string.dialog_dismiss else R.string.emote_menu_hint,
                        ),
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // End-aligned group: overflow + actions + whisper + send
            OptionalTourTooltip(
                tooltipState = tourState.inputActionsTooltipState,
                text = stringResource(R.string.tour_input_actions),
                onAdvance = tourState.onAdvance,
                onSkip = tourState.onSkip,
                focusable = true,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EndAlignedActionGroup(
                        allActions = allActions,
                        visibleActions = visibleActions,
                        iconSize = iconSize,
                        showQuickActions = showQuickActions,
                        tourState = tourState,
                        quickActionsExpanded = quickActionsExpanded,
                        canSend = canSend,
                        enabled = enabled,
                        hasLastMessage = hasLastMessage,
                        isStreamActive = isStreamActive,
                        isFullscreen = isFullscreen,
                        onOverflowExpandedChange = onOverflowExpandedChange,
                        onNewWhisper = onNewWhisper,
                        onSearchClick = onSearchClick,
                        onLastMessageClick = onLastMessageClick,
                        onToggleStream = onToggleStream,
                        onModActions = onModActions,
                        onToggleFullscreen = onToggleFullscreen,
                        onToggleInput = onToggleInput,
                        onDebugInfoClick = onDebugInfoClick,
                        onSend = onSend,
                        isRepeatedSendEnabled = isRepeatedSendEnabled,
                        onRepeatedSendChange = onRepeatedSendChange,
                    )
                }
            }
        }
    }
}

@Suppress("MultipleEmitters")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EndAlignedActionGroup(
    allActions: ImmutableList<InputAction>,
    visibleActions: ImmutableList<InputAction>,
    iconSize: Dp,
    showQuickActions: Boolean,
    tourState: TourOverlayState,
    quickActionsExpanded: Boolean,
    canSend: Boolean,
    enabled: Boolean,
    hasLastMessage: Boolean,
    isStreamActive: Boolean,
    isFullscreen: Boolean,
    onOverflowExpandedChange: (Boolean) -> Unit,
    onNewWhisper: (() -> Unit)?,
    onSearchClick: () -> Unit,
    onLastMessageClick: () -> Unit,
    onToggleStream: () -> Unit,
    onModActions: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onToggleInput: () -> Unit,
    onSend: () -> Unit,
    onDebugInfoClick: () -> Unit = {},
    isRepeatedSendEnabled: Boolean = false,
    onRepeatedSendChange: (Boolean) -> Unit = {},
) {
    // Overflow Button (leading the end-aligned group)
    if (showQuickActions) {
        val overflowButton: @Composable () -> Unit = {
            IconButton(
                onClick = {
                    if (tourState.overflowMenuTooltipState != null) {
                        tourState.onAdvance?.invoke()
                    } else {
                        onOverflowExpandedChange(!quickActionsExpanded)
                    }
                },
                modifier = Modifier.size(iconSize),
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.more),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        OptionalTourTooltip(
            tooltipState = tourState.overflowMenuTooltipState,
            text = stringResource(R.string.tour_overflow_menu),
            onAdvance = tourState.onAdvance,
            onSkip = tourState.onSkip,
        ) {
            overflowButton()
        }
    }

    // New Whisper Button (only on whisper tab)
    if (onNewWhisper != null) {
        IconButton(
            onClick = onNewWhisper,
            modifier = Modifier.size(iconSize),
        ) {
            Icon(
                imageVector = Icons.Default.AddComment,
                contentDescription = stringResource(R.string.whisper_new),
            )
        }
    }

    // Configurable action icons with animated visibility
    for (action in allActions) {
        AnimatedVisibility(
            visible = action in visibleActions,
            enter = expandHorizontally() + fadeIn(),
            exit = shrinkHorizontally() + fadeOut(),
        ) {
            InputActionButton(
                action = action,
                enabled = enabled,
                hasLastMessage = hasLastMessage,
                isStreamActive = isStreamActive,
                isFullscreen = isFullscreen,
                onSearchClick = onSearchClick,
                onLastMessageClick = onLastMessageClick,
                onToggleStream = onToggleStream,
                onModActions = onModActions,
                onToggleFullscreen = onToggleFullscreen,
                onToggleInput = onToggleInput,
                onDebugInfoClick = onDebugInfoClick,
                modifier = Modifier.size(iconSize),
            )
        }
    }

    // Send Button (Right)
    Spacer(modifier = Modifier.width(4.dp))
    SendButton(
        enabled = canSend,
        isRepeatedSendEnabled = isRepeatedSendEnabled,
        onSend = onSend,
        onRepeatedSendChange = onRepeatedSendChange,
        modifier = Modifier.size(44.dp),
    )
}
