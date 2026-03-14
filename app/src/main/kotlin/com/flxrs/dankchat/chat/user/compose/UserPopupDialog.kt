package com.flxrs.dankchat.chat.user.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Report
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.flxrs.dankchat.R
import com.flxrs.dankchat.chat.user.UserPopupComposeViewModel
import com.flxrs.dankchat.chat.user.UserPopupState
import com.flxrs.dankchat.chat.user.UserPopupStateParams
import com.flxrs.dankchat.data.twitch.badge.Badge
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

import com.flxrs.dankchat.chat.compose.BadgeUi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserPopupDialog(
    state: UserPopupState,
    badges: List<BadgeUi>,
    onBlockUser: () -> Unit,
    onUnblockUser: () -> Unit,
    onDismiss: () -> Unit,
    onMention: (String, String) -> Unit,
    onWhisper: (String) -> Unit,
    onOpenChannel: (String) -> Unit,
    onReport: (String) -> Unit,
    onMessageHistory: ((String) -> Unit)? = null,
) {
    var showBlockConfirmation by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (val s = state) {
                is UserPopupState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.padding(horizontal = 16.dp))
                    Text(
                        text = s.userName.formatWithDisplayName(s.displayName),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                is UserPopupState.NotLoggedIn -> {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier
                            .size(96.dp)
                            .padding(horizontal = 16.dp)
                    )
                    Text(
                        text = s.userName.formatWithDisplayName(s.displayName),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                is UserPopupState.Error -> {
                    Text(
                        text = stringResource(R.string.error_with_message, s.throwable?.message.orEmpty()),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                is UserPopupState.Success -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        AsyncImage(
                            model = s.avatarUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .clickable { onOpenChannel(s.userName.value) }
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text(
                                text = s.userName.formatWithDisplayName(s.displayName),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = stringResource(R.string.user_popup_created, s.created),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                            if (s.showFollowingSince) {
                                Text(
                                    text = s.followingSince?.let {
                                        stringResource(R.string.user_popup_following_since, it)
                                    } ?: stringResource(R.string.user_popup_not_following),
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )
                            }
                            if (badges.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(space = 4.dp, alignment = Alignment.CenterHorizontally),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    badges.forEach { badge ->
                                        val title = badge.badge.title
                                        if (title != null) {
                                            TooltipBox(
                                                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                                                tooltip = {
                                                    PlainTooltip {
                                                        Text(title)
                                                    }
                                                },
                                                state = rememberTooltipState(),
                                            ) {
                                                AsyncImage(
                                                    model = badge.url,
                                                    contentDescription = title,
                                                    modifier = Modifier.size(32.dp)
                                                )
                                            }
                                        } else {
                                            AsyncImage(
                                                model = badge.url,
                                                contentDescription = null,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    ListItem(
                        headlineContent = { Text(stringResource(R.string.user_popup_mention)) },
                        leadingContent = { Icon(Icons.Default.AlternateEmail, contentDescription = null) },
                        modifier = Modifier.clickable {
                            onMention(s.userName.value, s.displayName.value)
                            onDismiss()
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.user_popup_whisper)) },
                        leadingContent = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null) },
                        modifier = Modifier.clickable {
                            onWhisper(s.userName.value)
                            onDismiss()
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    if (onMessageHistory != null) {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.message_history)) },
                            leadingContent = { Icon(Icons.Default.History, contentDescription = null) },
                            modifier = Modifier.clickable {
                                onMessageHistory(s.userName.value)
                                onDismiss()
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                    ListItem(
                        headlineContent = { Text(if (s.isBlocked) stringResource(R.string.user_popup_unblock) else stringResource(R.string.user_popup_block)) },
                        leadingContent = { Icon(Icons.Default.Block, contentDescription = null) },
                        modifier = Modifier.clickable {
                            if (s.isBlocked) {
                                onUnblockUser()
                            } else {
                                showBlockConfirmation = true
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.user_popup_report)) },
                        leadingContent = { Icon(Icons.Default.Report, contentDescription = null) },
                        modifier = Modifier.clickable { onReport(s.userName.value) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }
    }

    if (showBlockConfirmation) {
        AlertDialog(
            onDismissRequest = { showBlockConfirmation = false },
            title = { Text(stringResource(R.string.confirm_user_block_title)) },
            text = { Text(stringResource(R.string.confirm_user_block_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onBlockUser()
                        showBlockConfirmation = false
                    }
                ) {
                    Text(stringResource(R.string.confirm_user_block_positive_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlockConfirmation = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }
}
