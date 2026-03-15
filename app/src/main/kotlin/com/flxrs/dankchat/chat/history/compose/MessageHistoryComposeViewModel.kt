package com.flxrs.dankchat.chat.history.compose

import android.content.Context
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.placeCursorAtEnd
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.text.TextRange
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flxrs.dankchat.chat.compose.ChatMessageMapper
import com.flxrs.dankchat.chat.compose.ChatMessageUiState
import com.flxrs.dankchat.chat.search.ChatItemFilter
import com.flxrs.dankchat.chat.search.ChatSearchFilter
import com.flxrs.dankchat.chat.search.ChatSearchFilterParser
import com.flxrs.dankchat.chat.search.SearchFilterSuggestions
import com.flxrs.dankchat.chat.suggestion.Suggestion
import com.flxrs.dankchat.data.DisplayName
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.repo.chat.ChatRepository
import com.flxrs.dankchat.data.repo.chat.UsersRepository
import com.flxrs.dankchat.data.twitch.message.PrivMessage
import com.flxrs.dankchat.preferences.DankChatPreferenceStore
import com.flxrs.dankchat.preferences.appearance.AppearanceSettingsDataStore
import com.flxrs.dankchat.preferences.chat.ChatSettingsDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import com.flxrs.dankchat.utils.extensions.isEven
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam

@KoinViewModel
class MessageHistoryComposeViewModel(
    @InjectedParam private val channel: UserName,
    chatRepository: ChatRepository,
    usersRepository: UsersRepository,
    private val chatMessageMapper: ChatMessageMapper,
    private val context: Context,
    private val appearanceSettingsDataStore: AppearanceSettingsDataStore,
    private val chatSettingsDataStore: ChatSettingsDataStore,
    private val preferenceStore: DankChatPreferenceStore,
) : ViewModel() {

    val searchFieldState = TextFieldState()

    private val searchQuery = snapshotFlow { searchFieldState.text.toString() }
        .distinctUntilChanged()

    private val filters: Flow<List<ChatSearchFilter>> = merge(
        searchQuery.take(1),
        searchQuery.drop(1).debounce(300),
    )
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
            .mapIndexed { index, item ->
                val altBg = index.isEven && appearanceSettings.checkeredMessages
                chatMessageMapper.mapToUiState(
                    item = item,
                    context = context,
                    appearanceSettings = appearanceSettings,
                    chatSettings = chatSettings,
                    preferenceStore = preferenceStore,
                    isAlternateBackground = altBg,
                )
            }
    }.flowOn(Dispatchers.Default)

    private val users: StateFlow<Set<DisplayName>> = usersRepository.getUsersFlow(channel)

    private val badgeNames: StateFlow<Set<String>> = chatRepository.getChat(channel)
        .map { items ->
            items.asSequence()
                .map { it.message }
                .filterIsInstance<PrivMessage>()
                .flatMap { it.badges }
                .mapNotNull { it.badgeTag?.substringBefore('/') }
                .toSet()
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val filterSuggestions: StateFlow<List<Suggestion>> = combine(
        searchQuery,
        users,
        badgeNames,
    ) { query, userSet, badges ->
        SearchFilterSuggestions.filter(query, users = userSet, badgeNames = badges)
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setInitialQuery(query: String) {
        if (query.isNotEmpty()) {
            val normalizedQuery = if (query.endsWith(' ')) query else "$query "
            searchFieldState.edit {
                replace(0, length, normalizedQuery)
                placeCursorAtEnd()
            }
        }
    }

    fun applySuggestion(suggestion: Suggestion) {
        val currentText = searchFieldState.text.toString()
        val lastSpaceIndex = currentText.trimEnd().lastIndexOf(' ')
        val prefix = when {
            lastSpaceIndex >= 0 -> currentText.substring(0, lastSpaceIndex + 1)
            else -> ""
        }
        val keyword = suggestion.toString()
        val suffix = when {
            keyword.endsWith(':') -> ""
            else -> " "
        }
        val newText = prefix + keyword + suffix
        searchFieldState.edit {
            replace(0, length, newText)
            selection = TextRange(newText.length)
        }
    }
}
