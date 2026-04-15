package com.flxrs.dankchat.data.repo.stream

import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.auth.AuthDataStore
import com.flxrs.dankchat.data.repo.data.DataRepository
import com.flxrs.dankchat.di.DispatchersProvider
import com.flxrs.dankchat.preferences.DankChatPreferenceStore
import com.flxrs.dankchat.preferences.stream.StreamsSettingsDataStore
import com.flxrs.dankchat.utils.AppLifecycleListener
import com.flxrs.dankchat.utils.AppLifecycleListener.AppLifecycle
import com.flxrs.dankchat.utils.DateTimeUtils
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.plusAssign
import kotlin.time.Duration.Companion.seconds

@Single
class StreamDataRepository(
    private val dataRepository: DataRepository,
    private val authDataStore: AuthDataStore,
    private val dankChatPreferenceStore: DankChatPreferenceStore,
    private val streamsSettingsDataStore: StreamsSettingsDataStore,
    private val appLifecycleListener: AppLifecycleListener,
    dispatchersProvider: DispatchersProvider,
) {
    private val scope = CoroutineScope(dispatchersProvider.io + SupervisorJob())
    private val activeChannels = MutableStateFlow<List<UserName>>(emptyList())
    private val _streamData = MutableStateFlow<ImmutableList<StreamData>>(persistentListOf())
    val streamData: StateFlow<ImmutableList<StreamData>> = _streamData.asStateFlow()
    private val _fetchCount = AtomicInt(0)
    val fetchCount: Int get() = _fetchCount.load()

    init {
        scope.launch {
            combine(
                activeChannels,
                streamsSettingsDataStore.settings
                    .map { FetchConfig(fetchStreams = it.fetchStreams, showStreamCategory = it.showStreamCategory) }
                    .distinctUntilChanged(),
                appLifecycleListener.appState,
            ) { channels, config, lifecycle -> Triple(channels, config, lifecycle) }
                .collectLatest { (channels, config, lifecycle) ->
                    if (channels.isEmpty() || !config.fetchStreams || !authDataStore.isLoggedIn) {
                        _streamData.value = persistentListOf()
                        return@collectLatest
                    }
                    if (lifecycle == AppLifecycle.Background) {
                        return@collectLatest
                    }
                    coroutineScope {
                        while (isActive) {
                            runCatching { fetchOnce(channels) }
                            delay(STREAM_REFRESH_RATE)
                        }
                    }
                }
        }
    }

    fun fetchStreamData(channels: List<UserName>) {
        activeChannels.value = channels
    }

    suspend fun fetchOnce(channels: List<UserName>) {
        val currentSettings = streamsSettingsDataStore.settings.first()
        _fetchCount += 1
        val data =
            dataRepository
                .getStreams(channels)
                ?.map {
                    val uptime = DateTimeUtils.calculateUptime(it.startedAt)
                    val category =
                        it.category
                            ?.takeIf { currentSettings.showStreamCategory }
                            ?.ifBlank { null }
                    val formatted = dankChatPreferenceStore.formatViewersString(it.viewerCount, uptime, category)
                    val formattedCompact = dankChatPreferenceStore.formatViewersStringCompact(it.viewerCount, uptime, category)

                    StreamData(
                        channel = it.userLogin,
                        formattedData = formatted,
                        formattedDataCompact = formattedCompact,
                        viewerCount = it.viewerCount,
                        startedAt = it.startedAt,
                        category = it.category,
                    )
                }.orEmpty()

        _streamData.value = data.toImmutableList()
    }

    fun cancelStreamData() {
        activeChannels.value = emptyList()
    }

    private data class FetchConfig(
        val fetchStreams: Boolean,
        val showStreamCategory: Boolean,
    )

    companion object {
        private val STREAM_REFRESH_RATE = 30.seconds
    }
}
