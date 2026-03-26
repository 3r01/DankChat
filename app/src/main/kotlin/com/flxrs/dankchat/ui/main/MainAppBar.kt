package com.flxrs.dankchat.ui.main

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.flxrs.dankchat.R
import kotlinx.coroutines.CancellationException

sealed interface AppBarMenu {
    data object Main : AppBarMenu
    data object Upload : AppBarMenu
    data object Channel : AppBarMenu
}

@Composable
fun InlineOverflowMenu(
    isLoggedIn: Boolean,
    onDismiss: () -> Unit,
    initialMenu: AppBarMenu = AppBarMenu.Main,
    onAction: (ToolbarAction) -> Unit,
) {
    var currentMenu by remember(initialMenu) { mutableStateOf(initialMenu) }
    var backProgress by remember { mutableFloatStateOf(0f) }

    PredictiveBackHandler { progress ->
        try {
            progress.collect { event ->
                backProgress = event.progress
            }
            when (currentMenu) {
                AppBarMenu.Main -> onDismiss()
                else            -> {
                    backProgress = 0f
                    currentMenu = AppBarMenu.Main
                }
            }
        } catch (_: CancellationException) {
            backProgress = 0f
        }
    }

    AnimatedContent(
        targetState = currentMenu,
        transitionSpec = {
            if (targetState != AppBarMenu.Main) {
                (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
            } else {
                (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
            }.using(SizeTransform(clip = false))
        },
        label = "InlineMenuTransition",
        modifier = Modifier.graphicsLayer {
            val scale = 1f - (backProgress * 0.1f)
            scaleX = scale
            scaleY = scale
            alpha = 1f - backProgress
        },
    ) { menu ->
        val density = LocalDensity.current
        val screenHeight = with(density) { LocalView.current.height.toDp() }
        Column(
            modifier = Modifier
                .width(200.dp)
                .heightIn(max = screenHeight * 0.5f)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
        ) {
                when (menu) {
                AppBarMenu.Main    -> {
                    if (!isLoggedIn) {
                        InlineMenuItem(text = stringResource(R.string.login), icon = Icons.AutoMirrored.Filled.Login) { onAction(ToolbarAction.Login); onDismiss() }
                    } else {
                        InlineMenuItem(text = stringResource(R.string.relogin), icon = Icons.Default.Refresh) { onAction(ToolbarAction.Relogin); onDismiss() }
                        InlineMenuItem(text = stringResource(R.string.logout), icon = Icons.AutoMirrored.Filled.Logout) { onAction(ToolbarAction.Logout); onDismiss() }
                    }

                    HorizontalDivider()

                    InlineMenuItem(text = stringResource(R.string.manage_channels), icon = Icons.Default.EditNote) { onAction(ToolbarAction.ManageChannels); onDismiss() }
                    InlineMenuItem(text = stringResource(R.string.remove_channel), icon = Icons.Default.RemoveCircleOutline) { onAction(ToolbarAction.RemoveChannel); onDismiss() }
                    InlineMenuItem(text = stringResource(R.string.reload_emotes), icon = Icons.Default.EmojiEmotions) { onAction(ToolbarAction.ReloadEmotes); onDismiss() }
                    InlineMenuItem(text = stringResource(R.string.reconnect), icon = Icons.Default.Autorenew) { onAction(ToolbarAction.Reconnect); onDismiss() }

                    HorizontalDivider()

                    InlineMenuItem(text = stringResource(R.string.upload_media), icon = Icons.Default.CloudUpload, hasSubMenu = true) { currentMenu = AppBarMenu.Upload }
                    InlineMenuItem(text = stringResource(R.string.channel), icon = Icons.Default.Info, hasSubMenu = true) { currentMenu = AppBarMenu.Channel }

                    HorizontalDivider()

                    InlineMenuItem(text = stringResource(R.string.settings), icon = Icons.Default.Settings) { onAction(ToolbarAction.OpenSettings); onDismiss() }
                }

                AppBarMenu.Upload  -> {
                    InlineSubMenuHeader(title = stringResource(R.string.upload_media), onBack = { currentMenu = AppBarMenu.Main })
                    InlineMenuItem(text = stringResource(R.string.take_picture), icon = Icons.Default.CameraAlt) { onAction(ToolbarAction.CaptureImage); onDismiss() }
                    InlineMenuItem(text = stringResource(R.string.record_video), icon = Icons.Default.Videocam) { onAction(ToolbarAction.CaptureVideo); onDismiss() }
                    InlineMenuItem(text = stringResource(R.string.choose_media), icon = Icons.Default.Image) { onAction(ToolbarAction.ChooseMedia); onDismiss() }
                }

                AppBarMenu.Channel -> {
                    InlineSubMenuHeader(title = stringResource(R.string.channel), onBack = { currentMenu = AppBarMenu.Main })
                    InlineMenuItem(text = stringResource(R.string.open_channel), icon = Icons.Default.OpenInBrowser) { onAction(ToolbarAction.OpenChannel); onDismiss() }
                    InlineMenuItem(text = stringResource(R.string.report_channel), icon = Icons.Default.Flag) { onAction(ToolbarAction.ReportChannel); onDismiss() }
                    if (isLoggedIn) {
                        InlineMenuItem(text = stringResource(R.string.block_channel), icon = Icons.Default.Block) { onAction(ToolbarAction.BlockChannel); onDismiss() }
                    }
                    InlineMenuItem(text = stringResource(R.string.clear_chat), icon = Icons.Default.DeleteSweep) { onAction(ToolbarAction.ClearChat); onDismiss() }
                }
            }
        }
    }
}

@Composable
private fun InlineMenuItem(text: String, icon: ImageVector, hasSubMenu: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (hasSubMenu) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
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
            .padding(horizontal = 12.dp, vertical = 8.dp),
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
