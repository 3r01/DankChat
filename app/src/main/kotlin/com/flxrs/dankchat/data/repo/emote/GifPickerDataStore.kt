package com.flxrs.dankchat.data.repo.emote

import android.content.Context
import com.flxrs.dankchat.data.api.twitchgql.TwitchGifPickerItem
import com.flxrs.dankchat.di.DispatchersProvider
import com.flxrs.dankchat.utils.datastore.createDataStore
import com.flxrs.dankchat.utils.datastore.safeData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.koin.core.annotation.Single

@Single
class GifPickerDataStore(
    context: Context,
    dispatchersProvider: DispatchersProvider,
) {
    private val dataStore = createDataStore(
        fileName = "gif_picker",
        context = context,
        defaultValue = GifPickerLibrary(),
        serializer = GifPickerLibrary.serializer(),
        scope = CoroutineScope(dispatchersProvider.io + SupervisorJob()),
    )

    val library = dataStore.safeData(GifPickerLibrary())

    suspend fun toggleFavorite(gif: TwitchGifPickerItem) {
        dataStore.updateData { it.toggleFavorite(gif) }
    }

    suspend fun recordSent(gif: TwitchGifPickerItem) {
        dataStore.updateData { it.recordSent(gif) }
    }
}
