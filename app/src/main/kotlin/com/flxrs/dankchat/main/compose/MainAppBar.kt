package com.flxrs.dankchat.main.compose

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.flxrs.dankchat.R

sealed interface AppBarMenu {
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
    onCaptureImage: () -> Unit,
    onCaptureVideo: () -> Unit,
    onChooseMedia: () -> Unit,
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
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.take_picture)) },
                                    onClick = {
                                        onCaptureImage()
                                        currentMenu = null
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.record_video)) },
                                    onClick = {
                                        onCaptureVideo()
                                        currentMenu = null
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.choose_media)) },
                                    onClick = {
                                        onChooseMedia()
                                        currentMenu = null
                                    }
                                )
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
fun ToolbarOverflowMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    isLoggedIn: Boolean,
    onLogin: () -> Unit,
    onRelogin: () -> Unit,
    onLogout: () -> Unit,
    onManageChannels: () -> Unit,
    onOpenChannel: () -> Unit,
    onRemoveChannel: () -> Unit,
    onReportChannel: () -> Unit,
    onBlockChannel: () -> Unit,
    onCaptureImage: () -> Unit,
    onCaptureVideo: () -> Unit,
    onChooseMedia: () -> Unit,
    onReloadEmotes: () -> Unit,
    onReconnect: () -> Unit,
    onClearChat: () -> Unit,
    onOpenSettings: () -> Unit,
    shape: Shape = MaterialTheme.shapes.medium,
    offset: DpOffset = DpOffset.Zero,
) {
    var currentMenu by remember { mutableStateOf<AppBarMenu?>(AppBarMenu.Main) }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = {
            onDismiss()
            currentMenu = AppBarMenu.Main
        },
        shape = shape,
        offset = offset
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
                                onClick = { onLogin(); onDismiss() }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.account)) },
                                onClick = { currentMenu = AppBarMenu.Account }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.manage_channels)) },
                            onClick = { onManageChannels(); onDismiss() }
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
                            onClick = { onOpenSettings(); onDismiss() }
                        )
                    }
                    AppBarMenu.Account -> {
                        SubMenuHeader(title = stringResource(R.string.account), onBack = { currentMenu = AppBarMenu.Main })
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.relogin)) },
                            onClick = { onRelogin(); onDismiss() }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.logout)) },
                            onClick = { onLogout(); onDismiss() }
                        )
                    }
                    AppBarMenu.Channel -> {
                        SubMenuHeader(title = stringResource(R.string.channel), onBack = { currentMenu = AppBarMenu.Main })
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.open_channel)) },
                            onClick = { onOpenChannel(); onDismiss() }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.remove_channel)) },
                            onClick = { onRemoveChannel(); onDismiss() }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.report_channel)) },
                            onClick = { onReportChannel(); onDismiss() }
                        )
                        if (isLoggedIn) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.block_channel)) },
                                onClick = { onBlockChannel(); onDismiss() }
                            )
                        }
                    }
                    AppBarMenu.Upload -> {
                        SubMenuHeader(title = stringResource(R.string.upload_media), onBack = { currentMenu = AppBarMenu.Main })
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.take_picture)) },
                            onClick = { onCaptureImage(); onDismiss() }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.record_video)) },
                            onClick = { onCaptureVideo(); onDismiss() }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.choose_media)) },
                            onClick = { onChooseMedia(); onDismiss() }
                        )
                    }
                    AppBarMenu.More -> {
                        SubMenuHeader(title = stringResource(R.string.more), onBack = { currentMenu = AppBarMenu.Main })
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.reload_emotes)) },
                            onClick = { onReloadEmotes(); onDismiss() }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.reconnect)) },
                            onClick = { onReconnect(); onDismiss() }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.clear_chat)) },
                            onClick = { onClearChat(); onDismiss() }
                        )
                    }
                    null -> {}
                }
            }
        }
    }
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

