package com.flxrs.dankchat.main.compose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.Immutable
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.repo.chat.ChatRepository
import com.flxrs.dankchat.preferences.DankChatPreferenceStore
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class ChannelPagerViewModel(
    private val chatRepository: ChatRepository,
    private val preferenceStore: DankChatPreferenceStore,
) : ViewModel() {

    val uiState: StateFlow<ChannelPagerUiState> = combine(
        preferenceStore.getChannelsWithRenamesFlow(),
        chatRepository.activeChannel,
    ) { channels, active ->
        ChannelPagerUiState(
            channels = channels.map { it.channel }.toImmutableList(),
            currentPage = channels.indexOfFirst { it.channel == active }
                .coerceAtLeast(0)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ChannelPagerUiState())

    fun onPageChanged(page: Int) {
        val channels = preferenceStore.channels
        if (page in channels.indices) {
            val channel = channels[page]
            chatRepository.setActiveChannel(channel)
            chatRepository.clearUnreadMessage(channel)
            chatRepository.clearMentionCount(channel)
        }
    }
}

@Immutable
data class ChannelPagerUiState(
    val channels: ImmutableList<UserName> = persistentListOf(),
    val currentPage: Int = 0
)
