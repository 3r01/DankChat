package com.flxrs.dankchat.chat.suggestion

import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.repo.chat.UsersRepository
import com.flxrs.dankchat.data.repo.emote.EmoteRepository
import com.flxrs.dankchat.preferences.chat.ChatSettingsDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

/**
 * Provides suggestion filtering logic for chat input.
 * Filters emotes, users, and commands based on input text.
 */
@Single
class SuggestionProvider(
    private val emoteRepository: EmoteRepository,
    private val usersRepository: UsersRepository,
    private val chatSettingsDataStore: ChatSettingsDataStore,
) {

    /**
     * Get filtered suggestions for the given input text and channel.
     * 
     * Returns a Flow that emits filtered suggestions whenever:
     * - Input text changes
     * - Available emotes/users/commands change
     * - Preference for emote ordering changes
     */
    fun getSuggestions(
        inputText: String,
        channel: UserName?
    ): Flow<List<Suggestion>> {
        if (inputText.isBlank() || channel == null) {
            return flowOf(emptyList())
        }

        // Extract the current word being typed
        val currentWord = extractCurrentWord(inputText)
        if (currentWord.isBlank() || currentWord.length < MIN_SUGGESTION_CHARS) {
            return flowOf(emptyList())
        }

        return combine(
            getEmoteSuggestions(channel, currentWord),
            getUserSuggestions(channel, currentWord),
            getCommandSuggestions(channel, currentWord),
            chatSettingsDataStore.settings.map { it.preferEmoteSuggestions }
        ) { emotes, users, commands, preferEmotes ->
            // Order suggestions based on user preference
            val orderedSuggestions = when {
                preferEmotes -> emotes + users + commands
                else -> users + emotes + commands
            }
            
            // Limit results to reasonable number
            orderedSuggestions.take(MAX_SUGGESTIONS)
        }
    }

    private fun getEmoteSuggestions(channel: UserName, constraint: String): Flow<List<Suggestion.EmoteSuggestion>> {
        return emoteRepository.getEmotes(channel).map { emotes ->
            val suggestions = emotes.suggestions.map { Suggestion.EmoteSuggestion(it) }
            filterEmotes(suggestions, constraint)
        }
    }

    private fun getUserSuggestions(channel: UserName, constraint: String): Flow<List<Suggestion.UserSuggestion>> {
        return usersRepository.getUsersFlow(channel).map { displayNameSet ->
            val suggestions = displayNameSet.map { Suggestion.UserSuggestion(it) }
            filterUsers(suggestions, constraint)
        }
    }

    private fun getCommandSuggestions(channel: UserName, constraint: String): Flow<List<Suggestion.CommandSuggestion>> {
        // TODO: Implement actual command fetching from CommandRepository
        // For now, return empty list
        return flowOf(emptyList())
    }

    /**
     * Extract the current word being typed from the full input text.
     * Assumes space-separated words.
     */
    private fun extractCurrentWord(text: String): String {
        val cursorPos = text.length
        val separator = ' '
        
        // Find start of current word
        var start = cursorPos
        while (start > 0 && text[start - 1] != separator) start--
        
        // Find end of current word
        var end = cursorPos
        while (end < text.length && text[end] != separator) end++
        
        return text.substring(start, end)
    }

    /**
     * Filter emote suggestions by constraint.
     * Prioritizes exact matches over case-insensitive matches.
     */
    private fun filterEmotes(
        suggestions: List<Suggestion.EmoteSuggestion>,
        constraint: String
    ): List<Suggestion.EmoteSuggestion> {
        val exactMatches = suggestions.filter { it.emote.code.contains(constraint) }
        val caseInsensitiveMatches = (suggestions - exactMatches.toSet()).filter {
            it.emote.code.contains(constraint, ignoreCase = true)
        }
        return exactMatches + caseInsensitiveMatches
    }

    /**
     * Filter user suggestions by constraint.
     * Handles @ prefix for mentions.
     */
    private fun filterUsers(
        suggestions: List<Suggestion.UserSuggestion>,
        constraint: String
    ): List<Suggestion.UserSuggestion> {
        return suggestions.mapNotNull { suggestion ->
            when {
                constraint.startsWith('@') -> suggestion.copy(withLeadingAt = true)
                else -> suggestion
            }.takeIf { it.toString().startsWith(constraint, ignoreCase = true) }
        }
    }

    /**
     * Filter command suggestions by constraint.
     */
    private fun filterCommands(
        suggestions: List<Suggestion.CommandSuggestion>,
        constraint: String
    ): List<Suggestion.CommandSuggestion> {
        return suggestions.filter { it.command.startsWith(constraint, ignoreCase = true) }
    }

    companion object {
        private const val MAX_SUGGESTIONS = 50
        private const val MIN_SUGGESTION_CHARS = 2
    }
}
