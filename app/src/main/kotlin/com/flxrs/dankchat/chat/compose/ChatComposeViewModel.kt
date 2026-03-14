package com.flxrs.dankchat.chat.compose

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flxrs.dankchat.chat.ChatItem
import com.flxrs.dankchat.chat.compose.ChatMessageMapper.toChatMessageUiState
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.repo.chat.ChatRepository
import com.flxrs.dankchat.preferences.DankChatPreferenceStore
import com.flxrs.dankchat.preferences.appearance.AppearanceSettings
import com.flxrs.dankchat.preferences.appearance.AppearanceSettingsDataStore
import com.flxrs.dankchat.preferences.chat.ChatSettings
import com.flxrs.dankchat.preferences.chat.ChatSettingsDataStore
import com.flxrs.dankchat.utils.DateTimeUtils
import com.flxrs.dankchat.utils.extensions.isEven
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * ViewModel for Compose-based chat display.
 *
 * Unlike ChatViewModel (which uses SavedStateHandle with Fragment navigation args),
 * this ViewModel takes the channel directly as a constructor parameter, making it
 * suitable for Compose usage where we can pass parameters via koinViewModel().
 */
@KoinViewModel
class ChatComposeViewModel(
    @InjectedParam private val channel: UserName,
    private val repository: ChatRepository,
    private val context: Context,
    private val appearanceSettingsDataStore: AppearanceSettingsDataStore,
    private val chatSettingsDataStore: ChatSettingsDataStore,
    private val preferenceStore: DankChatPreferenceStore,
) : ViewModel() {

    private val chat: StateFlow<List<ChatItem>> = repository
        .getChat(channel)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000L), emptyList())

    // Mapping cache: keyed on "${message.id}-${tag}-${altBg}" to avoid re-mapping unchanged messages
    private val mappingCache = HashMap<String, ChatMessageUiState>(256)
    private var lastAppearanceSettings: AppearanceSettings? = null
    private var lastChatSettings: ChatSettings? = null

    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(Locale.getDefault())

    val chatUiStates: StateFlow<List<ChatMessageUiState>> = combine(
        chat,
        appearanceSettingsDataStore.settings,
        chatSettingsDataStore.settings
    ) { messages, appearanceSettings, chatSettings ->
        // Clear cache when settings change (affects all mapped results)
        if (appearanceSettings != lastAppearanceSettings || chatSettings != lastChatSettings) {
            mappingCache.clear()
            lastAppearanceSettings = appearanceSettings
            lastChatSettings = chatSettings
        }

        val zone = ZoneId.systemDefault()
        val result = ArrayList<ChatMessageUiState>(messages.size + 8)
        var messageCount = 0

        for (index in messages.indices) {
            val item = messages[index]
            val isAlternateBackground = when (index) {
                messages.lastIndex -> messageCount++.isEven
                else               -> (index - messages.size - 1).isEven
            }
            val altBg = isAlternateBackground && appearanceSettings.checkeredMessages
            val cacheKey = "${item.message.id}-${item.tag}-$altBg"

            val mapped = mappingCache.getOrPut(cacheKey) {
                item.toChatMessageUiState(
                    context = context,
                    appearanceSettings = appearanceSettings,
                    chatSettings = chatSettings,
                    preferenceStore = preferenceStore,
                    isAlternateBackground = altBg
                )
            }
            result += mapped

            // Insert date separator between messages on different days
            if (index < messages.lastIndex) {
                val currentDay = Instant.ofEpochMilli(item.message.timestamp).atZone(zone).toLocalDate()
                val nextDay = Instant.ofEpochMilli(messages[index + 1].message.timestamp).atZone(zone).toLocalDate()
                if (currentDay != nextDay) {
                    val timestamp = if (chatSettings.showTimestamps) {
                        DateTimeUtils.timestampToLocalTime(
                            nextDay.atTime(LocalTime.MIDNIGHT).atZone(zone).toInstant().toEpochMilli(),
                            chatSettings.formatter
                        )
                    } else {
                        ""
                    }
                    result += ChatMessageUiState.DateSeparatorUi(
                        id = "date-sep-$nextDay",
                        timestamp = timestamp,
                        dateText = nextDay.format(dateFormatter),
                    )
                }
            }
        }

        result
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000L), emptyList())
}
