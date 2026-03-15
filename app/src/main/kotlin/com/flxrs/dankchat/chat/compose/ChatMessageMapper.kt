package com.flxrs.dankchat.chat.compose

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.flxrs.dankchat.R
import com.flxrs.dankchat.chat.ChatImportance
import com.flxrs.dankchat.chat.ChatItem
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.twitch.emote.ChatMessageEmoteType
import com.flxrs.dankchat.data.twitch.message.AutomodMessage
import com.flxrs.dankchat.data.twitch.message.Highlight
import com.flxrs.dankchat.data.twitch.message.Message
import com.flxrs.dankchat.data.twitch.message.HighlightType
import com.flxrs.dankchat.data.twitch.message.ModerationMessage
import com.flxrs.dankchat.data.twitch.message.NoticeMessage
import com.flxrs.dankchat.data.twitch.message.PointRedemptionMessage
import com.flxrs.dankchat.data.twitch.message.PrivMessage
import com.flxrs.dankchat.data.twitch.message.SystemMessage
import com.flxrs.dankchat.data.twitch.message.SystemMessageType
import com.flxrs.dankchat.data.twitch.message.UserNoticeMessage
import com.flxrs.dankchat.data.twitch.message.WhisperMessage
import com.flxrs.dankchat.data.twitch.message.aliasOrFormattedName
import com.flxrs.dankchat.data.twitch.message.recipientAliasOrFormattedName
import com.flxrs.dankchat.data.twitch.message.senderAliasOrFormattedName
import com.flxrs.dankchat.preferences.DankChatPreferenceStore
import com.flxrs.dankchat.preferences.appearance.AppearanceSettings
import com.flxrs.dankchat.preferences.chat.ChatSettings
import com.flxrs.dankchat.utils.DateTimeUtils
import com.google.android.material.color.MaterialColors

/**
 * Maps domain Message objects to Compose UI state objects.
 * Pre-computed all rendering decisions to minimize work during composition.
 */
object ChatMessageMapper {

    // Highlight colors - Light theme
    private val COLOR_SUB_HIGHLIGHT_LIGHT = Color(0xFFD1C4E9)
    private val COLOR_MENTION_HIGHLIGHT_LIGHT = Color(0xFFEF9A9A)
    private val COLOR_REDEMPTION_HIGHLIGHT_LIGHT = Color(0xFF93F1FF)
    private val COLOR_FIRST_MESSAGE_HIGHLIGHT_LIGHT = Color(0xFFC2F18D)
    private val COLOR_ELEVATED_MESSAGE_HIGHLIGHT_LIGHT = Color(0xFFFFE087)
    // Highlight colors - Dark theme
    private val COLOR_SUB_HIGHLIGHT_DARK = Color(0xFF543589)
    private val COLOR_MENTION_HIGHLIGHT_DARK = Color(0xFF773031)
    private val COLOR_REDEMPTION_HIGHLIGHT_DARK = Color(0xFF004F57)
    private val COLOR_FIRST_MESSAGE_HIGHLIGHT_DARK = Color(0xFF2D5000)
    private val COLOR_ELEVATED_MESSAGE_HIGHLIGHT_DARK = Color(0xFF574500)
    // Checkered background colors
    private val CHECKERED_LIGHT = Color(
        android.graphics.Color.argb(
            (255 * MaterialColors.ALPHA_DISABLED_LOW).toInt(),
            0, 0, 0
        )
    )
    private val CHECKERED_DARK = Color(
        android.graphics.Color.argb(
            (255 * MaterialColors.ALPHA_DISABLED_LOW).toInt(),
            255, 255, 255
        )
    )

