package com.flxrs.dankchat.chat.replies.compose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flxrs.dankchat.chat.compose.ChatDisplaySettings
import com.flxrs.dankchat.chat.compose.ChatMessageMapper
import com.flxrs.dankchat.chat.replies.RepliesState
import com.flxrs.dankchat.chat.replies.RepliesUiState
import com.flxrs.dankchat.data.repo.RepliesRepository
import com.flxrs.dankchat.preferences.DankChatPreferenceStore
import com.flxrs.dankchat.preferences.appearance.AppearanceSettingsDataStore
import com.flxrs.dankchat.preferences.chat.ChatSettingsDataStore
import com.flxrs.dankchat.utils.extensions.isEven
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam

@KoinViewModel
class RepliesComposeViewModel(
    @InjectedParam private val rootMessageId: String,
    repliesRepository: RepliesRepository,
    private val chatMessageMapper: ChatMessageMapper,
    private val appearanceSettingsDataStore: AppearanceSettingsDataStore,
    private val chatSettingsDataStore: ChatSettingsDataStore,
    private val preferenceStore: DankChatPreferenceStore,
) : ViewModel() {

    val chatDisplaySettings: StateFlow<ChatDisplaySettings> = combine(
        appearanceSettingsDataStore.settings,
        chatSettingsDataStore.settings,
    ) { appearance, chat ->
        ChatDisplaySettings(
            fontSize = appearance.fontSize.toFloat(),
            showLineSeparator = appearance.lineSeparator,
            animateGifs = chat.animateGifs,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ChatDisplaySettings())

    val state = repliesRepository.getThreadItemsFlow(rootMessageId)
        .map {
            when {
                it.isEmpty() -> RepliesState.NotFound
                else         -> RepliesState.Found(it)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RepliesState.Found(emptyList()))

    val uiState: StateFlow<RepliesUiState> = combine(
        state,
        appearanceSettingsDataStore.settings,
        chatSettingsDataStore.settings
    ) { repliesState, appearanceSettings, chatSettings ->
        when (repliesState) {
            is RepliesState.NotFound -> RepliesUiState.NotFound
            is RepliesState.Found    -> {
                val uiMessages = repliesState.items.mapIndexed { index, item ->
                    val altBg = index.isEven && appearanceSettings.checkeredMessages
                    chatMessageMapper.mapToUiState(
                        item = item,
                        appearanceSettings = appearanceSettings,
                        chatSettings = chatSettings,
                        preferenceStore = preferenceStore,
                        isAlternateBackground = altBg
                    )
                }
                RepliesUiState.Found(uiMessages)
            }
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RepliesUiState.Found(emptyList()))
}
