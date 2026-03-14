package com.flxrs.dankchat.chat.mention.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flxrs.dankchat.chat.compose.BadgeUi
import com.flxrs.dankchat.chat.compose.ChatScreen
import com.flxrs.dankchat.chat.mention.MentionViewModel
import com.flxrs.dankchat.data.twitch.emote.ChatMessageEmote
import com.flxrs.dankchat.preferences.appearance.AppearanceSettingsDataStore

/**
 * Standalone composable for mentions/whispers display.
 * Extracted from MentionChatFragment to enable pure Compose integration.
 * 
 * This composable:
 * - Collects mentions or whispers from MentionViewModel based on isWhisperTab
 * - Collects appearance settings
 * - Renders ChatScreen with channel prefix for mentions only
 */
@Composable
fun MentionComposable(
    mentionViewModel: MentionViewModel,
    appearanceSettingsDataStore: AppearanceSettingsDataStore,
    isWhisperTab: Boolean,
    onUserClick: (userId: String?, userName: String, displayName: String, channel: String?, badges: List<BadgeUi>, isLongPress: Boolean) -> Unit,
    onMessageLongClick: (messageId: String, channel: String?, fullMessage: String) -> Unit,
    onEmoteClick: (List<ChatMessageEmote>) -> Unit,
    modifier: Modifier = Modifier
) {
    val appearanceSettings by appearanceSettingsDataStore.settings.collectAsStateWithLifecycle(initialValue = appearanceSettingsDataStore.current())
    val messages by when {
        isWhisperTab -> mentionViewModel.whispersUiStates.collectAsStateWithLifecycle(initialValue = emptyList())
        else         -> mentionViewModel.mentionsUiStates.collectAsStateWithLifecycle(initialValue = emptyList())
    }
    
    ChatScreen(
        messages = messages,
        fontSize = appearanceSettings.fontSize.toFloat(),
        showChannelPrefix = !isWhisperTab,
        modifier = modifier,
        onUserClick = onUserClick,
        onMessageLongClick = onMessageLongClick,
        onEmoteClick = onEmoteClick
    )
}
