package com.flxrs.dankchat.ui.chat

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flxrs.dankchat.data.DisplayName
import com.flxrs.dankchat.data.UserId
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.preferences.DankChatPreferenceStore
import com.flxrs.dankchat.preferences.chat.ChatSettingsDataStore
import com.flxrs.dankchat.preferences.chat.UserLongClickBehavior
import com.flxrs.dankchat.ui.chat.emote.EmoteInfoViewModel
import com.flxrs.dankchat.ui.chat.message.MessageOptionsParams
import com.flxrs.dankchat.ui.chat.message.MessageOptionsViewModel
import com.flxrs.dankchat.ui.chat.user.UserPopupStateParams
import com.flxrs.dankchat.ui.chat.user.UserPopupViewModel
import com.flxrs.dankchat.ui.main.input.ChatInputViewModel
import kotlinx.collections.immutable.persistentListOf
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatComposable(
    channel: UserName,
    onReplyClick: (String, UserName) -> Unit,
    modifier: Modifier = Modifier,
    scrollModifier: Modifier = Modifier,
    isCollectionActive: Boolean = true,
    showInput: Boolean = true,
    isFullscreen: Boolean = false,
    showFabs: Boolean = true,
    onRecover: () -> Unit = {},
    fabMenuCallbacks: FabMenuCallbacks? = null,
    contentPadding: PaddingValues = PaddingValues(),
    onScrollToBottom: () -> Unit = {},
    onScrollDirectionChange: (Boolean) -> Unit = {},
    scrollToMessageId: String? = null,
    onScrollToMessageHandle: () -> Unit = {},
    recoveryFabTooltipState: TooltipState? = null,
    onTourAdvance: (() -> Unit)? = null,
    onTourSkip: (() -> Unit)? = null,
) {
    val viewModel: ChatViewModel =
        koinViewModel(
            key = channel.value,
            parameters = { parametersOf(channel) },
        )
    val emoteInfoViewModel: EmoteInfoViewModel = koinViewModel()
    val userPopupViewModel: UserPopupViewModel = koinViewModel()
    val messageOptionsViewModel: MessageOptionsViewModel = koinViewModel()
    val chatInputViewModel: ChatInputViewModel = koinViewModel()
    val chatSettingsDataStore: ChatSettingsDataStore = koinInject()
    val preferenceStore: DankChatPreferenceStore = koinInject()

    if (!isCollectionActive) return

    val messages by viewModel.chatUiStates.collectAsStateWithLifecycle(initialValue = persistentListOf())
    val displaySettings by viewModel.chatDisplaySettings.collectAsStateWithLifecycle()
    val userLongClickBehavior by chatSettingsDataStore.userLongClickBehavior.collectAsStateWithLifecycle(initialValue = UserLongClickBehavior.MentionsUser)
    val isLoggedIn = preferenceStore.isLoggedIn

    ChatScreen(
        messages = messages,
        fontSize = displaySettings.fontSize,
        callbacks =
            ChatScreenCallbacks(
                onUserClick = { userId, userName, displayName, ch, badges, isLongPress ->
                    val shouldOpenPopup =
                        when (userLongClickBehavior) {
                            UserLongClickBehavior.MentionsUser -> !isLongPress
                            UserLongClickBehavior.OpensPopup -> isLongPress
                        }
                    if (shouldOpenPopup) {
                        userPopupViewModel.show(
                            UserPopupStateParams(
                                targetUserId = userId?.let { UserId(it) },
                                targetUserName = UserName(userName),
                                targetDisplayName = DisplayName(displayName),
                                channel = ch?.let { UserName(it) },
                                badges = badges.map { it.badge },
                            ),
                        )
                    } else {
                        chatInputViewModel.mentionUser(UserName(userName), DisplayName(displayName))
                    }
                },
                onMessageLongClick = { messageId, ch, fullMessage ->
                    messageOptionsViewModel.show(
                        MessageOptionsParams(
                            messageId = messageId,
                            channel = ch?.let { UserName(it) },
                            fullMessage = fullMessage,
                            canModerate = isLoggedIn,
                            canReply = isLoggedIn,
                            canCopy = true,
                        ),
                    )
                },
                onEmoteClick = { emoteInfoViewModel.show(it) },
                onReplyClick = onReplyClick,
                onAutomodAllow = { heldMessageId, ch -> viewModel.manageAutomodMessage(heldMessageId, ch, allow = true) },
                onAutomodDeny = { heldMessageId, ch -> viewModel.manageAutomodMessage(heldMessageId, ch, allow = false) },
                onAutomodBanUser = { messageId, ch, fullMessage ->
                    messageOptionsViewModel.show(
                        MessageOptionsParams(
                            messageId = messageId,
                            channel = ch?.let { UserName(it) },
                            fullMessage = fullMessage,
                            canModerate = isLoggedIn,
                            canReply = false,
                            canCopy = false,
                            startWithBan = true,
                        ),
                    )
                },
            ),
        animateGifs = displaySettings.animateGifs,
        fullscreenButtonOpacity = displaySettings.fullscreenButtonOpacity,
        requireFullscreenExitConfirmation = displaySettings.requireFullscreenExitConfirmation,
        fabAnchor = displaySettings.fabAnchor,
        fabOffsetXFraction = displaySettings.fabOffsetXFraction,
        fabOffsetYFraction = displaySettings.fabOffsetYFraction,
        onFabPositionChange = viewModel::persistFabPosition,
        modifier = modifier.fillMaxSize(),
        showInput = showInput,
        isFullscreen = isFullscreen,
        showFabs = showFabs,
        onRecover = onRecover,
        fabMenuCallbacks = fabMenuCallbacks,
        contentPadding = contentPadding,
        scrollModifier = scrollModifier,
        onScrollToBottom = onScrollToBottom,
        onScrollDirectionChange = onScrollDirectionChange,
        scrollToMessageId = scrollToMessageId,
        onScrollToMessageHandle = onScrollToMessageHandle,
        recoveryFabTooltipState = recoveryFabTooltipState,
        onTourAdvance = onTourAdvance,
        onTourSkip = onTourSkip,
    )
}
