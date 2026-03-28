package com.flxrs.dankchat.di

import com.flxrs.dankchat.data.auth.AuthDataStore
import com.flxrs.dankchat.data.twitch.chat.ChatConnection
import com.flxrs.dankchat.data.twitch.chat.ChatConnectionType
import io.ktor.client.HttpClient
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

data object ReadConnection
data object WriteConnection

@Module
class ConnectionModule {

    @Single
    @Named(type = ReadConnection::class)
    fun provideReadConnection(
        httpClient: HttpClient,
        dispatchersProvider: DispatchersProvider,
        authDataStore: AuthDataStore,
    ): ChatConnection = ChatConnection(ChatConnectionType.Read, httpClient, authDataStore, dispatchersProvider)

    @Single
    @Named(type = WriteConnection::class)
    fun provideWriteConnection(
        httpClient: HttpClient,
        dispatchersProvider: DispatchersProvider,
        authDataStore: AuthDataStore,
    ): ChatConnection = ChatConnection(ChatConnectionType.Write, httpClient, authDataStore, dispatchersProvider)
}
