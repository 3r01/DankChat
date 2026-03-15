package com.flxrs.dankchat.chat.compose

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.flxrs.dankchat.data.DisplayName
import com.flxrs.dankchat.data.UserId
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.twitch.badge.Badge
import com.flxrs.dankchat.data.twitch.emote.ChatMessageEmote
import com.flxrs.dankchat.data.twitch.message.MessageThreadHeader

/**
 * UI state for rendering chat messages in Compose.
 * All rendering decisions are pre-computed to avoid work during recomposition.
 */
@Immutable
sealed interface ChatMessageUiState {
    val id: String
    val tag: Int // Used for invalidating/updating messages when emotes/badges change
    val timestamp: String
    val lightBackgroundColor: Color
    val darkBackgroundColor: Color
    val textAlpha: Float
    val enableRipple: Boolean

    /**
     * Regular chat message from a user
     */
    @Immutable
    data class PrivMessageUi(
        override val id: String,
        override val tag: Int,
        override val timestamp: String,
        override val lightBackgroundColor: Color,
        override val darkBackgroundColor: Color,
        override val textAlpha: Float,
        override val enableRipple: Boolean,
        val channel: UserName,
        val userId: UserId?,
        val userName: UserName,
        val displayName: DisplayName,
        val badges: List<BadgeUi>,
        val rawNameColor: Int,
        val nameText: String,
        val message: String,
        val emotes: List<EmoteUi>,
        val isAction: Boolean,
        val thread: ThreadUi?,
        val fullMessage: String, // For copying
    ) : ChatMessageUiState

    /**
     * System messages (connected, disconnected, etc.)
     */
    @Immutable
    data class SystemMessageUi(
        override val id: String,
        override val tag: Int,
        override val timestamp: String,
        override val lightBackgroundColor: Color,
        override val darkBackgroundColor: Color,
        override val textAlpha: Float,
        override val enableRipple: Boolean = false,
        val message: String,
    ) : ChatMessageUiState

    /**
     * Notice messages from Twitch
     */
    @Immutable
    data class NoticeMessageUi(
        override val id: String,
        override val tag: Int,
        override val timestamp: String,
        override val lightBackgroundColor: Color,
        override val darkBackgroundColor: Color,
        override val textAlpha: Float,
        override val enableRipple: Boolean = false,
        val message: String,
    ) : ChatMessageUiState

    /**
     * User notice messages (subscriptions, etc.)
     */
    @Immutable
    data class UserNoticeMessageUi(
        override val id: String,
        override val tag: Int,
        override val timestamp: String,
        override val lightBackgroundColor: Color,
        override val darkBackgroundColor: Color,
        override val textAlpha: Float,
        override val enableRipple: Boolean = false,
        val message: String,
        val shouldHighlight: Boolean,
    ) : ChatMessageUiState

    /**
     * Moderation messages (timeouts, bans, etc.)
     */
    @Immutable
    data class ModerationMessageUi(
        override val id: String,
        override val tag: Int,
        override val timestamp: String,
        override val lightBackgroundColor: Color,
        override val darkBackgroundColor: Color,
        override val textAlpha: Float,
        override val enableRipple: Boolean = false,
        val message: String,
    ) : ChatMessageUiState

    /**
     * Channel point redemption messages
     */
    @Immutable
    data class PointRedemptionMessageUi(
        override val id: String,
        override val tag: Int,
        override val timestamp: String,
        override val lightBackgroundColor: Color,
        override val darkBackgroundColor: Color,
        override val textAlpha: Float,
        override val enableRipple: Boolean = false,
        val nameText: String?,
        val title: String,
        val cost: Int,
        val rewardImageUrl: String,
        val requiresUserInput: Boolean,
    ) : ChatMessageUiState

    /**
     * Date separator inserted between messages from different days
     */
    @Immutable
    data class DateSeparatorUi(
        override val id: String,
        override val tag: Int = 0,
        override val timestamp: String,
        override val lightBackgroundColor: Color = Color.Transparent,
        override val darkBackgroundColor: Color = Color.Transparent,
        override val textAlpha: Float = 0.5f,
        override val enableRipple: Boolean = false,
        val dateText: String,
    ) : ChatMessageUiState

    /**
     * Whisper messages
     */
    @Immutable
    data class WhisperMessageUi(
        override val id: String,
        override val tag: Int,
        override val timestamp: String,
        override val lightBackgroundColor: Color,
        override val darkBackgroundColor: Color,
        override val textAlpha: Float,
        override val enableRipple: Boolean,
        val userId: UserId,
        val userName: UserName,
        val displayName: DisplayName,
        val badges: List<BadgeUi>,
        val rawSenderColor: Int,
        val rawRecipientColor: Int,
        val senderName: String,
        val recipientName: String,
        val message: String,
        val emotes: List<EmoteUi>,
        val fullMessage: String,
        val replyTargetName: UserName,
    ) : ChatMessageUiState
}

/**
 * UI state for badges
 */
@Immutable
data class BadgeUi(
    val url: String,
    val badge: Badge,
    val position: Int, // Position in message
)

/**
 * UI state for emotes
 */
@Immutable
data class EmoteUi(
    val code: String,
    val urls: List<String>,
    val position: IntRange,
    val isAnimated: Boolean,
    val isTwitch: Boolean,
    val scale: Int,
    val emotes: List<ChatMessageEmote>, // For click handling
    val cheerAmount: Int? = null,
    val cheerColor: Color? = null,
)

/**
 * UI state for reply threads
 */
@Immutable
data class ThreadUi(
    val rootId: String,
    val userName: String,
    val message: String,
)

/**
 * Converts MessageThreadHeader to ThreadUi
 */
fun MessageThreadHeader.toThreadUi(): ThreadUi = ThreadUi(
    rootId = rootId,
    userName = name.value,
    message = message,
)
