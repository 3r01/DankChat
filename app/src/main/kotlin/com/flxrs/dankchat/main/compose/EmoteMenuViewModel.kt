package com.flxrs.dankchat.main.compose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flxrs.dankchat.chat.emotemenu.EmoteItem
import com.flxrs.dankchat.chat.emotemenu.EmoteMenuTab
import com.flxrs.dankchat.chat.emotemenu.EmoteMenuTabItem
import com.flxrs.dankchat.data.repo.chat.ChatRepository
import com.flxrs.dankchat.data.repo.data.DataRepository
import com.flxrs.dankchat.data.repo.emote.EmoteUsageRepository
import com.flxrs.dankchat.data.repo.emote.Emotes
import com.flxrs.dankchat.data.twitch.emote.EmoteType
import com.flxrs.dankchat.utils.extensions.flatMapLatestOrDefault
import com.flxrs.dankchat.utils.extensions.moveToFront
import com.flxrs.dankchat.utils.extensions.toEmoteItems
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import org.koin.android.annotation.KoinViewModel
import kotlin.time.Duration.Companion.seconds

@KoinViewModel
class EmoteMenuViewModel(
    private val chatRepository: ChatRepository,
    private val dataRepository: DataRepository,
    private val emoteUsageRepository: EmoteUsageRepository,
) : ViewModel() {

    private val activeChannel = chatRepository.activeChannel

    private val emotes = activeChannel
        .flatMapLatestOrDefault(Emotes()) { dataRepository.getEmotes(it) }

    private val recentEmotes = emoteUsageRepository.getRecentUsages().distinctUntilChanged { old, new ->
        new.all { newEmote -> old.any { it.emoteId == newEmote.emoteId } }
    }

    val emoteTabItems: StateFlow<ImmutableList<EmoteMenuTabItem>> = combine(emotes, recentEmotes, activeChannel) { emotes, recentEmotes, channel ->
        withContext(Dispatchers.Default) {
            val sortedEmotes = emotes.sorted
            val availableRecents = recentEmotes.mapNotNull { usage ->
                sortedEmotes
                    .firstOrNull { it.id == usage.emoteId }
                    ?.copy(emoteType = EmoteType.RecentUsageEmote)
            }

            val groupedByType = sortedEmotes.groupBy {
                when (it.emoteType) {
                    is EmoteType.ChannelTwitchEmote,
                    is EmoteType.ChannelTwitchBitEmote,
                    is EmoteType.ChannelTwitchFollowerEmote -> EmoteMenuTab.SUBS

                    is EmoteType.ChannelFFZEmote,
                    is EmoteType.ChannelBTTVEmote,
                    is EmoteType.ChannelSevenTVEmote        -> EmoteMenuTab.CHANNEL

                    else                                    -> EmoteMenuTab.GLOBAL
                }
            }
            listOf(
                async { EmoteMenuTabItem(EmoteMenuTab.RECENT, availableRecents.toEmoteItems()) },
                async { EmoteMenuTabItem(EmoteMenuTab.SUBS, (groupedByType[EmoteMenuTab.SUBS] ?: emptyList()).moveToFront(channel).toEmoteItems()) },
                async { EmoteMenuTabItem(EmoteMenuTab.CHANNEL, (groupedByType[EmoteMenuTab.CHANNEL] ?: emptyList()).toEmoteItems()) },
                async { EmoteMenuTabItem(EmoteMenuTab.GLOBAL, (groupedByType[EmoteMenuTab.GLOBAL] ?: emptyList()).toEmoteItems()) }
            ).awaitAll().toImmutableList()
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        EmoteMenuTab.entries.map { EmoteMenuTabItem(it, emptyList()) }.toImmutableList()
    )
}
