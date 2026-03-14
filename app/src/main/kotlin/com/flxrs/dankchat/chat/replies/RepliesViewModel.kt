package com.flxrs.dankchat.chat.replies

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flxrs.dankchat.chat.compose.ChatMessageMapper.toChatMessageUiState
import com.flxrs.dankchat.chat.compose.ChatMessageUiState
import com.flxrs.dankchat.data.repo.RepliesRepository
import com.flxrs.dankchat.preferences.appearance.AppearanceSettingsDataStore
import com.flxrs.dankchat.preferences.chat.ChatSettingsDataStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.android.annotation.KoinViewModel
import kotlin.time.Duration.Companion.seconds

@KoinViewModel
class RepliesViewModel(
    repliesRepository: RepliesRepository,
    savedStateHandle: SavedStateHandle,
    private val context: Context,
    private val appearanceSettingsDataStore: AppearanceSettingsDataStore,
    private val chatSettingsDataStore: ChatSettingsDataStore,
) : ViewModel() {

    private val args = RepliesFragmentArgs.fromSavedStateHandle(savedStateHandle)

    val state = repliesRepository.getThreadItemsFlow(args.rootMessageId)
        .map {
            when {
                it.isEmpty() -> RepliesState.NotFound
                else         -> RepliesState.Found(it)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), RepliesState.Found(emptyList()))
        
    val uiState: StateFlow<RepliesUiState> = combine(
        state,
        appearanceSettingsDataStore.settings,
        chatSettingsDataStore.settings
    ) { repliesState, appearanceSettings, chatSettings ->
        when (repliesState) {
            is RepliesState.NotFound -> RepliesUiState.NotFound
            is RepliesState.Found -> {
                val uiMessages = repliesState.items.map { item ->
                    item.toChatMessageUiState(
                        context = context,
                        appearanceSettings = appearanceSettings,
                        chatSettings = chatSettings,
                        isAlternateBackground = false // No alternating in replies
                    )
                }
                RepliesUiState.Found(uiMessages)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), RepliesUiState.Found(emptyList()))
}

sealed interface RepliesUiState {
    data object NotFound : RepliesUiState
    data class Found(val items: List<ChatMessageUiState>) : RepliesUiState
}
