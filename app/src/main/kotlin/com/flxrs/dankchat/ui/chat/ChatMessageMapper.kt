package com.flxrs.dankchat.ui.chat

import androidx.compose.ui.graphics.Color
import com.flxrs.dankchat.R
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.chat.ChatImportance
import com.flxrs.dankchat.data.chat.ChatItem
import com.flxrs.dankchat.data.repo.chat.UsersRepository
import com.flxrs.dankchat.data.toUserName
import com.flxrs.dankchat.data.twitch.emote.ChatMessageEmoteType
import com.flxrs.dankchat.data.twitch.message.AutomodMessage
import com.flxrs.dankchat.data.twitch.message.Highlight
import com.flxrs.dankchat.data.twitch.message.HighlightType
import com.flxrs.dankchat.data.twitch.message.Message
import com.flxrs.dankchat.data.twitch.message.ModerationMessage
import com.flxrs.dankchat.data.twitch.message.NoticeMessage
import com.flxrs.dankchat.data.twitch.message.PointRedemptionMessage
import com.flxrs.dankchat.data.twitch.message.PrivMessage
import com.flxrs.dankchat.data.twitch.message.SystemMessage
import com.flxrs.dankchat.data.twitch.message.SystemMessageType
import com.flxrs.dankchat.data.twitch.message.UserNoticeMessage
import com.flxrs.dankchat.data.twitch.message.WhisperMessage
import com.flxrs.dankchat.data.twitch.message.aliasOrFormattedName
import com.flxrs.dankchat.data.twitch.message.highestPriorityHighlight
import com.flxrs.dankchat.data.twitch.message.recipientAliasOrFormattedName
import com.flxrs.dankchat.data.twitch.message.senderAliasOrFormattedName
import com.flxrs.dankchat.preferences.DankChatPreferenceStore
import com.flxrs.dankchat.preferences.chat.ChatSettings
import com.flxrs.dankchat.utils.DateTimeUtils
import com.flxrs.dankchat.utils.TextResource
import com.google.android.material.color.MaterialColors
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.koin.core.annotation.Single

/**
 * Maps domain Message objects to Compose UI state objects.
 * Pre-computed all rendering decisions to minimize work during composition.
 */
