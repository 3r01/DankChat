package com.flxrs.dankchat.chat.replies

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flxrs.dankchat.R
import com.flxrs.dankchat.chat.ChatFragment
import com.flxrs.dankchat.chat.compose.BadgeUi
import com.flxrs.dankchat.chat.compose.ChatScreen
import com.flxrs.dankchat.data.DisplayName
import com.flxrs.dankchat.data.UserId
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.twitch.badge.Badge
import com.flxrs.dankchat.main.MainFragment
import com.flxrs.dankchat.preferences.appearance.AppearanceSettingsDataStore
import com.flxrs.dankchat.preferences.chat.UserLongClickBehavior
import com.flxrs.dankchat.theme.DankChatTheme
import com.flxrs.dankchat.utils.extensions.showLongSnackbar
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class RepliesChatFragment : ChatFragment() {
    private val repliesViewModel: RepliesViewModel by viewModel(ownerProducer = { requireParentFragment() })
    private val appearanceSettingsDataStore: AppearanceSettingsDataStore by inject()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val appearanceSettings = appearanceSettingsDataStore.settings.collectAsStateWithLifecycle(initialValue = appearanceSettingsDataStore.current()).value

                @Suppress("MoveVariableDeclarationIntoWhen")
                val uiState = repliesViewModel.uiState.collectAsStateWithLifecycle(initialValue = RepliesUiState.Found(emptyList())).value

                when (uiState) {
                    is RepliesUiState.Found    -> {
                        DankChatTheme {
                            ChatScreen(
                                messages = uiState.items,
                                fontSize = appearanceSettings.fontSize.toFloat(),
                                onUserClick = { userId, userName, displayName, channel, badges, isLongPress ->
                                    onUserClick(
                                        targetUserId = userId?.let { UserId(it) },
                                        targetUserName = UserName(userName),
                                        targetDisplayName = DisplayName(displayName),
                                        channel = channel?.let { UserName(it) },
                                        badges = badges.map(BadgeUi::badge),
                                        isLongPress = isLongPress
                                    )
                                },
                                onMessageLongClick = { messageId, channel, fullMessage ->
                                    onMessageClick(messageId, channel?.let { UserName(it) }, fullMessage)
                                },
                            )
                        }
                    }

                    is RepliesUiState.NotFound -> {
                        // Show error - need to handle this in Compose or use side effect
                        androidx.compose.runtime.LaunchedEffect(Unit) {
                            view?.showLongSnackbar(getString(R.string.reply_thread_not_found))
                        }
                    }
                }
            }
        }
    }

    override fun onUserClick(targetUserId: UserId?, targetUserName: UserName, targetDisplayName: DisplayName, channel: UserName?, badges: List<Badge>, isLongPress: Boolean) {
        targetUserId ?: return
        val shouldLongClickMention = chatSettingsDataStore.current().userLongClickBehavior == UserLongClickBehavior.MentionsUser
        val shouldMention = (isLongPress && shouldLongClickMention) || (!isLongPress && !shouldLongClickMention)

        when {
            shouldMention && dankChatPreferenceStore.isLoggedIn -> (parentFragment?.parentFragment as? MainFragment)?.mentionUser(targetUserName, targetDisplayName)
            else                                                -> (parentFragment?.parentFragment as? MainFragment)?.openUserPopup(
                targetUserId = targetUserId,
                targetUserName = targetUserName,
                targetDisplayName = targetDisplayName,
                channel = channel,
                badges = badges,
                isWhisperPopup = false
            )
        }
    }

    override fun onMessageClick(messageId: String, channel: UserName?, fullMessage: String) {
        (parentFragment?.parentFragment as? MainFragment)?.openMessageSheet(messageId, channel, fullMessage, canReply = false, canModerate = false)
    }
}
