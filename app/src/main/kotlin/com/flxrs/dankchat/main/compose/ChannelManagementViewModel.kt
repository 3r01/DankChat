package com.flxrs.dankchat.main.compose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.repo.IgnoresRepository
import com.flxrs.dankchat.data.repo.channel.ChannelRepository
import com.flxrs.dankchat.data.repo.chat.ChatChannelProvider
import com.flxrs.dankchat.data.repo.chat.ChatConnector
import com.flxrs.dankchat.data.repo.chat.ChatMessageRepository
import com.flxrs.dankchat.data.repo.chat.ChatNotificationRepository
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
    private val chatChannelProvider: ChatChannelProvider,
    private val chatConnector: ChatConnector,
    private val chatMessageRepository: ChatMessageRepository,
    private val chatNotificationRepository: ChatNotificationRepository,
    private val ignoresRepository: IgnoresRepository,
    private val channelRepository: ChannelRepository,
) : ViewModel() {

    val channels: StateFlow<List<ChannelWithRename>> =
        preferenceStore.getChannelsWithRenamesFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Set initial active channel if not already set
        viewModelScope.launch {
            if (chatChannelProvider.activeChannel.value == null) {
                val firstChannel = preferenceStore.channels.firstOrNull()
                if (firstChannel != null) {
                    chatChannelProvider.setActiveChannel(firstChannel)
                }
            }
        }

        // Auto-load data when channels added and join if necessary
        viewModelScope.launch {
            var previousChannels = emptySet<UserName>()
            channels.collect { channelList ->
                val currentChannels = channelList.map { it.channel }.toSet()
                val newChannels = currentChannels - previousChannels

                newChannels.forEach { channel ->
                    chatRepository.joinChannel(channel)
                    channelDataCoordinator.loadChannelData(channel)
                }
                previousChannels = currentChannels
            }
        }
    }

    fun addChannel(channel: UserName) {
        val current = preferenceStore.channels
        if (channel !in current) {
            preferenceStore.channels = current + channel
            chatRepository.joinChannel(channel)
            chatChannelProvider.setActiveChannel(channel)
        }
    }

    fun removeChannel(channel: UserName) {
        val wasActive = chatChannelProvider.activeChannel.value == channel
        preferenceStore.removeChannel(channel)
        chatRepository.updateChannels(preferenceStore.channels)
        channelDataCoordinator.cleanupChannel(channel)

        if (wasActive) {
            chatChannelProvider.setActiveChannel(preferenceStore.channels.firstOrNull())
        }
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

    fun reloadEmotes(channel: UserName) {
        channelDataCoordinator.loadChannelData(channel)
    }

    fun reconnect() {
        chatConnector.reconnect()
    }

    fun clearChat(channel: UserName) {
        chatMessageRepository.clearMessages(channel)
    }

    fun blockChannel(channel: UserName) = viewModelScope.launch {
        runCatching {
            if (!preferenceStore.isLoggedIn) {
                return@launch
            }

            val channelId = channelRepository.getChannel(channel)?.id ?: return@launch
            ignoresRepository.addUserBlock(channelId, channel)
            removeChannel(channel)
        }
    }

    fun selectChannel(channel: UserName) {
        chatChannelProvider.setActiveChannel(channel)
        chatNotificationRepository.clearUnreadMessage(channel)
        chatNotificationRepository.clearMentionCount(channel)
    }

    fun applyChanges(updatedChannels: List<ChannelWithRename>) {
        val currentChannels = preferenceStore.channels
        val newChannelNames = updatedChannels.map { it.channel }
        val removedChannels = currentChannels - newChannelNames.toSet()

        // 1. Cleanup removed channels
        if (removedChannels.isNotEmpty()) {
            chatRepository.updateChannels(newChannelNames) // This handles join/part
            removedChannels.forEach { channel ->
                channelDataCoordinator.cleanupChannel(channel)
                // Remove rename
                preferenceStore.setRenamedChannel(ChannelWithRename(channel, null))
            }

            // 2. Update active channel if removed
            val activeChannel = chatChannelProvider.activeChannel.value
            if (activeChannel in removedChannels) {
                // Determine new active channel (try to keep index or go to first)
                // For simplicity, pick the first one, or null if empty
                val newActive = newChannelNames.firstOrNull()
                chatChannelProvider.setActiveChannel(newActive)
            }
        }

        // 3. Update order and list in preferences
        preferenceStore.channels = newChannelNames

        // 4. Apply renames
        updatedChannels.forEach {
            preferenceStore.setRenamedChannel(it)
        }
    }
}
