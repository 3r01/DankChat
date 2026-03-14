package com.flxrs.dankchat.main.compose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flxrs.dankchat.data.state.GlobalLoadingState
import com.flxrs.dankchat.domain.ChannelDataCoordinator
import com.flxrs.dankchat.preferences.appearance.AppearanceSettingsDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

/**
 * Minimal coordinator ViewModel for MainScreen.
 * 
 * Individual components have their own ViewModels:
 * - ChannelTabViewModel - Tab row state
 * - ChannelPagerViewModel - Pager state
 * - ChatInputViewModel - Input state
 * - ChannelManagementViewModel - Channel operations
 * 
 * This ViewModel only handles truly global concerns.
 */
@KoinViewModel
class MainScreenViewModel(
    private val channelDataCoordinator: ChannelDataCoordinator,
    private val appearanceSettingsDataStore: AppearanceSettingsDataStore,
) : ViewModel() {

    // Only expose truly global state
    val globalLoadingState: StateFlow<GlobalLoadingState> = 
        channelDataCoordinator.globalLoadingState

    val showInput: StateFlow<Boolean> = appearanceSettingsDataStore.settings
        .map { it.showInput }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _isFullscreen = MutableStateFlow(false)
    val isFullscreen: StateFlow<Boolean> = _isFullscreen.asStateFlow()

    private val _showAppBar = MutableStateFlow(true)
    val showAppBar: StateFlow<Boolean> = _showAppBar.asStateFlow()

    init {
        // Load global data once at startup
        channelDataCoordinator.loadGlobalData()
    }

    fun reloadGlobalData() {
        channelDataCoordinator.reloadGlobalData()
    }

    fun toggleInput() {
        viewModelScope.launch {
            appearanceSettingsDataStore.update { it.copy(showInput = !it.showInput) }
        }
    }

    fun toggleFullscreen() {
        _isFullscreen.update { !it }
    }

    fun toggleAppBar() {
        _showAppBar.update { !it }
    }

    fun retryDataLoading(dataFailures: Set<com.flxrs.dankchat.data.repo.data.DataLoadingFailure>, chatFailures: Set<com.flxrs.dankchat.data.repo.chat.ChatLoadingFailure>) {
        channelDataCoordinator.retryDataLoading(dataFailures, chatFailures)
    }
}
