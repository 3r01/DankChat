package com.flxrs.dankchat.ui.chat.suggestion

import io.mockk.mockk
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class SuggestionScoringTest {

    private val provider = SuggestionProvider(
        emoteRepository = mockk(),
        usersRepository = mockk(),
        commandRepository = mockk(),
        chatSettingsDataStore = mockk(),
        emoteUsageRepository = mockk(),
        emojiRepository = mockk(),
    )

    @Test
    fun `exact full match scores lowest`() {
        val score = provider.scoreEmote("Pog", "Pog", isRecentlyUsed = false)
        assertEquals(expected = 0, actual = score)
    }

    @Test
    fun `prefix exact case scores lower than prefix case-insensitive`() {
        val prefixExact = provider.scoreEmote("PogChamp", "Pog", isRecentlyUsed = false)
        val prefixInsensitive = provider.scoreEmote("POGGERS", "Pog", isRecentlyUsed = false)
        assertTrue(prefixExact < prefixInsensitive)
    }

    @Test
    fun `prefix scores lower than substring`() {
        val prefix = provider.scoreEmote("PogChamp", "Pog", isRecentlyUsed = false)
        val substring = provider.scoreEmote("wePog", "Pog", isRecentlyUsed = false)
        assertTrue(prefix < substring)
    }

    @Test
    fun `shorter emote beats longer within same tier`() {
        val shorter = provider.scoreEmote("PogU", "Pog", isRecentlyUsed = false)
        val longer = provider.scoreEmote("PogChamp", "Pog", isRecentlyUsed = false)
        assertTrue(shorter < longer)
    }

    @Test
    fun `recently used emote gets boost`() {
        val notRecent = provider.scoreEmote("PogChamp", "Pog", isRecentlyUsed = false)
        val recent = provider.scoreEmote("PogChamp", "Pog", isRecentlyUsed = true)
        assertTrue(recent < notRecent)
    }

    @Test
    fun `recently used substring does not beat non-used prefix`() {
        val prefix = provider.scoreEmote("PogChamp", "Pog", isRecentlyUsed = false)
        val recentSubstring = provider.scoreEmote("wePog", "Pog", isRecentlyUsed = true)
        assertTrue(prefix < recentSubstring)
    }

    @Test
    fun `no match returns negative score`() {
        val score = provider.scoreEmote("Kappa", "Pog", isRecentlyUsed = false)
        assertTrue(score < 0)
    }

    @Test
    fun `case insensitive substring scores highest tier`() {
        val score = provider.scoreEmote("pepog", "Pog", isRecentlyUsed = false)
        assertTrue(score >= 400)
    }
}
