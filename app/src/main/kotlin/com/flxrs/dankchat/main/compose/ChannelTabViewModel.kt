package com.flxrs.dankchat.main.compose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.repo.chat.ChatRepository
import com.flxrs.dankchat.data.state.ChannelLoadingState
import com.flxrs.dankchat.domain.ChannelDataCoordinator
import com.flxrs.dankchat.preferences.DankChatPreferenceStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class ChannelTabViewModel(
    private val chatRepository: ChatRepository,
    private val channelDataCoordinator: ChannelDataCoordinator,
    private val preferenceStore: DankChatPreferenceStore,
) : ViewModel() {

    val uiState: StateFlow<ChannelTabUiState> = combine(
        preferenceStore.getChannelsWithRenamesFlow(),
        chatRepository.activeChannel,
        chatRepository.unreadMessagesMap,
        chatRepository.channelMentionCount,
    ) { channels, active, unread, mentions ->
        ChannelTabUiState(
            tabs = channels.map { channelWithRename ->
                ChannelTabItem(
                    channel = channelWithRename.channel,
                    displayName = channelWithRename.rename?.value 
                        ?: channelWithRename.channel.value,
                    isSelected = channelWithRename.channel == active,
                    hasUnread = unread[channelWithRename.channel] ?: false,
                    mentionCount = mentions[channelWithRename.channel] ?: 0,
                    loadingState = channelDataCoordinator.getChannelLoadingState(
                        channelWithRename.channel
                    ).value
                )
            },
            selectedIndex = channels
                .indexOfFirst { it.channel == active }
                .coerceAtLeast(0),
            loading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ChannelTabUiState())

    fun selectTab(index: Int) {
        val channels = preferenceStore.channels
        if (index in channels.indices) {
            val channel = channels[index]
            chatRepository.setActiveChannel(channel)
            chatRepository.clearUnreadMessage(channel)
            chatRepository.clearMentionCount(channel)
        }
    }
}

data class ChannelTabUiState(
    val tabs: List<ChannelTabItem> = emptyList(),
    val selectedIndex: Int = 0,
    val loading: Boolean = true,
)

data class ChannelTabItem(
    val channel: UserName,
    val displayName: String,
    val isSelected: Boolean,
    val hasUnread: Boolean,
    val mentionCount: Int,
    val loadingState: ChannelLoadingState
)
