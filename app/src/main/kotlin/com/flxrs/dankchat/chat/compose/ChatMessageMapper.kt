package com.flxrs.dankchat.chat.compose

import android.content.Context
import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.flxrs.dankchat.R
import com.flxrs.dankchat.chat.ChatImportance
import com.flxrs.dankchat.chat.ChatItem
import com.flxrs.dankchat.data.twitch.emote.ChatMessageEmote
import com.flxrs.dankchat.data.twitch.emote.ChatMessageEmoteType
import com.flxrs.dankchat.data.twitch.message.ModerationMessage
import com.flxrs.dankchat.data.twitch.message.NoticeMessage
import com.flxrs.dankchat.data.twitch.message.PointRedemptionMessage
import com.flxrs.dankchat.data.twitch.message.PrivMessage
import com.flxrs.dankchat.data.twitch.message.SystemMessage
import com.flxrs.dankchat.data.twitch.message.SystemMessageType
import com.flxrs.dankchat.data.twitch.message.UserNoticeMessage
import com.flxrs.dankchat.data.twitch.message.WhisperMessage
import com.flxrs.dankchat.data.twitch.message.aliasOrFormattedName
import com.flxrs.dankchat.data.twitch.message.customOrUserColorOn
import com.flxrs.dankchat.data.twitch.message.recipientAliasOrFormattedName
import com.flxrs.dankchat.data.twitch.message.recipientColorOnBackground
import com.flxrs.dankchat.data.twitch.message.senderAliasOrFormattedName
import com.flxrs.dankchat.data.twitch.message.senderColorOnBackground
import com.flxrs.dankchat.preferences.appearance.AppearanceSettings
import com.flxrs.dankchat.preferences.chat.ChatSettings
import com.flxrs.dankchat.utils.DateTimeUtils
import com.google.android.material.color.MaterialColors

/**
 * Maps domain Message objects to Compose UI state objects.
 * Pre-computes all rendering decisions to minimize work during composition.
 */
object ChatMessageMapper {

    fun ChatItem.toChatMessageUiState(
        context: Context,
        appearanceSettings: AppearanceSettings,
        chatSettings: ChatSettings,
        isAlternateBackground: Boolean,
    ): ChatMessageUiState {
        val textAlpha = when (importance) {
            ChatImportance.SYSTEM -> 0.75f
            ChatImportance.DELETED -> 0.5f
            ChatImportance.REGULAR -> 1f
        }

        return when (val msg = message) {
            is SystemMessage -> msg.toSystemMessageUi(
                context = context,
                chatSettings = chatSettings,
                isAlternateBackground = isAlternateBackground,
                textAlpha = textAlpha
            )
            is NoticeMessage -> msg.toNoticeMessageUi(
                context = context,
                chatSettings = chatSettings,
                isAlternateBackground = isAlternateBackground,
                textAlpha = textAlpha
            )
            is UserNoticeMessage -> msg.toUserNoticeMessageUi(
                context = context,
                chatSettings = chatSettings,
                isAlternateBackground = isAlternateBackground,
                textAlpha = textAlpha
            )
            is PrivMessage -> msg.toPrivMessageUi(
                context = context,
                appearanceSettings = appearanceSettings,
                chatSettings = chatSettings,
                isAlternateBackground = isAlternateBackground,
                isMentionTab = isMentionTab,
                isInReplies = isInReplies,
                textAlpha = textAlpha
            )
            is ModerationMessage -> msg.toModerationMessageUi(
                context = context,
                chatSettings = chatSettings,
                isAlternateBackground = isAlternateBackground,
                textAlpha = textAlpha
            )
            is PointRedemptionMessage -> msg.toPointRedemptionMessageUi(
                context = context,
                chatSettings = chatSettings,
                textAlpha = textAlpha
            )
            is WhisperMessage -> msg.toWhisperMessageUi(
                context = context,
                chatSettings = chatSettings,
                isAlternateBackground = isAlternateBackground,
                textAlpha = textAlpha
            )
        }
    }

