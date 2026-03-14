package com.flxrs.dankchat.main.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.flxrs.dankchat.chat.compose.BadgeUi
import com.flxrs.dankchat.chat.mention.compose.MentionComposeViewModel
import com.flxrs.dankchat.chat.message.compose.MessageOptionsParams
import com.flxrs.dankchat.chat.user.UserPopupStateParams
import com.flxrs.dankchat.data.DisplayName
import com.flxrs.dankchat.data.UserId
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.twitch.emote.ChatMessageEmote
import com.flxrs.dankchat.main.compose.sheets.MentionSheet
import com.flxrs.dankchat.main.compose.sheets.RepliesSheet
import com.flxrs.dankchat.preferences.appearance.AppearanceSettingsDataStore

@Composable
fun FullScreenSheetOverlay(
    sheetState: FullScreenSheetState,
    isLoggedIn: Boolean,
    mentionViewModel: MentionComposeViewModel,
    appearanceSettingsDataStore: AppearanceSettingsDataStore,
    onDismiss: () -> Unit,
    onDismissReplies: () -> Unit,
    onUserClick: (UserPopupStateParams) -> Unit,
    onMessageLongClick: (MessageOptionsParams) -> Unit,
    onEmoteClick: (List<ChatMessageEmote>) -> Unit,
    onWhisperReply: (UserName) -> Unit = {},
    bottomContentPadding: Dp = 0.dp,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = sheetState !is FullScreenSheetState.Closed,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            val userClickHandler: (String?, String, String, String?, List<BadgeUi>, Boolean) -> Unit = { userId, userName, displayName, channel, badges, _ ->
                onUserClick(
                    UserPopupStateParams(
                        targetUserId = userId?.let { UserId(it) } ?: UserId(""),
                        targetUserName = UserName(userName),
                        targetDisplayName = DisplayName(displayName),
                        channel = channel?.let { UserName(it) },
                        badges = badges.map { it.badge }
                    )
                )
            }

            when (sheetState) {
                is FullScreenSheetState.Closed -> Unit
                is FullScreenSheetState.Mention -> {
                    MentionSheet(
                        mentionViewModel = mentionViewModel,
                        initialisWhisperTab = false,
                        appearanceSettingsDataStore = appearanceSettingsDataStore,
                        onDismiss = onDismiss,
                        onUserClick = userClickHandler,
                        onMessageLongClick = { messageId, channel, fullMessage ->
                            onMessageLongClick(
                                MessageOptionsParams(
                                    messageId = messageId,
                                    channel = channel?.let { UserName(it) },
                                    fullMessage = fullMessage,
                                    canModerate = isLoggedIn,
                                    canReply = isLoggedIn,
                                    canCopy = false
                                )
                            )
                        },
                        onEmoteClick = onEmoteClick,
                        onWhisperReply = onWhisperReply,
                        bottomContentPadding = bottomContentPadding,
                    )
                }
                is FullScreenSheetState.Whisper -> {
                    MentionSheet(
                        mentionViewModel = mentionViewModel,
                        initialisWhisperTab = true,
                        appearanceSettingsDataStore = appearanceSettingsDataStore,
                        onDismiss = onDismiss,
                        onUserClick = userClickHandler,
                        onMessageLongClick = { messageId, channel, fullMessage ->
                            onMessageLongClick(
                                MessageOptionsParams(
                                    messageId = messageId,
                                    channel = channel?.let { UserName(it) },
                                    fullMessage = fullMessage,
                                    canModerate = isLoggedIn,
                                    canReply = isLoggedIn,
                                    canCopy = false
                                )
                            )
                        },
                        onEmoteClick = onEmoteClick,
                        onWhisperReply = onWhisperReply,
                        bottomContentPadding = bottomContentPadding,
                    )
                }
                is FullScreenSheetState.Replies -> {
                    RepliesSheet(
                        rootMessageId = sheetState.replyMessageId,
                        appearanceSettingsDataStore = appearanceSettingsDataStore,
                        onDismiss = onDismissReplies,
                        onUserClick = userClickHandler,
                        onMessageLongClick = { messageId, channel, fullMessage ->
                            onMessageLongClick(
                                MessageOptionsParams(
                                    messageId = messageId,
                                    channel = channel?.let { UserName(it) },
                                    fullMessage = fullMessage,
                                    canModerate = isLoggedIn,
                                    canReply = isLoggedIn,
                                    canCopy = true
                                )
                            )
                        },
                        bottomContentPadding = bottomContentPadding,
                    )
                }
            }
        }
    }
}
