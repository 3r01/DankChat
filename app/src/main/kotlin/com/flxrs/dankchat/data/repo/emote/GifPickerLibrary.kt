package com.flxrs.dankchat.data.repo.emote

import com.flxrs.dankchat.data.api.twitchgql.TwitchGifPickerItem
import kotlinx.serialization.Serializable

@Serializable
data class GifPickerLibrary(
    val favorites: List<TwitchGifPickerItem> = emptyList(),
    val recent: List<TwitchGifPickerItem> = emptyList(),
) {
    fun toggleFavorite(gif: TwitchGifPickerItem): GifPickerLibrary = copy(
        favorites = if (favorites.any { it.id == gif.id }) {
            favorites.filterNot { it.id == gif.id }
        } else {
            listOf(gif.copy(searchTerm = null)) + favorites
        },
    )

    fun recordSent(gif: TwitchGifPickerItem): GifPickerLibrary = copy(
        recent = (listOf(gif.copy(searchTerm = null)) + recent.filterNot { it.id == gif.id }).take(RECENT_LIMIT),
    )
}

private const val RECENT_LIMIT = 50
