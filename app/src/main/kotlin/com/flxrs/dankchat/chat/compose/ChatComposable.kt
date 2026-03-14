package com.flxrs.dankchat.chat.compose

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flxrs.dankchat.chat.ChatViewModel
import com.flxrs.dankchat.data.twitch.emote.ChatMessageEmote
import com.flxrs.dankchat.preferences.appearance.AppearanceSettingsDataStore
import com.flxrs.dankchat.preferences.chat.ChatSettingsDataStore

/**
 * Standalone composable for chat display.
 * Extracted from ChatFragment to enable pure Compose integration.
 * 
 * This composable:
 * - Collects messages from ChatViewModel
 * - Collects settings from data stores
 * - Renders ChatScreen with all event handlers
 */
@Composable
fun ChatComposable(
    chatViewModel: ChatViewModel,
    appearanceSettingsDataStore: AppearanceSettingsDataStore,
    chatSettingsDataStore: ChatSettingsDataStore,
    onUserClick: (userId: String?, userName: String, displayName: String, channel: String?, badges: List<BadgeUi>, isLongPress: Boolean) -> Unit,
    onMessageLongClick: (messageId: String, channel: String?, fullMessage: String) -> Unit,
    onEmoteClick: (List<ChatMessageEmote>) -> Unit,
    onReplyClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val messages by chatViewModel.chatUiStates.collectAsStateWithLifecycle(initialValue = emptyList())
    val appearanceSettings by appearanceSettingsDataStore.settings.collectAsStateWithLifecycle(initialValue = appearanceSettingsDataStore.current())
    val chatSettings by chatSettingsDataStore.settings.collectAsStateWithLifecycle(initialValue = chatSettingsDataStore.current())
    
    ChatScreen(
        messages = messages,
        fontSize = appearanceSettings.fontSize.toFloat(),
        showLineSeparator = appearanceSettings.lineSeparator,
        animateGifs = chatSettings.animateGifs,
        modifier = modifier.fillMaxSize(),
        onUserClick = onUserClick,
        onMessageLongClick = onMessageLongClick,
        onEmoteClick = onEmoteClick,
        onReplyClick = onReplyClick
    )
}
