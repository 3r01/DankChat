package com.flxrs.dankchat.chat.compose

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.LocalPlatformContext
import coil3.imageLoader
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.twitch.emote.ChatMessageEmote
import com.flxrs.dankchat.preferences.appearance.AppearanceSettingsDataStore
import com.flxrs.dankchat.preferences.chat.ChatSettingsDataStore
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Standalone composable for chat display.
 * Extracted from ChatFragment to enable pure Compose integration.
 * 
 * This composable:
 * - Creates its own ChatComposeViewModel scoped to the channel
 * - Collects messages from ViewModel
 * - Collects settings from data stores
 * - Renders ChatScreen with all event handlers
 */
@Composable
fun ChatComposable(
    channel: UserName,
    onUserClick: (userId: String?, userName: String, displayName: String, channel: String?, badges: List<BadgeUi>, isLongPress: Boolean) -> Unit,
    onMessageLongClick: (messageId: String, channel: String?, fullMessage: String) -> Unit,
    onEmoteClick: (List<ChatMessageEmote>) -> Unit,
    onReplyClick: (String, UserName) -> Unit,
    modifier: Modifier = Modifier,
    showInput: Boolean = true,
    isFullscreen: Boolean = false,
    hasHelperText: Boolean = false,
    onRecover: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(),
    scrollModifier: Modifier = Modifier,
    onScrollToBottom: () -> Unit = {},
    onScrollDirectionChanged: (Boolean) -> Unit = {},
) {
    // Create ChatComposeViewModel with channel-specific key for proper scoping
    val viewModel: ChatComposeViewModel = koinViewModel(
        key = channel.value,
        parameters = { parametersOf(channel) }
    )

    val appearanceSettingsDataStore: AppearanceSettingsDataStore = koinInject()
    val chatSettingsDataStore: ChatSettingsDataStore = koinInject()
    
    val messages by viewModel.chatUiStates.collectAsStateWithLifecycle(initialValue = emptyList())
    val appearanceSettings by appearanceSettingsDataStore.settings.collectAsStateWithLifecycle(initialValue = appearanceSettingsDataStore.current())
    val chatSettings by chatSettingsDataStore.settings.collectAsStateWithLifecycle(initialValue = chatSettingsDataStore.current())

    // Create singleton coordinator using the app's ImageLoader (with disk cache, AnimatedImageDecoder, etc.)
    val context = LocalPlatformContext.current
    val emoteCoordinator = rememberEmoteAnimationCoordinator(context.imageLoader)

    CompositionLocalProvider(LocalEmoteAnimationCoordinator provides emoteCoordinator) {
    ChatScreen(
        messages = messages,
        fontSize = appearanceSettings.fontSize.toFloat(),
        showLineSeparator = appearanceSettings.lineSeparator,
        animateGifs = chatSettings.animateGifs,
        modifier = modifier.fillMaxSize(),
        onUserClick = onUserClick,
        onMessageLongClick = onMessageLongClick,
        onEmoteClick = onEmoteClick,
        onReplyClick = onReplyClick,
        showInput = showInput,
        isFullscreen = isFullscreen,
        hasHelperText = hasHelperText,
        onRecover = onRecover,
        contentPadding = contentPadding,
        scrollModifier = scrollModifier,
        onScrollToBottom = onScrollToBottom,
        onScrollDirectionChanged = onScrollDirectionChanged
    )
    } // CompositionLocalProvider
}
