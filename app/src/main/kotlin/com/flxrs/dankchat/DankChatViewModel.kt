package com.flxrs.dankchat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flxrs.dankchat.auth.AuthDataStore
import com.flxrs.dankchat.auth.AuthStateCoordinator
import com.flxrs.dankchat.data.repo.chat.ChatChannelProvider
import com.flxrs.dankchat.data.repo.chat.ChatConnector
import com.flxrs.dankchat.data.repo.data.DataRepository
import com.flxrs.dankchat.preferences.DankChatPreferenceStore
import com.flxrs.dankchat.preferences.appearance.AppearanceSettingsDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import kotlin.time.Duration.Companion.seconds

@KoinViewModel
class DankChatViewModel(
    private val chatChannelProvider: ChatChannelProvider,
    private val chatConnector: ChatConnector,
    private val preferenceStore: DankChatPreferenceStore,
    private val authDataStore: AuthDataStore,
    private val appearanceSettingsDataStore: AppearanceSettingsDataStore,
    private val dataRepository: DataRepository,
    private val authStateCoordinator: AuthStateCoordinator,
) : ViewModel() {

    val serviceEvents = dataRepository.serviceEvents
    private var initialConnectionStarted = false

    val activeChannel = chatChannelProvider.activeChannel
    val isLoggedIn: Flow<Boolean> = authDataStore.settings
        .map { it.isLoggedIn }
        .distinctUntilChanged()

    val isTrueDarkModeEnabled get() = appearanceSettingsDataStore.current().trueDarkTheme
    val keepScreenOn = appearanceSettingsDataStore.settings
        .map { it.keepScreenOn }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5.seconds),
            initialValue = appearanceSettingsDataStore.current().keepScreenOn,
        )

    init {
        viewModelScope.launch {
            authStateCoordinator.validateOnStartup()
            initialConnectionStarted = true
            val channels = preferenceStore.channels
            chatChannelProvider.setChannels(channels)
            chatConnector.connectAndJoin(channels)
        }
    }

    fun reconnectIfNecessary() {
        if (!initialConnectionStarted) return

        viewModelScope.launch {
            chatConnector.reconnectIfNecessary()
            dataRepository.reconnectIfNecessary()
        }
    }

    fun checkLogin() {
        if (authDataStore.isLoggedIn && authDataStore.oAuthKey.isNullOrBlank()) {
            authStateCoordinator.logout()
        }
    }

    fun clearDataForLogout() {
        authStateCoordinator.logout()
    }
}
