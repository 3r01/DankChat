package com.flxrs.dankchat.ui.main.dialog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.api.helix.HelixApiClient
import com.flxrs.dankchat.data.auth.AuthDataStore
import com.flxrs.dankchat.data.repo.channel.ChannelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam

@KoinViewModel
class ModActionsViewModel(
    @InjectedParam private val channel: UserName,
    private val helixApiClient: HelixApiClient,
    private val authDataStore: AuthDataStore,
    private val channelRepository: ChannelRepository,
) : ViewModel() {

    private val _shieldModeActive = MutableStateFlow<Boolean?>(null)
    val shieldModeActive: StateFlow<Boolean?> = _shieldModeActive.asStateFlow()

    init {
        fetchShieldMode()
    }

    private fun fetchShieldMode() {
        viewModelScope.launch {
            val channelId = channelRepository.getChannel(channel)?.id ?: return@launch
            val moderatorId = authDataStore.userIdString ?: return@launch
            helixApiClient.getShieldMode(channelId, moderatorId)
                .onSuccess { _shieldModeActive.value = it.isActive }
        }
    }
}