    fun ChatItem.toChatMessageUiState(
        context: Context,
        appearanceSettings: AppearanceSettings,
        chatSettings: ChatSettings,
        preferenceStore: DankChatPreferenceStore,
        isAlternateBackground: Boolean,
    ): ChatMessageUiState {
        val textAlpha = when (importance) {
            ChatImportance.SYSTEM  -> 1f
            ChatImportance.DELETED -> 0.5f
            ChatImportance.REGULAR -> 1f
        }

        return when (val msg = message) {
            is SystemMessage          -> msg.toSystemMessageUi(
                tag = this.tag,
                context = context,
                chatSettings = chatSettings,
                isAlternateBackground = isAlternateBackground,
                textAlpha = textAlpha
            )

            is NoticeMessage          -> msg.toNoticeMessageUi(
                tag = this.tag,
                context = context,
                chatSettings = chatSettings,
                isAlternateBackground = isAlternateBackground,
                textAlpha = textAlpha
            )

            is UserNoticeMessage      -> msg.toUserNoticeMessageUi(
                tag = this.tag,
                context = context,
                chatSettings = chatSettings,
                isAlternateBackground = isAlternateBackground,
                textAlpha = textAlpha
            )

            is PrivMessage            -> msg.toPrivMessageUi(
                tag = this.tag,
                context = context,
                appearanceSettings = appearanceSettings,
                chatSettings = chatSettings,
                isAlternateBackground = isAlternateBackground,
                isMentionTab = isMentionTab,
                isInReplies = isInReplies,
                textAlpha = textAlpha
            )

            is AutomodMessage         -> msg.toAutomodMessageUi(
                tag = this.tag,
                chatSettings = chatSettings,
                isAlternateBackground = isAlternateBackground,
                textAlpha = textAlpha
            )

            is ModerationMessage      -> msg.toModerationMessageUi(
                tag = this.tag,
                context = context,
                chatSettings = chatSettings,
                preferenceStore = preferenceStore,
                isAlternateBackground = isAlternateBackground,
                textAlpha = textAlpha
            )

            is PointRedemptionMessage -> msg.toPointRedemptionMessageUi(
                tag = this.tag,
                context = context,
                chatSettings = chatSettings,
                textAlpha = textAlpha
            )

            is WhisperMessage         -> msg.toWhisperMessageUi(
                tag = this.tag,
                context = context,
                chatSettings = chatSettings,
                isAlternateBackground = isAlternateBackground,
                textAlpha = textAlpha,
                currentUserName = preferenceStore.userName
            )
        }
    }

    private fun SystemMessage.toSystemMessageUi(
        tag: Int,
        context: Context,
        chatSettings: ChatSettings,
        isAlternateBackground: Boolean,
        textAlpha: Float,
    ): ChatMessageUiState.SystemMessageUi {
        val backgroundColors = calculateCheckeredBackgroundColors(isAlternateBackground, false)
        val timestamp = if (chatSettings.showTimestamps) {
            DateTimeUtils.timestampToLocalTime(timestamp, chatSettings.formatter)
        } else ""

        val message = when (type) {
            is SystemMessageType.Disconnected                  -> context.getString(R.string.system_message_disconnected)
            is SystemMessageType.NoHistoryLoaded               -> context.getString(R.string.system_message_no_history)
            is SystemMessageType.Connected                     -> context.getString(R.string.system_message_connected)
            is SystemMessageType.Reconnected                   -> context.getString(R.string.system_message_reconnected)
            is SystemMessageType.LoginExpired                  -> context.getString(R.string.login_expired)
            is SystemMessageType.ChannelNonExistent            -> context.getString(R.string.system_message_channel_non_existent)
            is SystemMessageType.MessageHistoryIgnored         -> context.getString(R.string.system_message_history_ignored)
            is SystemMessageType.MessageHistoryIncomplete      -> context.getString(R.string.system_message_history_recovering)
            is SystemMessageType.ChannelBTTVEmotesFailed       -> context.getString(R.string.system_message_bttv_emotes_failed, type.status)
            is SystemMessageType.ChannelFFZEmotesFailed        -> context.getString(R.string.system_message_ffz_emotes_failed, type.status)
            is SystemMessageType.ChannelSevenTVEmotesFailed    -> context.getString(R.string.system_message_7tv_emotes_failed, type.status)
            is SystemMessageType.Custom                        -> type.message
            is SystemMessageType.MessageHistoryUnavailable     -> when (type.status) {
                null -> context.getString(R.string.system_message_history_unavailable)
                else -> context.getString(R.string.system_message_history_unavailable_detailed, type.status)
            }

            is SystemMessageType.ChannelSevenTVEmoteAdded      -> context.getString(R.string.system_message_7tv_emote_added, type.actorName, type.emoteName)
            is SystemMessageType.ChannelSevenTVEmoteRemoved    -> context.getString(R.string.system_message_7tv_emote_removed, type.actorName, type.emoteName)
            is SystemMessageType.ChannelSevenTVEmoteRenamed    -> context.getString(
                R.string.system_message_7tv_emote_renamed,
                type.actorName,
                type.oldEmoteName,
                type.emoteName
            )

            is SystemMessageType.ChannelSevenTVEmoteSetChanged -> context.getString(R.string.system_message_7tv_emote_set_changed, type.actorName, type.newEmoteSetName)
        }

        return ChatMessageUiState.SystemMessageUi(
            id = id,
            tag = tag,
            timestamp = timestamp,
            lightBackgroundColor = backgroundColors.light,
            darkBackgroundColor = backgroundColors.dark,
            textAlpha = textAlpha,
            message = message
        )
    }

