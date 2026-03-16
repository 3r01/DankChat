package com.flxrs.dankchat.main.compose.dialogs

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ManageChannelsDialog(
    channels: List<ChannelWithRename>,
    onApplyChanges: (List<ChannelWithRename>) -> Unit,
    onChannelSelected: (UserName) -> Unit,
    onDismiss: () -> Unit,
) {
    var channelToDelete by remember { mutableStateOf<UserName?>(null) }
    var channelToEdit by remember { mutableStateOf<ChannelWithRename?>(null) }

    // Local state for smooth reordering and deferred updates
    val localChannels = remember { mutableStateListOf<ChannelWithRename>() }
    LaunchedEffect(channels) {
        if (localChannels.isEmpty() && channels.isNotEmpty()) {
            localChannels.addAll(channels)
        }
    }

    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        if (from.index in localChannels.indices && to.index in localChannels.indices) {
            localChannels.apply {
                add(to.index, removeAt(from.index))
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            onApplyChanges(localChannels.toList())
            onDismiss()
        },
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            state = lazyListState
        ) {
            itemsIndexed(localChannels, key = { _, it -> it.channel.value }) { index, channelWithRename ->
                ReorderableItem(reorderableState, key = channelWithRename.channel.value) { isDragging ->
                    val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp)

                    Surface(
                        shadowElevation = elevation,
                        color = if (isDragging) MaterialTheme.colorScheme.surfaceContainerHighest else Color.Transparent
                    ) {
                        Column {
                            ChannelItem(
                                channelWithRename = channelWithRename,
                                modifier = Modifier.longPressDraggableHandle(
                                    onDragStarted = { /* Optional haptic feedback here */ },
                                    onDragStopped = { /* Optional haptic feedback here */ }
                                ),
                                onNavigate = {
                                    onApplyChanges(localChannels.toList())
                                    onChannelSelected(channelWithRename.channel)
                                    onDismiss()
                                },
                                onEdit = { channelToEdit = channelWithRename },
                                onDelete = { channelToDelete = channelWithRename.channel }
                            )
                            if (index < localChannels.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }

            if (localChannels.isEmpty()) {
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

    if (channelToDelete != null) {
        ConfirmationDialog(
            title = stringResource(R.string.confirm_channel_removal_question),
            confirmText = stringResource(R.string.confirm_channel_removal_positive_button),
            onConfirm = {
                val channel = channelToDelete
                if (channel != null) {
                    localChannels.removeIf { it.channel == channel }
                }
                channelToDelete = null
            },
            onDismiss = { channelToDelete = null },
        )
    }

    channelToEdit?.let { channel ->
        EditChannelDialog(
            channelWithRename = channel,
            onRename = { userName, newName ->
                val index = localChannels.indexOfFirst { it.channel == userName }
                if (index != -1) {
                    val rename = newName?.ifBlank { null }?.let { UserName(it) }
                    localChannels[index] = localChannels[index].copy(rename = rename)
                }
            },
            onDismiss = { channelToEdit = null }
        )
    }
}

@Composable
private fun ChannelItem(
    channelWithRename: ChannelWithRename,
    modifier: Modifier = Modifier, // This modifier will carry the drag handle semantics
    onNavigate: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_drag_handle),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(8.dp)
        )

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
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        )

        IconButton(onClick = onNavigate) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = stringResource(R.string.open_channel)
            )
        }

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
