package com.flxrs.dankchat.main.compose.dialogs

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.flxrs.dankchat.R
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.preferences.model.ChannelWithRename
import java.util.Collections

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageChannelsDialog(
    channels: List<ChannelWithRename>,
    onRemoveChannel: (UserName) -> Unit,
    onRenameChannel: (UserName, String?) -> Unit,
    onReorder: (List<ChannelWithRename>) -> Unit,
    onDismiss: () -> Unit,
) {
    var channelToDelete by remember { mutableStateOf<UserName?>(null) }
    var channelToEdit by remember { mutableStateOf<ChannelWithRename?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.manage_channels),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                state = rememberLazyListState()
            ) {
                itemsIndexed(channels, key = { _, item -> item.channel.value }) { index, channelWithRename ->
                    ChannelItem(
                        channelWithRename = channelWithRename,
                        onEdit = { channelToEdit = channelWithRename },
                        onDelete = { channelToDelete = channelWithRename.channel },
                        onMoveUp = if (index > 0) {
                            {
                                val newList = channels.toMutableList()
                                Collections.swap(newList, index, index - 1)
                                onReorder(newList)
                            }
                        } else null,
                        onMoveDown = if (index < channels.size - 1) {
                            {
                                val newList = channels.toMutableList()
                                Collections.swap(newList, index, index + 1)
                                onReorder(newList)
                            }
                        } else null
                    )
                }
                
                if (channels.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.no_channels_added),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }

    if (channelToDelete != null) {
        AlertDialog(
            onDismissRequest = { channelToDelete = null },
            title = { Text(stringResource(R.string.confirm_channel_removal_title)) },
            text = { Text(stringResource(R.string.confirm_channel_removal_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        channelToDelete?.let(onRemoveChannel)
                        channelToDelete = null
                    }
                ) {
                    Text(stringResource(R.string.confirm_channel_removal_positive_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { channelToDelete = null }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }

    channelToEdit?.let { channel ->
        EditChannelDialog(
            channelWithRename = channel,
            onRename = onRenameChannel,
            onDismiss = { channelToEdit = null }
        )
    }
}

@Composable
private fun ChannelItem(
    channelWithRename: ChannelWithRename,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?
) {
    var showReorderMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            IconButton(onClick = { showReorderMenu = true }) {
                Icon(
                    painter = painterResource(R.drawable.ic_drag_handle),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            DropdownMenu(
                expanded = showReorderMenu,
                onDismissRequest = { showReorderMenu = false }
            ) {
                if (onMoveUp != null) {
                    DropdownMenuItem(
                        text = { Text("Move Up") },
                        onClick = {
                            onMoveUp()
                            showReorderMenu = false
                        }
                    )
                }
                if (onMoveDown != null) {
                    DropdownMenuItem(
                        text = { Text("Move Down") },
                        onClick = {
                            onMoveDown()
                            showReorderMenu = false
                        }
                    )
                }
            }
        }

        Text(
            text = buildAnnotatedString {
                append(channelWithRename.rename?.value ?: channelWithRename.channel.value)
                if (channelWithRename.rename != null && channelWithRename.rename != channelWithRename.channel) {
                    append(" ")
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), fontStyle = FontStyle.Italic)) {
                        append(channelWithRename.channel.value)
                    }
                }
            },
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f).padding(8.dp)
        )

        IconButton(onClick = onEdit) {
            Icon(
                painter = painterResource(R.drawable.ic_edit),
                contentDescription = stringResource(R.string.edit_dialog_title)
            )
        }

        IconButton(onClick = onDelete) {
            Icon(
                painter = painterResource(R.drawable.ic_delete_outline),
                contentDescription = stringResource(R.string.remove_channel)
            )
        }
    }
}