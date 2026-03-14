package com.flxrs.dankchat.chat

import android.graphics.drawable.Animatable
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.os.Bundle
import android.text.style.ImageSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.postDelayed
import androidx.fragment.app.Fragment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.flxrs.dankchat.chat.compose.ChatScreen
import com.flxrs.dankchat.data.DisplayName
import com.flxrs.dankchat.data.UserId
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.repo.emote.EmoteRepository
import com.flxrs.dankchat.data.twitch.badge.Badge
import com.flxrs.dankchat.data.twitch.emote.ChatMessageEmote
import com.flxrs.dankchat.databinding.ChatFragmentBinding
import com.flxrs.dankchat.main.MainFragment
import com.flxrs.dankchat.main.MainViewModel
import com.flxrs.dankchat.preferences.DankChatPreferenceStore
import com.flxrs.dankchat.preferences.appearance.AppearanceSettingsDataStore
import com.flxrs.dankchat.preferences.chat.ChatSettingsDataStore
import com.flxrs.dankchat.preferences.chat.UserLongClickBehavior
import com.flxrs.dankchat.preferences.developer.DeveloperSettingsDataStore
import com.flxrs.dankchat.utils.extensions.collectFlow
import com.flxrs.dankchat.utils.extensions.forEachLayer
import com.flxrs.dankchat.utils.extensions.forEachSpan
import com.flxrs.dankchat.utils.extensions.forEachViewHolder
import com.flxrs.dankchat.utils.insets.TranslateDeferringInsetsAnimationCallback
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

open class ChatFragment : Fragment() {
    private val viewModel: ChatViewModel by viewModel()
    private val mainViewModel: MainViewModel by viewModel(ownerProducer = { requireParentFragment() })
    private val emoteRepository: EmoteRepository by inject()
    private val appearanceSettingsDataStore: AppearanceSettingsDataStore by inject()
    private val developerSettingsDataStore: DeveloperSettingsDataStore by inject()
    protected val chatSettingsDataStore: ChatSettingsDataStore by inject()
    protected val dankChatPreferenceStore: DankChatPreferenceStore by inject()

    // Legacy support - will be removed
    protected var bindingRef: ChatFragmentBinding? = null
    protected val binding get() = bindingRef!!
    protected open lateinit var adapter: ChatAdapter
    protected open lateinit var manager: LinearLayoutManager

    // TODO move to viewmodel?
    protected open var isAtBottom = true
    
    private var useCompose = true // Feature flag for migration

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return if (useCompose) {
            ComposeView(requireContext()).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    val messages by viewModel.chatUiStates.collectAsStateWithLifecycle(initialValue = emptyList())
                    val appearanceSettings = appearanceSettingsDataStore.settings.collectAsStateWithLifecycle(initialValue = appearanceSettingsDataStore.current()).value
                    
                    ChatScreen(
                        messages = messages,
                        fontSize = appearanceSettings.fontSize.toFloat(),
                        modifier = Modifier.fillMaxSize(),
                        onUserClick = ::onUserClickCompose,
                        onMessageLongClick = { messageId, channel, fullMessage ->
                            onMessageClick(messageId, channel?.let { UserName(it) }, fullMessage)
                        },
                        onEmoteClick = ::onEmoteClickCompose,
                        onReplyClick = ::onReplyClick
                    )
                }
            }
        } else {
            // Legacy RecyclerView implementation
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
            binding.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (useCompose) {
            // Compose implementation doesn't need additional setup
            return
        }
        
        // Legacy setup
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

    override fun onStart() {
        super.onStart()
        // minSdk 30+ handles GIF animations natively
    }

    override fun onDestroyView() {
        if (!useCompose) {
            binding.chat.adapter = null
            binding.chat.layoutManager = null
            bindingRef = null
        }
        super.onDestroyView()
    }

    override fun onStop() {
        // minSdk 30+ handles animated drawables automatically
        super.onStop()
    }

    // Compose-specific callbacks
    private fun onUserClickCompose(
        targetUserId: String?,
        targetUserName: String,
        targetDisplayName: String,
        channel: String?,
        badges: List<Any>,
        isLongPress: Boolean
    ) {
        val userId = targetUserId?.let { UserId(it) }
        val userName = UserName(targetUserName)
        val displayName = DisplayName(targetDisplayName)
        val channelName = channel?.let { UserName(it) }
        onUserClick(userId, userName, displayName, channelName, emptyList(), isLongPress)
    }

    private fun onEmoteClickCompose(emotes: List<Any>) {
        // Convert back to ChatMessageEmote if needed
        val chatEmotes = emotes.filterIsInstance<ChatMessageEmote>()
        (parentFragment as? MainFragment)?.openEmoteSheet(chatEmotes)
    }

    private fun onReplyClick(rootMessageId: String) {
        (parentFragment as? MainFragment)?.openReplies(rootMessageId)
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

    // Legacy RecyclerView methods
    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        if (!useCompose) {
            savedInstanceState?.let {
                isAtBottom = it.getBoolean(AT_BOTTOM_STATE)
                binding.scrollBottom.isVisible = !isAtBottom
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (!useCompose) {
            outState.putBoolean(AT_BOTTOM_STATE, isAtBottom)
        }
    }

    protected open fun scrollToPosition(position: Int) {
        if (useCompose) return
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
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            mainViewModel.isScrolling(newState != RecyclerView.SCROLL_STATE_IDLE)
        }

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
