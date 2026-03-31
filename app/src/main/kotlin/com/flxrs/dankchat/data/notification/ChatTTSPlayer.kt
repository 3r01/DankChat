package com.flxrs.dankchat.data.notification

import android.content.Context
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.content.getSystemService
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.repo.chat.ChatChannelProvider
import com.flxrs.dankchat.data.repo.chat.ChatNotificationRepository
import com.flxrs.dankchat.data.twitch.emote.ChatMessageEmote
import com.flxrs.dankchat.data.twitch.message.Message
import com.flxrs.dankchat.data.twitch.message.NoticeMessage
import com.flxrs.dankchat.data.twitch.message.PrivMessage
import com.flxrs.dankchat.data.twitch.message.UserNoticeMessage
import com.flxrs.dankchat.preferences.tools.TTSMessageFormat
import com.flxrs.dankchat.preferences.tools.TTSPlayMode
import com.flxrs.dankchat.preferences.tools.ToolsSettings
import com.flxrs.dankchat.preferences.tools.ToolsSettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import java.util.Locale

@Single
class ChatTTSPlayer(
    private val context: Context,
    private val chatNotificationRepository: ChatNotificationRepository,
    private val chatChannelProvider: ChatChannelProvider,
    private val toolsSettingsDataStore: ToolsSettingsDataStore,
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var tts: TextToSpeech? = null
    private var audioManager: AudioManager? = null
    private var previousTTSUser: UserName? = null
    private var toolSettings = ToolsSettings()

    fun start() {
        toolsSettingsDataStore.ttsEnabled
            .onEach { setTTSEnabled(enabled = it) }
            .launchIn(scope)
        toolsSettingsDataStore.ttsForceEnglishChanged
            .onEach { setTTSVoice(forceEnglish = it) }
            .launchIn(scope)
        toolsSettingsDataStore.settings
            .onEach { toolSettings = it }
            .launchIn(scope)

        scope.launch {
            chatNotificationRepository.messageUpdates.collect { items ->
                items.forEach { (message) ->
                    processMessage(message)
                }
            }
        }
    }

    fun shutdown() {
        shutdownTTS()
    }

    private fun processMessage(message: Message) {
        if (!message.shouldPlayTTS()) {
            return
        }

        val channel = when (message) {
            is PrivMessage -> message.channel
            is UserNoticeMessage -> message.channel
            is NoticeMessage -> message.channel
            else -> return
        }

        val activeChannel = chatChannelProvider.activeChannel.value
        if (!toolSettings.ttsEnabled || channel != activeChannel || (audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0) <= 0) {
            return
        }

        if (tts == null) {
            scope.launch { initTTS() }
            return
        }

        if (message is PrivMessage && toolSettings.ttsUserNameIgnores.any { it.matches(message.name) || it.matches(message.displayName) }) {
            return
        }

        message.playTTSMessage()
    }

    private suspend fun setTTSEnabled(enabled: Boolean) {
        when {
            enabled -> initTTS()
            else -> shutdownTTS()
        }
    }

    private suspend fun initTTS() {
        val forceEnglish = toolsSettingsDataStore.settings.first().ttsForceEnglish
        audioManager = context.getSystemService()
        tts = TextToSpeech(context) { status ->
            when (status) {
                TextToSpeech.SUCCESS -> setTTSVoice(forceEnglish = forceEnglish)
                else -> shutdownAndDisableTTS()
            }
        }
    }

    private fun setTTSVoice(forceEnglish: Boolean) {
        val voice = when {
            forceEnglish -> tts?.voices?.find { it.locale == Locale.US && !it.isNetworkConnectionRequired }
            else -> tts?.defaultVoice
        }

        voice?.takeUnless { tts?.setVoice(it) == TextToSpeech.ERROR } ?: shutdownAndDisableTTS()
    }

    private fun shutdownAndDisableTTS() {
        shutdownTTS()
        scope.launch {
            toolsSettingsDataStore.update { it.copy(ttsEnabled = false) }
        }
    }

    private fun shutdownTTS() {
        tts?.shutdown()
        tts = null
        previousTTSUser = null
        audioManager = null
    }

    private fun Message.shouldPlayTTS(): Boolean = this is PrivMessage || this is NoticeMessage || this is UserNoticeMessage

    private fun Message.playTTSMessage() {
        val message = when (this) {
            is UserNoticeMessage -> {
                message
            }

            is NoticeMessage -> {
                message
            }

            else -> {
                if (this !is PrivMessage) return
                val filtered = message
                    .filterEmotes(emotes)
                    .filterUnicodeSymbols()
                    .filterUrls()

                if (filtered.isBlank()) {
                    return
                }

                when {
                    toolSettings.ttsMessageFormat == TTSMessageFormat.Message || name == previousTTSUser -> filtered
                    tts?.voice?.locale?.language == Locale.ENGLISH.language -> "$name said $filtered"
                    else -> "$name. $filtered"
                }.also { previousTTSUser = name }
            }
        }

        val queueMode = when (toolSettings.ttsPlayMode) {
            TTSPlayMode.Queue -> TextToSpeech.QUEUE_ADD
            TTSPlayMode.Newest -> TextToSpeech.QUEUE_FLUSH
        }
        tts?.speak(message, queueMode, null, null)
    }

    private fun String.filterEmotes(emotes: List<ChatMessageEmote>): String = when {
        toolSettings.ttsIgnoreEmotes -> {
            emotes.fold(this) { acc, emote ->
                acc.replace(emote.code, newValue = "", ignoreCase = true)
            }
        }

        else -> {
            this
        }
    }

    private fun String.filterUnicodeSymbols(): String = when {
        toolSettings.ttsIgnoreEmotes -> replace(UNICODE_SYMBOL_REGEX, replacement = "")
        else -> this
    }

    private fun String.filterUrls(): String = when {
        toolSettings.ttsIgnoreUrls -> replace(URL_REGEX, replacement = "")
        else -> this
    }

    companion object {
        private val UNICODE_SYMBOL_REGEX = "\\p{So}|\\p{Sc}|\\p{Sm}|\\p{Cn}".toRegex()
        private val URL_REGEX = "[(http(s)?):\\/\\/(www\\.)?a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&//=]*)".toRegex(RegexOption.IGNORE_CASE)
    }
}
