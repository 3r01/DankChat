package com.flxrs.dankchat.push

import kotlinx.serialization.Serializable

const val PUSH_PROTOCOL_VERSION = 1

@Serializable
data class PushConfiguration(
    val protocolVersion: Int = PUSH_PROTOCOL_VERSION,
    val revision: Long,
    val twitchUserId: String,
    val userName: String,
    val displayName: String? = null,
    val notifyWhispers: Boolean,
    val channels: List<PushChannel>,
    val rules: PushNotificationRules,
)

@Serializable
data class PushChannel(
    val id: String,
    val name: String,
)

@Serializable
data class PushNotificationRules(
    val notifyOnUsername: Boolean,
    val notifyOnParticipatedReply: Boolean,
    val messageHighlights: List<MessageHighlightRule>,
    val userHighlights: List<String>,
    val badgeHighlights: List<String>,
    val blacklistedUsers: List<BlacklistedUserRule>,
)

@Serializable
data class MessageHighlightRule(
    val pattern: String,
    val isRegex: Boolean,
    val isCaseSensitive: Boolean,
)

@Serializable
data class BlacklistedUserRule(
    val pattern: String,
    val isRegex: Boolean,
)

@Serializable
data class DeviceRegistration(
    val firebaseInstallationId: String,
)

@Serializable
data class ConfigurationResponse(
    val revision: Long,
    val changed: Boolean,
)

@Serializable
data class PushServerStatus(
    val protocolVersion: Int,
    val configurationRevision: Long?,
    val twitchAuthorized: Boolean,
    val registeredDevices: Int,
)

@Serializable
data class PushMessage(
    val protocolVersion: Int = PUSH_PROTOCOL_VERSION,
    val messageId: String,
    val timestamp: Long,
    val channelId: String? = null,
    val channelName: String? = null,
    val senderUserId: String,
    val senderUserName: String,
    val senderDisplayName: String,
    val text: String,
    val kind: PushMessageKind,
)

@Serializable
enum class PushMessageKind {
    Mention,
    Whisper,
}

data class ChatMessageCandidate(
    val senderUserName: String,
    val text: String,
    val badges: List<String>,
    val participatedReply: Boolean,
    val isSharedChatDuplicate: Boolean = false,
)
