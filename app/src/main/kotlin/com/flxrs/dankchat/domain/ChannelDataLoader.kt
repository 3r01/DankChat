package com.flxrs.dankchat.domain

import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.UserId
import com.flxrs.dankchat.data.repo.channel.Channel
import com.flxrs.dankchat.data.repo.channel.ChannelRepository
import com.flxrs.dankchat.data.repo.chat.ChatRepository
import com.flxrs.dankchat.data.repo.data.DataRepository
import com.flxrs.dankchat.data.state.ChannelLoadingFailure
import com.flxrs.dankchat.data.state.ChannelLoadingState
import com.flxrs.dankchat.di.DispatchersProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single

@Single
class ChannelDataLoader(
    private val dataRepository: DataRepository,
    private val chatRepository: ChatRepository,
    private val channelRepository: ChannelRepository,
    private val dispatchersProvider: DispatchersProvider
) {

    /**
     * Load all data for a single channel.
     * Returns a Flow of loading state for this channel.
     */
    fun loadChannelData(channel: UserName): Flow<ChannelLoadingState> = flow {
        emit(ChannelLoadingState.Loading)

        try {
            // Get channel info
            val channelInfo = channelRepository.getChannel(channel)
            if (channelInfo == null) {
                emit(ChannelLoadingState.Failed("Channel not found", emptyList()))
                return@flow
            }

            // Create flows if necessary
            dataRepository.createFlowsIfNecessary(listOf(channel))
            chatRepository.createFlowsIfNecessary(channel)

            // Load in parallel and collect all failures
            val failures = withContext(dispatchersProvider.io) {
                val badgesResult = async { loadChannelBadges(channel, channelInfo.id) }
                val emotesResults = async { loadChannelEmotes(channel, channelInfo) }
                val messagesResult = async { loadRecentMessages(channel) }

                listOfNotNull(
                    badgesResult.await(),
                    *emotesResults.await().toTypedArray(),
                    messagesResult.await()
                )
            }

            // Reparse emotes/badges - this updates the tag which triggers LazyColumn recomposition
            chatRepository.reparseAllEmotesAndBadges()

            when {
                failures.isEmpty() -> emit(ChannelLoadingState.Loaded)
                else -> emit(ChannelLoadingState.Failed("Some data failed to load", failures))
            }
        } catch (e: Exception) {
            emit(ChannelLoadingState.Failed(e.message ?: "Unknown error", emptyList()))
        }
    }

    private suspend fun loadChannelBadges(
        channel: UserName,
        channelId: UserId
    ): ChannelLoadingFailure.Badges? {
        return runCatching {
            dataRepository.loadChannelBadges(channel, channelId)
        }.fold(
            onSuccess = { null },
            onFailure = { ChannelLoadingFailure.Badges(channel, channelId, it) }
        )
    }

    private suspend fun loadChannelEmotes(
        channel: UserName,
        channelInfo: Channel
    ): List<ChannelLoadingFailure> {
        return withContext(dispatchersProvider.io) {
            val bttvResult = async {
                runCatching {
                    dataRepository.loadChannelBTTVEmotes(channel, channelInfo.displayName, channelInfo.id)
                }.fold(
                    onSuccess = { null },
                    onFailure = { ChannelLoadingFailure.BTTVEmotes(channel, it) }
                )
            }
            val ffzResult = async {
                runCatching {
                    dataRepository.loadChannelFFZEmotes(channel, channelInfo.id)
                }.fold(
                    onSuccess = { null },
                    onFailure = { ChannelLoadingFailure.FFZEmotes(channel, it) }
                )
            }
            val sevenTvResult = async {
                runCatching {
                    dataRepository.loadChannelSevenTVEmotes(channel, channelInfo.id)
                }.fold(
                    onSuccess = { null },
                    onFailure = { ChannelLoadingFailure.SevenTVEmotes(channel, it) }
                )
            }

            listOfNotNull(
                bttvResult.await(),
                ffzResult.await(),
                sevenTvResult.await()
            )
        }
    }

    private suspend fun loadRecentMessages(
        channel: UserName
    ): ChannelLoadingFailure.RecentMessages? {
        return runCatching {
            chatRepository.loadRecentMessagesIfEnabled(channel)
        }.fold(
            onSuccess = { null },
            onFailure = { ChannelLoadingFailure.RecentMessages(channel, it) }
        )
    }
}
