package com.flxrs.dankchat.ui.chat.suggestion

import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.repo.chat.UsersRepository
import com.flxrs.dankchat.data.repo.command.CommandRepository
import com.flxrs.dankchat.data.repo.emote.EmojiData
import com.flxrs.dankchat.data.repo.emote.EmojiRepository
import com.flxrs.dankchat.data.repo.emote.EmoteRepository
import com.flxrs.dankchat.data.repo.emote.EmoteUsageRepository
import com.flxrs.dankchat.preferences.chat.ChatSettingsDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

@Single
class SuggestionProvider(
    private val emoteRepository: EmoteRepository,
    private val usersRepository: UsersRepository,
    private val commandRepository: CommandRepository,
    private val chatSettingsDataStore: ChatSettingsDataStore,
    private val emoteUsageRepository: EmoteUsageRepository,
    private val emojiRepository: EmojiRepository,
) {

    fun getSuggestions(
        inputText: String,
        cursorPosition: Int,
        channel: UserName?
    ): Flow<List<Suggestion>> {
        if (inputText.isBlank() || channel == null) {
            return flowOf(emptyList())
        }

        val currentWord = extractCurrentWord(inputText, cursorPosition)
        if (currentWord.isBlank()) {
            return flowOf(emptyList())
        }

        // ':' trigger: emote + emoji mode with reduced min chars
        val isEmoteTrigger = currentWord.startsWith(':')
        val emoteQuery = when {
            isEmoteTrigger -> currentWord.removePrefix(":")
            else           -> currentWord
        }

        if (isEmoteTrigger && emoteQuery.isEmpty()) {
            return flowOf(emptyList())
        }
        if (!isEmoteTrigger && currentWord.length < MIN_SUGGESTION_CHARS) {
            return flowOf(emptyList())
        }

        if (isEmoteTrigger) {
            val emojiSuggestions = filterEmojis(emojiRepository.emojis.value, emoteQuery)
            return getEmoteSuggestionsScored(channel, emoteQuery).map { emotePairs ->
                (emotePairs + emojiSuggestions)
                    .sortedBy { it.second }
                    .map { it.first }
                    .take(MAX_SUGGESTIONS)
            }
        }

        return combine(
            getEmoteSuggestions(channel, currentWord),
            getUserSuggestions(channel, currentWord),
            getCommandSuggestions(channel, currentWord),
            chatSettingsDataStore.settings.map { it.preferEmoteSuggestions }
        ) { emotes, users, commands, preferEmotes ->
            val orderedSuggestions = when {
                preferEmotes -> emotes + users + commands
                else         -> users + emotes + commands
            }

            orderedSuggestions.take(MAX_SUGGESTIONS)
        }
    }

    private fun getEmoteSuggestions(channel: UserName, constraint: String): Flow<List<Suggestion.EmoteSuggestion>> {
        return emoteRepository.getEmotes(channel).map { emotes ->
            val recentIds = emoteUsageRepository.recentEmoteIds.value
            val suggestions = emotes.suggestions.map { Suggestion.EmoteSuggestion(it) }
            filterEmotes(suggestions, constraint, recentIds)
        }
    }

    private fun getEmoteSuggestionsScored(channel: UserName, constraint: String): Flow<List<Pair<Suggestion, Int>>> {
        return emoteRepository.getEmotes(channel).map { emotes ->
            val recentIds = emoteUsageRepository.recentEmoteIds.value
            val suggestions = emotes.suggestions.map { Suggestion.EmoteSuggestion(it) }
            filterEmotesScored(suggestions, constraint, recentIds)
        }
    }

    private fun getUserSuggestions(channel: UserName, constraint: String): Flow<List<Suggestion.UserSuggestion>> {
        return usersRepository.getUsersFlow(channel).map { displayNameSet ->
            val suggestions = displayNameSet.map { Suggestion.UserSuggestion(it) }
            filterUsers(suggestions, constraint)
        }
    }

    private fun getCommandSuggestions(channel: UserName, constraint: String): Flow<List<Suggestion.CommandSuggestion>> {
        return combine(
            commandRepository.getCommandTriggers(channel),
            commandRepository.getSupibotCommands(channel)
        ) { triggers, supibotCommands ->
            val allCommands = (triggers + supibotCommands).map { Suggestion.CommandSuggestion(it) }
            filterCommands(allCommands, constraint)
        }
    }

    internal fun extractCurrentWord(text: String, cursorPosition: Int): String {
        val cursorPos = cursorPosition.coerceIn(0, text.length)
        val separator = ' '

        var start = cursorPos
        while (start > 0 && text[start - 1] != separator) start--

        return text.substring(start, cursorPos)
    }

    internal fun scoreEmote(code: String, query: String, isRecentlyUsed: Boolean): Int {
        val tier = when {
            code == query                              -> 0
            code.startsWith(query)                     -> 100
            code.startsWith(query, ignoreCase = true)  -> 200
            code.contains(query)                       -> 300
            code.contains(query, ignoreCase = true)    -> 400
            else                                       -> -1
        }
        if (tier < 0) return tier

        val lengthPenalty = code.length - query.length
        val usageBoost = if (isRecentlyUsed) -50 else 0
        return tier + lengthPenalty + usageBoost
    }

    internal fun filterEmotes(
        suggestions: List<Suggestion.EmoteSuggestion>,
        constraint: String,
        recentEmoteIds: Set<String>,
    ): List<Suggestion.EmoteSuggestion> {
        return filterEmotesScored(suggestions, constraint, recentEmoteIds).map { it.first as Suggestion.EmoteSuggestion }
    }

    private fun filterEmotesScored(
        suggestions: List<Suggestion.EmoteSuggestion>,
        constraint: String,
        recentEmoteIds: Set<String>,
    ): List<Pair<Suggestion, Int>> {
        return suggestions
            .mapNotNull { suggestion ->
                val score = scoreEmote(suggestion.emote.code, constraint, suggestion.emote.id in recentEmoteIds)
                if (score < 0) null else (suggestion as Suggestion) to score
            }
            .sortedBy { it.second }
    }

    internal fun filterEmojis(
        emojis: List<EmojiData>,
        constraint: String,
    ): List<Pair<Suggestion, Int>> {
        return emojis.mapNotNull { emoji ->
            val score = scoreEmote(emoji.code, constraint, isRecentlyUsed = false)
            if (score < 0) null else (Suggestion.EmojiSuggestion(emoji) as Suggestion) to score
        }
    }

    internal fun filterUsers(
        suggestions: List<Suggestion.UserSuggestion>,
        constraint: String
    ): List<Suggestion.UserSuggestion> {
        return suggestions
            .mapNotNull { suggestion ->
                when {
                    constraint.startsWith('@') -> suggestion.copy(withLeadingAt = true)
                    else                       -> suggestion
                }.takeIf { it.toString().startsWith(constraint, ignoreCase = true) }
            }
            .sortedBy { it.name.value.lowercase() }
    }

    internal fun filterCommands(
        suggestions: List<Suggestion.CommandSuggestion>,
        constraint: String
    ): List<Suggestion.CommandSuggestion> {
        return suggestions
            .filter { it.command.startsWith(constraint, ignoreCase = true) }
            .sortedBy { it.command.lowercase() }
    }

    companion object {
        private const val MAX_SUGGESTIONS = 50
        private const val MIN_SUGGESTION_CHARS = 2
    }
}
