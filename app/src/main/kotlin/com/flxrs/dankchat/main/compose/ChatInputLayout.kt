package com.flxrs.dankchat.main.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
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
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
    isFullscreen: Boolean,
    isModerator: Boolean,
    isStreamActive: Boolean,
    hasStreamData: Boolean,
    onSend: () -> Unit,
    onLastMessageClick: () -> Unit,
    onEmoteClick: () -> Unit,
    onReplyDismiss: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onToggleInput: () -> Unit,
    onToggleStream: () -> Unit,
    onChangeRoomState: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val hint = when (inputState) {
        InputState.Default -> stringResource(R.string.hint_connected)
        InputState.Replying -> stringResource(R.string.hint_replying)
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

    var quickActionsExpanded by remember { androidx.compose.runtime.mutableStateOf(false) }
    val topEndRadius by androidx.compose.animation.core.animateDpAsState(
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

                // Text Field
                TextField(
                    state = textFieldState,
                    enabled = enabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
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
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 4.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Start
                    )
                }

                // Upload progress indicator
                AnimatedVisibility(
                    visible = isUploading,
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
                    // Emote/Keyboard Button (Left)
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
                        if (isEmoteMenuOpen) {
                            Icon(
                                imageVector = Icons.Default.Keyboard,
                                contentDescription = stringResource(R.string.dialog_dismiss),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.EmojiEmotions,
                                contentDescription = stringResource(R.string.emote_menu_hint),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Quick Actions Button
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

                    // History Button (Always visible)
                    IconButton(
                        onClick = onLastMessageClick,
                        enabled = enabled,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = stringResource(R.string.resume_scroll),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                            if (hasStreamData || isStreamActive) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(if (isStreamActive) R.string.menu_hide_stream else R.string.menu_show_stream)) },
                                    onClick = {
                                        onToggleStream()
                                        quickActionsExpanded = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (isStreamActive) Icons.Default.VideocamOff else Icons.Default.Videocam,
                                            contentDescription = null
                                        )
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(stringResource(if (isFullscreen) R.string.menu_exit_fullscreen else R.string.menu_fullscreen)) },
                                onClick = {
                                    onToggleFullscreen()
                                    quickActionsExpanded = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                        contentDescription = null
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_hide_input)) },
                                onClick = {
                                    onToggleInput()
                                    quickActionsExpanded = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.VisibilityOff,
                                        contentDescription = null
                                    )
                                }
                            )
                            if (isModerator) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.menu_room_state)) },
                                    onClick = {
                                        onChangeRoomState()
                                        quickActionsExpanded = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Shield,
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
    }
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
