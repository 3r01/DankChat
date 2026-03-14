package com.flxrs.dankchat.chat.mention

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.navArgs
import com.flxrs.dankchat.chat.ChatFragment
import com.flxrs.dankchat.chat.compose.ChatScreen
import com.flxrs.dankchat.data.DisplayName
import com.flxrs.dankchat.data.UserId
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.twitch.badge.Badge
import com.flxrs.dankchat.data.twitch.emote.ChatMessageEmote
import com.flxrs.dankchat.main.MainFragment
import com.flxrs.dankchat.preferences.appearance.AppearanceSettingsDataStore
import com.flxrs.dankchat.preferences.chat.UserLongClickBehavior
import com.flxrs.dankchat.theme.DankChatTheme
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class MentionChatFragment : ChatFragment() {
    private val args: MentionChatFragmentArgs by navArgs()
    private val mentionViewModel: MentionViewModel by viewModel(ownerProducer = { requireParentFragment() })
    private val appearanceSettingsDataStore: AppearanceSettingsDataStore by inject()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val appearanceSettings = appearanceSettingsDataStore.settings.collectAsStateWithLifecycle(initialValue = appearanceSettingsDataStore.current()).value
                val messages by when {
                    args.isWhisperTab -> mentionViewModel.whispersUiStates.collectAsStateWithLifecycle(initialValue = emptyList())
                    else              -> mentionViewModel.mentionsUiStates.collectAsStateWithLifecycle(initialValue = emptyList())
                }
                DankChatTheme {
                    ChatScreen(
                        messages = messages,
                        fontSize = appearanceSettings.fontSize.toFloat(),
                        showChannelPrefix = !args.isWhisperTab, // Only show for mentions, not whispers
                        onUserClick = { userId, userName, displayName, channel, badges, isLongPress ->
                            onUserClick(
                                targetUserId = userId?.let { UserId(it) },
                                targetUserName = UserName(userName),
                                targetDisplayName = DisplayName(displayName),
                                channel = channel?.let { UserName(it) },
                                badges = badges.filterIsInstance<Badge>(),
                                isLongPress = isLongPress
                            )
                        },
                        onMessageLongClick = { messageId, channel, fullMessage ->
                            onMessageClick(messageId, channel?.let { UserName(it) }, fullMessage)
                        },
                        onEmoteClick = {
                            val chatEmotes = it.filterIsInstance<ChatMessageEmote>()
                            (parentFragment?.parentFragment as? MainFragment)?.openEmoteSheet(chatEmotes)
                        }
                    )
                }
            }
        }
    }

    override fun onUserClick(
        targetUserId: UserId?,
        targetUserName: UserName,
        targetDisplayName: DisplayName,
        channel: UserName?,
        badges: List<Badge>,
        isLongPress: Boolean
    ) {
        targetUserId ?: return
        val shouldLongClickMention = chatSettingsDataStore.current().userLongClickBehavior == UserLongClickBehavior.MentionsUser
        val shouldMention = (isLongPress && shouldLongClickMention) || (!isLongPress && !shouldLongClickMention)

        when {
            shouldMention && dankChatPreferenceStore.isLoggedIn -> (parentFragment?.parentFragment as? MainFragment)?.whisperUser(targetUserName)
            else                                                -> (parentFragment?.parentFragment as? MainFragment)?.openUserPopup(
                targetUserId = targetUserId,
                targetUserName = targetUserName,
                targetDisplayName = targetDisplayName,
                channel = null,
                badges = badges,
                isWhisperPopup = true
            )
        }
    }

    override fun onMessageClick(messageId: String, channel: UserName?, fullMessage: String) {
        (parentFragment?.parentFragment as? MainFragment)?.openMessageSheet(messageId, channel, fullMessage, canReply = false, canModerate = false)
    }

    companion object {
        fun newInstance(isWhisperTab: Boolean = false) = MentionChatFragment().apply {
            arguments = MentionChatFragmentArgs(isWhisperTab).toBundle()
        }
    }
}
