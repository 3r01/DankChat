package com.flxrs.dankchat.ui.main.dialog

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.flxrs.dankchat.R

private enum class MessageOptionsSubView {
    Timeout,
    Ban,
    Delete,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageOptionsDialog(
    messageId: String,
    channel: String?,
    fullMessage: String,
    canModerate: Boolean,
    canReply: Boolean,
    canCopy: Boolean,
    canJump: Boolean,
    hasReplyThread: Boolean,
    onReply: () -> Unit,
    onReplyToOriginal: () -> Unit,
    onJumpToMessage: () -> Unit,
    onViewThread: () -> Unit,
    onCopy: () -> Unit,
    onCopyFullMessage: () -> Unit,
    onCopyMessageId: () -> Unit,
    onDelete: () -> Unit,
    onTimeout: (index: Int) -> Unit,
    onBan: () -> Unit,
    onUnban: () -> Unit,
    onDismiss: () -> Unit,
) {
    var subView by remember { mutableStateOf<MessageOptionsSubView?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        AnimatedContent(
            targetState = subView,
            transitionSpec = {
                when {
                    targetState != null -> slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                    else                -> slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                }
            },
            label = "MessageOptionsContent"
        ) { currentView ->
            when (currentView) {
                null           -> MessageOptionsMainView(
                    canReply = canReply,
                    canJump = canJump,
                    canCopy = canCopy,
                    canModerate = canModerate,
                    hasReplyThread = hasReplyThread,
                    channel = channel,
                    onReply = { onReply(); onDismiss() },
                    onReplyToOriginal = { onReplyToOriginal(); onDismiss() },
                    onJumpToMessage = { onJumpToMessage(); onDismiss() },
                    onViewThread = { onViewThread(); onDismiss() },
                    onCopy = { onCopy(); onDismiss() },
                    onCopyFullMessage = { onCopyFullMessage(); onDismiss() },
                    onCopyMessageId = { onCopyMessageId(); onDismiss() },
                    onUnban = { onUnban(); onDismiss() },
                    onTimeout = { subView = MessageOptionsSubView.Timeout },
                    onBan = { subView = MessageOptionsSubView.Ban },
                    onDelete = { subView = MessageOptionsSubView.Delete },
                )

                MessageOptionsSubView.Timeout -> TimeoutSubView(
                    onConfirm = { index ->
                        onTimeout(index)
                        onDismiss()
                    },
                    onBack = { subView = null },
                )

                MessageOptionsSubView.Ban     -> ConfirmationSubView(
                    title = stringResource(R.string.confirm_user_ban_message),
                    confirmText = stringResource(R.string.confirm_user_ban_positive_button),
                    onConfirm = {
                        onBan()
                        onDismiss()
                    },
                    onBack = { subView = null },
                )

                MessageOptionsSubView.Delete  -> ConfirmationSubView(
                    title = stringResource(R.string.confirm_user_delete_message),
                    confirmText = stringResource(R.string.confirm_user_delete_positive_button),
                    onConfirm = {
                        onDelete()
                        onDismiss()
                    },
                    onBack = { subView = null },
                )
            }
        }
    }
}

@Composable
private fun MessageOptionsMainView(
    canReply: Boolean,
    canJump: Boolean,
    canCopy: Boolean,
    canModerate: Boolean,
    hasReplyThread: Boolean,
    channel: String?,
    onReply: () -> Unit,
    onReplyToOriginal: () -> Unit,
    onJumpToMessage: () -> Unit,
    onViewThread: () -> Unit,
    onCopy: () -> Unit,
    onCopyFullMessage: () -> Unit,
    onCopyMessageId: () -> Unit,
    onUnban: () -> Unit,
    onTimeout: () -> Unit,
    onBan: () -> Unit,
    onDelete: () -> Unit,
) {
    var moreExpanded by remember { mutableStateOf(false) }
    val arrowRotation by animateFloatAsState(
        targetValue = if (moreExpanded) 180f else 0f,
        label = "arrowRotation",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        if (canReply) {
            MessageOptionItem(Icons.AutoMirrored.Filled.Reply, stringResource(R.string.message_reply), onReply)
        }
        if (canReply && hasReplyThread) {
            MessageOptionItem(Icons.AutoMirrored.Filled.Reply, stringResource(R.string.message_reply_original), onReplyToOriginal)
            MessageOptionItem(Icons.AutoMirrored.Filled.Reply, stringResource(R.string.message_view_thread), onViewThread)
        }
        if (canJump && channel != null) {
            MessageOptionItem(Icons.AutoMirrored.Filled.OpenInNew, stringResource(R.string.message_jump_to), onJumpToMessage)
        }
        if (canCopy) {
            MessageOptionItem(Icons.Default.ContentCopy, stringResource(R.string.message_copy), onCopy)
            ListItem(
                headlineContent = { Text(stringResource(R.string.message_more_actions)) },
                leadingContent = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                trailingContent = {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.rotate(arrowRotation),
                    )
                },
                modifier = Modifier.clickable { moreExpanded = !moreExpanded },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
            AnimatedVisibility(
                visible = moreExpanded,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column {
                    MessageOptionItem(Icons.Default.ContentCopy, stringResource(R.string.message_copy_full), onCopyFullMessage)
                    MessageOptionItem(Icons.Default.ContentCopy, stringResource(R.string.message_copy_id), onCopyMessageId)
                }
            }
        }
        if (canModerate) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            MessageOptionItem(Icons.Default.Timer, stringResource(R.string.user_popup_timeout), onTimeout)
            MessageOptionItem(Icons.Default.Delete, stringResource(R.string.user_popup_delete), onDelete)
            MessageOptionItem(Icons.Default.Gavel, stringResource(R.string.user_popup_ban), onBan)
            MessageOptionItem(Icons.Default.Gavel, stringResource(R.string.user_popup_unban), onUnban)
        }
    }
}

@Composable
private fun MessageOptionItem(
    icon: ImageVector,
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
private fun TimeoutSubView(
    onConfirm: (Int) -> Unit,
    onBack: () -> Unit,
) {
    val choices = stringArrayResource(R.array.timeout_entries)
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    val currentIndex = sliderPosition.toInt()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
    ) {
        Text(
            text = stringResource(R.string.confirm_user_timeout_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        Text(
            text = choices[currentIndex],
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 8.dp),
        )

        Slider(
            value = sliderPosition,
            onValueChange = { sliderPosition = it },
            valueRange = 0f..(choices.size - 1).toFloat(),
            steps = choices.size - 2,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
        ) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.dialog_cancel))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(onClick = { onConfirm(currentIndex) }, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.confirm_user_timeout_positive_button))
            }
        }
    }
}

@Composable
private fun ConfirmationSubView(
    title: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 16.dp),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
        ) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.dialog_cancel))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(onClick = onConfirm, modifier = Modifier.weight(1f)) {
                Text(confirmText)
            }
        }
    }
}
