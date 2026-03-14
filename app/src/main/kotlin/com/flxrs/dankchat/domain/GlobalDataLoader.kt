package com.flxrs.dankchat.domain

import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.repo.IgnoresRepository
import com.flxrs.dankchat.data.repo.command.CommandRepository
import com.flxrs.dankchat.data.repo.data.DataRepository
import com.flxrs.dankchat.di.DispatchersProvider
import kotlinx.coroutines.Dispatchers
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
                async { dataRepository.loadDankChatBadges() },
                async { dataRepository.loadGlobalBadges() },
                async { dataRepository.loadGlobalBTTVEmotes() },
                async { dataRepository.loadGlobalFFZEmotes() },
                async { dataRepository.loadGlobalSevenTVEmotes() },
                async { commandRepository.loadSupibotCommands() },
                async { ignoresRepository.loadUserBlocks() }
            )
            Unit
        }
    }

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
