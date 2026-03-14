package com.flxrs.dankchat.domain

import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.repo.chat.ChatRepository
import com.flxrs.dankchat.data.repo.chat.UserStateRepository
import com.flxrs.dankchat.data.state.ChannelLoadingState
import com.flxrs.dankchat.data.state.GlobalLoadingState
import com.flxrs.dankchat.di.DispatchersProvider
import com.flxrs.dankchat.preferences.DankChatPreferenceStore
import com.flxrs.dankchat.data.repo.chat.ChatLoadingFailure
import com.flxrs.dankchat.data.repo.chat.ChatLoadingStep
import com.flxrs.dankchat.data.repo.data.DataLoadingFailure
import com.flxrs.dankchat.data.repo.data.DataLoadingStep
import com.flxrs.dankchat.data.repo.data.DataRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import org.koin.core.annotation.Single
import java.util.concurrent.ConcurrentHashMap

@Single
class ChannelDataCoordinator(
    private val channelDataLoader: ChannelDataLoader,
    private val globalDataLoader: GlobalDataLoader,
    private val chatRepository: ChatRepository,
    private val dataRepository: DataRepository,
    private val userStateRepository: UserStateRepository,
    private val preferenceStore: DankChatPreferenceStore,
    dispatchersProvider: DispatchersProvider
) {

    private val scope = CoroutineScope(SupervisorJob() + dispatchersProvider.default)
    private var globalLoadJob: Job? = null

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

            // After channel data loaded, wait for global data too, then reparse
            globalLoadJob?.join()
            chatRepository.reparseAllEmotesAndBadges()
        }
    }

    /**
     * Load global data (once at startup)
     */
    fun loadGlobalData() {
        globalLoadJob = scope.launch {
            _globalLoadingState.value = GlobalLoadingState.Loading
            dataRepository.clearDataLoadingFailures()

            val results = globalDataLoader.loadGlobalData()

            // Load user state emotes if logged in
            if (preferenceStore.isLoggedIn) {
                loadUserStateEmotesIfAvailable()
            }

            val failures = dataRepository.dataLoadingFailures.value
            _globalLoadingState.value = when {
                failures.isEmpty() -> GlobalLoadingState.Loaded
                else               -> GlobalLoadingState.Failed(
                    message = "${failures.size} provider(s) failed to load",
                    failures = failures
                )
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

    /**
     * Retry specific failed data and chat steps
     */
    fun retryDataLoading(dataFailures: Set<DataLoadingFailure>, chatFailures: Set<ChatLoadingFailure>) {
        scope.launch {
            _globalLoadingState.value = GlobalLoadingState.Loading
            
            // Collect channels that need retry
            val channelsToRetry = mutableSetOf<UserName>()
            
            val dataResults = dataFailures.map { failure ->
                async {
                    when (val step = failure.step) {
                        is DataLoadingStep.GlobalSevenTVEmotes  -> globalDataLoader.loadGlobalSevenTVEmotes()
                        is DataLoadingStep.GlobalBTTVEmotes     -> globalDataLoader.loadGlobalBTTVEmotes()
                        is DataLoadingStep.GlobalFFZEmotes      -> globalDataLoader.loadGlobalFFZEmotes()
                        is DataLoadingStep.GlobalBadges         -> globalDataLoader.loadGlobalBadges()
                        is DataLoadingStep.DankChatBadges       -> globalDataLoader.loadDankChatBadges()
                        is DataLoadingStep.ChannelBadges        -> channelsToRetry.add(step.channel)
                        is DataLoadingStep.ChannelSevenTVEmotes -> channelsToRetry.add(step.channel)
                        is DataLoadingStep.ChannelFFZEmotes     -> channelsToRetry.add(step.channel)
                        is DataLoadingStep.ChannelBTTVEmotes    -> channelsToRetry.add(step.channel)
                    }
                }
            }

            chatFailures.forEach { failure ->
                when (val step = failure.step) {
                    is ChatLoadingStep.RecentMessages -> channelsToRetry.add(step.channel)
                }
            }

            dataResults.awaitAll()
            channelsToRetry.forEach { loadChannelData(it) }

            _globalLoadingState.value = GlobalLoadingState.Loaded
        }
    }
}