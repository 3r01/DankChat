package com.flxrs.dankchat.main.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.flxrs.dankchat.R
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.main.InputState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ChatInputLayout(
    textFieldState: TextFieldState,
    inputState: InputState,
    enabled: Boolean,
    canSend: Boolean,
    showReplyOverlay: Boolean,
    replyName: UserName?,
    onSend: () -> Unit,
    onLastMessageClick: () -> Unit,
    onEmoteClick: () -> Unit,
    onReplyDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hint = when (inputState) {
        InputState.Default -> stringResource(R.string.hint_connected)
        InputState.Replying -> stringResource(R.string.hint_replying)
        InputState.NotLoggedIn -> stringResource(R.string.hint_not_logged_int)
        InputState.Disconnected -> stringResource(R.string.hint_disconnected)
    }

    Surface(
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.fillMaxWidth()
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 0.dp), // Reduce bottom padding as actions are below
                label = { Text(hint) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    errorIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(0.dp),
                lineLimits = TextFieldLineLimits.MultiLine(
                    minHeightInLines = 1,
                    maxHeightInLines = 5
                )
            )

            // Actions Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
            ) {
                // Emote Button (Left)
                IconButton(
                    onClick = onEmoteClick,
                    enabled = enabled,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEmotions,
                        contentDescription = stringResource(R.string.emote_menu_hint),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // History Button (Always visible)
                IconButton(
                    onClick = onLastMessageClick,
                    enabled = enabled,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = stringResource(R.string.resume_scroll), // Using resume_scroll as a placeholder for "History/Last message" context
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