@Single
class ChatMessageMapper(
    private val usersRepository: UsersRepository,
) {
    fun mapToUiState(
        item: ChatItem,
        chatSettings: ChatSettings,
        preferenceStore: DankChatPreferenceStore,
        isAlternateBackground: Boolean,
    ): ChatMessageUiState {
        val textAlpha =
            when (item.importance) {
                ChatImportance.SYSTEM -> 1f
                ChatImportance.DELETED -> 0.5f
                ChatImportance.REGULAR -> 1f
            }

        return when (val msg = item.message) {
            is SystemMessage -> {
                msg.toSystemMessageUi(
                    tag = item.tag,
                    chatSettings = chatSettings,
                    isAlternateBackground = isAlternateBackground,
                    textAlpha = textAlpha,
                )
            }

            is NoticeMessage -> {
                msg.toNoticeMessageUi(
                    tag = item.tag,
                    chatSettings = chatSettings,
                    isAlternateBackground = isAlternateBackground,
                    textAlpha = textAlpha,
                )
            }

            is UserNoticeMessage -> {
                msg.toUserNoticeMessageUi(
                    tag = item.tag,
                    chatSettings = chatSettings,
                    isAlternateBackground = isAlternateBackground,
                    textAlpha = textAlpha,
                )
            }

            is PrivMessage -> {
                msg.toPrivMessageUi(
                    tag = item.tag,
                    chatSettings = chatSettings,
                    isAlternateBackground = isAlternateBackground,
                    isMentionTab = item.isMentionTab,
                    isInReplies = item.isInReplies,
                    textAlpha = textAlpha,
                )
            }

            is AutomodMessage -> {
                msg.toAutomodMessageUi(
                    tag = item.tag,
                    chatSettings = chatSettings,
                    textAlpha = textAlpha,
                )
            }

            is ModerationMessage -> {
                msg.toModerationMessageUi(
                    tag = item.tag,
                    chatSettings = chatSettings,
                    preferenceStore = preferenceStore,
                    isAlternateBackground = isAlternateBackground,
                    textAlpha = textAlpha,
                )
            }

            is PointRedemptionMessage -> {
                msg.toPointRedemptionMessageUi(
                    tag = item.tag,
                    chatSettings = chatSettings,
                    textAlpha = textAlpha,
                )
            }

            is WhisperMessage -> {
                msg.toWhisperMessageUi(
                    tag = item.tag,
                    chatSettings = chatSettings,
                    isAlternateBackground = isAlternateBackground,
                    textAlpha = textAlpha,
                    currentUserName = preferenceStore.userName,
                )
            }
        }
    }

    private fun SystemMessage.toSystemMessageUi(
        tag: Int,
        chatSettings: ChatSettings,
        isAlternateBackground: Boolean,
        textAlpha: Float,
    ): ChatMessageUiState.SystemMessageUi {
        val backgroundColors = calculateCheckeredBackgroundColors(isAlternateBackground, false)
        val timestamp =
            if (chatSettings.showTimestamps) {
                DateTimeUtils.timestampToLocalTime(timestamp, chatSettings.formatter)
            } else {
                ""
            }

        val message =
            when (type) {
                is SystemMessageType.Disconnected -> {
                    TextResource.Res(R.string.system_message_disconnected)
                }

                is SystemMessageType.NoHistoryLoaded -> {
                    TextResource.Res(R.string.system_message_no_history)
                }

                is SystemMessageType.Connected -> {
                    TextResource.Res(R.string.system_message_connected)
                }

                is SystemMessageType.Reconnected -> {
                    TextResource.Res(R.string.system_message_reconnected)
                }

                is SystemMessageType.LoginExpired -> {
                    TextResource.Res(R.string.login_expired)
                }

                is SystemMessageType.ChannelNonExistent -> {
                    TextResource.Res(R.string.system_message_channel_non_existent)
                }

                is SystemMessageType.MessageHistoryIgnored -> {
                    TextResource.Res(R.string.system_message_history_ignored)
                }

                is SystemMessageType.MessageHistoryIncomplete -> {
                    TextResource.Res(R.string.system_message_history_recovering)
                }

                is SystemMessageType.ChannelBTTVEmotesFailed -> {
                    TextResource.Res(R.string.system_message_bttv_emotes_failed, persistentListOf(type.status))
                }

                is SystemMessageType.ChannelFFZEmotesFailed -> {
                    TextResource.Res(R.string.system_message_ffz_emotes_failed, persistentListOf(type.status))
                }

                is SystemMessageType.ChannelSevenTVEmotesFailed -> {
                    TextResource.Res(R.string.system_message_7tv_emotes_failed, persistentListOf(type.status))
                }

                is SystemMessageType.Custom -> {
                    TextResource.Plain(type.message)
                }

                is SystemMessageType.Debug -> {
                    TextResource.Plain(type.message)
                }

                is SystemMessageType.SendNotLoggedIn -> {
                    TextResource.Res(R.string.system_message_send_not_logged_in)
                }

                is SystemMessageType.SendChannelNotResolved -> {
                    TextResource.Res(R.string.system_message_send_channel_not_resolved, persistentListOf(type.channel))
                }

                is SystemMessageType.SendNotDelivered -> {
                    TextResource.Res(R.string.system_message_send_not_delivered)
                }

                is SystemMessageType.SendDropped -> {
                    TextResource.Res(R.string.system_message_send_dropped, persistentListOf(type.reason, type.code))
                }

                is SystemMessageType.SendMissingScopes -> {
                    TextResource.Res(R.string.system_message_send_missing_scopes)
                }

                is SystemMessageType.SendNotAuthorized -> {
                    TextResource.Res(R.string.system_message_send_not_authorized)
                }

                is SystemMessageType.SendMessageTooLarge -> {
                    TextResource.Res(R.string.system_message_send_message_too_large)
                }

                is SystemMessageType.SendRateLimited -> {
                    TextResource.Res(R.string.system_message_send_rate_limited)
                }

                is SystemMessageType.SendFailed -> {
                    TextResource.Res(R.string.system_message_send_failed, persistentListOf(type.message ?: ""))
                }

                is SystemMessageType.MessageHistoryUnavailable -> {
                    when (type.status) {
                        null -> TextResource.Res(R.string.system_message_history_unavailable)
                        else -> TextResource.Res(R.string.system_message_history_unavailable_detailed, persistentListOf(type.status))
                    }
                }

                is SystemMessageType.ChannelSevenTVEmoteAdded -> {
                    TextResource.Res(R.string.system_message_7tv_emote_added, persistentListOf(type.actorName, type.emoteName))
                }

                is SystemMessageType.ChannelSevenTVEmoteRemoved -> {
                    TextResource.Res(R.string.system_message_7tv_emote_removed, persistentListOf(type.actorName, type.emoteName))
                }

                is SystemMessageType.ChannelSevenTVEmoteRenamed -> {
                    TextResource.Res(
                        R.string.system_message_7tv_emote_renamed,
                        persistentListOf(type.actorName, type.oldEmoteName, type.emoteName),
                    )
                }

                is SystemMessageType.ChannelSevenTVEmoteSetChanged -> {
                    TextResource.Res(R.string.system_message_7tv_emote_set_changed, persistentListOf(type.actorName, type.newEmoteSetName))
                }

                is SystemMessageType.AutomodActionFailed -> {
                    val actionRes = TextResource.Res(if (type.allow) R.string.automod_allow else R.string.automod_deny)
                    val errorResId =
                        when (type.statusCode) {
                            400 -> R.string.automod_error_already_processed
                            401 -> R.string.automod_error_not_authenticated
                            403 -> R.string.automod_error_not_authorized
                            404 -> R.string.automod_error_not_found
                            else -> R.string.automod_error_unknown
                        }
                    TextResource.Res(errorResId, persistentListOf(actionRes))
                }
            }

        return ChatMessageUiState.SystemMessageUi(
            id = id,
            tag = tag,
            timestamp = timestamp,
            lightBackgroundColor = backgroundColors.light,
            darkBackgroundColor = backgroundColors.dark,
            textAlpha = textAlpha,
            message = message,
        )
    }

    private fun NoticeMessage.toNoticeMessageUi(
        tag: Int,
        chatSettings: ChatSettings,
        isAlternateBackground: Boolean,
        textAlpha: Float,
    ): ChatMessageUiState.NoticeMessageUi {
        val backgroundColors = calculateCheckeredBackgroundColors(isAlternateBackground, false)
        val timestamp =
            if (chatSettings.showTimestamps) {
                DateTimeUtils.timestampToLocalTime(timestamp, chatSettings.formatter)
            } else {
                ""
            }

        return ChatMessageUiState.NoticeMessageUi(
            id = id,
            tag = tag,
            timestamp = timestamp,
            lightBackgroundColor = backgroundColors.light,
            darkBackgroundColor = backgroundColors.dark,
            textAlpha = textAlpha,
            message = message,
        )
    }

    private fun UserNoticeMessage.toUserNoticeMessageUi(
        tag: Int,
        chatSettings: ChatSettings,
        isAlternateBackground: Boolean,
        textAlpha: Float,
    ): ChatMessageUiState.UserNoticeMessageUi {
        val shouldHighlight =
            highlights.any {
                it.type == HighlightType.Subscription ||
                    it.type == HighlightType.Announcement
            }
        val backgroundColors =
            when {
                shouldHighlight -> getHighlightColors(HighlightType.Subscription)
                else -> calculateCheckeredBackgroundColors(isAlternateBackground, false)
            }
        val timestamp =
            if (chatSettings.showTimestamps) {
                DateTimeUtils.timestampToLocalTime(timestamp, chatSettings.formatter)
            } else {
                ""
            }

        val displayName = tags["display-name"].orEmpty()
        val login = tags["login"]?.toUserName()
        val rawNameColor =
            tags["color"]?.ifBlank { null }?.let(android.graphics.Color::parseColor)
                ?: login?.let { usersRepository.getCachedUserColor(it) }
                ?: Message.DEFAULT_COLOR

        return ChatMessageUiState.UserNoticeMessageUi(
            id = id,
            tag = tag,
            timestamp = timestamp,
            lightBackgroundColor = backgroundColors.light,
            darkBackgroundColor = backgroundColors.dark,
            textAlpha = textAlpha,
            isHighlighted = shouldHighlight,
            message = message,
            displayName = displayName,
            rawNameColor = rawNameColor,
            shouldHighlight = shouldHighlight,
        )
    }

    private fun ModerationMessage.toModerationMessageUi(
        tag: Int,
        chatSettings: ChatSettings,
        preferenceStore: DankChatPreferenceStore,
        isAlternateBackground: Boolean,
        textAlpha: Float,
    ): ChatMessageUiState.ModerationMessageUi {
        val backgroundColors = calculateCheckeredBackgroundColors(isAlternateBackground, false)
        val timestamp =
            if (chatSettings.showTimestamps) {
                DateTimeUtils.timestampToLocalTime(timestamp, chatSettings.formatter)
            } else {
                ""
            }

        val arguments =
            buildList {
                duration?.let(::add)
                reason?.takeIf { it.isNotBlank() }?.let(::add)
                sourceBroadcasterDisplay?.toString()?.let(::add)
            }.toImmutableList()

        return ChatMessageUiState.ModerationMessageUi(
            id = id,
            tag = tag,
            timestamp = timestamp,
            lightBackgroundColor = backgroundColors.light,
            darkBackgroundColor = backgroundColors.dark,
            textAlpha = textAlpha,
            message = getSystemMessage(preferenceStore.userName, chatSettings.showTimedOutMessages),
            creatorName = creatorUserDisplay?.toString(),
            targetName = targetUserDisplay?.toString(),
            creatorColor = creatorUserDisplay?.let { usersRepository.getCachedUserColor(UserName(it.toString())) } ?: Message.DEFAULT_COLOR,
            targetColor = targetUser?.let { usersRepository.getCachedUserColor(it) } ?: Message.DEFAULT_COLOR,
            arguments = arguments,
        )
    }

    private fun AutomodMessage.toAutomodMessageUi(
        tag: Int,
        chatSettings: ChatSettings,
        textAlpha: Float,
    ): ChatMessageUiState.AutomodMessageUi {
        val timestamp =
            if (chatSettings.showTimestamps) {
                DateTimeUtils.timestampToLocalTime(timestamp, chatSettings.formatter)
            } else {
                ""
            }

        val uiStatus =
            when (status) {
                AutomodMessage.Status.Pending -> ChatMessageUiState.AutomodMessageUi.AutomodMessageStatus.Pending
                AutomodMessage.Status.Approved -> ChatMessageUiState.AutomodMessageUi.AutomodMessageStatus.Approved
                AutomodMessage.Status.Denied -> ChatMessageUiState.AutomodMessageUi.AutomodMessageStatus.Denied
                AutomodMessage.Status.Expired -> ChatMessageUiState.AutomodMessageUi.AutomodMessageStatus.Expired
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
            badges =
                badges
                    .mapIndexed { index, badge ->
                        BadgeUi(
                            url = badge.url,
                            badge = badge,
                            position = index,
                            drawableResId =
                                when (badge.badgeTag) {
                                    "automod/1" -> R.drawable.ic_automod_badge
                                    else -> null
                                },
                        )
                    }.toImmutableList(),
            userDisplayName = userName.formatWithDisplayName(userDisplayName),
            rawNameColor = color,
            messageText = messageText?.takeIf { it.isNotEmpty() },
            reason = reason,
            status = uiStatus,
            isUserSide = isUserSide,
        )
    }

    private fun PrivMessage.toPrivMessageUi(
        tag: Int,
        chatSettings: ChatSettings,
        isAlternateBackground: Boolean,
        isMentionTab: Boolean,
        isInReplies: Boolean,
        textAlpha: Float,
    ): ChatMessageUiState.PrivMessageUi {
        val backgroundColors =
            when {
                timedOut && !chatSettings.showTimedOutMessages -> BackgroundColors(Color.Transparent, Color.Transparent)
                highlights.isNotEmpty() -> highlights.toBackgroundColors()
                else -> calculateCheckeredBackgroundColors(isAlternateBackground, true)
            }

        val timestamp =
            if (chatSettings.showTimestamps) {
                DateTimeUtils.timestampToLocalTime(timestamp, chatSettings.formatter)
            } else {
                ""
            }

        val nameText =
            when {
                !chatSettings.showUsernames -> ""
                isAction -> "$aliasOrFormattedName "
                aliasOrFormattedName.isBlank() -> ""
                else -> "$aliasOrFormattedName: "
            }

        val allowedBadges = badges.filter { it.type in chatSettings.visibleBadgeTypes }
        val badgeUis =
            allowedBadges
                .mapIndexed { index, badge ->
                    BadgeUi(
                        url = badge.url,
                        badge = badge,
                        position = index,
                    )
                }.toImmutableList()

        val emoteUis =
            emotes
                .groupBy { it.position }
                .map { (position, emoteGroup) ->
                    // Check if any emote in the group is animated - we need to check the type
                    val hasAnimated =
                        emoteGroup.any { emote ->
                            when (emote.type) {
                                is ChatMessageEmoteType.TwitchEmote -> false

                                // Twitch emotes can be animated but we don't have that info here
                                is ChatMessageEmoteType.ChannelFFZEmote,
                                is ChatMessageEmoteType.GlobalFFZEmote,
                                is ChatMessageEmoteType.ChannelBTTVEmote,
                                is ChatMessageEmoteType.GlobalBTTVEmote,
                                -> true

                                // Assume third-party can be animated
                                is ChatMessageEmoteType.ChannelSevenTVEmote,
                                is ChatMessageEmoteType.GlobalSevenTVEmote,
                                -> true

                                is ChatMessageEmoteType.Cheermote -> true
                            }
                        }

                    val firstEmote = emoteGroup.first()
                    EmoteUi(
                        code = firstEmote.code,
                        urls = emoteGroup.map { it.url }.toImmutableList(),
                        position = position,
                        isAnimated = hasAnimated,
                        isTwitch = emoteGroup.any { it.isTwitch },
                        scale = firstEmote.scale,
                        emotes = emoteGroup.toImmutableList(),
                        cheerAmount = firstEmote.cheerAmount,
                        cheerColor = firstEmote.cheerColor?.let { Color(it) },
                    )
                }.toImmutableList()

        val threadUi =
            if (thread != null && !isInReplies) {
                thread.toThreadUi()
            } else {
                null
            }

        val highlightHeader =
            highlights.highestPriorityHighlight()?.let {
                when (it.type) {
                    HighlightType.FirstMessage -> TextResource.Res(R.string.highlight_header_first_time_chat)
                    HighlightType.ElevatedMessage -> TextResource.Res(R.string.highlight_header_elevated_chat)
                    else -> null
                }
            }

        val fullMessage =
            buildString {
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
            isHighlighted = highlights.isNotEmpty(),
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
            highlightHeader = highlightHeader,
            fullMessage = fullMessage,
        )
    }

    private fun PointRedemptionMessage.toPointRedemptionMessageUi(
        tag: Int,
        chatSettings: ChatSettings,
        textAlpha: Float,
    ): ChatMessageUiState.PointRedemptionMessageUi {
        val backgroundColors = getHighlightColors(HighlightType.ChannelPointRedemption)
        val timestamp =
            if (chatSettings.showTimestamps) {
                DateTimeUtils.timestampToLocalTime(timestamp, chatSettings.formatter)
            } else {
                ""
            }

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
            requiresUserInput = requiresUserInput,
        )
    }

    private fun WhisperMessage.toWhisperMessageUi(
        tag: Int,
        chatSettings: ChatSettings,
        isAlternateBackground: Boolean,
        textAlpha: Float,
        currentUserName: UserName?,
    ): ChatMessageUiState.WhisperMessageUi {
        val backgroundColors = calculateCheckeredBackgroundColors(isAlternateBackground, true)
        val timestamp =
            if (chatSettings.showTimestamps) {
                DateTimeUtils.timestampToLocalTime(timestamp, chatSettings.formatter)
            } else {
                ""
            }

        val allowedBadges = badges.filter { it.type in chatSettings.visibleBadgeTypes }
        val badgeUis =
            allowedBadges
                .mapIndexed { index, badge ->
                    BadgeUi(
                        url = badge.url,
                        badge = badge,
                        position = index,
                    )
                }.toImmutableList()

        val emoteUis =
            emotes
                .groupBy { it.position }
                .map { (position, emoteGroup) ->
                    // Check if any emote in the group is animated
                    val hasAnimated =
                        emoteGroup.any { emote ->
                            when (emote.type) {
                                is ChatMessageEmoteType.TwitchEmote -> false

                                is ChatMessageEmoteType.ChannelFFZEmote,
                                is ChatMessageEmoteType.GlobalFFZEmote,
                                is ChatMessageEmoteType.ChannelBTTVEmote,
                                is ChatMessageEmoteType.GlobalBTTVEmote,
                                -> true

                                is ChatMessageEmoteType.ChannelSevenTVEmote,
                                is ChatMessageEmoteType.GlobalSevenTVEmote,
                                -> true

                                is ChatMessageEmoteType.Cheermote -> true
                            }
                        }

                    val firstEmote = emoteGroup.first()
                    EmoteUi(
                        code = firstEmote.code,
                        urls = emoteGroup.map { it.url }.toImmutableList(),
                        position = position,
                        isAnimated = hasAnimated,
                        isTwitch = emoteGroup.any { it.isTwitch },
                        scale = firstEmote.scale,
                        emotes = emoteGroup.toImmutableList(),
                        cheerAmount = firstEmote.cheerAmount,
                        cheerColor = firstEmote.cheerColor?.let { Color(it) },
                    )
                }.toImmutableList()

        val fullMessage =
            buildString {
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
            replyTargetName = if (currentUserName != null && name.value.equals(currentUserName.value, ignoreCase = true)) recipientName else name,
        )
    }

    data class BackgroundColors(
        val light: Color,
        val dark: Color,
    )

    private fun calculateCheckeredBackgroundColors(
        isAlternateBackground: Boolean,
        enableCheckered: Boolean,
    ): BackgroundColors =
        if (enableCheckered && isAlternateBackground) {
            BackgroundColors(CHECKERED_LIGHT, CHECKERED_DARK)
        } else {
            BackgroundColors(Color.Transparent, Color.Transparent)
        }

    private fun getHighlightColors(type: HighlightType): BackgroundColors =
        when (type) {
            HighlightType.Subscription,
            HighlightType.Announcement,
            -> {
                BackgroundColors(
                    light = COLOR_SUB_HIGHLIGHT_LIGHT,
                    dark = COLOR_SUB_HIGHLIGHT_DARK,
                )
            }

            HighlightType.ChannelPointRedemption -> {
                BackgroundColors(
                    light = COLOR_REDEMPTION_HIGHLIGHT_LIGHT,
                    dark = COLOR_REDEMPTION_HIGHLIGHT_DARK,
                )
            }

            HighlightType.ElevatedMessage -> {
                BackgroundColors(
                    light = COLOR_ELEVATED_MESSAGE_HIGHLIGHT_LIGHT,
                    dark = COLOR_ELEVATED_MESSAGE_HIGHLIGHT_DARK,
                )
            }

            HighlightType.FirstMessage -> {
                BackgroundColors(
                    light = COLOR_FIRST_MESSAGE_HIGHLIGHT_LIGHT,
                    dark = COLOR_FIRST_MESSAGE_HIGHLIGHT_DARK,
                )
            }

            HighlightType.Username,
            HighlightType.Custom,
            HighlightType.Reply,
            HighlightType.Badge,
            HighlightType.Notification,
            -> {
                BackgroundColors(
                    light = COLOR_MENTION_HIGHLIGHT_LIGHT,
                    dark = COLOR_MENTION_HIGHLIGHT_DARK,
                )
            }
        }

    private fun Set<Highlight>.toBackgroundColors(): BackgroundColors {
        val highlight =
            this.maxByOrNull { it.type.priority.value }
                ?: return BackgroundColors(Color.Transparent, Color.Transparent)

        val customColor = highlight.customColor
        if (customColor != null && customColor !in DEFAULT_HIGHLIGHT_COLOR_INTS) {
            val color = Color(customColor)
            return BackgroundColors(color, color)
        }

        return getHighlightColors(highlight.type)
    }

    companion object {
        // Highlight colors - Light theme (all dark enough for white text)
        private val COLOR_SUB_HIGHLIGHT_LIGHT = Color(0xFF7E57C2)
        private val COLOR_MENTION_HIGHLIGHT_LIGHT = Color(0xFFCF5050)
        private val COLOR_REDEMPTION_HIGHLIGHT_LIGHT = Color(0xFF458B93)
        private val COLOR_FIRST_MESSAGE_HIGHLIGHT_LIGHT = Color(0xFF558B2F)
        private val COLOR_ELEVATED_MESSAGE_HIGHLIGHT_LIGHT = Color(0xFFB08D2A)

        // Highlight colors - Dark theme
        private val COLOR_SUB_HIGHLIGHT_DARK = Color(0xFF6A45A0)
        private val COLOR_MENTION_HIGHLIGHT_DARK = Color(0xFF8C3A3B)
        private val COLOR_REDEMPTION_HIGHLIGHT_DARK = Color(0xFF00606B)
        private val COLOR_FIRST_MESSAGE_HIGHLIGHT_DARK = Color(0xFF3A6600)
        private val COLOR_ELEVATED_MESSAGE_HIGHLIGHT_DARK = Color(0xFF6B5800)

        fun defaultHighlightColorInt(
            type: HighlightType,
            isDark: Boolean,
        ): Int =
            when (type) {
                HighlightType.Subscription, HighlightType.Announcement -> if (isDark) 0xFF6A45A0 else 0xFF7E57C2
                HighlightType.Username, HighlightType.Custom, HighlightType.Reply, HighlightType.Notification, HighlightType.Badge -> if (isDark) 0xFF8C3A3B else 0xFFCF5050
                HighlightType.ChannelPointRedemption -> if (isDark) 0xFF00606B else 0xFF458B93
                HighlightType.FirstMessage -> if (isDark) 0xFF3A6600 else 0xFF558B2F
                HighlightType.ElevatedMessage -> if (isDark) 0xFF6B5800 else 0xFFB08D2A
            }.toInt()

        private val DEFAULT_HIGHLIGHT_COLOR_INTS =
            setOf(
                // Current defaults
                0xFF7E57C2.toInt(), // sub light
                0xFF6A45A0.toInt(), // sub dark
                0xFFCF5050.toInt(), // mention light
                0xFF8C3A3B.toInt(), // mention dark
                0xFF458B93.toInt(), // redemption light
                0xFF00606B.toInt(), // redemption dark
                0xFF558B2F.toInt(), // first message light
                0xFF3A6600.toInt(), // first message dark
                0xFFB08D2A.toInt(), // elevated light
                0xFF6B5800.toInt(), // elevated dark
                // Legacy defaults
                0xFFD1C4E9.toInt(),
                0xFF543589.toInt(), // sub (v1)
                0xFFEF9A9A.toInt(),
                0xFF773031.toInt(), // mention (v1)
                0xFF93F1FF.toInt(),
                0xFF004F57.toInt(), // redemption (v1)
                0xFFC2F18D.toInt(),
                0xFF2D5000.toInt(), // first message (v1)
                0xFFFFE087.toInt(),
                0xFF574500.toInt(), // elevated (v1)
                0xFFB5A0D4.toInt(),
                0xFFE57373.toInt(), // sub/mention (v2 light)
                0xFFA8D8DF.toInt(),
                0xFFAED581.toInt(),
                0xFFEDD59A.toInt(), // redemption/first/elevated (v2 light)
            )

        // Checkered background colors
        private val CHECKERED_LIGHT =
            Color(
                android.graphics.Color.argb(
                    (255 * MaterialColors.ALPHA_DISABLED_LOW).toInt(),
                    0,
                    0,
                    0,
                ),
            )
        private val CHECKERED_DARK =
            Color(
                android.graphics.Color.argb(
                    (255 * MaterialColors.ALPHA_DISABLED_LOW).toInt(),
                    255,
                    255,
                    255,
                ),
            )
    }
}
