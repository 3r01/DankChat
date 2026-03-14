package com.flxrs.dankchat.main.compose.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.flxrs.dankchat.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageOptionsDialog(
    messageId: String,
    channel: String?,
    fullMessage: String,
    canModerate: Boolean,
    canReply: Boolean,
    canCopy: Boolean,
    hasReplyThread: Boolean,
    onReply: () -> Unit,
    onViewThread: () -> Unit,
    onCopy: () -> Unit,
    onMoreActions: () -> Unit,
    onDelete: () -> Unit,
    onTimeout: (index: Int) -> Unit,
    onBan: () -> Unit,
    onUnban: () -> Unit,
    onDismiss: () -> Unit,
) {
    var showTimeoutDialog by remember { mutableStateOf(false) }
    var showBanDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            if (canReply) {
                MessageOptionItem(
                    icon = Icons.AutoMirrored.Filled.Reply,
                    text = stringResource(R.string.message_reply),
                    onClick = {
                        onReply()
                        onDismiss()
                    }
                )
            }
            if (hasReplyThread) {
                MessageOptionItem(
                    icon = Icons.AutoMirrored.Filled.Reply, // Using same icon for thread view
                    text = stringResource(R.string.message_view_thread),
                    onClick = {
                        onViewThread()
                        onDismiss()
                    }
                )
            }
            
            if (canCopy) {
                MessageOptionItem(
                    icon = Icons.Default.ContentCopy,
                    text = stringResource(R.string.message_copy),
                    onClick = {
                        onCopy()
                        onDismiss()
                    }
                )

                MessageOptionItem(
                    icon = Icons.Default.MoreVert,
                    text = stringResource(R.string.message_more_actions),
                    onClick = {
                        onMoreActions()
                        // Don't call onDismiss() here if the state management in MainScreen
                        // handles switching sheets, but the user said "it closes the current sheet"
                        // If we are using ModalBottomSheet, opening another one usually dismisses the first.
                        onDismiss()
                    }
                )
            }

            if (canModerate) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                MessageOptionItem(
                    icon = Icons.Default.Timer,
                    text = stringResource(R.string.user_popup_timeout),
                    onClick = { showTimeoutDialog = true }
                )
                
                MessageOptionItem(
                    icon = Icons.Default.Delete,
                    text = stringResource(R.string.user_popup_delete),
                    onClick = { showDeleteDialog = true }
                )
                
                MessageOptionItem(
                    icon = Icons.Default.Gavel,
                    text = stringResource(R.string.user_popup_ban),
                    onClick = { showBanDialog = true }
                )
                
                MessageOptionItem(
                    icon = Icons.Default.Gavel, // Using same icon for unban
                    text = stringResource(R.string.user_popup_unban),
                    onClick = {
                        onUnban()
                        onDismiss()
                    }
                )
            }
        }
    }

    if (showTimeoutDialog) {
        TimeoutConfirmDialog(
            onConfirm = { index ->
                onTimeout(index)
                showTimeoutDialog = false
                onDismiss()
            },
            onDismiss = { showTimeoutDialog = false }
        )
    }

    if (showBanDialog) {
        ConfirmationDialog(
            title = stringResource(R.string.confirm_user_ban_question),
            confirmText = stringResource(R.string.confirm_user_ban_positive_button),
            onConfirm = {
                onBan()
                showBanDialog = false
                onDismiss()
            },
            onDismiss = { showBanDialog = false },
        )
    }

    if (showDeleteDialog) {
        ConfirmationDialog(
            title = stringResource(R.string.confirm_user_delete_question),
            confirmText = stringResource(R.string.confirm_user_delete_positive_button),
            onConfirm = {
                onDelete()
                showDeleteDialog = false
                onDismiss()
            },
            onDismiss = { showDeleteDialog = false },
        )
    }
}

@Composable
private fun MessageOptionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(text) },
        leadingContent = { Icon(icon, contentDescription = null) },
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun TimeoutConfirmDialog(
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val choices = stringArrayResource(R.array.timeout_entries)
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    val currentIndex = sliderPosition.toInt()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.confirm_user_timeout_title)) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = choices[currentIndex],
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Slider(
                    value = sliderPosition,
                    onValueChange = { sliderPosition = it },
                    valueRange = 0f..(choices.size - 1).toFloat(),
                    steps = choices.size - 2
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(currentIndex) }) {
                Text(stringResource(R.string.confirm_user_timeout_positive_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        }
    )
}
