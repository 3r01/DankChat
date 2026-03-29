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

    suspend fun loadChannelData(channel: UserName): ChannelLoadingState {
        return try {
            // Phase 1: No auth needed — create flows and load message history
            dataRepository.createFlowsIfNecessary(listOf(channel))
            chatRepository.createFlowsIfNecessary(channel)
            chatRepository.loadRecentMessagesIfEnabled(channel)

            // Phase 2: Needs channel info (Helix or IRC fallback) for emotes/badges
            val channelInfo = channelRepository.getChannel(channel)
                ?: getChannelsUseCase(listOf(channel)).firstOrNull()
            if (channelInfo == null) {
                return ChannelLoadingState.Failed(emptyList())
            }

            val failures = withContext(dispatchersProvider.io) {
                val badgesResult = async { loadChannelBadges(channel, channelInfo.id) }
                val emotesResults = async { loadChannelEmotes(channel, channelInfo) }

                listOfNotNull(
                    badgesResult.await(),
                    *emotesResults.await().toTypedArray(),
                )
            }

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
                failures.isEmpty() -> ChannelLoadingState.Loaded
                else               -> ChannelLoadingState.Failed(failures)
            }
        } catch (_: Exception) {
            ChannelLoadingState.Failed(emptyList())
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

}
