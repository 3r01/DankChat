package com.flxrs.dankchat.domain

import com.flxrs.dankchat.data.UserId
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.api.ApiException
import com.flxrs.dankchat.data.repo.channel.Channel
import com.flxrs.dankchat.data.repo.channel.ChannelRepository
import com.flxrs.dankchat.data.repo.chat.ChatMessageRepository
import com.flxrs.dankchat.data.repo.chat.ChatRepository
import com.flxrs.dankchat.data.repo.data.DataRepository
import com.flxrs.dankchat.data.state.ChannelLoadingFailure
import com.flxrs.dankchat.data.state.ChannelLoadingState
import com.flxrs.dankchat.data.twitch.message.SystemMessageType
import com.flxrs.dankchat.di.DispatchersProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single

@Single
class ChannelDataLoader(
    private val dataRepository: DataRepository,
    private val chatRepository: ChatRepository,
    private val chatMessageRepository: ChatMessageRepository,
    private val channelRepository: ChannelRepository,
    private val getChannelsUseCase: GetChannelsUseCase,
    private val dispatchersProvider: DispatchersProvider
) {

    /**
     * Load all data for a single channel.
     * Returns a Flow of loading state for this channel.
     */
    fun loadChannelData(channel: UserName): Flow<ChannelLoadingState> = flow {
        emit(ChannelLoadingState.Loading)

        try {
            // Get channel info - uses GetChannelsUseCase which waits for IRC ROOMSTATE
            // if not logged in, matching the legacy MainViewModel.loadData behavior
            val channelInfo = channelRepository.getChannel(channel)
                ?: getChannelsUseCase(listOf(channel)).firstOrNull()
            if (channelInfo == null) {
                emit(ChannelLoadingState.Failed("Channel not found", emptyList()))
                return@flow
            }

            // Create flows if necessary
            dataRepository.createFlowsIfNecessary(listOf(channel))
            chatRepository.createFlowsIfNecessary(channel)

            // Load recent message history first with priority
            // loadRecentMessagesIfEnabled handles errors internally and posts its own system messages
            chatRepository.loadRecentMessagesIfEnabled(channel)

            // Load other data in parallel
            val failures = withContext(dispatchersProvider.io) {
                val badgesResult = async { loadChannelBadges(channel, channelInfo.id) }
                val emotesResults = async { loadChannelEmotes(channel, channelInfo) }

                listOfNotNull(
                    badgesResult.await(),
                    *emotesResults.await().toTypedArray(),
                )
            }

            // Report failures as system messages like legacy implementation
            failures.forEach { failure ->
                val status = (failure.error as? ApiException)?.status?.value?.toString() ?: "0"
                val systemMessageType = when (failure) {
                    is ChannelLoadingFailure.SevenTVEmotes -> SystemMessageType.ChannelSevenTVEmotesFailed(status)
                    is ChannelLoadingFailure.BTTVEmotes    -> SystemMessageType.ChannelBTTVEmotesFailed(status)
                    is ChannelLoadingFailure.FFZEmotes     -> SystemMessageType.ChannelFFZEmotesFailed(status)
                    else                                   -> null
                }
                systemMessageType?.let {
                    chatMessageRepository.addSystemMessage(channel, it)
                }
            }

            when {
                failures.isEmpty() -> emit(ChannelLoadingState.Loaded)
                else               -> emit(ChannelLoadingState.Failed("Some data failed to load", failures))
            }
        } catch (e: Exception) {
            emit(ChannelLoadingState.Failed(e.message ?: "Unknown error", emptyList()))
        }
    }

    suspend fun loadChannelBadges(
        channel: UserName,
        channelId: UserId
    ): ChannelLoadingFailure.Badges? {
        return dataRepository.loadChannelBadges(channel, channelId).fold(
            onSuccess = { null },
            onFailure = { ChannelLoadingFailure.Badges(channel, channelId, it) }
        )
    }

    suspend fun loadChannelEmotes(
        channel: UserName,
        channelInfo: Channel
    ): List<ChannelLoadingFailure> {
        return withContext(dispatchersProvider.io) {
            val bttvResult = async {
                dataRepository.loadChannelBTTVEmotes(channel, channelInfo.displayName, channelInfo.id).fold(
                    onSuccess = { null },
                    onFailure = { ChannelLoadingFailure.BTTVEmotes(channel, it) }
                )
            }
            val ffzResult = async {
                dataRepository.loadChannelFFZEmotes(channel, channelInfo.id).fold(
                    onSuccess = { null },
                    onFailure = { ChannelLoadingFailure.FFZEmotes(channel, it) }
                )
            }
            val sevenTvResult = async {
                dataRepository.loadChannelSevenTVEmotes(channel, channelInfo.id).fold(
                    onSuccess = { null },
                    onFailure = { ChannelLoadingFailure.SevenTVEmotes(channel, it) }
                )
            }
            val cheermotesResult = async {
                dataRepository.loadChannelCheermotes(channel, channelInfo.id).fold(
                    onSuccess = { null },
                    onFailure = { ChannelLoadingFailure.Cheermotes(channel, it) }
                )
            }
            listOfNotNull(
                bttvResult.await(),
                ffzResult.await(),
                sevenTvResult.await(),
                cheermotesResult.await(),
            )
        }
    }

    suspend fun loadRecentMessages(channel: UserName) {
        // loadRecentMessagesIfEnabled handles errors internally and posts its own system messages
        chatRepository.loadRecentMessagesIfEnabled(channel)
    }
}
