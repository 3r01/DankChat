package com.flxrs.dankchat.chat.mention

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import com.flxrs.dankchat.chat.ChatFragment
import com.flxrs.dankchat.data.DisplayName
import com.flxrs.dankchat.data.UserId
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.twitch.badge.Badge
import com.flxrs.dankchat.main.MainFragment
import com.flxrs.dankchat.preferences.chat.UserLongClickBehavior
import com.flxrs.dankchat.utils.extensions.collectFlow
import org.koin.androidx.viewmodel.ext.android.viewModel

class MentionChatFragment : ChatFragment() {
    private val args: MentionChatFragmentArgs by navArgs()
    private val mentionViewModel: MentionViewModel by viewModel(ownerProducer = { requireParentFragment() })

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        val chatFlow = when {
            args.isWhisperTab -> mentionViewModel.whispers
            else              -> mentionViewModel.mentions
        }
        collectFlow(chatFlow) { adapter.submitList(it) }
        return view
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
