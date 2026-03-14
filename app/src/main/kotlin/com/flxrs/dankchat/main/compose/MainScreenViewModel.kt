package com.flxrs.dankchat.main.compose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.repo.channel.ChannelRepository
import com.flxrs.dankchat.data.repo.chat.ChatRepository
import com.flxrs.dankchat.data.state.ChannelLoadingState
import com.flxrs.dankchat.data.state.GlobalLoadingState
import com.flxrs.dankchat.domain.ChannelDataCoordinator
import com.flxrs.dankchat.preferences.DankChatPreferenceStore
import com.flxrs.dankchat.preferences.model.ChannelWithRename
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class MainScreenViewModel(
    private val chatRepository: ChatRepository,
    private val channelRepository: ChannelRepository,
    private val preferenceStore: DankChatPreferenceStore,
    private val channelDataCoordinator: ChannelDataCoordinator,
) : ViewModel() {

    val channels: StateFlow<List<ChannelWithRename>> = preferenceStore.getChannelsWithRenamesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeChannel: StateFlow<UserName?> = chatRepository.activeChannel

    val activeChannelIndex: StateFlow<Int> = combine(channels, activeChannel) { channels, active ->
        channels.indexOfFirst { it.channel == active }.coerceAtLeast(0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val unreadMessagesMap: StateFlow<Map<UserName, Boolean>> =
        chatRepository.unreadMessagesMap
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val channelMentionCount: StateFlow<Map<UserName, Int>> =
        chatRepository.channelMentionCount
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val totalMentionCount: StateFlow<Int> = channelMentionCount.map { it.values.sum() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Global loading state
    val globalLoadingState: StateFlow<GlobalLoadingState> = channelDataCoordinator.globalLoadingState

    init {
        // Load global data once at startup
        channelDataCoordinator.loadGlobalData()

        // Observe channels and load data when added
        viewModelScope.launch {
            channels.collect { channelList ->
                channelList.forEach { channelWithRename ->
                    channelDataCoordinator.loadChannelData(channelWithRename.channel)
                }
            }
        }
    }

    /**
     * Get loading state for a specific channel
     */
    fun getChannelLoadingState(channel: UserName): StateFlow<ChannelLoadingState> {
        return channelDataCoordinator.getChannelLoadingState(channel)
    }

    fun setActiveChannel(channel: UserName) {
        chatRepository.setActiveChannel(channel)
        clearUnreadMessage(channel)
        clearMentionCount(channel)
    }

    fun addChannel(channel: UserName) {
        val currentChannels = preferenceStore.channels
        if (channel !in currentChannels) {
            preferenceStore.channels = currentChannels + channel
            // Data loading triggered automatically by channel observer
        }
    }

    fun removeChannel(channel: UserName) {
        preferenceStore.removeChannel(channel)
        channelDataCoordinator.cleanupChannel(channel)
    }

    fun renameChannel(channel: UserName, displayName: String?) {
        val rename = displayName?.ifBlank { null }?.let { UserName(it) }
        preferenceStore.setRenamedChannel(ChannelWithRename(channel, rename))
    }

    fun clearUnreadMessage(channel: UserName) {
        chatRepository.clearUnreadMessage(channel)
    }

    fun clearMentionCount(channel: UserName) {
        chatRepository.clearMentionCount(channel)
    }

    fun retryChannelLoading(channel: UserName) {
        channelDataCoordinator.loadChannelData(channel)
    }

    fun reloadAllChannels() {
        channelDataCoordinator.reloadAllChannels()
    }

    fun reloadGlobalData() {
        channelDataCoordinator.reloadGlobalData()
    }
}
