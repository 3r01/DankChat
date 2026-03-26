package com.flxrs.dankchat.ui.chat.suggestion

import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.repo.chat.UsersRepository
import com.flxrs.dankchat.data.repo.command.CommandRepository
import com.flxrs.dankchat.data.repo.emote.EmojiData
import com.flxrs.dankchat.data.repo.emote.EmojiRepository
import com.flxrs.dankchat.data.repo.emote.EmoteRepository
import com.flxrs.dankchat.data.repo.emote.EmoteUsageRepository
import com.flxrs.dankchat.data.twitch.emote.GenericEmote
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
            val emojiResults = filterEmojis(emojiRepository.emojis.value, emoteQuery)
            return getScoredEmoteSuggestions(channel, emoteQuery).map { emoteResults ->
                mergeSorted(emoteResults, emojiResults)
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
            filterEmotes(emotes.suggestions, constraint, recentIds)
        }
    }

    private fun getScoredEmoteSuggestions(channel: UserName, constraint: String): Flow<List<ScoredSuggestion>> {
        return emoteRepository.getEmotes(channel).map { emotes ->
            val recentIds = emoteUsageRepository.recentEmoteIds.value
            filterEmotesScored(emotes.suggestions, constraint, recentIds)
        }
    }

    private fun getUserSuggestions(channel: UserName, constraint: String): Flow<List<Suggestion.UserSuggestion>> {
        return usersRepository.getUsersFlow(channel).map { displayNameSet ->
            filterUsers(displayNameSet, constraint)
        }
    }

    private fun getCommandSuggestions(channel: UserName, constraint: String): Flow<List<Suggestion.CommandSuggestion>> {
        return combine(
            commandRepository.getCommandTriggers(channel),
            commandRepository.getSupibotCommands(channel)
        ) { triggers, supibotCommands ->
            filterCommands(triggers + supibotCommands, constraint)
        }
    }

    // Merge two pre-sorted lists in O(n+m) without intermediate allocations
    private fun mergeSorted(a: List<ScoredSuggestion>, b: List<ScoredSuggestion>): List<Suggestion> {
        val result = mutableListOf<Suggestion>()
        var i = 0
        var j = 0
        while (result.size < MAX_SUGGESTIONS && (i < a.size || j < b.size)) {
            val pick = when {
                i >= a.size              -> b[j++]
                j >= b.size              -> a[i++]
                a[i].score <= b[j].score -> a[i++]
                else                     -> b[j++]
            }
            result.add(pick.suggestion)
        }
        return result
    }

    internal fun extractCurrentWord(text: String, cursorPosition: Int): String {
        val cursorPos = cursorPosition.coerceIn(0, text.length)
        val separator = ' '

        var start = cursorPos
        while (start > 0 && text[start - 1] != separator) start--

        return text.substring(start, cursorPos)
    }

    // Scoring based on Chatterino2's SmartEmoteStrategy by Mm2PL
    // https://github.com/Chatterino/chatterino2/pull/4987
    internal fun scoreEmote(code: String, query: String, isRecentlyUsed: Boolean): Int {
        val matchIndex = code.indexOf(query, ignoreCase = true)
        if (matchIndex < 0) return NO_MATCH

        var caseDiffs = 0
        for (i in query.indices) {
            if (code[matchIndex + i] != query[i]) caseDiffs++
        }

        val extraChars = code.length - query.length
        val caseCost = if (caseDiffs == 0) -10 else caseDiffs
        val usageBoost = if (isRecentlyUsed) -50 else 0
        return caseCost + extraChars * 100 + usageBoost
    }

    // Score raw GenericEmotes, only wrap matches
    internal fun filterEmotes(
        emotes: List<GenericEmote>,
        constraint: String,
        recentEmoteIds: Set<String>,
    ): List<Suggestion.EmoteSuggestion> {
        return filterEmotesScored(emotes, constraint, recentEmoteIds).map { it.suggestion as Suggestion.EmoteSuggestion }
    }

    private fun filterEmotesScored(
        emotes: List<GenericEmote>,
        constraint: String,
        recentEmoteIds: Set<String>,
    ): List<ScoredSuggestion> {
        return emotes
            .mapNotNull { emote ->
                val score = scoreEmote(emote.code, constraint, emote.id in recentEmoteIds)
                if (score == NO_MATCH) null else ScoredSuggestion(Suggestion.EmoteSuggestion(emote), score)
            }
            .sortedBy { it.score }
    }

    // Score raw EmojiData, only wrap matches
    internal fun filterEmojis(
        emojis: List<EmojiData>,
        constraint: String,
    ): List<ScoredSuggestion> {
        return emojis
            .mapNotNull { emoji ->
                val score = scoreEmote(emoji.code, constraint, isRecentlyUsed = false)
                if (score == NO_MATCH) null else ScoredSuggestion(Suggestion.EmojiSuggestion(emoji), score)
            }
            .sortedBy { it.score }
    }

    // Filter raw DisplayName set, only wrap matches
    internal fun filterUsers(
        users: Set<com.flxrs.dankchat.data.DisplayName>,
        constraint: String,
    ): List<Suggestion.UserSuggestion> {
        val withAt = constraint.startsWith('@')
        return users
            .mapNotNull { name ->
                val suggestion = Suggestion.UserSuggestion(name, withLeadingAt = withAt)
                suggestion.takeIf { it.toString().startsWith(constraint, ignoreCase = true) }
            }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name.value })
    }

    // Filter raw command strings, only wrap matches
    internal fun filterCommands(
        commands: List<String>,
        constraint: String,
    ): List<Suggestion.CommandSuggestion> {
        return commands
            .filter { it.startsWith(constraint, ignoreCase = true) }
            .sortedWith(String.CASE_INSENSITIVE_ORDER)
            .map { Suggestion.CommandSuggestion(it) }
    }

    companion object {
        internal const val NO_MATCH = Int.MIN_VALUE
        private const val MAX_SUGGESTIONS = 50
        private const val MIN_SUGGESTION_CHARS = 2
    }
}

internal class ScoredSuggestion(val suggestion: Suggestion, val score: Int)
