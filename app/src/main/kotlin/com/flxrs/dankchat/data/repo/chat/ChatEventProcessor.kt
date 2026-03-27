package com.flxrs.dankchat.data.repo.chat

import android.util.Log
import com.flxrs.dankchat.R
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.api.eventapi.AutomodHeld
import com.flxrs.dankchat.data.api.eventapi.AutomodUpdate
import com.flxrs.dankchat.data.api.eventapi.ModerationAction
import com.flxrs.dankchat.data.api.eventapi.SystemMessage
import com.flxrs.dankchat.data.api.eventapi.UserMessageHeld
import com.flxrs.dankchat.data.api.eventapi.UserMessageUpdated
import com.flxrs.dankchat.data.api.eventapi.dto.messages.notification.AutomodMessageStatus
import com.flxrs.dankchat.data.api.eventapi.dto.messages.notification.AutomodReasonDto
import com.flxrs.dankchat.data.api.eventapi.dto.messages.notification.BlockedTermReasonDto
import com.flxrs.dankchat.data.auth.AuthDataStore
import com.flxrs.dankchat.data.chat.ChatImportance
import com.flxrs.dankchat.data.chat.ChatItem
import com.flxrs.dankchat.data.chat.toMentionTabItems
import com.flxrs.dankchat.data.irc.IrcMessage
import com.flxrs.dankchat.data.repo.channel.ChannelRepository
import com.flxrs.dankchat.data.toDisplayName
import com.flxrs.dankchat.data.toUserId
import com.flxrs.dankchat.data.toUserName
import com.flxrs.dankchat.data.twitch.badge.Badge
import com.flxrs.dankchat.data.twitch.badge.BadgeType
import com.flxrs.dankchat.data.twitch.chat.ChatEvent
import com.flxrs.dankchat.data.twitch.chat.ConnectionState
import com.flxrs.dankchat.data.twitch.message.AutomodMessage
import com.flxrs.dankchat.data.twitch.message.Message
import com.flxrs.dankchat.data.twitch.message.ModerationMessage
import com.flxrs.dankchat.data.twitch.message.NoticeMessage
import com.flxrs.dankchat.data.twitch.message.PointRedemptionMessage
import com.flxrs.dankchat.data.twitch.message.PrivMessage
import com.flxrs.dankchat.data.twitch.message.SystemMessageType
import com.flxrs.dankchat.data.twitch.message.UserNoticeMessage
import com.flxrs.dankchat.data.twitch.message.WhisperMessage
import com.flxrs.dankchat.data.twitch.message.hasMention
import com.flxrs.dankchat.data.twitch.pubsub.PubSubMessage
import com.flxrs.dankchat.di.DispatchersProvider
import com.flxrs.dankchat.preferences.chat.ChatSettingsDataStore
import com.flxrs.dankchat.utils.TextResource
import com.flxrs.dankchat.utils.extensions.withoutInvisibleChar
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.annotation.Single
import java.util.concurrent.ConcurrentHashMap

