package com.flxrs.dankchat.main.compose.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
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
    
    // Local state for smooth reordering
    val localChannels = remember { mutableStateListOf<ChannelWithRename>() }
    // Update local channels when external source changes
    LaunchedEffect(channels) {
        localChannels.clear()
        localChannels.addAll(channels)
    }

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
                itemsIndexed(localChannels, key = { _, item -> item.channel.value }) { index, channelWithRename ->
                    var offsetY by remember { mutableFloatStateOf(0f) }
                    var isDragging by remember { mutableStateOf(false) }
                    val itemHeight = remember { mutableIntStateOf(0) }
                    
                    ChannelItem(
                        channelWithRename = channelWithRename,
                        isDragging = isDragging,
                        offsetY = offsetY,
                        onGloballyPositioned = { coordinates ->
                            itemHeight.intValue = coordinates.size.height
                        },
                        onDragStart = { isDragging = true },
                        onDragEnd = { 
                            isDragging = false
                            offsetY = 0f
                            onReorder(localChannels.toList())
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            offsetY += dragAmount.y
                            
                            val height = itemHeight.intValue.toFloat()
                            if (height > 0) {
                                val threshold = height * 0.5f // Swap slightly earlier than full height
                                if (offsetY > threshold && index < localChannels.lastIndex) {
                                    Collections.swap(localChannels, index, index + 1)
                                    offsetY -= height
                                } else if (offsetY < -threshold && index > 0) {
                                    Collections.swap(localChannels, index, index - 1)
                                    offsetY += height
                                }
                            }
                        },
                        onEdit = { channelToEdit = channelWithRename },
                        onDelete = { channelToDelete = channelWithRename.channel }
                    )
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
    isDragging: Boolean,
    offsetY: Float,
    onGloballyPositioned: (androidx.compose.ui.layout.LayoutCoordinates) -> Unit,
    onDragStart: (androidx.compose.ui.geometry.Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDrag: (androidx.compose.ui.input.pointer.PointerInputChange, androidx.compose.ui.geometry.Offset) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val density = LocalDensity.current
    val elevation = if (isDragging) 8.dp else 0.dp
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(if (isDragging) 1f else 0f)
            .graphicsLayer {
                translationY = offsetY
                shadowElevation = with(density) { elevation.toPx() }
            }
            .background(if (isDragging) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surface)
            .onGloballyPositioned(onGloballyPositioned)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_drag_handle),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .pointerInput(Unit) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = onDragStart,
                        onDragEnd = onDragEnd,
                        onDragCancel = onDragEnd,
                        onDrag = onDrag
                    )
                }
                .padding(8.dp)
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