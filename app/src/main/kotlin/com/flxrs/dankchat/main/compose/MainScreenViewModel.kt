package com.flxrs.dankchat.main.compose

import androidx.lifecycle.ViewModel
import com.flxrs.dankchat.data.state.GlobalLoadingState
import com.flxrs.dankchat.domain.ChannelDataCoordinator
import kotlinx.coroutines.flow.StateFlow
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
) : ViewModel() {

    // Only expose truly global state
    val globalLoadingState: StateFlow<GlobalLoadingState> = 
        channelDataCoordinator.globalLoadingState

    init {
        // Load global data once at startup
        channelDataCoordinator.loadGlobalData()
    }

    fun reloadGlobalData() {
        channelDataCoordinator.reloadGlobalData()
    }
}
