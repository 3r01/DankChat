package com.flxrs.dankchat.main.compose

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.repo.chat.UserStateRepository
import com.flxrs.dankchat.data.state.GlobalLoadingState
import com.flxrs.dankchat.domain.ChannelDataCoordinator
import com.flxrs.dankchat.preferences.DankChatPreferenceStore
import com.flxrs.dankchat.preferences.appearance.AppearanceSettingsDataStore
import com.flxrs.dankchat.preferences.appearance.InputAction
import com.flxrs.dankchat.preferences.developer.DeveloperSettingsDataStore
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
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
@OptIn(FlowPreview::class)
@KoinViewModel
class MainScreenViewModel(
    private val channelDataCoordinator: ChannelDataCoordinator,
    private val appearanceSettingsDataStore: AppearanceSettingsDataStore,
    private val preferenceStore: DankChatPreferenceStore,
    private val developerSettingsDataStore: DeveloperSettingsDataStore,
    private val userStateRepository: UserStateRepository,
) : ViewModel() {

    // Only expose truly global state
    val globalLoadingState: StateFlow<GlobalLoadingState> =
        channelDataCoordinator.globalLoadingState

    private val _isFullscreen = MutableStateFlow(false)
    private val _gestureInputHidden = MutableStateFlow(false)
    private val _gestureToolbarHidden = MutableStateFlow(false)

    val uiState: StateFlow<MainScreenUiState> = combine(
        appearanceSettingsDataStore.settings,
        developerSettingsDataStore.settings.map { it.repeatedSending },
        _isFullscreen,
        _gestureInputHidden,
        _gestureToolbarHidden,
    ) { appearance, repeatedSending, isFullscreen, gestureInputHidden, gestureToolbarHidden ->
        MainScreenUiState(
            isFullscreen = isFullscreen,
            showInput = appearance.showInput,
            inputActions = appearance.inputActions.toImmutableList(),
            showCharacterCounter = appearance.showCharacterCounter,
            isRepeatedSendEnabled = repeatedSending,
            gestureInputHidden = gestureInputHidden,
            gestureToolbarHidden = gestureToolbarHidden,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MainScreenUiState())

    fun isModeratorInChannel(channel: UserName?): Boolean = userStateRepository.isModeratorInChannel(channel)

    // Keyboard height persistence — debounced to avoid thrashing during animation
    private val _keyboardHeightUpdates = MutableSharedFlow<KeyboardHeightUpdate>(extraBufferCapacity = 1)

    private val _keyboardHeightPx = MutableStateFlow(0)
    val keyboardHeightPx: StateFlow<Int> = _keyboardHeightPx.asStateFlow()

    fun setGestureInputHidden(hidden: Boolean) {
        _gestureInputHidden.value = hidden
    }

    fun setGestureToolbarHidden(hidden: Boolean) {
        _gestureToolbarHidden.value = hidden
    }

    fun resetGestureState() {
        _gestureInputHidden.value = false
        _gestureToolbarHidden.value = false
    }

    init {
        channelDataCoordinator.loadGlobalData()

        viewModelScope.launch {
            _keyboardHeightUpdates
                .debounce(300)
                .collect { (heightPx, isLandscape) ->
                    _keyboardHeightPx.value = heightPx
                    if (isLandscape) {
                        preferenceStore.keyboardHeightLandscape = heightPx
                    } else {
                        preferenceStore.keyboardHeightPortrait = heightPx
                    }
                }
        }
    }

    fun initKeyboardHeight(isLandscape: Boolean) {
        val persisted = if (isLandscape) preferenceStore.keyboardHeightLandscape else preferenceStore.keyboardHeightPortrait
        _keyboardHeightPx.value = persisted
    }

    fun trackKeyboardHeight(heightPx: Int, isLandscape: Boolean, minHeightPx: Float) {
        if (heightPx > minHeightPx) {
            _keyboardHeightUpdates.tryEmit(KeyboardHeightUpdate(heightPx, isLandscape))
        }
    }

    fun reloadGlobalData() {
        channelDataCoordinator.reloadGlobalData()
    }

    fun toggleInput() {
        viewModelScope.launch {
            appearanceSettingsDataStore.update { it.copy(showInput = !it.showInput) }
        }
    }

    fun updateInputActions(actions: ImmutableList<InputAction>) {
        viewModelScope.launch {
            appearanceSettingsDataStore.update { it.copy(inputActions = actions) }
        }
    }

    fun toggleFullscreen() {
        _isFullscreen.update { !it }
    }

    fun retryDataLoading(dataFailures: Set<com.flxrs.dankchat.data.repo.data.DataLoadingFailure>, chatFailures: Set<com.flxrs.dankchat.data.repo.chat.ChatLoadingFailure>) {
        channelDataCoordinator.retryDataLoading(dataFailures, chatFailures)
    }
}

private data class KeyboardHeightUpdate(val heightPx: Int, val isLandscape: Boolean)

@Immutable
data class MainScreenUiState(
    val isFullscreen: Boolean = false,
    val showInput: Boolean = true,
    val inputActions: ImmutableList<InputAction> = persistentListOf(),
    val showCharacterCounter: Boolean = false,
    val isRepeatedSendEnabled: Boolean = false,
    val gestureInputHidden: Boolean = false,
    val gestureToolbarHidden: Boolean = false,
) {
    val effectiveShowInput: Boolean get() = showInput && !gestureInputHidden
    val effectiveShowAppBar: Boolean get() = !gestureToolbarHidden
}
