package com.flxrs.dankchat.domain

import com.flxrs.dankchat.data.auth.AuthStateCoordinator
import com.flxrs.dankchat.data.auth.StartupValidationHolder
import com.flxrs.dankchat.data.notification.RemotePushCoordinator
import com.flxrs.dankchat.data.repo.chat.ChatChannelProvider
import com.flxrs.dankchat.data.repo.chat.ChatConnector
import com.flxrs.dankchat.data.repo.data.DataRepository
import com.flxrs.dankchat.data.state.GlobalLoadingState
import com.flxrs.dankchat.data.toUserName
import com.flxrs.dankchat.di.DispatchersProvider
import com.flxrs.dankchat.utils.AppLifecycleListener
import com.flxrs.dankchat.utils.AppLifecycleListener.AppLifecycle
import com.flxrs.dankchat.utils.ForegroundServiceState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalCoroutinesApi::class)
internal class ConnectionCoordinatorTest {
    private val testDispatcher = StandardTestDispatcher()
    private val dispatchersProvider =
        object : DispatchersProvider {
            override val default: CoroutineDispatcher = testDispatcher
            override val io: CoroutineDispatcher = testDispatcher
            override val main: CoroutineDispatcher = testDispatcher
            override val immediate: CoroutineDispatcher = testDispatcher
        }

    private val appState = MutableStateFlow<AppLifecycle>(AppLifecycle.Foreground)
    private val channels = MutableStateFlow(persistentListOf("forsen".toUserName()))
    private val loadingState = MutableStateFlow<GlobalLoadingState>(GlobalLoadingState.Loaded)
    private val chatConnector: ChatConnector = mockk(relaxed = true)
    private val dataRepository: DataRepository = mockk(relaxed = true)
    private val chatChannelProvider: ChatChannelProvider = mockk { every { channels } returns this@ConnectionCoordinatorTest.channels }
    private val channelDataCoordinator: ChannelDataCoordinator = mockk { every { globalLoadingState } returns loadingState }
    private val authStateCoordinator: AuthStateCoordinator = mockk { coEvery { validateOnStartup() } returns null }
    private val startupValidationHolder: StartupValidationHolder = mockk { coEvery { awaitResolved() } returns Unit }
    private val appLifecycleListener: AppLifecycleListener = mockk { every { appState } returns this@ConnectionCoordinatorTest.appState }
    private val foregroundServiceState: ForegroundServiceState = mockk(relaxed = true)
    private val remotePushCoordinator: RemotePushCoordinator = mockk { every { isEnabled() } returns true }

    private lateinit var coordinator: ConnectionCoordinator

    @BeforeEach
    fun setup() {
        coordinator =
            ConnectionCoordinator(
                chatConnector = chatConnector,
                dataRepository = dataRepository,
                chatChannelProvider = chatChannelProvider,
                channelDataCoordinator = channelDataCoordinator,
                authStateCoordinator = authStateCoordinator,
                startupValidationHolder = startupValidationHolder,
                appLifecycleListener = appLifecycleListener,
                foregroundServiceState = foregroundServiceState,
                remotePushCoordinator = remotePushCoordinator,
                dispatchersProvider = dispatchersProvider,
            )
    }

    @Test
    fun `returning during grace period keeps connections active`() = runTest(testDispatcher) {
        coordinator.initialize()
        runCurrent()

        appState.value = AppLifecycle.Background
        advanceTimeBy(4.minutes)
        appState.value = AppLifecycle.Foreground
        runCurrent()

        coVerify(exactly = 0) { chatConnector.pauseForRemotePush() }
        verify(exactly = 0) { dataRepository.pauseForRemotePush() }
        verify(exactly = 0) { foregroundServiceState.setActive(false) }
    }

    @Test
    fun `connections pause after grace period and resume on foreground`() = runTest(testDispatcher) {
        coordinator.initialize()
        runCurrent()

        appState.value = AppLifecycle.Background
        advanceTimeBy(5.minutes)
        runCurrent()

        coVerify { chatConnector.pauseForRemotePush() }
        verify { dataRepository.pauseForRemotePush() }
        verify { foregroundServiceState.setActive(false) }

        appState.value = AppLifecycle.Foreground
        runCurrent()

        verify { foregroundServiceState.setActive(true) }
        verify { chatConnector.resumeAfterRemotePush(channels.value) }
        coVerify { dataRepository.reconnectIfNecessary() }
    }
}
