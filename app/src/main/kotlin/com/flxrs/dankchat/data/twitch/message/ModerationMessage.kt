package com.flxrs.dankchat.data.twitch.message

import com.flxrs.dankchat.R
import com.flxrs.dankchat.chat.compose.TextResource
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
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import java.util.UUID

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

    private val durationSuffix: TextResource
        get() = duration?.let { TextResource.Res(R.string.mod_duration_suffix, listOf(it)) } ?: TextResource.Plain("")
    private val creatorSuffix: TextResource
        get() = creatorUserDisplay?.let { TextResource.Res(R.string.mod_by_creator_suffix, listOf(it.toString())) } ?: TextResource.Plain("")
    private val quotedReasonSuffix: TextResource
        get() = reason.takeUnless { it.isNullOrBlank() }?.let { TextResource.Res(R.string.mod_reason_suffix, listOf(it)) } ?: TextResource.Plain("")
    private val reasonsSuffix: TextResource
        get() = reason.takeUnless { it.isNullOrBlank() }?.let { TextResource.Res(R.string.mod_reasons_suffix, listOf(it)) } ?: TextResource.Plain("")
    private val quotedTermsOrBlank get() = reason.takeUnless { it.isNullOrBlank() } ?: "terms"

    private fun sayingSuffix(showDeletedMessage: Boolean): TextResource {
        if (!showDeletedMessage) return TextResource.Plain("")

        val fullReason = reason.orEmpty()
        val trimmed = when {
            fullReason.length > 50 -> "${fullReason.take(50)}…"
            else                   -> fullReason
        }
        return TextResource.Res(R.string.mod_saying_suffix, listOf(trimmed))
    }

    private fun countSuffix(): TextResource {
        return when {
            stackCount > 1 -> TextResource.PluralRes(R.plurals.mod_count_suffix, stackCount, listOf(stackCount))
            else           -> TextResource.Plain("")
        }
    }

    private fun minutesSuffix(): TextResource {
        return durationInt?.takeIf { it > 0 }?.let { TextResource.PluralRes(R.plurals.mod_minutes_suffix, it, listOf(it)) } ?: TextResource.Plain("")
    }

    private fun secondsSuffix(): TextResource {
        return durationInt?.let { TextResource.PluralRes(R.plurals.mod_seconds_suffix, it, listOf(it)) } ?: TextResource.Plain("")
    }

    fun getSystemMessage(currentUser: UserName?, showDeletedMessage: Boolean): TextResource {
        return when (action) {
            Action.Timeout         -> when (targetUser) {
                currentUser -> TextResource.Res(R.string.mod_timeout_self, listOf(durationSuffix, creatorSuffix, quotedReasonSuffix, countSuffix()))
                else        -> when (creatorUserDisplay) {
                    null -> TextResource.Res(R.string.mod_timeout_no_creator, listOf(targetUserDisplay.toString(), durationSuffix, countSuffix()))
                    else -> TextResource.Res(R.string.mod_timeout_by_creator, listOf(creatorUserDisplay.toString(), targetUserDisplay.toString(), durationSuffix, countSuffix()))
                }
            }

            Action.Untimeout       -> TextResource.Res(R.string.mod_untimeout, listOf(creatorUserDisplay.toString(), targetUserDisplay.toString()))
            Action.Ban             -> when (targetUser) {
                currentUser -> TextResource.Res(R.string.mod_ban_self, listOf(creatorSuffix, quotedReasonSuffix))
                else        -> when (creatorUserDisplay) {
                    null -> TextResource.Res(R.string.mod_ban_no_creator, listOf(targetUserDisplay.toString()))
                    else -> TextResource.Res(R.string.mod_ban_by_creator, listOf(creatorUserDisplay.toString(), targetUserDisplay.toString(), quotedReasonSuffix))
                }
            }

            Action.Unban           -> TextResource.Res(R.string.mod_unban, listOf(creatorUserDisplay.toString(), targetUserDisplay.toString()))
            Action.Mod             -> TextResource.Res(R.string.mod_modded, listOf(creatorUserDisplay.toString(), targetUserDisplay.toString()))
            Action.Unmod           -> TextResource.Res(R.string.mod_unmodded, listOf(creatorUserDisplay.toString(), targetUserDisplay.toString()))
            Action.Delete          -> when (creatorUserDisplay) {
                null -> TextResource.Res(R.string.mod_delete_no_creator, listOf(targetUserDisplay.toString(), sayingSuffix(showDeletedMessage)))
                else -> TextResource.Res(R.string.mod_delete_by_creator, listOf(creatorUserDisplay.toString(), targetUserDisplay.toString(), sayingSuffix(showDeletedMessage)))
            }

            Action.Clear           -> when (creatorUserDisplay) {
                null -> TextResource.Res(R.string.mod_clear_no_creator)
                else -> TextResource.Res(R.string.mod_clear_by_creator, listOf(creatorUserDisplay.toString()))
            }

            Action.Vip             -> TextResource.Res(R.string.mod_vip_added, listOf(creatorUserDisplay.toString(), targetUserDisplay.toString()))
            Action.Unvip           -> TextResource.Res(R.string.mod_vip_removed, listOf(creatorUserDisplay.toString(), targetUserDisplay.toString()))
            Action.Warn            -> {
                val suffix = when (val r = reasonsSuffix) {
                    is TextResource.Plain -> TextResource.Plain(".")
                    else                  -> r
                }
                TextResource.Res(R.string.mod_warn, listOf(creatorUserDisplay.toString(), targetUserDisplay.toString(), suffix))
            }
            Action.Raid            -> TextResource.Res(R.string.mod_raid, listOf(creatorUserDisplay.toString(), targetUserDisplay.toString()))
            Action.Unraid          -> TextResource.Res(R.string.mod_unraid, listOf(creatorUserDisplay.toString(), targetUserDisplay.toString()))
            Action.EmoteOnly       -> TextResource.Res(R.string.mod_emote_only_on, listOf(creatorUserDisplay.toString()))
            Action.EmoteOnlyOff    -> TextResource.Res(R.string.mod_emote_only_off, listOf(creatorUserDisplay.toString()))
            Action.Followers       -> TextResource.Res(R.string.mod_followers_on, listOf(creatorUserDisplay.toString(), minutesSuffix()))
            Action.FollowersOff    -> TextResource.Res(R.string.mod_followers_off, listOf(creatorUserDisplay.toString()))
            Action.UniqueChat      -> TextResource.Res(R.string.mod_unique_chat_on, listOf(creatorUserDisplay.toString()))
            Action.UniqueChatOff   -> TextResource.Res(R.string.mod_unique_chat_off, listOf(creatorUserDisplay.toString()))
            Action.Slow            -> TextResource.Res(R.string.mod_slow_on, listOf(creatorUserDisplay.toString(), secondsSuffix()))
            Action.SlowOff         -> TextResource.Res(R.string.mod_slow_off, listOf(creatorUserDisplay.toString()))
            Action.Subscribers     -> TextResource.Res(R.string.mod_subscribers_on, listOf(creatorUserDisplay.toString()))
            Action.SubscribersOff  -> TextResource.Res(R.string.mod_subscribers_off, listOf(creatorUserDisplay.toString()))
            Action.SharedTimeout   -> TextResource.Res(R.string.mod_shared_timeout, listOf(creatorUserDisplay.toString(), targetUserDisplay.toString(), durationSuffix, sourceBroadcasterDisplay.toString(), countSuffix()))
            Action.SharedUntimeout -> TextResource.Res(R.string.mod_shared_untimeout, listOf(creatorUserDisplay.toString(), targetUserDisplay.toString(), sourceBroadcasterDisplay.toString()))
            Action.SharedBan       -> TextResource.Res(R.string.mod_shared_ban, listOf(creatorUserDisplay.toString(), targetUserDisplay.toString(), sourceBroadcasterDisplay.toString(), quotedReasonSuffix))
            Action.SharedUnban     -> TextResource.Res(R.string.mod_shared_unban, listOf(creatorUserDisplay.toString(), targetUserDisplay.toString(), sourceBroadcasterDisplay.toString()))
            Action.SharedDelete       -> TextResource.Res(R.string.mod_shared_delete, listOf(creatorUserDisplay.toString(), targetUserDisplay.toString(), sourceBroadcasterDisplay.toString(), sayingSuffix(showDeletedMessage)))
            Action.AddBlockedTerm     -> TextResource.Res(R.string.automod_moderation_added_blocked_term, listOf(creatorUserDisplay.toString(), quotedTermsOrBlank))
            Action.AddPermittedTerm   -> TextResource.Res(R.string.automod_moderation_added_permitted_term, listOf(creatorUserDisplay.toString(), quotedTermsOrBlank))
            Action.RemoveBlockedTerm  -> TextResource.Res(R.string.automod_moderation_removed_blocked_term, listOf(creatorUserDisplay.toString(), quotedTermsOrBlank))
            Action.RemovePermittedTerm -> TextResource.Res(R.string.automod_moderation_removed_permitted_term, listOf(creatorUserDisplay.toString(), quotedTermsOrBlank))
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
            val id = tags["id"] ?: UUID.randomUUID().toString()
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
            val id = tags["id"] ?: UUID.randomUUID().toString()

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
            ChannelModerateAction.Ban               -> data.ban?.reason
            ChannelModerateAction.Delete            -> data.delete?.messageBody
            ChannelModerateAction.Timeout           -> data.timeout?.reason
            ChannelModerateAction.SharedChatBan     -> data.sharedChatBan?.reason
            ChannelModerateAction.SharedChatDelete  -> data.sharedChatDelete?.messageBody
            ChannelModerateAction.SharedChatTimeout -> data.sharedChatTimeout?.reason
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
            ChannelModerateAction.SharedChatDelete       -> Action.SharedDelete
            ChannelModerateAction.AddBlockedTerm       -> Action.AddBlockedTerm
            ChannelModerateAction.AddPermittedTerm     -> Action.AddPermittedTerm
            ChannelModerateAction.RemoveBlockedTerm    -> Action.RemoveBlockedTerm
            ChannelModerateAction.RemovePermittedTerm  -> Action.RemovePermittedTerm
            else                                       -> error("Unexpected moderation action $this")
        }
    }
}
