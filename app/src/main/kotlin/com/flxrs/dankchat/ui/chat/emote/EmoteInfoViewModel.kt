package com.flxrs.dankchat.ui.chat.emote

import androidx.lifecycle.ViewModel
import com.flxrs.dankchat.data.repo.emote.EmoteRepository
import kotlinx.collections.immutable.toImmutableList
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class EmoteInfoViewModel(
    @InjectedParam private val emoteIds: List<String>,
    private val emoteRepository: EmoteRepository,
) : ViewModel() {
    val items =
        emoteIds
            .mapNotNull { emoteRepository.findEmoteById(it)?.toEmoteInfoItem() }
            .toImmutableList()
}
