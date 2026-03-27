package com.flxrs.dankchat.data.debug

import com.flxrs.dankchat.data.api.eventapi.EventSubClient
import com.flxrs.dankchat.data.api.eventapi.EventSubClientState
import com.flxrs.dankchat.data.api.seventv.eventapi.SevenTVEventApiClient
import com.flxrs.dankchat.data.twitch.pubsub.PubSubManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import org.koin.core.annotation.Single

@Single
class ConnectionDebugSection(
    private val eventSubClient: EventSubClient,
    private val pubSubManager: PubSubManager,
    private val sevenTVEventApiClient: SevenTVEventApiClient,
) : DebugSection {

    override val order = 3
    override val baseTitle = "Connection"

    override fun entries(): Flow<DebugSectionSnapshot> {
        val ticker = flow {
            while (true) {
                emit(Unit)
                delay(2_000)
            }
        }
        return combine(eventSubClient.state, eventSubClient.topics, ticker) { state, topics, _ ->
            val eventSubStatus = when (state) {
                is EventSubClientState.Connected    -> "Connected (${state.sessionId.take(8)}...)"
                is EventSubClientState.Connecting   -> "Connecting"
                is EventSubClientState.Disconnected -> "Disconnected"
                is EventSubClientState.Failed       -> "Failed"
            }

            val pubSubStatus = when {
                pubSubManager.connectedAndHasWhisperTopic -> "Connected (whispers)"
                pubSubManager.connected                   -> "Connected"
                else                                      -> "Disconnected"
            }

            val sevenTvStatus = sevenTVEventApiClient.status()
            val sevenTvText = when {
                sevenTvStatus.connected -> "Connected (${sevenTvStatus.subscriptionCount} subs)"
                else                    -> "Disconnected"
            }

            DebugSectionSnapshot(
                title = baseTitle,
                entries = listOf(
                    DebugEntry("PubSub", pubSubStatus),
                    DebugEntry("EventSub", eventSubStatus),
                    DebugEntry("EventSub topics", "${topics.size}"),
                    DebugEntry("7TV EventAPI", sevenTvText),
                )
            )
        }
    }
}
