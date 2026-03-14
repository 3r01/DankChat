package com.flxrs.dankchat.main.compose

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.flxrs.dankchat.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppBar(
    isLoggedIn: Boolean,
    totalMentionCount: Int,
    onAddChannel: () -> Unit,
    onOpenMentions: () -> Unit,
    onLogin: () -> Unit,
    onRelogin: () -> Unit,
    onLogout: () -> Unit,
    onManageChannels: () -> Unit,
    onOpenChannel: () -> Unit,
    onRemoveChannel: () -> Unit,
    onReportChannel: () -> Unit,
    onBlockChannel: () -> Unit,
    onReloadEmotes: () -> Unit,
    onReconnect: () -> Unit,
    onClearChat: () -> Unit,
    onCaptureImage: () -> Unit,
    onCaptureVideo: () -> Unit,
    onChooseMedia: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    var showChannelMenu by remember { mutableStateOf(false) }
    var showAccountMenu by remember { mutableStateOf(false) }
    var showUploadMenu by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = { Text("DankChat") },
        actions = {
            // Add channel button (always visible)
            IconButton(onClick = onAddChannel) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.add_channel)
                )
            }

            IconButton(onClick = onOpenMentions) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = stringResource(R.string.mentions_title),
                    tint = if (totalMentionCount > 0) {
                        MaterialTheme.colorScheme.error
                    } else {
                        LocalContentColor.current
                    }
                )
            }

            // Overflow menu
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.more)
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                // Login/Account section
                if (!isLoggedIn) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.login)) },
                        onClick = {
                            onLogin()
                            showMenu = false
                        }
                    )
                } else {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.account)) },
                        onClick = {
                            showAccountMenu = true
                        }
                    )

                    DropdownMenu(
                        expanded = showAccountMenu,
                        onDismissRequest = { showAccountMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.relogin)) },
                            onClick = {
                                onRelogin()
                                showAccountMenu = false
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.logout)) },
                            onClick = {
                                onLogout()
                                showAccountMenu = false
                                showMenu = false
                            }
                        )
                    }
                }

                // Manage channels
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.manage_channels)) },
                    onClick = {
                        onManageChannels()
                        showMenu = false
                    }
                )

                // Channel submenu
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.channel)) },
                    onClick = {
                        showChannelMenu = true
                    }
                )

                DropdownMenu(
                    expanded = showChannelMenu,
                    onDismissRequest = { showChannelMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.open_channel)) },
                        onClick = {
                            onOpenChannel()
                            showChannelMenu = false
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.remove_channel)) },
                        onClick = {
                            onRemoveChannel()
                            showChannelMenu = false
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.report_channel)) },
                        onClick = {
                            onReportChannel()
                            showChannelMenu = false
                            showMenu = false
                        }
                    )
                    if (isLoggedIn) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.block_channel)) },
                            onClick = {
                                onBlockChannel()
                                showChannelMenu = false
                                showMenu = false
                            }
                        )
                    }
                }

                // Upload media submenu
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.upload_media)) },
                    onClick = {
                        showUploadMenu = true
                    }
                )

                DropdownMenu(
                    expanded = showUploadMenu,
                    onDismissRequest = { showUploadMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.take_picture)) },
                        onClick = {
                            onCaptureImage()
                            showUploadMenu = false
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.record_video)) },
                        onClick = {
                            onCaptureVideo()
                            showUploadMenu = false
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.choose_media)) },
                        onClick = {
                            onChooseMedia()
                            showUploadMenu = false
                            showMenu = false
                        }
                    )
                }

                // More submenu
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.more)) },
                    onClick = {
                        showMoreMenu = true
                    }
                )

                DropdownMenu(
                    expanded = showMoreMenu,
                    onDismissRequest = { showMoreMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.reload_emotes)) },
                        onClick = {
                            onReloadEmotes()
                            showMoreMenu = false
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.reconnect)) },
                        onClick = {
                            onReconnect()
                            showMoreMenu = false
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.clear_chat)) },
                        onClick = {
                            onClearChat()
                            showMoreMenu = false
                            showMenu = false
                        }
                    )
                }

                // Settings
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.settings)) },
                    onClick = {
                        onOpenSettings()
                        showMenu = false
                    }
                )
            }
        },
        modifier = modifier
    )
}
