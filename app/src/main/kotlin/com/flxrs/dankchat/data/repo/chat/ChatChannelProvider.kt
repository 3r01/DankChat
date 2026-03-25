package com.flxrs.dankchat.data.repo.chat

import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.preferences.DankChatPreferenceStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.annotation.Single

@Single
class ChatChannelProvider(preferenceStore: DankChatPreferenceStore) {

    private val _activeChannel = MutableStateFlow<UserName?>(null)
    private val _channels = MutableStateFlow<List<UserName>?>(preferenceStore.channels.takeIf { it.isNotEmpty() })

    val activeChannel: StateFlow<UserName?> = _activeChannel.asStateFlow()
    val channels: StateFlow<List<UserName>?> = _channels.asStateFlow()

    fun setActiveChannel(channel: UserName?) {
        _activeChannel.value = channel
    }

    fun setChannels(channels: List<UserName>) {
        _channels.value = channels
    }
}
