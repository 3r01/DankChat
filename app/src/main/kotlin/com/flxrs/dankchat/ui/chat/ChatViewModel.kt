package com.flxrs.dankchat.ui.chat

import android.util.LruCache
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.api.helix.HelixApiClient
import com.flxrs.dankchat.data.api.helix.HelixApiException
import com.flxrs.dankchat.data.auth.AuthDataStore
import com.flxrs.dankchat.data.chat.ChatItem
import com.flxrs.dankchat.data.repo.chat.ChatMessageRepository
import com.flxrs.dankchat.data.twitch.message.SystemMessageType
import com.flxrs.dankchat.di.DispatchersProvider
import com.flxrs.dankchat.preferences.DankChatPreferenceStore
import com.flxrs.dankchat.preferences.appearance.AppearanceSettings
import com.flxrs.dankchat.preferences.appearance.AppearanceSettingsDataStore
import com.flxrs.dankchat.preferences.chat.ChatSettings
import com.flxrs.dankchat.preferences.chat.ChatSettingsDataStore
import com.flxrs.dankchat.utils.DateTimeUtils
import com.flxrs.dankchat.utils.extensions.isEven
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

private val logger = KotlinLogging.logger("ChatViewModel")

@KoinViewModel
class ChatViewModel(
    @InjectedParam private val channel: UserName,
    private val chatMessageRepository: ChatMessageRepository,
    private val chatMessageMapper: ChatMessageMapper,
    private val helixApiClient: HelixApiClient,
    private val authDataStore: AuthDataStore,
    private val preferenceStore: DankChatPreferenceStore,
    appearanceSettingsDataStore: AppearanceSettingsDataStore,
    chatSettingsDataStore: ChatSettingsDataStore,
    dispatchersProvider: DispatchersProvider,
) : ViewModel() {
    val chatDisplaySettings: StateFlow<ChatDisplaySettings> =
        combine(
            appearanceSettingsDataStore.settings,
            chatSettingsDataStore.settings,
        ) { appearance, chat ->
            ChatDisplaySettings(
                fontSize = appearance.fontSize.toFloat(),
                animateGifs = chat.animateGifs,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ChatDisplaySettings())

    private val chat: StateFlow<List<ChatItem>> =
        chatMessageRepository
            .getChat(channel)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000L), emptyList())

    // Mapping cache: keyed on "${message.id}-${tag}-${altBg}" to avoid re-mapping unchanged messages
    private val mappingCache = LruCache<String, ChatMessageUiState>(512)
    private var lastAppearanceSettings: AppearanceSettings? = null
    private var lastChatSettings: ChatSettings? = null

    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(Locale.getDefault())

    val chatUiStates: StateFlow<ImmutableList<ChatMessageUiState>> =
        combine(
            chat,
            appearanceSettingsDataStore.settings,
            chatSettingsDataStore.settings,
        ) { messages, appearanceSettings, chatSettings ->
            // Clear cache when settings change (affects all mapped results)
            if (appearanceSettings != lastAppearanceSettings || chatSettings != lastChatSettings) {
                mappingCache.evictAll()
                lastAppearanceSettings = appearanceSettings
                lastChatSettings = chatSettings
            }

            val zone = ZoneId.systemDefault()
            val result = ArrayList<ChatMessageUiState>(messages.size + 8)
            for (index in messages.indices) {
                val item = messages[index]
                val isAlternateBackground = index.isEven
                val altBg = isAlternateBackground && appearanceSettings.checkeredMessages
                val cacheKey = "${item.message.id}-${item.tag}-$altBg"

                val mapped =
                    mappingCache[cacheKey] ?: chatMessageMapper
                        .mapToUiState(
                            item = item,
                            chatSettings = chatSettings,
                            preferenceStore = preferenceStore,
                            isAlternateBackground = altBg,
                        ).also { mappingCache.put(cacheKey, it) }
                result += mapped

                // Insert date separator between messages on different days
                if (index < messages.lastIndex) {
                    val currentDay = Instant.ofEpochMilli(item.message.timestamp).atZone(zone).toLocalDate()
                    val nextDay = Instant.ofEpochMilli(messages[index + 1].message.timestamp).atZone(zone).toLocalDate()
                    if (currentDay != nextDay) {
                        val timestamp =
                            if (chatSettings.showTimestamps) {
                                DateTimeUtils.timestampToLocalTime(
                                    nextDay
                                        .atTime(LocalTime.MIDNIGHT)
                                        .atZone(zone)
                                        .toInstant()
                                        .toEpochMilli(),
                                    chatSettings.formatter,
                                )
                            } else {
                                ""
                            }
                        result +=
                            ChatMessageUiState.DateSeparatorUi(
                                id = "date-sep-$nextDay",
                                timestamp = timestamp,
                                dateText = nextDay.format(dateFormatter),
                            )
                    }
                }
            }

            chatMessageMapper
                .run {
                    result.withHighlightLayout(showLineSeparator = appearanceSettings.lineSeparator)
                }.toImmutableList()
        }.flowOn(dispatchersProvider.default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000L), persistentListOf())

    fun manageAutomodMessage(
        heldMessageId: String,
        channel: UserName,
        allow: Boolean,
    ) {
        viewModelScope.launch {
            val userId = authDataStore.userIdString ?: return@launch
            val action = if (allow) "ALLOW" else "DENY"

            helixApiClient
                .manageAutomodMessage(userId, heldMessageId, action)
                .onFailure { error ->
                    logger.error(error) { "Failed to $action automod message $heldMessageId" }
                    val statusCode = (error as? HelixApiException)?.status?.value
                    chatMessageRepository.addSystemMessage(
                        channel,
                        SystemMessageType.AutomodActionFailed(statusCode = statusCode, allow = allow),
                    )
                }
        }
    }
}

@Immutable
data class ChatDisplaySettings(
    val fontSize: Float = 14f,
    val animateGifs: Boolean = true,
)
