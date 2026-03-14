package com.flxrs.dankchat.domain

import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.repo.IgnoresRepository
import com.flxrs.dankchat.data.repo.command.CommandRepository
import com.flxrs.dankchat.data.repo.data.DataRepository
import com.flxrs.dankchat.di.DispatchersProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single

@Single
class GlobalDataLoader(
    private val dataRepository: DataRepository,
    private val commandRepository: CommandRepository,
    private val ignoresRepository: IgnoresRepository,
    private val dispatchersProvider: DispatchersProvider
) {

    /**
     * Load all global data (badges, emotes, commands, blocks)
     */
    suspend fun loadGlobalData(): Result<Unit> = withContext(dispatchersProvider.io) {
        runCatching {
            awaitAll(
                async { loadDankChatBadges() },
                async { loadGlobalBadges() },
                async { loadGlobalBTTVEmotes() },
                async { loadGlobalFFZEmotes() },
                async { loadGlobalSevenTVEmotes() },
                async { loadSupibotCommands() },
                async { loadUserBlocks() }
            )
            Unit
        }
    }

    suspend fun loadDankChatBadges() = dataRepository.loadDankChatBadges()
    suspend fun loadGlobalBadges() = dataRepository.loadGlobalBadges()
    suspend fun loadGlobalBTTVEmotes() = dataRepository.loadGlobalBTTVEmotes()
    suspend fun loadGlobalFFZEmotes() = dataRepository.loadGlobalFFZEmotes()
    suspend fun loadGlobalSevenTVEmotes() = dataRepository.loadGlobalSevenTVEmotes()
    suspend fun loadSupibotCommands() = commandRepository.loadSupibotCommands()
    suspend fun loadUserBlocks() = ignoresRepository.loadUserBlocks()

    /**
     * Load user-specific global emotes (requires login)
     */
    suspend fun loadUserStateEmotes(
        globalEmoteSets: List<String>,
        followerEmoteSets: Map<UserName, List<String>>
    ) {
        dataRepository.loadUserStateEmotes(globalEmoteSets, followerEmoteSets)
    }
}