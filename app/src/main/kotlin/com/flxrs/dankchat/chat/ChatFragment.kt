package com.flxrs.dankchat.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.flxrs.dankchat.data.DisplayName
import com.flxrs.dankchat.data.UserId
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.repo.emote.EmoteRepository
import com.flxrs.dankchat.data.twitch.badge.Badge
import com.flxrs.dankchat.databinding.ChatFragmentBinding
import com.flxrs.dankchat.main.MainFragment
import com.flxrs.dankchat.main.MainViewModel
import com.flxrs.dankchat.preferences.DankChatPreferenceStore
import com.flxrs.dankchat.preferences.appearance.AppearanceSettingsDataStore
import com.flxrs.dankchat.preferences.chat.ChatSettingsDataStore
import com.flxrs.dankchat.preferences.chat.UserLongClickBehavior
import com.flxrs.dankchat.preferences.developer.DeveloperSettingsDataStore
import com.flxrs.dankchat.utils.extensions.collectFlow
import com.flxrs.dankchat.utils.insets.TranslateDeferringInsetsAnimationCallback
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

open class ChatFragment : Fragment() {
    protected val viewModel: ChatViewModel by viewModel()
    private val mainViewModel: MainViewModel by viewModel(ownerProducer = { requireParentFragment() })
    private val emoteRepository: EmoteRepository by inject()
    protected val appearanceSettingsDataStore: AppearanceSettingsDataStore by inject()
    private val developerSettingsDataStore: DeveloperSettingsDataStore by inject()
    protected val chatSettingsDataStore: ChatSettingsDataStore by inject()
    protected val dankChatPreferenceStore: DankChatPreferenceStore by inject()

    protected var bindingRef: ChatFragmentBinding? = null
    protected val binding get() = bindingRef!!
    protected open lateinit var adapter: ChatAdapter
    protected open lateinit var manager: LinearLayoutManager

    protected open var isAtBottom = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        bindingRef = ChatFragmentBinding.inflate(inflater, container, false).apply {
            chatLayout.layoutTransition?.setAnimateParentHierarchy(false)
            scrollBottom.setOnClickListener {
                scrollBottom.visibility = View.GONE
                mainViewModel.isScrolling(false)
                isAtBottom = true
                binding.chat.stopScroll()
                scrollToPosition(position = adapter.itemCount - 1)
            }
        }

        collectFlow(viewModel.chat) { adapter.submitList(it) }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val itemDecoration = DividerItemDecoration(view.context, LinearLayoutManager.VERTICAL)
        manager = LinearLayoutManager(view.context, RecyclerView.VERTICAL, false).apply { stackFromEnd = true }
        adapter = ChatAdapter(
            emoteRepository = emoteRepository,
            dankChatPreferenceStore = dankChatPreferenceStore,
            chatSettingsDataStore = chatSettingsDataStore,
            developerSettingsDataStore = developerSettingsDataStore,
            appearanceSettingsDataStore = appearanceSettingsDataStore,
            onListChanged = ::scrollToPosition,
            onUserClick = ::onUserClick,
            onMessageLongClick = ::onMessageClick,
            onReplyClick = ::onReplyClick,
            onEmoteClick = { emotes ->
                (parentFragment as? MainFragment)?.openEmoteSheet(emotes)
            },
        ).apply { stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY }
        binding.chat.setup(adapter, manager)
        ViewCompat.setWindowInsetsAnimationCallback(
            binding.chat,
            TranslateDeferringInsetsAnimationCallback(
                view = binding.chat,
                persistentInsetTypes = WindowInsetsCompat.Type.systemBars(),
                deferredInsetTypes = WindowInsetsCompat.Type.ime(),
            )
        )

        collectFlow(appearanceSettingsDataStore.lineSeparator) {
            when {
                it && binding.chat.itemDecorationCount == 0  -> binding.chat.addItemDecoration(itemDecoration)
                !it && binding.chat.itemDecorationCount == 1 -> binding.chat.removeItemDecoration(itemDecoration)
            }
        }
        collectFlow(chatSettingsDataStore.restartChat) {
            binding.chat.swapAdapter(adapter, false)
        }
    }

    override fun onDestroyView() {
        binding.chat.adapter = null
        binding.chat.layoutManager = null
        bindingRef = null
        super.onDestroyView()
    }

    protected open fun onUserClick(
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
            shouldMention && dankChatPreferenceStore.isLoggedIn -> (parentFragment as? MainFragment)?.mentionUser(targetUserName, targetDisplayName)
            else                                                -> (parentFragment as? MainFragment)?.openUserPopup(
                targetUserId = targetUserId,
                targetUserName = targetUserName,
                targetDisplayName = targetDisplayName,
                channel = channel,
                badges = badges,
                isWhisperPopup = false
            )
        }
    }

    protected open fun onMessageClick(messageId: String, channel: UserName?, fullMessage: String) {
        (parentFragment as? MainFragment)?.openMessageSheet(messageId, channel, fullMessage, canReply = true, canModerate = true)
    }

    private fun onReplyClick(rootMessageId: String) {
        (parentFragment as? MainFragment)?.openReplies(rootMessageId)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        savedInstanceState?.let {
            isAtBottom = it.getBoolean(AT_BOTTOM_STATE)
            binding.scrollBottom.isVisible = !isAtBottom
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(AT_BOTTOM_STATE, isAtBottom)
    }

    protected open fun scrollToPosition(position: Int) {
        bindingRef ?: return
        if (position > 0 && isAtBottom) {
            manager.scrollToPositionWithOffset(position, 0)
        }
    }

    private fun RecyclerView.setup(chatAdapter: ChatAdapter, manager: LinearLayoutManager) {
        setItemViewCacheSize(OFFSCREEN_VIEW_CACHE_SIZE)
        adapter = chatAdapter
        layoutManager = manager
        itemAnimator = null
        isNestedScrollingEnabled = false
        addOnScrollListener(ChatScrollListener())
    }

    private inner class ChatScrollListener : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            if (dy < 0) {
                isAtBottom = false
                bindingRef?.scrollBottom?.show()
            } else if (dy > 0 && !isAtBottom && !recyclerView.canScrollVertically(1)) {
                isAtBottom = true
                bindingRef?.scrollBottom?.visibility = View.GONE
            }
        }
    }

    companion object {
        private const val AT_BOTTOM_STATE = "chat_at_bottom_state"
        private const val OFFSCREEN_VIEW_CACHE_SIZE = 10

        fun newInstance(channel: UserName) = ChatFragment().apply {
            arguments = ChatFragmentArgs(channel).toBundle()
        }
    }
}
