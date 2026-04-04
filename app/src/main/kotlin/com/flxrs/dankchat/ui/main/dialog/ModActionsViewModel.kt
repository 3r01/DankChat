package com.flxrs.dankchat.ui.main.dialog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.auth.AuthDataStore
import com.flxrs.dankchat.data.repo.ShieldModeRepository
import com.flxrs.dankchat.data.repo.channel.ChannelRepository
import com.flxrs.dankchat.data.twitch.message.RoomState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam

@KoinViewModel
class ModActionsViewModel(
    @InjectedParam private val channel: UserName,
    private val shieldModeRepository: ShieldModeRepository,
    channelRepository: ChannelRepository,
    authDataStore: AuthDataStore,
) : ViewModel() {
    val shieldModeActive: StateFlow<Boolean?> = shieldModeRepository.getState(channel)
    val roomState: StateFlow<RoomState?> = channelRepository
        .getRoomStateFlow(channel)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), channelRepository.getRoomState(channel))
    val isBroadcaster: Boolean = authDataStore.userIdString == channelRepository.getRoomState(channel)?.channelId

    init {
        viewModelScope.launch {
            shieldModeRepository.fetch(channel)
        }
    }
}
