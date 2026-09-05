package com.flxrs.dankchat.data.repo.emote

import com.flxrs.dankchat.data.api.twitchgql.TwitchGifPickerItem
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class GifPickerLibraryTest {
    @Test
    fun `favourites toggle by GIF id and survive serialization`() {
        val gif = gif("forsen")
        val library = GifPickerLibrary().toggleFavorite(gif)
        val restored = Json.decodeFromString<GifPickerLibrary>(Json.encodeToString(library))
        assertEquals(listOf("forsen"), restored.favorites.map { it.id })
        assertNull(restored.favorites.single().searchTerm)
        assertTrue(restored.toggleFavorite(gif.copy(title = "Updated title")).favorites.isEmpty())
    }

    @Test
    fun `recents are bounded and resending moves an existing GIF to the front`() {
        val library = (1..60)
            .fold(GifPickerLibrary()) { current, id -> current.recordSent(gif(id.toString())) }
            .recordSent(gif("30"))
        assertEquals(50, library.recent.size)
        assertEquals(listOf("30", "60", "59"), library.recent.take(3).map { it.id })
        assertEquals(
            50,
            library.recent
                .map { it.id }
                .distinct()
                .size,
        )
        assertTrue(library.favorites.isEmpty())
    }

    private fun gif(id: String) = TwitchGifPickerItem(id, id, "https://example.com/$id.gif", "https://example.com/$id.webp", 200, 200, "forsen")
}
