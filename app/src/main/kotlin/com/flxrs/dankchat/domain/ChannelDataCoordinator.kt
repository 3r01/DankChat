package com.flxrs.dankchat.domain

import com.flxrs.dankchat.auth.AuthDataStore
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.repo.chat.ChatLoadingStep
import com.flxrs.dankchat.data.repo.chat.ChatMessageRepository
import com.flxrs.dankchat.data.repo.data.DataLoadingStep
import com.flxrs.dankchat.data.repo.data.DataRepository
import com.flxrs.dankchat.data.repo.data.DataUpdateEventMessage
import com.flxrs.dankchat.data.state.ChannelLoadingState
import com.flxrs.dankchat.data.state.GlobalLoadingState
import com.flxrs.dankchat.data.twitch.message.SystemMessageType
import com.flxrs.dankchat.di.DispatchersProvider
import com.flxrs.dankchat.preferences.DankChatPreferenceStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import java.util.concurrent.ConcurrentHashMap

@Single
class ChannelDataCoordinator(
    private val channelDataLoader: ChannelDataLoader,
    private val globalDataLoader: GlobalDataLoader,
    private val chatMessageRepository: ChatMessageRepository,
    private val dataRepository: DataRepository,
    private val authDataStore: AuthDataStore,
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

    init {
        scope.launch {
            dataRepository.dataUpdateEvents.collect { event ->
                when (event) {
                    is DataUpdateEventMessage.ActiveEmoteSetChanged -> {
                        chatMessageRepository.addSystemMessage(event.channel, SystemMessageType.ChannelSevenTVEmoteSetChanged(event.actorName, event.emoteSetName))
                    }

                    is DataUpdateEventMessage.EmoteSetUpdated       -> {
                        val (channel, update) = event
                        update.added.forEach { chatMessageRepository.addSystemMessage(channel, SystemMessageType.ChannelSevenTVEmoteAdded(update.actorName, it.name)) }
                        update.updated.forEach { chatMessageRepository.addSystemMessage(channel, SystemMessageType.ChannelSevenTVEmoteRenamed(update.actorName, it.oldName, it.name)) }
                        update.removed.forEach { chatMessageRepository.addSystemMessage(channel, SystemMessageType.ChannelSevenTVEmoteRemoved(update.actorName, it.name)) }
                    }
                }
            }
        }

        scope.launch {
            chatMessageRepository.chatLoadingFailures.collect { chatFailures ->
                if (chatFailures.isNotEmpty()) {
                    _globalLoadingState.update { current ->
                        when (current) {
                            is GlobalLoadingState.Failed -> current.copy(chatFailures = chatFailures)
                            is GlobalLoadingState.Loaded -> GlobalLoadingState.Failed(chatFailures = chatFailures)

                            else                         -> current
                        }
                    }
                }
            }
        }
    }

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
            loadChannelDataSuspend(channel)
        }
    }

    private suspend fun loadChannelDataSuspend(channel: UserName) {
        val stateFlow = channelStates.getOrPut(channel) {
            MutableStateFlow(ChannelLoadingState.Idle)
        }
        stateFlow.value = ChannelLoadingState.Loading
        stateFlow.value = channelDataLoader.loadChannelData(channel)
        chatMessageRepository.reparseAllEmotesAndBadges()
    }

    /**
     * Load global data (once at startup)
     */
    fun loadGlobalData() {
        globalLoadJob = scope.launch {
            _globalLoadingState.value = GlobalLoadingState.Loading
            dataRepository.clearDataLoadingFailures()

            globalDataLoader.loadGlobalData()

            // Reparse after global emotes load so 3rd party globals are visible immediately
            chatMessageRepository.reparseAllEmotesAndBadges()

            // Load user emotes if logged in — only block on first page, rest loads async
            if (authDataStore.isLoggedIn) {
                val userId = authDataStore.userIdString
                if (userId != null) {
                    val firstPageLoaded = CompletableDeferred<Unit>()
                    launch {
                        globalDataLoader.loadUserEmotes(userId) { firstPageLoaded.complete(Unit) }
                            .onSuccess { chatMessageRepository.reparseAllEmotesAndBadges() }
                            .onFailure { firstPageLoaded.complete(Unit) }
                    }
                    firstPageLoaded.await()
                    chatMessageRepository.reparseAllEmotesAndBadges()
                }
            }

            val dataFailures = dataRepository.dataLoadingFailures.value
            val chatFailures = chatMessageRepository.chatLoadingFailures.value
            _globalLoadingState.value = when {
                dataFailures.isEmpty() && chatFailures.isEmpty() -> GlobalLoadingState.Loaded
                else                                             -> GlobalLoadingState.Failed(
                    failures = dataFailures,
                    chatFailures = chatFailures,
                )
            }
        }
    }

    /**
     * Cancel ongoing global data loading (e.g., on logout)
     */
    fun cancelGlobalLoading() {
        globalLoadJob?.cancel()
        globalLoadJob = null
    }

    /**
     * Cleanup when a channel is removed
     */
    fun cleanupChannel(channel: UserName) {
        channelStates.remove(channel)
        scope.launch {
            dataRepository.removeChannels(listOf(channel))
        }
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
    fun retryDataLoading(failedState: GlobalLoadingState.Failed) {
        scope.launch {
            _globalLoadingState.value = GlobalLoadingState.Loading
            dataRepository.clearDataLoadingFailures()
            chatMessageRepository.clearChatLoadingFailures()

            val channelsToRetry = mutableSetOf<UserName>()

            val dataResults = failedState.failures.map { failure ->
                async {
                    when (val step = failure.step) {
                        is DataLoadingStep.GlobalSevenTVEmotes  -> globalDataLoader.loadGlobalSevenTVEmotes()
                        is DataLoadingStep.GlobalBTTVEmotes     -> globalDataLoader.loadGlobalBTTVEmotes()
                        is DataLoadingStep.GlobalFFZEmotes      -> globalDataLoader.loadGlobalFFZEmotes()
                        is DataLoadingStep.GlobalBadges         -> globalDataLoader.loadGlobalBadges()
                        is DataLoadingStep.DankChatBadges       -> globalDataLoader.loadDankChatBadges()
                        is DataLoadingStep.TwitchEmotes         -> {
                            val userId = authDataStore.userIdString
                            if (userId != null) {
                                val firstPageLoaded = CompletableDeferred<Unit>()
                                launch {
                                    globalDataLoader.loadUserEmotes(userId) { firstPageLoaded.complete(Unit) }
                                        .onSuccess { chatMessageRepository.reparseAllEmotesAndBadges() }
                                        .onFailure { firstPageLoaded.complete(Unit) }
                                }
                                firstPageLoaded.await()
                                chatMessageRepository.reparseAllEmotesAndBadges()
                            }
                        }

                        is DataLoadingStep.ChannelBadges        -> channelsToRetry.add(step.channel)
                        is DataLoadingStep.ChannelSevenTVEmotes -> channelsToRetry.add(step.channel)
                        is DataLoadingStep.ChannelFFZEmotes     -> channelsToRetry.add(step.channel)
                        is DataLoadingStep.ChannelBTTVEmotes    -> channelsToRetry.add(step.channel)
                        is DataLoadingStep.ChannelCheermotes    -> channelsToRetry.add(step.channel)
                    }
                }
            }

            failedState.chatFailures.forEach { failure ->
                when (val step = failure.step) {
                    is ChatLoadingStep.RecentMessages -> channelsToRetry.add(step.channel)
                }
            }

            dataResults.awaitAll()
            channelsToRetry.map { channel ->
                async { loadChannelDataSuspend(channel) }
            }.awaitAll()

            val remainingDataFailures = dataRepository.dataLoadingFailures.value
            val remainingChatFailures = chatMessageRepository.chatLoadingFailures.value
            _globalLoadingState.value = when {
                remainingDataFailures.isEmpty() && remainingChatFailures.isEmpty() -> GlobalLoadingState.Loaded
                else                                                               -> GlobalLoadingState.Failed(
                    failures = remainingDataFailures,
                    chatFailures = remainingChatFailures,
                )
            }
        }
    }

}