    private fun SystemMessage.toSystemMessageUi(
        context: Context,
        chatSettings: ChatSettings,
        isAlternateBackground: Boolean,
        textAlpha: Float,
    ): ChatMessageUiState.SystemMessageUi {
        val backgroundColor = calculateCheckeredBackground(context, isAlternateBackground, false)
        val timestamp = if (chatSettings.showTimestamps) {
            DateTimeUtils.timestampToLocalTime(timestamp, chatSettings.formatter)
        } else ""

        val message = when (type) {
            is SystemMessageType.Disconnected -> context.getString(R.string.system_message_disconnected)
            is SystemMessageType.NoHistoryLoaded -> context.getString(R.string.system_message_no_history)
            is SystemMessageType.Connected -> context.getString(R.string.system_message_connected)
            is SystemMessageType.Reconnected -> context.getString(R.string.system_message_reconnected)
            is SystemMessageType.LoginExpired -> context.getString(R.string.login_expired)
            is SystemMessageType.ChannelNonExistent -> context.getString(R.string.system_message_channel_non_existent)
            is SystemMessageType.MessageHistoryIgnored -> context.getString(R.string.system_message_history_ignored)
            is SystemMessageType.MessageHistoryIncomplete -> context.getString(R.string.system_message_history_recovering)
            is SystemMessageType.ChannelBTTVEmotesFailed -> context.getString(R.string.system_message_bttv_emotes_failed, type.status)
            is SystemMessageType.ChannelFFZEmotesFailed -> context.getString(R.string.system_message_ffz_emotes_failed, type.status)
            is SystemMessageType.ChannelSevenTVEmotesFailed -> context.getString(R.string.system_message_7tv_emotes_failed, type.status)
            is SystemMessageType.Custom -> type.message
            is SystemMessageType.MessageHistoryUnavailable -> when (type.status) {
                null -> context.getString(R.string.system_message_history_unavailable)
                else -> context.getString(R.string.system_message_history_unavailable_detailed, type.status)
            }
            is SystemMessageType.ChannelSevenTVEmoteAdded -> context.getString(R.string.system_message_7tv_emote_added, type.actorName, type.emoteName)
            is SystemMessageType.ChannelSevenTVEmoteRemoved -> context.getString(R.string.system_message_7tv_emote_removed, type.actorName, type.emoteName)
            is SystemMessageType.ChannelSevenTVEmoteRenamed -> context.getString(
                R.string.system_message_7tv_emote_renamed,
                type.actorName,
                type.oldEmoteName,
                type.emoteName
            )
            is SystemMessageType.ChannelSevenTVEmoteSetChanged -> context.getString(R.string.system_message_7tv_emote_set_changed, type.actorName, type.newEmoteSetName)
        }

        return ChatMessageUiState.SystemMessageUi(
            id = id,
            timestamp = timestamp,
            backgroundColor = backgroundColor,
            textAlpha = textAlpha,
            message = message
        )
    }

    private fun NoticeMessage.toNoticeMessageUi(
        context: Context,
        chatSettings: ChatSettings,
        isAlternateBackground: Boolean,
        textAlpha: Float,
    ): ChatMessageUiState.NoticeMessageUi {
        val backgroundColor = calculateCheckeredBackground(context, isAlternateBackground, false)
        val timestamp = if (chatSettings.showTimestamps) {
            DateTimeUtils.timestampToLocalTime(timestamp, chatSettings.formatter)
        } else ""

        return ChatMessageUiState.NoticeMessageUi(
            id = id,
            timestamp = timestamp,
            backgroundColor = backgroundColor,
            textAlpha = textAlpha,
            message = message
        )
    }

    private fun UserNoticeMessage.toUserNoticeMessageUi(
        context: Context,
        chatSettings: ChatSettings,
        isAlternateBackground: Boolean,
        textAlpha: Float,
    ): ChatMessageUiState.UserNoticeMessageUi {
        val shouldHighlight = highlights.any { 
            it.type == com.flxrs.dankchat.data.twitch.message.HighlightType.Subscription ||
            it.type == com.flxrs.dankchat.data.twitch.message.HighlightType.Announcement
        }
        val backgroundColor = when {
            shouldHighlight -> ContextCompat.getColor(context, R.color.color_sub_highlight)
            else -> calculateCheckeredBackground(context, isAlternateBackground, false)
        }
        val timestamp = if (chatSettings.showTimestamps) {
            DateTimeUtils.timestampToLocalTime(timestamp, chatSettings.formatter)
        } else ""

        return ChatMessageUiState.UserNoticeMessageUi(
            id = id,
            timestamp = timestamp,
            backgroundColor = backgroundColor,
            textAlpha = textAlpha,
            message = message,
            shouldHighlight = shouldHighlight
        )
    }