    private fun NoticeMessage.toNoticeMessageUi(
        tag: Int,
        context: Context,
        chatSettings: ChatSettings,
        isAlternateBackground: Boolean,
        textAlpha: Float,
    ): ChatMessageUiState.NoticeMessageUi {
        val backgroundColors = calculateCheckeredBackgroundColors(isAlternateBackground, false)
        val timestamp = if (chatSettings.showTimestamps) {
            DateTimeUtils.timestampToLocalTime(timestamp, chatSettings.formatter)
        } else ""

        return ChatMessageUiState.NoticeMessageUi(
            id = id,
            tag = tag,
            timestamp = timestamp,
            lightBackgroundColor = backgroundColors.light,
            darkBackgroundColor = backgroundColors.dark,
            textAlpha = textAlpha,
            message = message
        )
    }

    private fun UserNoticeMessage.toUserNoticeMessageUi(
        tag: Int,
        context: Context,
        chatSettings: ChatSettings,
        isAlternateBackground: Boolean,
        textAlpha: Float,
    ): ChatMessageUiState.UserNoticeMessageUi {
        val shouldHighlight = highlights.any {
            it.type == HighlightType.Subscription ||
                    it.type == HighlightType.Announcement
        }
        val backgroundColors = when {
            shouldHighlight -> getHighlightColors(HighlightType.Subscription)
            else            -> calculateCheckeredBackgroundColors(isAlternateBackground, false)
        }
        val timestamp = if (chatSettings.showTimestamps) {
            DateTimeUtils.timestampToLocalTime(timestamp, chatSettings.formatter)
        } else ""

        return ChatMessageUiState.UserNoticeMessageUi(
            id = id,
            tag = tag,
            timestamp = timestamp,
            lightBackgroundColor = backgroundColors.light,
            darkBackgroundColor = backgroundColors.dark,
            textAlpha = textAlpha,
            message = message,
            shouldHighlight = shouldHighlight
        )
    }

    private fun ModerationMessage.toModerationMessageUi(
        tag: Int,
        context: Context,
        chatSettings: ChatSettings,
        preferenceStore: DankChatPreferenceStore,
        isAlternateBackground: Boolean,
        textAlpha: Float,
    ): ChatMessageUiState.ModerationMessageUi {
        val backgroundColors = calculateCheckeredBackgroundColors(isAlternateBackground, false)
        val timestamp = if (chatSettings.showTimestamps) {
            DateTimeUtils.timestampToLocalTime(timestamp, chatSettings.formatter)
        } else ""

        return ChatMessageUiState.ModerationMessageUi(
            id = id,
            tag = tag,
            timestamp = timestamp,
            lightBackgroundColor = backgroundColors.light,
            darkBackgroundColor = backgroundColors.dark,
            textAlpha = textAlpha,
            message = getSystemMessage(preferenceStore.userName, chatSettings.showTimedOutMessages)
        )
    }

