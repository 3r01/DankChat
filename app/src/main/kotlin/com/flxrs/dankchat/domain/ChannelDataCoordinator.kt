package com.flxrs.dankchat.domain

import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.repo.chat.UserStateRepository
import com.flxrs.dankchat.data.state.ChannelLoadingState
import com.flxrs.dankchat.data.state.GlobalLoadingState
import com.flxrs.dankchat.di.DispatchersProvider
import com.flxrs.dankchat.preferences.DankChatPreferenceStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import java.util.concurrent.ConcurrentHashMap

@Single
class ChannelDataCoordinator(
    private val channelDataLoader: ChannelDataLoader,
    private val globalDataLoader: GlobalDataLoader,
    private val userStateRepository: UserStateRepository,
    private val preferenceStore: DankChatPreferenceStore,
    dispatchersProvider: DispatchersProvider
) {

    private val scope = CoroutineScope(SupervisorJob() + dispatchersProvider.default)

    // Track loading state per channel
    private val channelStates = ConcurrentHashMap<UserName, MutableStateFlow<ChannelLoadingState>>()

    // Global loading state
    private val _globalLoadingState = MutableStateFlow<GlobalLoadingState>(GlobalLoadingState.Idle)
    val globalLoadingState: StateFlow<GlobalLoadingState> = _globalLoadingState.asStateFlow()

    /**
     * Get loading state for a specific channel
     */
    fun getChannelLoadingState(channel: UserName): StateFlow<ChannelLoadingState> {
        return channelStates.getOrPut(channel) {
            MutableStateFlow(ChannelLoadingState.Idle)
        }
    }

    /**
     * Load data when a channel is added
     */
    fun loadChannelData(channel: UserName) {
        scope.launch {
            val stateFlow = channelStates.getOrPut(channel) {
                MutableStateFlow(ChannelLoadingState.Idle)
            }

            channelDataLoader.loadChannelData(channel)
                .collect { state ->
                    stateFlow.value = state
                }
        }
    }

    /**
     * Load global data (once at startup)
     */
    fun loadGlobalData() {
        scope.launch {
            _globalLoadingState.value = GlobalLoadingState.Loading

            globalDataLoader.loadGlobalData()
                .onSuccess {
                    // Load user state emotes if logged in
                    if (preferenceStore.isLoggedIn) {
                        loadUserStateEmotesIfAvailable()
                    }
                    _globalLoadingState.value = GlobalLoadingState.Loaded
                }
                .onFailure { error ->
                    _globalLoadingState.value = GlobalLoadingState.Failed(error.message ?: "Unknown error")
                }
        }
    }

    /**
     * Load user-specific emotes after global data is loaded
     */
    private suspend fun loadUserStateEmotesIfAvailable() {
        val channels = preferenceStore.channels
        if (channels.isEmpty()) return

        val userState = userStateRepository.tryGetUserStateOrFallback(minChannelsSize = channels.size)
        userState?.let {
            globalDataLoader.loadUserStateEmotes(it.globalEmoteSets, it.followerEmoteSets)
        }
    }

    /**
     * Cleanup when a channel is removed
     */
    fun cleanupChannel(channel: UserName) {
        channelStates.remove(channel)
    }

    /**
     * Reload all channels (e.g., on reconnect)
     */
    fun reloadAllChannels() {
        scope.launch {
            preferenceStore.channels.forEach { channel ->
                loadChannelData(channel)
            }
        }
    }

    /**
     * Reload global data
     */
    fun reloadGlobalData() {
        loadGlobalData()
    }
}
