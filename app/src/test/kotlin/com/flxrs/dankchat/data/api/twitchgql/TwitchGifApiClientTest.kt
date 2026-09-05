package com.flxrs.dankchat.data.api.twitchgql

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class TwitchGifApiClientTest {
    @Test
    fun `GIFs require every permission gate`() {
        assertTrue(TwitchGifConfig(isEnabled = true, isAllowlisted = true, canSend = true).isAvailable())
        assertFalse(TwitchGifConfig(isEnabled = false, isAllowlisted = true, canSend = true).isAvailable())
        assertFalse(TwitchGifConfig(isEnabled = true, isAllowlisted = false, canSend = true).isAvailable())
        assertFalse(TwitchGifConfig(isEnabled = true, isAllowlisted = true, canSend = false).isAvailable())
    }

    @Test
    fun `Twitch content ratings map to Giphy ratings`() {
        assertEquals("pg-13", "PG_13".toGiphyRating())
        assertEquals("pg", "G_PG".toGiphyRating())
        assertEquals("g", "G".toGiphyRating())
    }
}
