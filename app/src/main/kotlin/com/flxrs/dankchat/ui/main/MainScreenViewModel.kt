package com.flxrs.dankchat.ui.main

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
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
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
    val globalLoadingState: StateFlow<GlobalLoadingState> =
        channelDataCoordinator.globalLoadingState

    private val _isFullscreen = MutableStateFlow(false)
    private val _gestureInputHidden = MutableStateFlow(false)
    private val _gestureToolbarHidden = MutableStateFlow(false)

    val uiState: StateFlow<MainScreenUiState> =
        combine(
            appearanceSettingsDataStore.settings,
            developerSettingsDataStore.settings,
            _isFullscreen,
            _gestureInputHidden,
            _gestureToolbarHidden,
        ) { appearance, developerSettings, isFullscreen, gestureInputHidden, gestureToolbarHidden ->
            MainScreenUiState(
                isFullscreen = isFullscreen,
                showInput = appearance.showInput,
                inputActions = appearance.inputActions.toImmutableList(),
                showCharacterCounter = appearance.showCharacterCounter,
                isRepeatedSendEnabled = developerSettings.repeatedSending,
                debugMode = developerSettings.debugMode,
                gestureInputHidden = gestureInputHidden,
                gestureToolbarHidden = gestureToolbarHidden,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MainScreenUiState())

    init {
        viewModelScope.launch {
            developerSettingsDataStore.settings
                .map { it.debugMode }
                .distinctUntilChanged()
                .collect { enabled ->
                    appearanceSettingsDataStore.update { appearance ->
                        val actions = appearance.inputActions
                        when {
                            enabled && InputAction.Debug !in actions && actions.size < MAX_INPUT_ACTIONS -> {
                                appearance.copy(inputActions = actions + InputAction.Debug)
                            }

                            !enabled && InputAction.Debug in actions -> {
                                appearance.copy(inputActions = actions - InputAction.Debug)
                            }

                            else -> {
                                appearance
                            }
                        }
                    }
                }
        }
    }

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

    fun trackKeyboardHeight(
        heightPx: Int,
        isLandscape: Boolean,
        minHeightPx: Float,
    ) {
        if (heightPx > minHeightPx) {
            _keyboardHeightUpdates.tryEmit(KeyboardHeightUpdate(heightPx, isLandscape))
        }
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

    fun retryDataLoading(failedState: GlobalLoadingState.Failed) {
        channelDataCoordinator.retryDataLoading(failedState)
    }

    companion object {
        private const val MAX_INPUT_ACTIONS = 4
    }
}

private data class KeyboardHeightUpdate(
    val heightPx: Int,
    val isLandscape: Boolean,
)
