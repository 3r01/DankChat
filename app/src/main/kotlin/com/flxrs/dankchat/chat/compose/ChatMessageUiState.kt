package com.flxrs.dankchat.chat.compose

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.flxrs.dankchat.data.DisplayName
import com.flxrs.dankchat.data.UserId
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.twitch.badge.Badge
import com.flxrs.dankchat.data.twitch.emote.ChatMessageEmote
import com.flxrs.dankchat.data.twitch.message.Message
import com.flxrs.dankchat.data.twitch.message.MessageThreadHeader
import kotlinx.collections.immutable.ImmutableList

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
    val isHighlighted: Boolean

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
        override val isHighlighted: Boolean,
        val channel: UserName,
        val userId: UserId?,
        val userName: UserName,
        val displayName: DisplayName,
        val badges: ImmutableList<BadgeUi>,
        val rawNameColor: Int,
        val nameText: String,
        val message: String,
        val emotes: ImmutableList<EmoteUi>,
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
        override val isHighlighted: Boolean = false,
        val message: TextResource,
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
        override val isHighlighted: Boolean = false,
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
        override val isHighlighted: Boolean = false,
        val message: String,
        val displayName: String = "",
        val rawNameColor: Int = Message.DEFAULT_COLOR,
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
        override val isHighlighted: Boolean = false,
        val message: TextResource,
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
        override val isHighlighted: Boolean = true,
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
        override val isHighlighted: Boolean = false,
        val dateText: String,
    ) : ChatMessageUiState

    /**
     * AutoMod held messages with approve/deny actions
     */
    @Immutable
    data class AutomodMessageUi(
        override val id: String,
        override val tag: Int,
        override val timestamp: String,
        override val lightBackgroundColor: Color,
        override val darkBackgroundColor: Color,
        override val textAlpha: Float,
        override val enableRipple: Boolean = false,
        override val isHighlighted: Boolean = false,
        val heldMessageId: String,
        val channel: UserName,
        val badges: ImmutableList<BadgeUi>,
        val userDisplayName: String,
        val rawNameColor: Int,
        val messageText: String,
        val reason: TextResource,
        val status: AutomodMessageStatus,
    ) : ChatMessageUiState {
        enum class AutomodMessageStatus { Pending, Approved, Denied, Expired }
    }

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
        override val isHighlighted: Boolean = false,
        val userId: UserId,
        val userName: UserName,
        val displayName: DisplayName,
        val badges: ImmutableList<BadgeUi>,
        val rawSenderColor: Int,
        val rawRecipientColor: Int,
        val senderName: String,
        val recipientName: String,
        val message: String,
        val emotes: ImmutableList<EmoteUi>,
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
    val drawableResId: Int? = null,
)

/**
 * UI state for emotes
 */
@Immutable
data class EmoteUi(
    val code: String,
    val urls: ImmutableList<String>,
    val position: IntRange,
    val isAnimated: Boolean,
    val isTwitch: Boolean,
    val scale: Int,
    val emotes: ImmutableList<ChatMessageEmote>, // For click handling
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