    private fun AutomodMessage.toAutomodMessageUi(
        tag: Int,
        chatSettings: ChatSettings,
        isAlternateBackground: Boolean,
        textAlpha: Float,
    ): ChatMessageUiState.AutomodMessageUi {
        val timestamp = if (chatSettings.showTimestamps) {
            DateTimeUtils.timestampToLocalTime(timestamp, chatSettings.formatter)
        } else ""

        val uiStatus = when (status) {
            AutomodMessage.Status.Pending  -> ChatMessageUiState.AutomodMessageUi.AutomodMessageStatus.Pending
            AutomodMessage.Status.Approved -> ChatMessageUiState.AutomodMessageUi.AutomodMessageStatus.Approved
            AutomodMessage.Status.Denied   -> ChatMessageUiState.AutomodMessageUi.AutomodMessageStatus.Denied
            AutomodMessage.Status.Expired  -> ChatMessageUiState.AutomodMessageUi.AutomodMessageStatus.Expired
        }

        return ChatMessageUiState.AutomodMessageUi(
            id = id,
            tag = tag,
            timestamp = timestamp,
            lightBackgroundColor = Color.Unspecified,
            darkBackgroundColor = Color.Unspecified,
            textAlpha = textAlpha,
            heldMessageId = heldMessageId,
            channel = channel,
            badges = badges.mapIndexed { index, badge ->
                BadgeUi(
                    url = badge.url,
                    badge = badge,
                    position = index,
                    drawableResId = when (badge.badgeTag) {
                        "automod/1" -> R.drawable.ic_automod_badge
                        else        -> null
                    },
                )
            },
            userDisplayName = userName.formatWithDisplayName(userDisplayName),
            rawNameColor = color,
            messageText = messageText,
            reason = reason,
            status = uiStatus,
        )
    }

    private fun PrivMessage.toPrivMessageUi(
        tag: Int,
        context: Context,
        appearanceSettings: AppearanceSettings,
        chatSettings: ChatSettings,
        isAlternateBackground: Boolean,
        isMentionTab: Boolean,
        isInReplies: Boolean,
        textAlpha: Float,
    ): ChatMessageUiState.PrivMessageUi {
        val backgroundColors = when {
            timedOut && !chatSettings.showTimedOutMessages -> BackgroundColors(Color.Transparent, Color.Transparent)
            highlights.isNotEmpty()                        -> highlights.toBackgroundColors()
            else                                           -> calculateCheckeredBackgroundColors(isAlternateBackground, true)
        }

        val timestamp = if (chatSettings.showTimestamps) {
            DateTimeUtils.timestampToLocalTime(timestamp, chatSettings.formatter)
        } else ""

        val nameText = when {
            !chatSettings.showUsernames    -> ""
            isAction                       -> "$aliasOrFormattedName "
            aliasOrFormattedName.isBlank() -> ""
            else                           -> "$aliasOrFormattedName: "
        }

        val allowedBadges = badges.filter { it.type in chatSettings.visibleBadgeTypes }
        val badgeUis = allowedBadges.mapIndexed { index, badge ->
            BadgeUi(
                url = badge.url,
                badge = badge,
                position = index
            )
        }

        val emoteUis = emotes.groupBy { it.position }.map { (position, emoteGroup) ->
            // Check if any emote in the group is animated - we need to check the type
            val hasAnimated = emoteGroup.any { emote ->
                when (emote.type) {
                    is ChatMessageEmoteType.TwitchEmote        -> false // Twitch emotes can be animated but we don't have that info here
                    is ChatMessageEmoteType.ChannelFFZEmote,
                    is ChatMessageEmoteType.GlobalFFZEmote,
                    is ChatMessageEmoteType.ChannelBTTVEmote,
                    is ChatMessageEmoteType.GlobalBTTVEmote    -> true // Assume third-party can be animated
                    is ChatMessageEmoteType.ChannelSevenTVEmote,
                    is ChatMessageEmoteType.GlobalSevenTVEmote -> true
                    is ChatMessageEmoteType.Cheermote          -> true
                }
            }

            val firstEmote = emoteGroup.first()
            EmoteUi(
                code = firstEmote.code,
                urls = emoteGroup.map { it.url },
                position = position,
                isAnimated = hasAnimated,
                isTwitch = emoteGroup.any { it.isTwitch },
                scale = firstEmote.scale,
                emotes = emoteGroup,
                cheerAmount = firstEmote.cheerAmount,
                cheerColor = firstEmote.cheerColor?.let { Color(it) },
            )
        }

        val threadUi = if (thread != null && !isInReplies) {
            thread.toThreadUi()
        } else null

        val fullMessage = buildString {
            if (isMentionTab && highlights.any { it.isMention }) {
                append("#$channel ")
            }
            if (timestamp.isNotEmpty()) {
                append("$timestamp ")
            }
            append(nameText)
            append(message)
        }

        // Store raw color for normalization at render time (needs Compose theme context)
        val rawNameColor = userDisplay?.color ?: color

        return ChatMessageUiState.PrivMessageUi(
            id = id,
            tag = tag,
            timestamp = timestamp,
            lightBackgroundColor = backgroundColors.light,
            darkBackgroundColor = backgroundColors.dark,
            textAlpha = textAlpha,
            enableRipple = true,
            channel = channel,
            userId = userId,
            userName = name,
            displayName = displayName,
            badges = badgeUis,
            rawNameColor = rawNameColor,
            nameText = nameText,
            message = message,
            emotes = emoteUis,
            isAction = isAction,
            thread = threadUi,
            fullMessage = fullMessage
        )
    }

