package com.flxrs.dankchat.domain

import com.flxrs.dankchat.data.auth.AuthEvent
import com.flxrs.dankchat.data.auth.AuthStateCoordinator
import com.flxrs.dankchat.data.auth.StartupValidationHolder
import com.flxrs.dankchat.data.notification.RemoteMentionHistoryRepository
import com.flxrs.dankchat.data.notification.RemotePushCoordinator
import com.flxrs.dankchat.data.repo.chat.ChatChannelProvider
import com.flxrs.dankchat.data.repo.chat.ChatConnector
import com.flxrs.dankchat.data.repo.chat.ChatEventProcessor
import com.flxrs.dankchat.data.repo.data.DataRepository
import com.flxrs.dankchat.data.state.GlobalLoadingState
import com.flxrs.dankchat.di.DispatchersProvider
import com.flxrs.dankchat.preferences.battery.BatterySettingsDataStore
import com.flxrs.dankchat.preferences.chat.ChatSettingsDataStore
import com.flxrs.dankchat.utils.AppLifecycleListener
import com.flxrs.dankchat.utils.AppLifecycleListener.AppLifecycle
import com.flxrs.dankchat.utils.ForegroundServiceState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single

@Single
class ConnectionCoordinator(
    private val chatConnector: ChatConnector,
    private val dataRepository: DataRepository,
    private val chatChannelProvider: ChatChannelProvider,
    private val channelDataCoordinator: ChannelDataCoordinator,
    private val authStateCoordinator: AuthStateCoordinator,
    private val startupValidationHolder: StartupValidationHolder,
    private val appLifecycleListener: AppLifecycleListener,
    private val foregroundServiceState: ForegroundServiceState,
    private val remotePushCoordinator: RemotePushCoordinator,
    private val remoteMentionHistoryRepository: RemoteMentionHistoryRepository,
    private val batterySettingsDataStore: BatterySettingsDataStore,
    private val chatSettingsDataStore: ChatSettingsDataStore,
    private val chatEventProcessor: ChatEventProcessor,
    dispatchersProvider: DispatchersProvider,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatchersProvider.default)

    fun initialize() {
        scope.launch {
            if (remotePushCoordinator.isEnabled() && appLifecycleListener.appState.value is AppLifecycle.Background) {
                appLifecycleListener.appState.first { it is AppLifecycle.Foreground }
            }
            val result = authStateCoordinator.validateOnStartup()
            when (result) {
                is AuthEvent.TokenInvalid -> Unit
                else -> chatConnector.connectAndJoin(chatChannelProvider.channels.value.orEmpty())
            }
        }

        scope.launch {
            startupValidationHolder.awaitResolved()
            var wasInBackground = false
            var pausedForRemotePush = false
            appLifecycleListener.appState.collectLatest { state ->
                when (state) {
                    is AppLifecycle.Background -> {
                        wasInBackground = true
                        if (remotePushCoordinator.isEnabled()) {
                            delay(batterySettingsDataStore.current().remotePushDisconnectDelay.duration)
                            if (!remotePushCoordinator.isEnabled()) return@collectLatest
                            pausedForRemotePush = true
                            withContext(NonCancellable) {
                                chatConnector.pauseForRemotePush()
                                dataRepository.pauseForRemotePush()
                                foregroundServiceState.setActive(false)
                            }
                        }
                    }

                    is AppLifecycle.Foreground -> {
                        if (wasInBackground) {
                            wasInBackground = false
                            foregroundServiceState.setActive(true)
                            if (pausedForRemotePush) {
                                pausedForRemotePush = false
                                val resumedChannels = chatConnector.resumeAfterRemotePush(chatChannelProvider.channels.value.orEmpty())
                                if (chatSettingsDataStore.settings.first().loadMessageHistoryOnReconnect) {
                                    resumedChannels.forEach { channel ->
                                        scope.launch { chatEventProcessor.loadRecentMessages(channel, isReconnect = true) }
                                    }
                                }
                                scope.launch { remoteMentionHistoryRepository.restore() }
                            } else {
                                chatConnector.reconnectIfNecessary()
                            }
                            dataRepository.reconnectIfNecessary()

                            val loadingState = channelDataCoordinator.globalLoadingState.value
                            if (loadingState is GlobalLoadingState.Failed) {
                                channelDataCoordinator.retryDataLoading(loadingState)
                            }
                        }
                    }
                }
            }
        }
    }
}