    private fun ModerationMessage.toModerationMessageUi(
        context: Context,
        chatSettings: ChatSettings,
        isAlternateBackground: Boolean,
        textAlpha: Float,
    ): ChatMessageUiState.ModerationMessageUi {
        val backgroundColor = calculateCheckeredBackground(context, isAlternateBackground, false)
        val timestamp = if (chatSettings.showTimestamps) {
            DateTimeUtils.timestampToLocalTime(timestamp, chatSettings.formatter)
        } else ""

        return ChatMessageUiState.ModerationMessageUi(
            id = id,
            timestamp = timestamp,
            backgroundColor = backgroundColor,
            textAlpha = textAlpha,
            message = "" // TODO: Implement getSystemMessage
        )
    }

    private fun PrivMessage.toPrivMessageUi(
        context: Context,
        appearanceSettings: AppearanceSettings,
        chatSettings: ChatSettings,
        isAlternateBackground: Boolean,
        isMentionTab: Boolean,
        isInReplies: Boolean,
        textAlpha: Float,
    ): ChatMessageUiState.PrivMessageUi {
        val bgColor = when {
            timedOut && !chatSettings.showTimedOutMessages -> Color.TRANSPARENT
            highlights.isNotEmpty() -> highlights.toBackgroundColor(context)
            else -> calculateCheckeredBackground(context, isAlternateBackground, true)
        }

        val timestamp = if (chatSettings.showTimestamps) {
            DateTimeUtils.timestampToLocalTime(timestamp, chatSettings.formatter)
        } else ""

        val nameText = when {
            !chatSettings.showUsernames -> ""
            isAction -> "$aliasOrFormattedName "
            aliasOrFormattedName.isBlank() -> ""
            else -> "$aliasOrFormattedName: "
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
                    is ChatMessageEmoteType.TwitchEmote -> false // Twitch emotes can be animated but we don't have that info here
                    is ChatMessageEmoteType.ChannelFFZEmote,
                    is ChatMessageEmoteType.GlobalFFZEmote,
                    is ChatMessageEmoteType.ChannelBTTVEmote,
                    is ChatMessageEmoteType.GlobalBTTVEmote -> true // Assume third-party can be animated
                    is ChatMessageEmoteType.ChannelSevenTVEmote,
                    is ChatMessageEmoteType.GlobalSevenTVEmote -> true
                }
            }
            
            EmoteUi(
                code = emoteGroup.first().code,
                urls = emoteGroup.map { it.url },
                position = position,
                isAnimated = hasAnimated,
                isTwitch = emoteGroup.any { it.isTwitch },
                scale = emoteGroup.first().scale,
                emotes = emoteGroup
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

        return ChatMessageUiState.PrivMessageUi(
            id = id,
            timestamp = timestamp,
            backgroundColor = bgColor,
            textAlpha = textAlpha,
            enableRipple = true,
            channel = channel,
            userId = userId,
            userName = name,
            displayName = displayName,
            badges = badgeUis,
            nameColor = customOrUserColorOn(bgColor),
            nameText = nameText,
            message = message,
            emotes = emoteUis,
            isAction = isAction,
            thread = threadUi,
            fullMessage = fullMessage
        )
    }

    private fun PointRedemptionMessage.toPointRedemptionMessageUi(
        context: Context,
        chatSettings: ChatSettings,
        textAlpha: Float,
    ): ChatMessageUiState.PointRedemptionMessageUi {
        val backgroundColor = ContextCompat.getColor(context, R.color.color_redemption_highlight)
        val timestamp = if (chatSettings.showTimestamps) {
            DateTimeUtils.timestampToLocalTime(timestamp, chatSettings.formatter)
        } else ""

        val nameText = if (!requiresUserInput) aliasOrFormattedName else null

        return ChatMessageUiState.PointRedemptionMessageUi(
            id = id,
            timestamp = timestamp,
            backgroundColor = backgroundColor,
            textAlpha = textAlpha,
            nameText = nameText,
            title = title,
            cost = cost,
            rewardImageUrl = rewardImageUrl,
            requiresUserInput = requiresUserInput
        )
    }