@Composable
fun InlineOverflowMenu(
    isLoggedIn: Boolean,
    isStreamActive: Boolean = false,
    hasStreamData: Boolean = false,
    onDismiss: () -> Unit,
    onLogin: () -> Unit,
    onRelogin: () -> Unit,
    onLogout: () -> Unit,
    onManageChannels: () -> Unit,
    onOpenChannel: () -> Unit,
    onRemoveChannel: () -> Unit,
    onReportChannel: () -> Unit,
    onBlockChannel: () -> Unit,
    onCaptureImage: () -> Unit,
    onCaptureVideo: () -> Unit,
    onChooseMedia: () -> Unit,
    onReloadEmotes: () -> Unit,
    onReconnect: () -> Unit,
    onClearChat: () -> Unit,
    onToggleStream: () -> Unit = {},
    onOpenSettings: () -> Unit,
    initialMenu: AppBarMenu = AppBarMenu.Main,
) {
    var currentMenu by remember(initialMenu) { mutableStateOf(initialMenu) }

    AnimatedContent(
        targetState = currentMenu,
        transitionSpec = {
            if (targetState != AppBarMenu.Main) {
                (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
            } else {
                (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
            }.using(SizeTransform(clip = false))
        },
        label = "InlineMenuTransition"
    ) { menu ->
        Column {
            when (menu) {
                AppBarMenu.Main -> {
                    if (!isLoggedIn) {
                        InlineMenuItem(text = stringResource(R.string.login)) { onLogin(); onDismiss() }
                    } else {
                        InlineMenuItem(text = stringResource(R.string.account), hasSubMenu = true) { currentMenu = AppBarMenu.Account }
                    }
                    InlineMenuItem(text = stringResource(R.string.manage_channels)) { onManageChannels(); onDismiss() }
                    InlineMenuItem(text = stringResource(R.string.channel), hasSubMenu = true) { currentMenu = AppBarMenu.Channel }
                    InlineMenuItem(text = stringResource(R.string.upload_media), hasSubMenu = true) { currentMenu = AppBarMenu.Upload }
                    InlineMenuItem(text = stringResource(R.string.more), hasSubMenu = true) { currentMenu = AppBarMenu.More }
                    InlineMenuItem(text = stringResource(R.string.settings)) { onOpenSettings(); onDismiss() }
                }
                AppBarMenu.Account -> {
                    InlineSubMenuHeader(title = stringResource(R.string.account), onBack = { currentMenu = AppBarMenu.Main })
                    InlineMenuItem(text = stringResource(R.string.relogin)) { onRelogin(); onDismiss() }
                    InlineMenuItem(text = stringResource(R.string.logout)) { onLogout(); onDismiss() }
                }
                AppBarMenu.Channel -> {
                    InlineSubMenuHeader(title = stringResource(R.string.channel), onBack = { currentMenu = AppBarMenu.Main })
                    if (hasStreamData || isStreamActive) {
                        InlineMenuItem(text = stringResource(if (isStreamActive) R.string.menu_hide_stream else R.string.menu_show_stream)) { onToggleStream(); onDismiss() }
                    }
                    InlineMenuItem(text = stringResource(R.string.open_channel)) { onOpenChannel(); onDismiss() }
                    InlineMenuItem(text = stringResource(R.string.remove_channel)) { onRemoveChannel(); onDismiss() }
                    InlineMenuItem(text = stringResource(R.string.report_channel)) { onReportChannel(); onDismiss() }
                    if (isLoggedIn) {
                        InlineMenuItem(text = stringResource(R.string.block_channel)) { onBlockChannel(); onDismiss() }
                    }
                }
                AppBarMenu.Upload -> {
                    InlineSubMenuHeader(title = stringResource(R.string.upload_media), onBack = { currentMenu = AppBarMenu.Main })
                    InlineMenuItem(text = stringResource(R.string.take_picture)) { onCaptureImage(); onDismiss() }
                    InlineMenuItem(text = stringResource(R.string.record_video)) { onCaptureVideo(); onDismiss() }
                    InlineMenuItem(text = stringResource(R.string.choose_media)) { onChooseMedia(); onDismiss() }
                }
                AppBarMenu.More -> {
                    InlineSubMenuHeader(title = stringResource(R.string.more), onBack = { currentMenu = AppBarMenu.Main })
                    InlineMenuItem(text = stringResource(R.string.reload_emotes)) { onReloadEmotes(); onDismiss() }
                    InlineMenuItem(text = stringResource(R.string.reconnect)) { onReconnect(); onDismiss() }
                    InlineMenuItem(text = stringResource(R.string.clear_chat)) { onClearChat(); onDismiss() }
                }
            }
        }
    }
}

@Composable
private fun InlineMenuItem(text: String, hasSubMenu: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (hasSubMenu) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InlineSubMenuHeader(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onBack)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}