    private fun PointRedemptionMessage.toPointRedemptionMessageUi(
        tag: Int,
        context: Context,
        chatSettings: ChatSettings,
        textAlpha: Float,
    ): ChatMessageUiState.PointRedemptionMessageUi {
        val backgroundColors = getHighlightColors(HighlightType.ChannelPointRedemption)
        val timestamp = if (chatSettings.showTimestamps) {
            DateTimeUtils.timestampToLocalTime(timestamp, chatSettings.formatter)
        } else ""

        val nameText = if (!requiresUserInput) aliasOrFormattedName else null

        return ChatMessageUiState.PointRedemptionMessageUi(
            id = id,
            tag = tag,
            timestamp = timestamp,
            lightBackgroundColor = backgroundColors.light,
            darkBackgroundColor = backgroundColors.dark,
            textAlpha = textAlpha,
            nameText = nameText,
            title = title,
            cost = cost,
            rewardImageUrl = rewardImageUrl,
            requiresUserInput = requiresUserInput
        )
    }

    private fun WhisperMessage.toWhisperMessageUi(
        tag: Int,
        context: Context,
        chatSettings: ChatSettings,
        isAlternateBackground: Boolean,
        textAlpha: Float,
        currentUserName: UserName?,
    ): ChatMessageUiState.WhisperMessageUi {
        val backgroundColors = calculateCheckeredBackgroundColors(isAlternateBackground, true)
        val timestamp = if (chatSettings.showTimestamps) {
            DateTimeUtils.timestampToLocalTime(timestamp, chatSettings.formatter)
        } else ""

        val allowedBadges = badges.filter { it.type in chatSettings.visibleBadgeTypes }
        val badgeUis = allowedBadges.mapIndexed { index, badge ->
            BadgeUi(
                url = badge.url,
                badge = badge,
                position = index
            )
        }

        val emoteUis = emotes.groupBy { it.position }.map { (position, emoteGroup) ->
            // Check if any emote in the group is animated
            val hasAnimated = emoteGroup.any { emote ->
                when (emote.type) {
                    is ChatMessageEmoteType.TwitchEmote        -> false
                    is ChatMessageEmoteType.ChannelFFZEmote,
                    is ChatMessageEmoteType.GlobalFFZEmote,
                    is ChatMessageEmoteType.ChannelBTTVEmote,
                    is ChatMessageEmoteType.GlobalBTTVEmote    -> true
                    is ChatMessageEmoteType.ChannelSevenTVEmote,
                    is ChatMessageEmoteType.GlobalSevenTVEmote -> true
                    is ChatMessageEmoteType.Cheermote          -> true
                }
            }

            val firstEmote = emoteGroup.first()
            EmoteUi(
                code = firstEmote.code,
                urls = emoteGroup.map { it.url },
                position = position,
                isAnimated = hasAnimated,
                isTwitch = emoteGroup.any { it.isTwitch },
                scale = firstEmote.scale,
                emotes = emoteGroup,
                cheerAmount = firstEmote.cheerAmount,
                cheerColor = firstEmote.cheerColor?.let { Color(it) },
            )
        }

        val fullMessage = buildString {
            if (timestamp.isNotEmpty()) {
                append("$timestamp ")
            }
            append("$senderAliasOrFormattedName -> $recipientAliasOrFormattedName: ")
            append(message)
        }

        // Store raw colors for normalization at render time (needs Compose theme context)
        val rawSenderColor = userDisplay?.color ?: color
        val rawRecipientColor = recipientDisplay?.color ?: recipientColor

        return ChatMessageUiState.WhisperMessageUi(
            id = id,
            tag = tag,
            timestamp = timestamp,
            lightBackgroundColor = backgroundColors.light,
            darkBackgroundColor = backgroundColors.dark,
            textAlpha = textAlpha,
            enableRipple = true,
            userId = userId ?: error("Whisper must have userId"),
            userName = name,
            displayName = displayName,
            badges = badgeUis,
            rawSenderColor = rawSenderColor,
            rawRecipientColor = rawRecipientColor,
            senderName = senderAliasOrFormattedName,
            recipientName = recipientAliasOrFormattedName,
            message = message,
            emotes = emoteUis,
            fullMessage = fullMessage,
            replyTargetName = if (currentUserName != null && name.value.equals(currentUserName.value, ignoreCase = true)) recipientName else name
        )
    }

