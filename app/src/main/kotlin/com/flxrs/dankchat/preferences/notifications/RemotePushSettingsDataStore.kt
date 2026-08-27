package com.flxrs.dankchat.preferences.notifications

import android.content.Context
import com.flxrs.dankchat.di.DispatchersProvider
import com.flxrs.dankchat.utils.datastore.createDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking
import org.koin.core.annotation.Single

@Single
class RemotePushSettingsDataStore(
    context: Context,
    dispatchersProvider: DispatchersProvider,
) {
    private val scope = CoroutineScope(dispatchersProvider.io + SupervisorJob())
    private val dataStore =
        createDataStore(
            fileName = "remote_push",
            context = context,
            defaultValue = RemotePushSettings(),
            serializer = RemotePushSettings.serializer(),
            scope = scope,
        )

    val settings = dataStore.data
    val currentSettings =
        settings.stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = runBlocking { settings.first() },
        )

    fun current() = currentSettings.value

    suspend fun update(transform: suspend (RemotePushSettings) -> RemotePushSettings) {
        runCatching { dataStore.updateData(transform) }
    }
}
