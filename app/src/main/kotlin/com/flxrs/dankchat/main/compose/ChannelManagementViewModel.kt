package com.flxrs.dankchat.main.compose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.repo.chat.ChatRepository
import com.flxrs.dankchat.domain.ChannelDataCoordinator
import com.flxrs.dankchat.preferences.DankChatPreferenceStore
import com.flxrs.dankchat.preferences.model.ChannelWithRename
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class ChannelManagementViewModel(
    private val preferenceStore: DankChatPreferenceStore,
    private val channelDataCoordinator: ChannelDataCoordinator,
    private val chatRepository: ChatRepository,
) : ViewModel() {

    val channels: StateFlow<List<ChannelWithRename>> = 
        preferenceStore.getChannelsWithRenamesFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Set initial active channel if not already set
        viewModelScope.launch {
            if (chatRepository.activeChannel.value == null) {
                val firstChannel = preferenceStore.channels.firstOrNull()
                if (firstChannel != null) {
                    chatRepository.setActiveChannel(firstChannel)
                }
            }
        }
        
        // Auto-load data when channels added
        viewModelScope.launch {
            channels.collect { channelList ->
                channelList.forEach { channelWithRename ->
                    channelDataCoordinator.loadChannelData(channelWithRename.channel)
                }
            }
        }
    }

    fun addChannel(channel: UserName) {
        val current = preferenceStore.channels
        if (channel !in current) {
            preferenceStore.channels = current + channel
            chatRepository.setActiveChannel(channel)
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

    fun retryChannelLoading(channel: UserName) {
        channelDataCoordinator.loadChannelData(channel)
    }

    fun reloadAllChannels() {
        channelDataCoordinator.reloadAllChannels()
    }
}