    data class BackgroundColors(val light: Color, val dark: Color)

    private fun calculateCheckeredBackgroundColors(
        isAlternateBackground: Boolean,
        enableCheckered: Boolean,
    ): BackgroundColors {
        return if (enableCheckered && isAlternateBackground) {
            BackgroundColors(CHECKERED_LIGHT, CHECKERED_DARK)
        } else {
            BackgroundColors(Color.Transparent, Color.Transparent)
        }
    }

    private fun getHighlightColors(type: HighlightType): BackgroundColors {
        return when (type) {
            HighlightType.Subscription,
            HighlightType.Announcement           -> BackgroundColors(
                light = COLOR_SUB_HIGHLIGHT_LIGHT,
                dark = COLOR_SUB_HIGHLIGHT_DARK,
            )

            HighlightType.ChannelPointRedemption -> BackgroundColors(
                light = COLOR_REDEMPTION_HIGHLIGHT_LIGHT,
                dark = COLOR_REDEMPTION_HIGHLIGHT_DARK,
            )

            HighlightType.ElevatedMessage        -> BackgroundColors(
                light = COLOR_ELEVATED_MESSAGE_HIGHLIGHT_LIGHT,
                dark = COLOR_ELEVATED_MESSAGE_HIGHLIGHT_DARK,
            )

            HighlightType.FirstMessage           -> BackgroundColors(
                light = COLOR_FIRST_MESSAGE_HIGHLIGHT_LIGHT,
                dark = COLOR_FIRST_MESSAGE_HIGHLIGHT_DARK,
            )

            HighlightType.Username,
            HighlightType.Custom,
            HighlightType.Reply,
            HighlightType.Badge,
            HighlightType.Notification           -> BackgroundColors(
                light = COLOR_MENTION_HIGHLIGHT_LIGHT,
                dark = COLOR_MENTION_HIGHLIGHT_DARK,
            )
        }
    }

    private fun Set<Highlight>.toBackgroundColors(): BackgroundColors {
        val highlight = this.maxByOrNull { it.type.priority.value }
            ?: return BackgroundColors(Color.Transparent, Color.Transparent)

        if (highlight.customColor != null) {
            val color = Color(highlight.customColor)
            return BackgroundColors(color, color)
        }

        return getHighlightColors(highlight.type)
    }
}