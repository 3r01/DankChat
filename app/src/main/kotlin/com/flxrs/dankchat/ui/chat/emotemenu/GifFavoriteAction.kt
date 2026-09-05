package com.flxrs.dankchat.ui.chat.emotemenu

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flxrs.dankchat.R
import com.flxrs.dankchat.data.api.twitchgql.TwitchGifPickerItem
import com.flxrs.dankchat.data.repo.emote.GifPickerDataStore
import com.flxrs.dankchat.data.repo.emote.GifPickerLibrary
import com.flxrs.dankchat.ui.main.MainEvent
import com.flxrs.dankchat.ui.main.MainEventBus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
internal fun GifFavoriteAction(
    gif: TwitchGifPickerItem,
    onDone: () -> Unit,
    showTitle: Boolean = false,
) {
    val dataStore: GifPickerDataStore = koinInject()
    val eventBus: MainEventBus = koinInject()
    val library by dataStore.library.collectAsStateWithLifecycle(GifPickerLibrary())
    val isFavorite = library.favorites.any { it.id == gif.id }
    val scope = rememberCoroutineScope()
    ListItem(
        headlineContent = { Text(stringResource(if (isFavorite) R.string.gif_remove_favorite else R.string.gif_add_favorite)) },
        supportingContent = if (showTitle) {
            { Text(gif.title, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        } else {
            null
        },
        leadingContent = { Icon(if (isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder, contentDescription = null) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable {
            scope.launch {
                try {
                    dataStore.toggleFavorite(gif)
                    onDone()
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    eventBus.emitEvent(MainEvent.Error(error))
                }
            }
        },
    )
}
