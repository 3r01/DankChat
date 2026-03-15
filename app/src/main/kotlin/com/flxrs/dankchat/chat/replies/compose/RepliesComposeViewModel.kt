package com.flxrs.dankchat.chat.replies.compose

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flxrs.dankchat.chat.compose.ChatMessageMapper
import com.flxrs.dankchat.chat.replies.RepliesState
import com.flxrs.dankchat.chat.replies.RepliesUiState
import com.flxrs.dankchat.data.repo.RepliesRepository
import com.flxrs.dankchat.preferences.DankChatPreferenceStore
import com.flxrs.dankchat.preferences.appearance.AppearanceSettingsDataStore
import com.flxrs.dankchat.preferences.chat.ChatSettingsDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.android.annotation.KoinViewModel
import com.flxrs.dankchat.utils.extensions.isEven
import org.koin.core.annotation.InjectedParam
import kotlin.time.Duration.Companion.seconds

@KoinViewModel
class RepliesComposeViewModel(
    @InjectedParam private val rootMessageId: String,
    repliesRepository: RepliesRepository,
    private val chatMessageMapper: ChatMessageMapper,
    private val context: Context,
    private val appearanceSettingsDataStore: AppearanceSettingsDataStore,
    private val chatSettingsDataStore: ChatSettingsDataStore,
    private val preferenceStore: DankChatPreferenceStore,
) : ViewModel() {

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
            is RepliesState.Found -> {
                val uiMessages = repliesState.items.mapIndexed { index, item ->
                    val altBg = index.isEven && appearanceSettings.checkeredMessages
                    chatMessageMapper.mapToUiState(
                        item = item,
                        context = context,
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
