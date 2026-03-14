package com.flxrs.dankchat.main.compose

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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

private sealed interface AppBarMenu {
    data object Main : AppBarMenu
    data object Account : AppBarMenu
    data object Channel : AppBarMenu
    data object Upload : AppBarMenu
    data object More : AppBarMenu
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppBar(
    isLoggedIn: Boolean,
    totalMentionCount: Int,
    onAddChannel: () -> Unit,
    onOpenMentions: () -> Unit,
    onOpenWhispers: () -> Unit,
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
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentMenu by remember { mutableStateOf<AppBarMenu?>(null) }

        TopAppBar(
            title = { Text(stringResource(R.string.app_name)) },        actions = {
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
            IconButton(onClick = { currentMenu = AppBarMenu.Main }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.more)
                )
            }

            DropdownMenu(
                expanded = currentMenu != null,
                onDismissRequest = { currentMenu = null },
                shape = MaterialTheme.shapes.medium
            ) {
                AnimatedContent(
                    targetState = currentMenu,
                    transitionSpec = {
                        if (targetState != AppBarMenu.Main) {
                            (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
                        } else {
                            (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
                        }.using(SizeTransform(clip = false))
                    },
                    label = "MenuTransition"
                ) { menu ->
                    Column {
                        when (menu) {
                            AppBarMenu.Main -> {
                                if (!isLoggedIn) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.login)) },
                                        onClick = {
                                            onLogin()
                                            currentMenu = null
                                        }
                                    )
                                } else {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.account)) },
                                        onClick = { currentMenu = AppBarMenu.Account }
                                    )
                                }

                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.manage_channels)) },
                                    onClick = {
                                        onManageChannels()
                                        currentMenu = null
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.channel)) },
                                    onClick = { currentMenu = AppBarMenu.Channel }
                                )

                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.upload_media)) },
                                    onClick = { currentMenu = AppBarMenu.Upload }
                                )

                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.more)) },
                                    onClick = { currentMenu = AppBarMenu.More }
                                )

                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.settings)) },
                                    onClick = {
                                        onOpenSettings()
                                        currentMenu = null
                                    }
                                )
                            }

                            AppBarMenu.Account -> {
                                SubMenuHeader(title = stringResource(R.string.account), onBack = { currentMenu = AppBarMenu.Main })
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.relogin)) },
                                    onClick = {
                                        onRelogin()
                                        currentMenu = null
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.logout)) },
                                    onClick = {
                                        onLogout()
                                        currentMenu = null
                                    }
                                )
                            }

                            AppBarMenu.Channel -> {
                                SubMenuHeader(title = stringResource(R.string.channel), onBack = { currentMenu = AppBarMenu.Main })
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.open_channel)) },
                                    onClick = {
                                        onOpenChannel()
                                        currentMenu = null
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.remove_channel)) },
                                    onClick = {
                                        onRemoveChannel()
                                        currentMenu = null
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.report_channel)) },
                                    onClick = {
                                        onReportChannel()
                                        currentMenu = null
                                    }
                                )
                                if (isLoggedIn) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.block_channel)) },
                                        onClick = {
                                            onBlockChannel()
                                            currentMenu = null
                                        }
                                    )
                                }
                            }

                            AppBarMenu.Upload -> {
                                SubMenuHeader(title = stringResource(R.string.upload_media), onBack = { currentMenu = AppBarMenu.Main })
                            }

                            AppBarMenu.More -> {
                                SubMenuHeader(title = stringResource(R.string.more), onBack = { currentMenu = AppBarMenu.Main })
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.reload_emotes)) },
                                    onClick = {
                                        onReloadEmotes()
                                        currentMenu = null
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.reconnect)) },
                                    onClick = {
                                        onReconnect()
                                        currentMenu = null
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.clear_chat)) },
                                    onClick = {
                                        onClearChat()
                                        currentMenu = null
                                    }
                                )
                            }
                            
                            null -> {}
                        }
                    }
                }
            }
        },
        modifier = modifier
    )
}

@Composable
private fun SubMenuHeader(title: String, onBack: () -> Unit) {
    DropdownMenuItem(
        text = { 
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            ) 
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        onClick = onBack
    )
}