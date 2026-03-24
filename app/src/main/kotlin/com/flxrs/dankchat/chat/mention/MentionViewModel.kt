package com.flxrs.dankchat.chat.mention

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flxrs.dankchat.chat.ChatItem
import com.flxrs.dankchat.data.repo.chat.ChatNotificationRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.stateIn
import org.koin.android.annotation.KoinViewModel
import kotlin.time.Duration.Companion.seconds

@KoinViewModel
class MentionViewModel(chatNotificationRepository: ChatNotificationRepository) : ViewModel() {

    val mentions: StateFlow<List<ChatItem>> = chatNotificationRepository.mentions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeout = 5.seconds), emptyList())
    val whispers: StateFlow<List<ChatItem>> = chatNotificationRepository.whispers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeout = 5.seconds), emptyList())

    val hasMentions: StateFlow<Boolean> = chatNotificationRepository.hasMentions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeout = 5.seconds), false)
    val hasWhispers: StateFlow<Boolean> = chatNotificationRepository.hasWhispers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeout = 5.seconds), false)
}
