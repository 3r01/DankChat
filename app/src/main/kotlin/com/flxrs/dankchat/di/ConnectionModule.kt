package com.flxrs.dankchat.di

import com.flxrs.dankchat.data.auth.AuthDataStore
import com.flxrs.dankchat.data.twitch.chat.ChatConnection
import com.flxrs.dankchat.data.twitch.chat.ChatConnectionType
import com.flxrs.dankchat.utils.ForegroundServiceState
import io.ktor.client.HttpClient
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

const val READ_CONNECTION = "ReadConnection"
const val WRITE_CONNECTION = "WriteConnection"

@Module
class ConnectionModule {
    @Single
    @Named(READ_CONNECTION)
    fun provideReadConnection(
        httpClient: HttpClient,
        dispatchersProvider: DispatchersProvider,
        authDataStore: AuthDataStore,
        foregroundServiceState: ForegroundServiceState,
    ): ChatConnection = ChatConnection(ChatConnectionType.Read, httpClient, authDataStore, dispatchersProvider, foregroundServiceState.active)

    @Single
    @Named(WRITE_CONNECTION)
    fun provideWriteConnection(
        httpClient: HttpClient,
        dispatchersProvider: DispatchersProvider,
        authDataStore: AuthDataStore,
        foregroundServiceState: ForegroundServiceState,
    ): ChatConnection = ChatConnection(ChatConnectionType.Write, httpClient, authDataStore, dispatchersProvider, foregroundServiceState.active)
}
