package com.flxrs.dankchat.preferences.whispers

import android.content.Context
import com.flxrs.dankchat.di.DispatchersProvider
import com.flxrs.dankchat.utils.datastore.createDataStore
import com.flxrs.dankchat.utils.datastore.safeData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking
import org.koin.core.annotation.Single

@Single
class WhisperHistorySettingsDataStore(
    context: Context,
    dispatchersProvider: DispatchersProvider,
) {
    private val scope = CoroutineScope(dispatchersProvider.io + SupervisorJob())
    private val dataStore =
        createDataStore(
            fileName = "whisper_history",
            context = context,
            defaultValue = WhisperHistorySettings(),
            serializer = WhisperHistorySettings.serializer(),
            scope = scope,
        )

    val settings = dataStore.safeData(WhisperHistorySettings())
    private val currentSettings =
        settings.stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = runBlocking { settings.first() },
        )

    fun tokenFor(userId: String): String? = currentSettings.value.webOAuthTokens[userId]

    suspend fun saveToken(
        userId: String,
        token: String,
    ) {
        dataStore.updateData { current ->
            current.copy(webOAuthTokens = current.webOAuthTokens + (userId to token))
        }
    }

    suspend fun clearToken(userId: String) {
        dataStore.updateData { current ->
            current.copy(webOAuthTokens = current.webOAuthTokens - userId)
        }
    }
}