    private fun WhisperMessage.toWhisperMessageUi(
        context: Context,
        chatSettings: ChatSettings,
        isAlternateBackground: Boolean,
        textAlpha: Float,
    ): ChatMessageUiState.WhisperMessageUi {
        val backgroundColor = calculateCheckeredBackground(context, isAlternateBackground, true)
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
                    is ChatMessageEmoteType.TwitchEmote -> false
                    is ChatMessageEmoteType.ChannelFFZEmote,
                    is ChatMessageEmoteType.GlobalFFZEmote,
                    is ChatMessageEmoteType.ChannelBTTVEmote,
                    is ChatMessageEmoteType.GlobalBTTVEmote -> true
                    is ChatMessageEmoteType.ChannelSevenTVEmote,
                    is ChatMessageEmoteType.GlobalSevenTVEmote -> true
                }
            }
            
            EmoteUi(
                code = emoteGroup.first().code,
                urls = emoteGroup.map { it.url },
                position = position,
                isAnimated = hasAnimated,
                isTwitch = emoteGroup.any { it.isTwitch },
                scale = emoteGroup.first().scale,
                emotes = emoteGroup
            )
        }

        val fullMessage = buildString {
            if (timestamp.isNotEmpty()) {
                append("$timestamp ")
            }
            append("$senderAliasOrFormattedName -> $recipientAliasOrFormattedName: ")
            append(message)
        }

        return ChatMessageUiState.WhisperMessageUi(
            id = id,
            timestamp = timestamp,
            backgroundColor = backgroundColor,
            textAlpha = textAlpha,
            enableRipple = true,
            userId = userId ?: error("Whisper must have userId"),
            userName = name,
            displayName = displayName,
            badges = badgeUis,
            senderColor = senderColorOnBackground(backgroundColor),
            recipientColor = recipientColorOnBackground(backgroundColor),
            senderName = senderAliasOrFormattedName,
            recipientName = recipientAliasOrFormattedName,
            message = message,
            emotes = emoteUis,
            fullMessage = fullMessage
        )
    }

    private fun calculateCheckeredBackground(
        context: Context,
        isAlternateBackground: Boolean,
        enableCheckered: Boolean, // Will be controlled by settings
    ): Int {
        return when {
            enableCheckered && isAlternateBackground -> {
                // Manual calculation since we don't have a View
                val backgroundColor = android.graphics.Color.TRANSPARENT
                val surfaceInverse = ContextCompat.getColor(context, android.R.color.white)
                // Use alpha blending for checkered effect
                android.graphics.Color.argb(
                    (255 * MaterialColors.ALPHA_DISABLED_LOW).toInt(),
                    android.graphics.Color.red(surfaceInverse),
                    android.graphics.Color.green(surfaceInverse),
                    android.graphics.Color.blue(surfaceInverse)
                )
            }
            else -> ContextCompat.getColor(context, android.R.color.transparent)
        }
    }

    @ColorInt
    private fun Set<com.flxrs.dankchat.data.twitch.message.Highlight>.toBackgroundColor(context: Context): Int {
        val highlight = this.maxByOrNull { it.type.priority.value }
            ?: return ContextCompat.getColor(context, android.R.color.transparent)
        return when (highlight.type) {
            com.flxrs.dankchat.data.twitch.message.HighlightType.Subscription,
            com.flxrs.dankchat.data.twitch.message.HighlightType.Announcement -> ContextCompat.getColor(context, R.color.color_sub_highlight)
            com.flxrs.dankchat.data.twitch.message.HighlightType.ChannelPointRedemption -> ContextCompat.getColor(context, R.color.color_redemption_highlight)
            com.flxrs.dankchat.data.twitch.message.HighlightType.ElevatedMessage -> ContextCompat.getColor(context, R.color.color_elevated_message_highlight)
            com.flxrs.dankchat.data.twitch.message.HighlightType.FirstMessage -> ContextCompat.getColor(context, R.color.color_first_message_highlight)
            com.flxrs.dankchat.data.twitch.message.HighlightType.Username,
            com.flxrs.dankchat.data.twitch.message.HighlightType.Custom,
            com.flxrs.dankchat.data.twitch.message.HighlightType.Reply,
            com.flxrs.dankchat.data.twitch.message.HighlightType.Notification -> ContextCompat.getColor(context, R.color.color_mention_highlight)
        }
    }
}