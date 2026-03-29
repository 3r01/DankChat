package com.flxrs.dankchat.domain

import com.flxrs.dankchat.data.UserId
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.repo.IgnoresRepository
import com.flxrs.dankchat.data.repo.command.CommandRepository
import com.flxrs.dankchat.data.repo.data.DataRepository
import com.flxrs.dankchat.di.DispatchersProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single

@Single
class GlobalDataLoader(
    private val dataRepository: DataRepository,
    private val commandRepository: CommandRepository,
    private val ignoresRepository: IgnoresRepository,
    private val dispatchersProvider: DispatchersProvider
) {

    suspend fun loadGlobalData(): List<Result<Unit>> = withContext(dispatchersProvider.io) {
        val results = awaitAll(
            async { loadDankChatBadges() },
            async { loadGlobalBTTVEmotes() },
            async { loadGlobalFFZEmotes() },
            async { loadGlobalSevenTVEmotes() },
        )
        launch { loadSupibotCommands() }
        results
    }

    suspend fun loadAuthGlobalData(): List<Result<Unit>> = withContext(dispatchersProvider.io) {
        val results = awaitAll(
            async { loadGlobalBadges() },
        )
        launch { loadUserBlocks() }
        results
    }

    suspend fun loadDankChatBadges(): Result<Unit> = dataRepository.loadDankChatBadges()
    suspend fun loadGlobalBadges(): Result<Unit> = dataRepository.loadGlobalBadges()
    suspend fun loadGlobalBTTVEmotes(): Result<Unit> = dataRepository.loadGlobalBTTVEmotes()
    suspend fun loadGlobalFFZEmotes(): Result<Unit> = dataRepository.loadGlobalFFZEmotes()
    suspend fun loadGlobalSevenTVEmotes(): Result<Unit> = dataRepository.loadGlobalSevenTVEmotes()
    suspend fun loadSupibotCommands() = commandRepository.loadSupibotCommands()
    suspend fun loadUserBlocks() = ignoresRepository.loadUserBlocks()

    suspend fun loadUserEmotes(userId: UserId, onFirstPageLoaded: (() -> Unit)? = null): Result<Unit> {
        return dataRepository.loadUserEmotes(userId, onFirstPageLoaded)
    }
}
