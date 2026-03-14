package com.flxrs.dankchat.chat.history.compose

import android.content.Context
import androidx.lifecycle.ViewModel
import com.flxrs.dankchat.chat.compose.ChatMessageMapper.toChatMessageUiState
import com.flxrs.dankchat.chat.compose.ChatMessageUiState
import com.flxrs.dankchat.chat.search.ChatItemFilter
import com.flxrs.dankchat.chat.search.ChatSearchFilter
import com.flxrs.dankchat.chat.search.ChatSearchFilterParser
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.repo.chat.ChatRepository
import com.flxrs.dankchat.preferences.DankChatPreferenceStore
import com.flxrs.dankchat.preferences.appearance.AppearanceSettingsDataStore
import com.flxrs.dankchat.preferences.chat.ChatSettingsDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam

@KoinViewModel
class MessageHistoryComposeViewModel(
    @InjectedParam private val channel: UserName,
    chatRepository: ChatRepository,
    private val context: Context,
    private val appearanceSettingsDataStore: AppearanceSettingsDataStore,
    private val chatSettingsDataStore: ChatSettingsDataStore,
    private val preferenceStore: DankChatPreferenceStore,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val filters: Flow<List<ChatSearchFilter>> = _searchQuery
        .debounce(300)
        .map { ChatSearchFilterParser.parse(it) }
        .distinctUntilChanged()

    val historyUiStates: Flow<List<ChatMessageUiState>> = combine(
        chatRepository.getChat(channel),
        filters,
        appearanceSettingsDataStore.settings,
        chatSettingsDataStore.settings,
    ) { messages, activeFilters, appearanceSettings, chatSettings ->
        messages
            .filter { ChatItemFilter.matches(it, activeFilters) }
            .map {
                it.toChatMessageUiState(
                    context = context,
                    appearanceSettings = appearanceSettings,
                    chatSettings = chatSettings,
                    preferenceStore = preferenceStore,
                    isAlternateBackground = false,
                )
            }
    }.flowOn(Dispatchers.Default)

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
}
