package com.flxrs.dankchat.ui.chat.history

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.placeCursorAtEnd
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.text.TextRange
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flxrs.dankchat.data.DisplayName
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.repo.chat.ChatMessageRepository
import com.flxrs.dankchat.data.repo.chat.UsersRepository
import com.flxrs.dankchat.data.twitch.message.PrivMessage
import com.flxrs.dankchat.preferences.DankChatPreferenceStore
import com.flxrs.dankchat.preferences.appearance.AppearanceSettingsDataStore
import com.flxrs.dankchat.preferences.chat.ChatSettingsDataStore
import com.flxrs.dankchat.ui.chat.ChatDisplaySettings
import com.flxrs.dankchat.ui.chat.ChatMessageMapper
import com.flxrs.dankchat.ui.chat.ChatMessageUiState
import com.flxrs.dankchat.ui.chat.search.ChatItemFilter
import com.flxrs.dankchat.ui.chat.search.ChatSearchFilter
import com.flxrs.dankchat.ui.chat.search.ChatSearchFilterParser
import com.flxrs.dankchat.ui.chat.search.SearchFilterSuggestions
import com.flxrs.dankchat.ui.chat.suggestion.Suggestion
import com.flxrs.dankchat.utils.extensions.isEven
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
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
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam

@KoinViewModel
class MessageHistoryViewModel(
    @InjectedParam private val channel: UserName,
    chatMessageRepository: ChatMessageRepository,
    usersRepository: UsersRepository,
    private val chatMessageMapper: ChatMessageMapper,
    private val appearanceSettingsDataStore: AppearanceSettingsDataStore,
    private val chatSettingsDataStore: ChatSettingsDataStore,
    private val preferenceStore: DankChatPreferenceStore,
) : ViewModel() {
    val chatDisplaySettings: StateFlow<ChatDisplaySettings> =
        combine(
            appearanceSettingsDataStore.settings,
            chatSettingsDataStore.settings,
        ) { appearance, chat ->
            ChatDisplaySettings(
                fontSize = appearance.fontSize.toFloat(),
                showLineSeparator = appearance.lineSeparator,
                animateGifs = chat.animateGifs,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ChatDisplaySettings())

    val searchFieldState = TextFieldState()

    private val searchQuery =
        snapshotFlow { searchFieldState.text.toString() }
            .distinctUntilChanged()

    private val filters: Flow<List<ChatSearchFilter>> =
        merge(
            searchQuery.take(1),
            searchQuery.drop(1).debounce(300),
        ).map { ChatSearchFilterParser.parse(it) }
            .distinctUntilChanged()

    val historyUiStates: Flow<List<ChatMessageUiState>> =
        combine(
            chatMessageRepository.getChat(channel),
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
                        chatSettings = chatSettings,
                        preferenceStore = preferenceStore,
                        isAlternateBackground = altBg,
                    )
                }
        }.flowOn(Dispatchers.Default)

    private val users: StateFlow<ImmutableSet<DisplayName>> =
        usersRepository
            .getUsersFlow(channel)
            .map { it.toImmutableSet() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), persistentSetOf())

    private val badgeNames: StateFlow<ImmutableSet<String>> =
        chatMessageRepository
            .getChat(channel)
            .map { items ->
                items
                    .asSequence()
                    .map { it.message }
                    .filterIsInstance<PrivMessage>()
                    .flatMap { it.badges }
                    .mapNotNull { it.badgeTag?.substringBefore('/') }
                    .toImmutableSet()
            }.flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), persistentSetOf())

    val filterSuggestions: StateFlow<ImmutableList<Suggestion>> =
        combine(
            searchQuery,
            users,
            badgeNames,
        ) { query, userSet, badges ->
            SearchFilterSuggestions.filter(query, users = userSet, badgeNames = badges).toImmutableList()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), persistentListOf())

    fun setInitialQuery(query: String) {
        if (query.isNotEmpty()) {
            val normalizedQuery = if (query.endsWith(' ')) query else "$query "
            searchFieldState.edit {
                replace(0, length, normalizedQuery)
                placeCursorAtEnd()
            }
        }
    }

    fun insertText(text: String) {
        searchFieldState.edit {
            append(text)
            placeCursorAtEnd()
        }
    }

    fun applySuggestion(suggestion: Suggestion) {
        val currentText = searchFieldState.text.toString()
        val lastSpaceIndex = currentText.trimEnd().lastIndexOf(' ')
        val prefix =
            when {
                lastSpaceIndex >= 0 -> currentText.substring(0, lastSpaceIndex + 1)
                else -> ""
            }
        val keyword = suggestion.toString()
        val suffix =
            when {
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
