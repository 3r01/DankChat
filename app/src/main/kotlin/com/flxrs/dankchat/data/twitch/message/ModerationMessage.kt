package com.flxrs.dankchat.data.twitch.message

import com.flxrs.dankchat.R
import com.flxrs.dankchat.data.DisplayName
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.api.eventapi.dto.messages.notification.ChannelModerateAction
import com.flxrs.dankchat.data.api.eventapi.dto.messages.notification.ChannelModerateDto
import com.flxrs.dankchat.data.irc.IrcMessage
import com.flxrs.dankchat.data.toDisplayName
import com.flxrs.dankchat.data.toUserName
import com.flxrs.dankchat.data.twitch.pubsub.dto.moderation.ModerationActionData
import com.flxrs.dankchat.data.twitch.pubsub.dto.moderation.ModerationActionType
import com.flxrs.dankchat.utils.DateTimeUtils
import com.flxrs.dankchat.utils.TextResource
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import java.util.UUID
import kotlin.time.Instant

data class ModerationMessage(
    override val timestamp: Long = System.currentTimeMillis(),
    override val id: String = UUID.randomUUID().toString(),
    override val highlights: Set<Highlight> = emptySet(),
    val channel: UserName,
    val action: Action,
    val creatorUserDisplay: DisplayName? = null,
    val targetUser: UserName? = null,
    val targetUserDisplay: DisplayName? = null,
    val sourceBroadcasterDisplay: DisplayName? = null,
    val targetMsgId: String? = null,
    val durationInt: Int? = null,
    val duration: String? = null,
    val reason: String? = null,
    val fromEventSource: Boolean = false,
    val stackCount: Int = 0,
) : Message() {
    enum class Action {
        Timeout,
        Untimeout,
        Ban,
        Unban,
        Mod,
        Unmod,
        Clear,
        Delete,
        Vip,
        Unvip,
        Warn,
        Raid,
        Unraid,
        EmoteOnly,
        EmoteOnlyOff,
        Followers,
        FollowersOff,
        UniqueChat,
        UniqueChatOff,
        Slow,
        SlowOff,
        Subscribers,
        SubscribersOff,
        SharedBan,
        SharedUnban,
        SharedTimeout,
        SharedUntimeout,
        SharedDelete,
        AddBlockedTerm,
        AddPermittedTerm,
        RemoveBlockedTerm,
        RemovePermittedTerm,
    }

    private val hasReason get() = !reason.isNullOrBlank()
    private val quotedTermsOrBlank get() = reason.takeUnless { it.isNullOrBlank() } ?: "terms"

    private fun trimmedMessage(showDeletedMessage: Boolean): String? {
        if (!showDeletedMessage) return null
        val fullReason = reason.orEmpty()
        return when {
            fullReason.length > 50 -> "${fullReason.take(50)}…"
            else                   -> fullReason
        }.takeIf { it.isNotEmpty() }
    }

    private fun countSuffix(): TextResource {
        return when {
            stackCount > 1 -> TextResource.PluralRes(R.plurals.mod_count_suffix, stackCount, persistentListOf(stackCount))
            else           -> TextResource.Plain("")
        }
    }

    private fun formatMinutesDuration(minutes: Int): TextResource {
        val parts = DateTimeUtils.decomposeMinutes(minutes).map { it.toTextResource() }
        return joinDurationParts(parts, fallback = { TextResource.PluralRes(R.plurals.duration_minutes, 0, persistentListOf(0)) })
    }

    private fun formatSecondsDuration(seconds: Int): TextResource {
        val parts = DateTimeUtils.decomposeSeconds(seconds).map { it.toTextResource() }
        return joinDurationParts(parts, fallback = { TextResource.PluralRes(R.plurals.duration_seconds, 0, persistentListOf(0)) })
    }

    private fun DateTimeUtils.DurationPart.toTextResource(): TextResource {
        val pluralRes = when (unit) {
            DateTimeUtils.DurationUnit.WEEKS   -> R.plurals.duration_weeks
            DateTimeUtils.DurationUnit.DAYS    -> R.plurals.duration_days
            DateTimeUtils.DurationUnit.HOURS   -> R.plurals.duration_hours
            DateTimeUtils.DurationUnit.MINUTES -> R.plurals.duration_minutes
            DateTimeUtils.DurationUnit.SECONDS -> R.plurals.duration_seconds
        }
        return TextResource.PluralRes(pluralRes, value, persistentListOf(value))
    }

    private fun joinDurationParts(parts: List<TextResource>, fallback: () -> TextResource): TextResource = when (parts.size) {
        0    -> fallback()
        1    -> parts[0]
        2    -> TextResource.Res(R.string.duration_join_2, persistentListOf(parts[0], parts[1]))
        else -> TextResource.Res(R.string.duration_join_3, persistentListOf(parts[0], parts[1], parts[2]))
    }

    fun getSystemMessage(currentUser: UserName?, showDeletedMessage: Boolean): TextResource {
        val creator = creatorUserDisplay.toString()
        val target = targetUserDisplay.toString()
        val dur = duration.orEmpty()
        val source = sourceBroadcasterDisplay.toString()

        val message = when (action) {
            Action.Timeout             -> when (targetUser) {
                currentUser -> when (creatorUserDisplay) {
                    null -> TextResource.Res(R.string.mod_timeout_self_irc, persistentListOf(dur))
                    else -> when {
                        hasReason -> TextResource.Res(R.string.mod_timeout_self_reason, persistentListOf(dur, creator, reason.orEmpty()))
                        else      -> TextResource.Res(R.string.mod_timeout_self, persistentListOf(dur, creator))
                    }
                }

                else        -> when (creatorUserDisplay) {
                    null -> TextResource.Res(R.string.mod_timeout_no_creator, persistentListOf(target, dur))
                    else -> when {
                        hasReason -> TextResource.Res(R.string.mod_timeout_by_creator_reason, persistentListOf(creator, target, dur, reason.orEmpty()))
                        else      -> TextResource.Res(R.string.mod_timeout_by_creator, persistentListOf(creator, target, dur))
                    }
                }
            }

            Action.Untimeout           -> TextResource.Res(R.string.mod_untimeout, persistentListOf(creator, target))

            Action.Ban                 -> when (targetUser) {
                currentUser -> when (creatorUserDisplay) {
                    null -> TextResource.Res(R.string.mod_ban_self_irc)
                    else -> when {
                        hasReason -> TextResource.Res(R.string.mod_ban_self_reason, persistentListOf(creator, reason.orEmpty()))
                        else      -> TextResource.Res(R.string.mod_ban_self, persistentListOf(creator))
                    }
                }

                else        -> when (creatorUserDisplay) {
                    null -> TextResource.Res(R.string.mod_ban_no_creator, persistentListOf(target))
                    else -> when {
                        hasReason -> TextResource.Res(R.string.mod_ban_by_creator_reason, persistentListOf(creator, target, reason.orEmpty()))
                        else      -> TextResource.Res(R.string.mod_ban_by_creator, persistentListOf(creator, target))
                    }
                }
            }

            Action.Unban               -> TextResource.Res(R.string.mod_unban, persistentListOf(creator, target))
            Action.Mod                 -> TextResource.Res(R.string.mod_modded, persistentListOf(creator, target))
            Action.Unmod               -> TextResource.Res(R.string.mod_unmodded, persistentListOf(creator, target))

            Action.Delete              -> {
                val msg = trimmedMessage(showDeletedMessage)
                when (creatorUserDisplay) {
                    null -> when (msg) {
                        null -> TextResource.Res(R.string.mod_delete_no_creator, persistentListOf(target))
                        else -> TextResource.Res(R.string.mod_delete_no_creator_message, persistentListOf(target, msg))
                    }

                    else -> when (msg) {
                        null -> TextResource.Res(R.string.mod_delete_by_creator, persistentListOf(creator, target))
                        else -> TextResource.Res(R.string.mod_delete_by_creator_message, persistentListOf(creator, target, msg))
                    }
                }
            }

            Action.Clear               -> when (creatorUserDisplay) {
                null -> TextResource.Res(R.string.mod_clear_no_creator)
                else -> TextResource.Res(R.string.mod_clear_by_creator, persistentListOf(creator))
            }

            Action.Vip                 -> TextResource.Res(R.string.mod_vip_added, persistentListOf(creator, target))
            Action.Unvip               -> TextResource.Res(R.string.mod_vip_removed, persistentListOf(creator, target))

            Action.Warn                -> when {
                hasReason -> TextResource.Res(R.string.mod_warn_reason, persistentListOf(creator, target, reason.orEmpty()))
                else      -> TextResource.Res(R.string.mod_warn, persistentListOf(creator, target))
            }

            Action.Raid                -> TextResource.Res(R.string.mod_raid, persistentListOf(creator, target))
            Action.Unraid              -> TextResource.Res(R.string.mod_unraid, persistentListOf(creator, target))
            Action.EmoteOnly           -> TextResource.Res(R.string.mod_emote_only_on, persistentListOf(creator))
            Action.EmoteOnlyOff        -> TextResource.Res(R.string.mod_emote_only_off, persistentListOf(creator))

            Action.Followers           -> when (val mins = durationInt?.takeIf { it > 0 }) {
                null -> TextResource.Res(R.string.mod_followers_on, persistentListOf(creator))
                else -> TextResource.Res(R.string.mod_followers_on_duration, persistentListOf(creator, formatMinutesDuration(mins)))
            }

            Action.FollowersOff        -> TextResource.Res(R.string.mod_followers_off, persistentListOf(creator))
            Action.UniqueChat          -> TextResource.Res(R.string.mod_unique_chat_on, persistentListOf(creator))
            Action.UniqueChatOff       -> TextResource.Res(R.string.mod_unique_chat_off, persistentListOf(creator))

            Action.Slow                -> when (val secs = durationInt) {
                null -> TextResource.Res(R.string.mod_slow_on, persistentListOf(creator))
                else -> TextResource.Res(R.string.mod_slow_on_duration, persistentListOf(creator, formatSecondsDuration(secs)))
            }

            Action.SlowOff             -> TextResource.Res(R.string.mod_slow_off, persistentListOf(creator))
            Action.Subscribers         -> TextResource.Res(R.string.mod_subscribers_on, persistentListOf(creator))
            Action.SubscribersOff      -> TextResource.Res(R.string.mod_subscribers_off, persistentListOf(creator))

            Action.SharedTimeout       -> when {
                hasReason -> TextResource.Res(R.string.mod_shared_timeout_reason, persistentListOf(creator, target, dur, source, reason.orEmpty()))
                else      -> TextResource.Res(R.string.mod_shared_timeout, persistentListOf(creator, target, dur, source))
            }

            Action.SharedUntimeout     -> TextResource.Res(R.string.mod_shared_untimeout, persistentListOf(creator, target, source))

            Action.SharedBan           -> when {
                hasReason -> TextResource.Res(R.string.mod_shared_ban_reason, persistentListOf(creator, target, source, reason.orEmpty()))
                else      -> TextResource.Res(R.string.mod_shared_ban, persistentListOf(creator, target, source))
            }

            Action.SharedUnban         -> TextResource.Res(R.string.mod_shared_unban, persistentListOf(creator, target, source))

            Action.SharedDelete        -> {
                when (val msg = trimmedMessage(showDeletedMessage)) {
                    null -> TextResource.Res(R.string.mod_shared_delete, persistentListOf(creator, target, source))
                    else -> TextResource.Res(R.string.mod_shared_delete_message, persistentListOf(creator, target, source, msg))
                }
            }

            Action.AddBlockedTerm      -> TextResource.Res(R.string.automod_moderation_added_blocked_term, persistentListOf(creator, quotedTermsOrBlank))
            Action.AddPermittedTerm    -> TextResource.Res(R.string.automod_moderation_added_permitted_term, persistentListOf(creator, quotedTermsOrBlank))
            Action.RemoveBlockedTerm   -> TextResource.Res(R.string.automod_moderation_removed_blocked_term, persistentListOf(creator, quotedTermsOrBlank))
            Action.RemovePermittedTerm -> TextResource.Res(R.string.automod_moderation_removed_permitted_term, persistentListOf(creator, quotedTermsOrBlank))
        }

        return when (val count = countSuffix()) {
            is TextResource.Plain -> message
            else                  -> TextResource.Res(R.string.mod_message_with_count, persistentListOf(message, count))
        }
    }

    val canClearMessages: Boolean = action in listOf(Action.Clear, Action.Ban, Action.Timeout, Action.SharedTimeout, Action.SharedBan)
    val canStack: Boolean = canClearMessages && action != Action.Clear

    companion object {
        fun parseClearChat(message: IrcMessage): ModerationMessage = with(message) {
            val channel = params[0].substring(1)
            val target = params.getOrNull(1)
            val durationSeconds = tags["ban-duration"]?.toIntOrNull()
            val duration = durationSeconds?.let { DateTimeUtils.formatSeconds(it) }
            val ts = tags["tmi-sent-ts"]?.toLongOrNull() ?: System.currentTimeMillis()
            val id = tags["id"] ?: "clearchat-$ts-$channel-${target ?: "all"}"
            val action = when {
                target == null          -> Action.Clear
                durationSeconds == null -> Action.Ban
                else                    -> Action.Timeout
            }

            return ModerationMessage(
                timestamp = ts,
                id = id,
                channel = channel.toUserName(),
                action = action,
                targetUserDisplay = target?.toDisplayName(),
                targetUser = target?.toUserName(),
                durationInt = durationSeconds,
                duration = duration,
                stackCount = if (target != null && duration != null) 1 else 0,
                fromEventSource = false,
            )
        }

        fun parseClearMessage(message: IrcMessage): ModerationMessage = with(message) {
            val channel = params[0].substring(1)
            val target = tags["login"]
            val targetMsgId = tags["target-msg-id"]
            val reason = params.getOrNull(1)
            val ts = tags["tmi-sent-ts"]?.toLongOrNull() ?: System.currentTimeMillis()
            val id = tags["id"] ?: "clearmsg-$ts-$channel-${target ?: ""}"

            return ModerationMessage(
                timestamp = ts,
                id = id,
                channel = channel.toUserName(),
                action = Action.Delete,
                targetUserDisplay = target?.toDisplayName(),
                targetUser = target?.toUserName(),
                targetMsgId = targetMsgId,
                reason = reason,
                fromEventSource = false,
            )
        }

        fun parseModerationAction(timestamp: Instant, channel: UserName, data: ModerationActionData): ModerationMessage {
            val seconds = data.args?.getOrNull(1)?.toIntOrNull()
            val duration = parseDuration(seconds, data)
            val targetUser = parseTargetUser(data)
            val targetMsgId = parseTargetMsgId(data)
            val reason = parseReason(data)
            val timeZone = TimeZone.currentSystemDefault()

            return ModerationMessage(
                timestamp = timestamp.toLocalDateTime(timeZone).toInstant(timeZone).toEpochMilliseconds(),
                id = data.msgId ?: UUID.randomUUID().toString(),
                channel = channel,
                action = data.moderationAction.toAction(),
                creatorUserDisplay = data.creator?.toDisplayName(),
                targetUser = targetUser,
                targetUserDisplay = targetUser?.toDisplayName(),
                targetMsgId = targetMsgId,
                durationInt = seconds,
                duration = duration,
                reason = reason,
                stackCount = if (data.targetUserName != null && duration != null) 1 else 0,
                fromEventSource = true,
            )
        }

        fun parseModerationAction(id: String, timestamp: Instant, channel: UserName, data: ChannelModerateDto): ModerationMessage {
            val timeZone = TimeZone.currentSystemDefault()
            val timestampMillis = timestamp.toLocalDateTime(timeZone).toInstant(timeZone).toEpochMilliseconds()
            val duration = parseDuration(timestamp, data)
            val formattedDuration = duration?.let { DateTimeUtils.formatSeconds(it) }
            val userPair = parseTargetUser(data)
            val targetMsgId = parseTargetMsgId(data)
            val reason = parseReason(data)

            return ModerationMessage(
                timestamp = timestampMillis,
                id = id,
                channel = channel,
                action = data.action.toAction(),
                creatorUserDisplay = data.moderatorUserName,
                sourceBroadcasterDisplay = data.sourceBroadcasterUserName,
                targetUser = userPair?.first,
                targetUserDisplay = userPair?.second,
                targetMsgId = targetMsgId,
                durationInt = duration,
                duration = formattedDuration,
                reason = reason,
                fromEventSource = true,
            )
        }

        private fun parseDuration(seconds: Int?, data: ModerationActionData): String? = when (data.moderationAction) {
            ModerationActionType.Timeout -> seconds?.let { DateTimeUtils.formatSeconds(seconds) }
            else                         -> null
        }

        private fun parseDuration(timestamp: Instant, data: ChannelModerateDto): Int? = when (data.action) {
            ChannelModerateAction.Timeout           -> data.timeout?.let { it.expiresAt.epochSeconds - timestamp.epochSeconds }?.toInt()
            ChannelModerateAction.SharedChatTimeout -> data.sharedChatTimeout?.let { it.expiresAt.epochSeconds - timestamp.epochSeconds }?.toInt()
            ChannelModerateAction.Followers         -> data.followers?.followDurationMinutes
            ChannelModerateAction.Slow              -> data.slow?.waitTimeSeconds
            else                                    -> null
        }

        private fun parseReason(data: ModerationActionData): String? = when (data.moderationAction) {
            ModerationActionType.Ban,
            ModerationActionType.Delete  -> data.args?.getOrNull(1)

            ModerationActionType.Timeout -> data.args?.getOrNull(2)
            else                         -> null
        }

        private fun parseReason(data: ChannelModerateDto): String? = when (data.action) {
            ChannelModerateAction.Ban                 -> data.ban?.reason
            ChannelModerateAction.Delete              -> data.delete?.messageBody
            ChannelModerateAction.Timeout             -> data.timeout?.reason
            ChannelModerateAction.SharedChatBan       -> data.sharedChatBan?.reason
            ChannelModerateAction.SharedChatDelete    -> data.sharedChatDelete?.messageBody
            ChannelModerateAction.SharedChatTimeout   -> data.sharedChatTimeout?.reason
            ChannelModerateAction.Warn                -> data.warn?.let { listOfNotNull(it.reason).plus(it.chatRulesCited.orEmpty()).joinToString() }
            ChannelModerateAction.AddBlockedTerm,
            ChannelModerateAction.AddPermittedTerm,
            ChannelModerateAction.RemoveBlockedTerm,
            ChannelModerateAction.RemovePermittedTerm -> data.automodTerms?.terms?.joinToString(" and ") { "\"$it\"" }

            else                                      -> null
        }

        private fun parseTargetUser(data: ModerationActionData): UserName? = when (data.moderationAction) {
            ModerationActionType.Delete -> data.args?.getOrNull(0)?.toUserName()
            else                        -> data.targetUserName
        }

        private fun parseTargetUser(data: ChannelModerateDto): Pair<UserName, DisplayName>? = when (data.action) {
            ChannelModerateAction.Timeout             -> data.timeout?.let { it.userLogin to it.userName }
            ChannelModerateAction.Untimeout           -> data.untimeout?.let { it.userLogin to it.userName }
            ChannelModerateAction.Ban                 -> data.ban?.let { it.userLogin to it.userName }
            ChannelModerateAction.Unban               -> data.unban?.let { it.userLogin to it.userName }
            ChannelModerateAction.Mod                 -> data.mod?.let { it.userLogin to it.userName }
            ChannelModerateAction.Unmod               -> data.unmod?.let { it.userLogin to it.userName }
            ChannelModerateAction.Delete              -> data.delete?.let { it.userLogin to it.userName }
            ChannelModerateAction.Vip                 -> data.vip?.let { it.userLogin to it.userName }
            ChannelModerateAction.Unvip               -> data.unvip?.let { it.userLogin to it.userName }
            ChannelModerateAction.Warn                -> data.warn?.let { it.userLogin to it.userName }
            ChannelModerateAction.Raid                -> data.raid?.let { it.userLogin to it.userName }
            ChannelModerateAction.Unraid              -> data.unraid?.let { it.userLogin to it.userName }
            ChannelModerateAction.SharedChatTimeout   -> data.sharedChatTimeout?.let { it.userLogin to it.userName }
            ChannelModerateAction.SharedChatUntimeout -> data.sharedChatUntimeout?.let { it.userLogin to it.userName }
            ChannelModerateAction.SharedChatBan       -> data.sharedChatBan?.let { it.userLogin to it.userName }
            ChannelModerateAction.SharedChatUnban     -> data.sharedChatUnban?.let { it.userLogin to it.userName }
            ChannelModerateAction.SharedChatDelete    -> data.sharedChatDelete?.let { it.userLogin to it.userName }
            else                                      -> null
        }

        private fun parseTargetMsgId(data: ModerationActionData): String? = when (data.moderationAction) {
            ModerationActionType.Delete -> data.args?.getOrNull(2)
            else                        -> null
        }

        private fun parseTargetMsgId(data: ChannelModerateDto): String? = when (data.action) {
            ChannelModerateAction.Delete           -> data.delete?.messageId
            ChannelModerateAction.SharedChatDelete -> data.sharedChatDelete?.messageId
            else                                   -> null
        }

        private fun ModerationActionType.toAction() = when (this) {
            ModerationActionType.Timeout   -> Action.Timeout
            ModerationActionType.Untimeout -> Action.Untimeout
            ModerationActionType.Ban       -> Action.Ban
            ModerationActionType.Unban     -> Action.Unban
            ModerationActionType.Mod       -> Action.Mod
            ModerationActionType.Unmod     -> Action.Unmod
            ModerationActionType.Clear     -> Action.Clear
            ModerationActionType.Delete    -> Action.Delete
        }

        private fun ChannelModerateAction.toAction() = when (this) {
            ChannelModerateAction.Timeout             -> Action.Timeout
            ChannelModerateAction.Untimeout           -> Action.Untimeout
            ChannelModerateAction.Ban                 -> Action.Ban
            ChannelModerateAction.Unban               -> Action.Unban
            ChannelModerateAction.Mod                 -> Action.Mod
            ChannelModerateAction.Unmod               -> Action.Unmod
            ChannelModerateAction.Clear               -> Action.Clear
            ChannelModerateAction.Delete              -> Action.Delete
            ChannelModerateAction.Vip                 -> Action.Vip
            ChannelModerateAction.Unvip               -> Action.Unvip
            ChannelModerateAction.Warn                -> Action.Warn
            ChannelModerateAction.Raid                -> Action.Raid
            ChannelModerateAction.Unraid              -> Action.Unraid
            ChannelModerateAction.EmoteOnly           -> Action.EmoteOnly
            ChannelModerateAction.EmoteOnlyOff        -> Action.EmoteOnlyOff
            ChannelModerateAction.Followers           -> Action.Followers
            ChannelModerateAction.FollowersOff        -> Action.FollowersOff
            ChannelModerateAction.UniqueChat          -> Action.UniqueChat
            ChannelModerateAction.UniqueChatOff       -> Action.UniqueChatOff
            ChannelModerateAction.Slow                -> Action.Slow
            ChannelModerateAction.SlowOff             -> Action.SlowOff
            ChannelModerateAction.Subscribers         -> Action.Subscribers
            ChannelModerateAction.SubscribersOff      -> Action.SubscribersOff
            ChannelModerateAction.SharedChatTimeout   -> Action.SharedTimeout
            ChannelModerateAction.SharedChatUntimeout -> Action.SharedUntimeout
            ChannelModerateAction.SharedChatBan       -> Action.SharedBan
            ChannelModerateAction.SharedChatUnban     -> Action.SharedUnban
            ChannelModerateAction.SharedChatDelete    -> Action.SharedDelete
            ChannelModerateAction.AddBlockedTerm      -> Action.AddBlockedTerm
            ChannelModerateAction.AddPermittedTerm    -> Action.AddPermittedTerm
            ChannelModerateAction.RemoveBlockedTerm   -> Action.RemoveBlockedTerm
            ChannelModerateAction.RemovePermittedTerm -> Action.RemovePermittedTerm
            else                                      -> error("Unexpected moderation action $this")
        }
    }
}
