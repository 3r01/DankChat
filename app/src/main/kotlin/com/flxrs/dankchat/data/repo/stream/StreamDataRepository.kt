package com.flxrs.dankchat.data.repo.stream

import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.repo.data.DataRepository
import com.flxrs.dankchat.di.DispatchersProvider
import com.flxrs.dankchat.main.StreamData
import com.flxrs.dankchat.preferences.DankChatPreferenceStore
import com.flxrs.dankchat.preferences.stream.StreamsSettingsDataStore
import com.flxrs.dankchat.utils.DateTimeUtils
import com.flxrs.dankchat.utils.extensions.timer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import kotlin.time.Duration.Companion.seconds

@Single
class StreamDataRepository(
    private val dataRepository: DataRepository,
    private val dankChatPreferenceStore: DankChatPreferenceStore,
    private val streamsSettingsDataStore: StreamsSettingsDataStore,
    dispatchersProvider: DispatchersProvider,
) {
    private val scope = CoroutineScope(dispatchersProvider.io + SupervisorJob())
    private var fetchTimerJob: Job? = null
    private val _streamData = MutableStateFlow<List<StreamData>>(emptyList())
    val streamData: StateFlow<List<StreamData>> = _streamData.asStateFlow()

    fun fetchStreamData(channels: List<UserName>) {
        cancelStreamData()
        channels.ifEmpty { return }

        scope.launch {
            val settings = streamsSettingsDataStore.settings.first()
            if (!dankChatPreferenceStore.isLoggedIn || !settings.fetchStreams) {
                return@launch
            }

            fetchTimerJob = timer(STREAM_REFRESH_RATE) {
                val data = dataRepository.getStreams(channels)?.map {
                    val uptime = DateTimeUtils.calculateUptime(it.startedAt)
                    val category = it.category
                        ?.takeIf { settings.showStreamCategory }
                        ?.ifBlank { null }
                    val formatted = dankChatPreferenceStore.formatViewersString(it.viewerCount, uptime, category)

                    StreamData(channel = it.userLogin, formattedData = formatted)
                }.orEmpty()

                _streamData.value = data
            }
        }
    }

    fun cancelStreamData() {
        fetchTimerJob?.cancel()
        fetchTimerJob = null
        _streamData.value = emptyList()
    }

    companion object {
        private val STREAM_REFRESH_RATE = 30.seconds
    }
}