@Single
class ChatEventProcessor(
    private val messageProcessor: MessageProcessor,
    private val chatMessageRepository: ChatMessageRepository,
    private val chatConnector: ChatConnector,
    private val chatNotificationRepository: ChatNotificationRepository,
    private val chatChannelProvider: ChatChannelProvider,
    private val recentMessagesHandler: RecentMessagesHandler,
    private val userStateRepository: UserStateRepository,
    private val usersRepository: UsersRepository,
    private val authDataStore: AuthDataStore,
    private val channelRepository: ChannelRepository,
    private val chatSettingsDataStore: ChatSettingsDataStore,
    dispatchersProvider: DispatchersProvider,
) {

    private val scope = CoroutineScope(SupervisorJob() + dispatchersProvider.default)
    private val lastMessage = ConcurrentHashMap<UserName, String>()
    private val knownRewards = ConcurrentHashMap<String, PubSubMessage.PointRedemption>()
    private val knownAutomodHeldIds: MutableSet<String> = ConcurrentHashMap.newKeySet()
    private val rewardMutex = Mutex()

    init {
        scope.launch { collectReadConnectionEvents() }
        scope.launch { collectWriteConnectionEvents() }
        scope.launch { collectPubSubEvents() }
        scope.launch { collectEventSubEvents() }
    }

    fun getLastMessage(channel: UserName): String? = lastMessage[channel]

    fun getLastMessageForDisplay(channel: UserName?): String? = channel?.let { lastMessage[it]?.withoutInvisibleChar }

    fun setLastMessage(channel: UserName, message: String) {
        lastMessage[channel] = message
    }

    fun removeLastMessage(channel: UserName) {
        lastMessage.remove(channel)
    }

    suspend fun loadRecentMessages(channel: UserName, isReconnect: Boolean = false) {
        val result = recentMessagesHandler.load(channel, isReconnect)
        chatNotificationRepository.addMentionsDeduped(result.mentionItems)
        usersRepository.updateUsers(channel, result.userSuggestions)
    }

    private suspend fun collectReadConnectionEvents() {
        chatConnector.readEvents.collect { event ->
            when (event) {
                is ChatEvent.Connected          -> handleConnected(event.channel, event.isAnonymous)
                is ChatEvent.Closed             -> handleDisconnect()
                is ChatEvent.ChannelNonExistent -> postSystemMessageAndReconnect(SystemMessageType.ChannelNonExistent(event.channel), setOf(event.channel))
                is ChatEvent.LoginFailed        -> postSystemMessageAndReconnect(SystemMessageType.LoginExpired)
                is ChatEvent.Message            -> onMessage(event.message)
                is ChatEvent.Error              -> handleDisconnect()
            }
        }
    }

    private suspend fun collectWriteConnectionEvents() {
        chatConnector.writeEvents.collect { event ->
            if (event is ChatEvent.Message) {
                onWriterMessage(event.message)
            }
        }
    }

    private suspend fun collectPubSubEvents() {
        chatConnector.pubSubEvents.collect { pubSubMessage ->
            when (pubSubMessage) {
                is PubSubMessage.PointRedemption -> handlePubSubReward(pubSubMessage)
                is PubSubMessage.Whisper         -> handlePubSubWhisper(pubSubMessage)
                is PubSubMessage.ModeratorAction -> handlePubSubModeration(pubSubMessage)
            }
        }
    }

    private suspend fun collectEventSubEvents() {
        chatConnector.eventSubEvents.collect { eventMessage ->
            when (eventMessage) {
                is ModerationAction  -> handleEventSubModeration(eventMessage)
                is AutomodHeld       -> handleAutomodHeld(eventMessage)
                is AutomodUpdate     -> handleAutomodUpdate(eventMessage)
                is UserMessageHeld   -> handleUserMessageHeld(eventMessage)
                is UserMessageUpdated -> handleUserMessageUpdated(eventMessage)
                is SystemMessage     -> postSystemMessageAndReconnect(type = SystemMessageType.Custom(eventMessage.message))
            }
        }
    }

    private suspend fun handlePubSubReward(pubSubMessage: PubSubMessage.PointRedemption) {
        if (messageProcessor.isUserBlocked(pubSubMessage.data.user.id)) {
            return
        }

        if (pubSubMessage.data.reward.requiresUserInput) {
            val id = pubSubMessage.data.reward.id
            rewardMutex.withLock {
                when {
                    knownRewards.containsKey(id) -> {
                        Log.d(TAG, "Removing known reward $id")
                        knownRewards.remove(id)
                    }

                    else                         -> {
                        Log.d(TAG, "Received pubsub reward message with id $id")
                        knownRewards[id] = pubSubMessage
                    }
                }
            }
        } else {
            val message = runCatching {
                messageProcessor.processReward(
                    PointRedemptionMessage.parsePointReward(pubSubMessage.timestamp, pubSubMessage.data)
                )
            }.getOrNull() ?: return

            chatMessageRepository.addMessages(pubSubMessage.channelName, listOf(ChatItem(message)))
        }
    }

    private suspend fun handlePubSubWhisper(pubSubMessage: PubSubMessage.Whisper) {
        if (messageProcessor.isUserBlocked(pubSubMessage.data.userId)) {
            return
        }

        val message = runCatching {
            messageProcessor.processWhisper(WhisperMessage.fromPubSub(pubSubMessage.data)) as? WhisperMessage
        }.getOrNull() ?: return

        val item = ChatItem(message, isMentionTab = true)
        chatNotificationRepository.addWhisper(item)

        if (pubSubMessage.data.userId == userStateRepository.userState.value.userId) {
            return
        }

        val userForSuggestion = message.name.valueOrDisplayName(message.displayName).toDisplayName()
        usersRepository.updateGlobalUser(message.name.lowercase(), userForSuggestion)
        chatNotificationRepository.incrementMentionCount(WhisperMessage.WHISPER_CHANNEL, 1)
        chatNotificationRepository.emitNotification(listOf(item))
    }

    private fun handlePubSubModeration(pubSubMessage: PubSubMessage.ModeratorAction) {
        val (timestamp, channelId, data) = pubSubMessage
        val channelName = channelRepository.tryGetUserNameById(channelId) ?: return
        val message = runCatching {
            ModerationMessage.parseModerationAction(timestamp, channelName, data)
        }.getOrElse { return }

        chatMessageRepository.applyModerationMessage(message)
    }

    private fun handleEventSubModeration(eventMessage: ModerationAction) {
        val (id, timestamp, channelName, data) = eventMessage
        val message = runCatching {
            ModerationMessage.parseModerationAction(id, timestamp, channelName, data)
        }.getOrElse {
            Log.d(TAG, "Failed to parse event sub moderation message: $it")
            return
        }

        chatMessageRepository.applyModerationMessage(message)
    }

    private fun handleAutomodHeld(eventMessage: AutomodHeld) {
        val data = eventMessage.data
        knownAutomodHeldIds.add(data.messageId)
        val reason = formatAutomodReason(data.reason, data.automod, data.blockedTerm, data.message.text)
        val userColor = usersRepository.getCachedUserColor(data.userLogin) ?: Message.DEFAULT_COLOR
        val automodBadge = Badge.GlobalBadge(
            title = "AutoMod",
            badgeTag = "automod/1",
            badgeInfo = null,
            url = "",
            type = BadgeType.Authority,
        )
        val automodMsg = AutomodMessage(
            timestamp = eventMessage.timestamp.toEpochMilliseconds(),
            id = eventMessage.id,
            channel = eventMessage.channelName,
            heldMessageId = data.messageId,
            userName = data.userLogin,
            userDisplayName = data.userName,
            messageText = data.message.text,
            reason = reason,
            badges = listOf(automodBadge),
            color = userColor,
        )
        chatMessageRepository.addMessages(eventMessage.channelName, listOf(ChatItem(automodMsg, importance = ChatImportance.SYSTEM)))
    }

    private fun handleAutomodUpdate(eventMessage: AutomodUpdate) {
        knownAutomodHeldIds.remove(eventMessage.data.messageId)
        val newStatus = when (eventMessage.data.status) {
            AutomodMessageStatus.Approved -> AutomodMessage.Status.Approved
            AutomodMessageStatus.Denied   -> AutomodMessage.Status.Denied
            AutomodMessageStatus.Expired  -> AutomodMessage.Status.Expired
        }
        chatMessageRepository.updateAutomodMessageStatus(eventMessage.channelName, eventMessage.data.messageId, newStatus)
    }

    private fun handleUserMessageHeld(eventMessage: UserMessageHeld) {
        val data = eventMessage.data
        val automodBadge = Badge.GlobalBadge(
            title = "AutoMod",
            badgeTag = "automod/1",
            badgeInfo = null,
            url = "",
            type = BadgeType.Authority,
        )
        val automodMsg = AutomodMessage(
            timestamp = eventMessage.timestamp.toEpochMilliseconds(),
            id = eventMessage.id,
            channel = eventMessage.channelName,
            heldMessageId = data.messageId,
            userName = data.userLogin,
            userDisplayName = data.userName,
            messageText = null,
            reason = TextResource.Res(R.string.automod_user_held),
            badges = listOf(automodBadge),
            isUserSide = true,
        )
        chatMessageRepository.addMessages(eventMessage.channelName, listOf(ChatItem(automodMsg, importance = ChatImportance.SYSTEM)))
    }

    private fun handleUserMessageUpdated(eventMessage: UserMessageUpdated) {
        val data = eventMessage.data
        val automodBadge = Badge.GlobalBadge(
            title = "AutoMod",
            badgeTag = "automod/1",
            badgeInfo = null,
            url = "",
            type = BadgeType.Authority,
        )
        val reason = when (data.status) {
            AutomodMessageStatus.Approved -> TextResource.Res(R.string.automod_user_accepted)
            AutomodMessageStatus.Denied   -> TextResource.Res(R.string.automod_user_denied)
            AutomodMessageStatus.Expired  -> TextResource.Res(R.string.automod_status_expired)
        }
        val automodMsg = AutomodMessage(
            timestamp = eventMessage.timestamp.toEpochMilliseconds(),
            id = eventMessage.id,
            channel = eventMessage.channelName,
            heldMessageId = data.messageId,
            userName = data.userLogin,
            userDisplayName = data.userName,
            messageText = null,
            reason = reason,
            badges = listOf(automodBadge),
            isUserSide = true,
            status = when (data.status) {
                AutomodMessageStatus.Approved -> AutomodMessage.Status.Approved
                AutomodMessageStatus.Denied   -> AutomodMessage.Status.Denied
                AutomodMessageStatus.Expired  -> AutomodMessage.Status.Expired
            },
        )
        chatMessageRepository.addMessages(eventMessage.channelName, listOf(ChatItem(automodMsg, importance = ChatImportance.SYSTEM)))
    }

    private suspend fun onMessage(msg: IrcMessage) {
        when (msg.command) {
            "CLEARCHAT"       -> handleClearChat(msg)
            "CLEARMSG"        -> handleClearMsg(msg)
            "ROOMSTATE"       -> channelRepository.handleRoomState(msg)
            "USERSTATE"       -> userStateRepository.handleUserState(msg)
            "GLOBALUSERSTATE" -> userStateRepository.handleGlobalUserState(msg)
            "WHISPER"         -> handleWhisper(msg)
            else              -> handleMessage(msg)
        }
    }

    private suspend fun onWriterMessage(message: IrcMessage) {
        when (message.command) {
            "USERSTATE"       -> userStateRepository.handleUserState(message)
            "GLOBALUSERSTATE" -> userStateRepository.handleGlobalUserState(message)
            "NOTICE"          -> handleMessage(message)
        }
    }

    private fun handleDisconnect() {
        val state = ConnectionState.DISCONNECTED
        chatConnector.setAllConnectionStates(state)
        postSystemMessageAndReconnect(state.toSystemMessageType())
    }

    private fun handleConnected(channel: UserName, isAnonymous: Boolean) {
        val state = when {
            isAnonymous -> ConnectionState.CONNECTED_NOT_LOGGED_IN
            else        -> ConnectionState.CONNECTED
        }
        val transitioning = chatChannelProvider.channels.value.orEmpty()
            .filter { chatConnector.getConnectionState(it).value != state }
            .toSet()

        chatConnector.setAllConnectionStates(state)

        if (transitioning.isNotEmpty()) {
            postSystemMessageAndReconnect(state.toSystemMessageType(), transitioning)
        }
    }

    private fun handleClearChat(msg: IrcMessage) {
        val parsed = runCatching {
            ModerationMessage.parseClearChat(msg)
        }.getOrElse { return }

        chatMessageRepository.applyModerationMessage(parsed)
    }

    private fun handleClearMsg(msg: IrcMessage) {
        val parsed = runCatching {
            ModerationMessage.parseClearMessage(msg)
        }.getOrElse { return }

        chatMessageRepository.applyModerationMessage(parsed)
    }

    private suspend fun handleWhisper(ircMessage: IrcMessage) {
        if (chatConnector.connectedAndHasWhisperTopic) {
            return
        }

        val userId = ircMessage.tags["user-id"]?.toUserId()
        if (messageProcessor.isUserBlocked(userId)) {
            return
        }

        val userState = userStateRepository.userState.value
        val recipient = userState.displayName ?: return
        val message = runCatching {
            messageProcessor.processWhisper(
                WhisperMessage.parseFromIrc(ircMessage, recipient, userState.color)
            ) as? WhisperMessage
        }.getOrNull() ?: return

        val userForSuggestion = message.name.valueOrDisplayName(message.displayName).toDisplayName()
        usersRepository.updateGlobalUser(message.name.lowercase(), userForSuggestion)

        val item = ChatItem(message, isMentionTab = true)
        chatNotificationRepository.addWhisper(item)
        chatNotificationRepository.incrementMentionCount(WhisperMessage.WHISPER_CHANNEL, 1)
    }

    private suspend fun handleMessage(ircMessage: IrcMessage) {
        if (ircMessage.command == "NOTICE") {
            val msgId = ircMessage.tags["msg-id"]
            if (msgId in NoticeMessage.ROOM_STATE_CHANGE_MSG_IDS) {
                val channel = ircMessage.params[0].substring(1).toUserName()
                if (chatConnector.connectedAndHasModerateTopic(channel)) {
                    return
                }
            }
            if (msgId in AUTOMOD_NOTICE_MSG_IDS && chatConnector.connectedAndHasUserMessageTopic) {
                return
            }
        }

        if (messageProcessor.isUserBlocked(ircMessage.tags["user-id"]?.toUserId())) {
            return
        }

        val additionalMessages = resolveRewardMessages(ircMessage)

        val message = runCatching {
            messageProcessor.processIrcMessage(ircMessage) { channel, id ->
                chatMessageRepository.findMessage(id, channel, chatNotificationRepository.whispers)
            }
        }.getOrElse {
            Log.e(TAG, "Failed to parse message", it)
            return
        } ?: return

        if (message is NoticeMessage && usersRepository.isGlobalChannel(message.channel)) {
            chatMessageRepository.broadcastToAllChannels(ChatItem(message, importance = ChatImportance.SYSTEM))
            return
        }

        trackUserState(message)

        val items = buildList {
            if (message is UserNoticeMessage && message.childMessage != null) {
                add(ChatItem(message.childMessage))
            }
            val importance = when (message) {
                is NoticeMessage -> ChatImportance.SYSTEM
                else             -> ChatImportance.REGULAR
            }
            add(ChatItem(message, importance = importance))
        }

        val channel = when (message) {
            is PrivMessage       -> message.channel
            is UserNoticeMessage -> message.channel
            is NoticeMessage     -> message.channel
            else                 -> return
        }

        chatMessageRepository.addMessages(channel, additionalMessages + items)
        chatNotificationRepository.emitNotification(items)

        val mentions = items
            .filter { it.message.highlights.hasMention() }
            .toMentionTabItems()

        if (mentions.isNotEmpty()) {
            chatNotificationRepository.addMentions(mentions)
        }

        if (channel != chatChannelProvider.activeChannel.value) {
            if (mentions.isNotEmpty()) {
                chatNotificationRepository.incrementMentionCount(channel, mentions.size)
            }

            if (message is PrivMessage) {
                chatNotificationRepository.setUnreadIfInactive(channel)
            }
        }
    }

    private suspend fun resolveRewardMessages(ircMessage: IrcMessage): List<ChatItem> {
        val rewardId = ircMessage.tags["custom-reward-id"]?.takeIf { it.isNotEmpty() } ?: return emptyList()
        val isAutomodApproval = knownAutomodHeldIds.remove(rewardId)
        if (isAutomodApproval) {
            return emptyList()
        }

        val reward = rewardMutex.withLock {
            knownRewards[rewardId]
                ?.also {
                    Log.d(TAG, "Removing known reward $rewardId")
                    knownRewards.remove(rewardId)
                }
                ?: run {
                    Log.d(TAG, "Waiting for pubsub reward message with id $rewardId")
                    withTimeoutOrNull(PUBSUB_TIMEOUT) {
                        chatConnector.pubSubEvents
                            .filterIsInstance<PubSubMessage.PointRedemption>()
                            .first { it.data.reward.id == rewardId }
                    }?.also { knownRewards[rewardId] = it }
                }
        }

        return reward?.let {
            val processed = messageProcessor.processReward(PointRedemptionMessage.parsePointReward(it.timestamp, it.data))
            listOfNotNull(processed?.let(::ChatItem))
        }.orEmpty()
    }

    private fun trackUserState(message: Message) {
        if (message !is PrivMessage) {
            return
        }

        if (message.color != Message.DEFAULT_COLOR) {
            usersRepository.cacheUserColor(message.name, message.color)
        }

        if (message.name == authDataStore.userName) {
            val previousLastMessage = lastMessage[message.channel].orEmpty()
            val lastMessageWasCommand = previousLastMessage.startsWith('.') || previousLastMessage.startsWith('/')
            if (!lastMessageWasCommand && previousLastMessage.withoutInvisibleChar != message.originalMessage.withoutInvisibleChar) {
                lastMessage[message.channel] = message.originalMessage
            }

            val hasVip = message.badges.any { badge -> badge.badgeTag?.startsWith("vip") == true }
            when {
                hasVip -> userStateRepository.addVipChannel(message.channel)
                else   -> userStateRepository.removeVipChannel(message.channel)
            }
        }

        val userForSuggestion = message.name.valueOrDisplayName(message.displayName).toDisplayName()
        usersRepository.updateUser(message.channel, message.name.lowercase(), userForSuggestion)
    }

    private fun postSystemMessageAndReconnect(type: SystemMessageType, channels: Set<UserName> = chatChannelProvider.channels.value.orEmpty().toSet()) {
        val reconnectedChannels = chatMessageRepository.addSystemMessageToChannels(type, channels)
        reconnectedChannels.forEach { channel ->
            scope.launch {
                if (chatSettingsDataStore.settings.first().loadMessageHistoryOnReconnect) {
                    loadRecentMessages(channel, isReconnect = true)
                }
            }
        }
    }

    private fun ConnectionState.toSystemMessageType(): SystemMessageType = when (this) {
        ConnectionState.DISCONNECTED            -> SystemMessageType.Disconnected
        ConnectionState.CONNECTED,
        ConnectionState.CONNECTED_NOT_LOGGED_IN -> SystemMessageType.Connected
    }

    private fun formatAutomodReason(
        reason: String,
        automod: AutomodReasonDto?,
        blockedTerm: BlockedTermReasonDto?,
        messageText: String,
    ): TextResource = when {
        reason == "automod" && automod != null          -> TextResource.Res(R.string.automod_reason_category, persistentListOf(automod.category, automod.level))
        reason == "blocked_term" && blockedTerm != null -> {
            val terms = blockedTerm.termsFound.joinToString { found ->
                val start = found.boundary.startPos
                val end = (found.boundary.endPos + 1).coerceAtMost(messageText.length)
                "\"${messageText.substring(start, end)}\""
            }
            val count = blockedTerm.termsFound.size
            TextResource.PluralRes(R.plurals.automod_reason_blocked_terms, count, persistentListOf(count, terms))
        }

        else                                            -> TextResource.Plain(reason)
    }

    companion object {
        private val TAG = ChatEventProcessor::class.java.simpleName
        private const val PUBSUB_TIMEOUT = 5000L
        private val AUTOMOD_NOTICE_MSG_IDS = setOf("msg_rejected", "msg_rejected_mandatory")
    }
}
