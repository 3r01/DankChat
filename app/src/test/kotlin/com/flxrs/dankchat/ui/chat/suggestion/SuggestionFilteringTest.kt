package com.flxrs.dankchat.ui.chat.suggestion

import com.flxrs.dankchat.data.DisplayName
import com.flxrs.dankchat.data.twitch.emote.EmoteType
import com.flxrs.dankchat.data.twitch.emote.GenericEmote
import io.mockk.mockk
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class SuggestionFilteringTest {

    private val provider = SuggestionProvider(
        emoteRepository = mockk(),
        usersRepository = mockk(),
        commandRepository = mockk(),
        chatSettingsDataStore = mockk(),
        emoteUsageRepository = mockk(),
    )

    private fun emote(code: String, id: String = code) = Suggestion.EmoteSuggestion(
        GenericEmote(code = code, url = "", lowResUrl = "", id = id, scale = 1, emoteType = EmoteType.GlobalTwitchEmote)
    )

    // region filterEmotes

    @Test
    fun `emotes sorted by score - prefix before substring`() {
        val suggestions = listOf(emote("wePog"), emote("PogChamp"), emote("Pog"))
        val result = provider.filterEmotes(suggestions, "Pog", emptySet())

        assertEquals(
            expected = listOf("Pog", "PogChamp", "wePog"),
            actual = result.map { it.emote.code },
        )
    }

    @Test
    fun `emotes sorted by score - shorter before longer in same tier`() {
        val suggestions = listOf(emote("PogChamp"), emote("PogU"), emote("PogSlide"))
        val result = provider.filterEmotes(suggestions, "Pog", emptySet())

        assertEquals(
            expected = listOf("PogU", "PogChamp", "PogSlide"),
            actual = result.map { it.emote.code },
        )
    }

    @Test
    fun `emotes sorted by score - exact case prefix before case-insensitive prefix`() {
        val suggestions = listOf(emote("POGGERS"), emote("PogChamp"))
        val result = provider.filterEmotes(suggestions, "Pog", emptySet())

        assertEquals(
            expected = listOf("PogChamp", "POGGERS"),
            actual = result.map { it.emote.code },
        )
    }

    @Test
    fun `recently used emote boosted within tier`() {
        val suggestions = listOf(emote("PogChamp", id = "1"), emote("PogU", id = "2"))
        val result = provider.filterEmotes(suggestions, "Pog", setOf("1"))

        // PogChamp (105 - 50 = 55) beats PogU (101)
        assertEquals(
            expected = listOf("PogChamp", "PogU"),
            actual = result.map { it.emote.code },
        )
    }

    @Test
    fun `recently used substring does not beat non-used prefix`() {
        val suggestions = listOf(emote("wePog", id = "1"), emote("PogChamp", id = "2"))
        val result = provider.filterEmotes(suggestions, "Pog", setOf("1"))

        assertEquals(
            expected = listOf("PogChamp", "wePog"),
            actual = result.map { it.emote.code },
        )
    }

    @Test
    fun `non-matching emotes are excluded`() {
        val suggestions = listOf(emote("Kappa"), emote("PogChamp"), emote("LUL"))
        val result = provider.filterEmotes(suggestions, "Pog", emptySet())

        assertEquals(
            expected = listOf("PogChamp"),
            actual = result.map { it.emote.code },
        )
    }

    // endregion

    // region filterUsers

    @Test
    fun `users sorted alphabetically`() {
        val suggestions = listOf(
            Suggestion.UserSuggestion(DisplayName("Zed")),
            Suggestion.UserSuggestion(DisplayName("Alice")),
            Suggestion.UserSuggestion(DisplayName("Mike")),
        )
        val result = provider.filterUsers(suggestions, "")

        assertEquals(
            expected = listOf("Alice", "Mike", "Zed"),
            actual = result.map { it.name.value },
        )
    }

    @Test
    fun `users filtered by prefix and sorted`() {
        val suggestions = listOf(
            Suggestion.UserSuggestion(DisplayName("Bob")),
            Suggestion.UserSuggestion(DisplayName("Anna")),
            Suggestion.UserSuggestion(DisplayName("Alex")),
        )
        val result = provider.filterUsers(suggestions, "A")

        assertEquals(
            expected = listOf("Alex", "Anna"),
            actual = result.map { it.name.value },
        )
    }

    @Test
    fun `users with at-prefix get leading at`() {
        val suggestions = listOf(
            Suggestion.UserSuggestion(DisplayName("Bob")),
            Suggestion.UserSuggestion(DisplayName("Bea")),
        )
        val result = provider.filterUsers(suggestions, "@B")

        assertEquals(
            expected = listOf("@Bea", "@Bob"),
            actual = result.map { it.toString() },
        )
    }

    // endregion

    // region filterCommands

    @Test
    fun `commands sorted alphabetically`() {
        val suggestions = listOf(
            Suggestion.CommandSuggestion("/timeout"),
            Suggestion.CommandSuggestion("/ban"),
            Suggestion.CommandSuggestion("/mod"),
        )
        val result = provider.filterCommands(suggestions, "/")

        assertEquals(
            expected = listOf("/ban", "/mod", "/timeout"),
            actual = result.map { it.command },
        )
    }

    @Test
    fun `commands filtered by prefix`() {
        val suggestions = listOf(
            Suggestion.CommandSuggestion("/timeout"),
            Suggestion.CommandSuggestion("/ban"),
            Suggestion.CommandSuggestion("/title"),
        )
        val result = provider.filterCommands(suggestions, "/ti")

        assertEquals(
            expected = listOf("/timeout", "/title"),
            actual = result.map { it.command },
        )
    }

    // endregion
}
