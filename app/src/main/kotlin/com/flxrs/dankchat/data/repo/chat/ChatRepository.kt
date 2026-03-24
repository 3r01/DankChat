package com.flxrs.dankchat.data.repo.chat

import android.graphics.Color
import android.util.Log
import com.flxrs.dankchat.R
import com.flxrs.dankchat.auth.AuthDataStore
import com.flxrs.dankchat.chat.ChatImportance
import com.flxrs.dankchat.chat.ChatItem
import com.flxrs.dankchat.chat.compose.TextResource
import com.flxrs.dankchat.chat.toMentionTabItems
import com.flxrs.dankchat.data.DisplayName
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.api.eventapi.AutomodHeld
import com.flxrs.dankchat.data.api.eventapi.AutomodUpdate
import com.flxrs.dankchat.data.api.eventapi.EventSubManager
import com.flxrs.dankchat.data.api.eventapi.ModerationAction
import com.flxrs.dankchat.data.api.eventapi.SystemMessage
import com.flxrs.dankchat.data.api.eventapi.dto.messages.notification.AutomodMessageStatus
import com.flxrs.dankchat.data.api.eventapi.dto.messages.notification.AutomodReasonDto
import com.flxrs.dankchat.data.api.eventapi.dto.messages.notification.BlockedTermReasonDto
import com.flxrs.dankchat.data.irc.IrcMessage
import com.flxrs.dankchat.data.repo.emote.EmoteRepository
import com.flxrs.dankchat.data.toDisplayName
import com.flxrs.dankchat.data.toUserId
import com.flxrs.dankchat.data.toUserName
import com.flxrs.dankchat.data.twitch.badge.Badge
import com.flxrs.dankchat.data.twitch.badge.BadgeType
import com.flxrs.dankchat.data.twitch.chat.ChatConnection
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
import com.flxrs.dankchat.data.twitch.message.toChatItem
import com.flxrs.dankchat.data.twitch.pubsub.PubSubManager
import com.flxrs.dankchat.data.twitch.pubsub.PubSubMessage
import com.flxrs.dankchat.di.DispatchersProvider
import com.flxrs.dankchat.di.ReadConnection
import com.flxrs.dankchat.di.WriteConnection
import com.flxrs.dankchat.preferences.DankChatPreferenceStore
import com.flxrs.dankchat.preferences.chat.ChatSettingsDataStore
import com.flxrs.dankchat.utils.extensions.INVISIBLE_CHAR
import com.flxrs.dankchat.utils.extensions.addAndLimit
import com.flxrs.dankchat.utils.extensions.addSystemMessage
import com.flxrs.dankchat.utils.extensions.assign
import com.flxrs.dankchat.utils.extensions.clear
import com.flxrs.dankchat.utils.extensions.codePointAsString
import com.flxrs.dankchat.utils.extensions.firstValue
import com.flxrs.dankchat.utils.extensions.increment
import com.flxrs.dankchat.utils.extensions.mutableSharedFlowOf
import com.flxrs.dankchat.utils.extensions.replaceOrAddModerationMessage
import com.flxrs.dankchat.utils.extensions.replaceWithTimeout
import com.flxrs.dankchat.utils.extensions.withoutInvisibleChar
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import java.util.concurrent.ConcurrentHashMap

@Single
class ChatRepository(
    private val messageProcessor: MessageProcessor,
    private val recentMessagesHandler: RecentMessagesHandler,
    private val emoteRepository: EmoteRepository,
    private val userStateRepository: UserStateRepository,
    private val usersRepository: UsersRepository,
    private val authDataStore: AuthDataStore,
    private val dankChatPreferenceStore: DankChatPreferenceStore,
    private val chatSettingsDataStore: ChatSettingsDataStore,
    private val pubSubManager: PubSubManager,
    private val eventSubManager: EventSubManager,
    @Named(type = ReadConnection::class) private val readConnection: ChatConnection,
    @Named(type = WriteConnection::class) private val writeConnection: ChatConnection,
    dispatchersProvider: DispatchersProvider,
) {

    private val scope = CoroutineScope(SupervisorJob() + dispatchersProvider.default)
    private val _activeChannel = MutableStateFlow<UserName?>(null)
    private val _channels = MutableStateFlow<List<UserName>?>(null)

    private val _notificationsFlow = MutableSharedFlow<List<ChatItem>>(replay = 0, extraBufferCapacity = 10)
    private val _channelMentionCount = mutableSharedFlowOf(mutableMapOf<UserName, Int>())
    private val _unreadMessagesMap = mutableSharedFlowOf(mutableMapOf<UserName, Boolean>())
    private val messages = ConcurrentHashMap<UserName, MutableStateFlow<List<ChatItem>>>()
    private val _mentions = MutableStateFlow<List<ChatItem>>(emptyList())
    private val _whispers = MutableStateFlow<List<ChatItem>>(emptyList())
    private val connectionState = ConcurrentHashMap<UserName, MutableStateFlow<ConnectionState>>()
    private val _chatLoadingFailures = MutableStateFlow(emptySet<ChatLoadingFailure>())

    private var lastMessage = ConcurrentHashMap<UserName, String>()
    private val knownRewards = ConcurrentHashMap<String, PubSubMessage.PointRedemption>()
    private val knownAutomodHeldIds: MutableSet<String> = ConcurrentHashMap.newKeySet()
    private val rewardMutex = Mutex()

    private val scrollBackLengthFlow = chatSettingsDataStore.debouncedScrollBack
        .onEach { length ->
            messages.forEach { (_, messagesFlow) ->
                if (messagesFlow.value.size > length) {
                    messagesFlow.update {
                        it.takeLast(length)
                    }
                }
            }
        }
        .stateIn(scope, SharingStarted.Eagerly, 500)
    private val scrollBackLength get() = scrollBackLengthFlow.value

    private val channelRepository get() = messageProcessor.channelRepository

    init {
        scope.launch { collectReadConnectionEvents() }
        scope.launch { collectWriteConnectionEvents() }
        scope.launch { collectPubSubEvents() }
        scope.launch { collectEventSubEvents() }
    }

    private suspend fun collectReadConnectionEvents() {
        readConnection.messages.collect { event ->
            when (event) {
                is ChatEvent.Connected          -> handleConnected(event.channel, event.isAnonymous)
                is ChatEvent.Closed             -> handleDisconnect()
                is ChatEvent.ChannelNonExistent -> makeAndPostSystemMessage(SystemMessageType.ChannelNonExistent(event.channel), setOf(event.channel))
                is ChatEvent.LoginFailed        -> makeAndPostSystemMessage(SystemMessageType.LoginExpired)
                is ChatEvent.Message            -> onMessage(event.message)
                is ChatEvent.Error              -> handleDisconnect()
            }
        }
    }

    private suspend fun collectWriteConnectionEvents() {
        writeConnection.messages.collect { event ->
            if (event is ChatEvent.Message) {
                onWriterMessage(event.message)
            }
        }
    }

    private suspend fun collectPubSubEvents() {
        pubSubManager.messages.collect { pubSubMessage ->
            when (pubSubMessage) {
                is PubSubMessage.PointRedemption -> handlePubSubReward(pubSubMessage)
                is PubSubMessage.Whisper         -> handlePubSubWhisper(pubSubMessage)
                is PubSubMessage.ModeratorAction -> handlePubSubModeration(pubSubMessage)
            }
        }
    }

    private suspend fun collectEventSubEvents() {
        eventSubManager.events.collect { eventMessage ->
            when (eventMessage) {
                is ModerationAction -> handleEventSubModeration(eventMessage)
                is AutomodHeld      -> handleAutomodHeld(eventMessage)
                is AutomodUpdate    -> handleAutomodUpdate(eventMessage)
                is SystemMessage    -> makeAndPostSystemMessage(type = SystemMessageType.Custom(eventMessage.message))
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

            messages[pubSubMessage.channelName]?.update {
                it.addAndLimit(ChatItem(message), scrollBackLength, messageProcessor::onMessageRemoved)
            }
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
        _whispers.update { current ->
            current.addAndLimit(item, scrollBackLength, messageProcessor::onMessageRemoved)
        }

        if (pubSubMessage.data.userId == userStateRepository.userState.value.userId) {
            return
        }

        val userForSuggestion = message.name.valueOrDisplayName(message.displayName).toDisplayName()
        usersRepository.updateGlobalUser(message.name.lowercase(), userForSuggestion)
        _channelMentionCount.increment(WhisperMessage.WHISPER_CHANNEL, 1)
        _notificationsFlow.tryEmit(listOf(item))
    }

    private fun handlePubSubModeration(pubSubMessage: PubSubMessage.ModeratorAction) {
        val (timestamp, channelId, data) = pubSubMessage
        val channelName = channelRepository.tryGetUserNameById(channelId) ?: return
        val message = runCatching {
            ModerationMessage.parseModerationAction(timestamp, channelName, data)
        }.getOrElse { return }

        applyModerationMessage(message)
    }

    private fun handleEventSubModeration(eventMessage: ModerationAction) {
        val (id, timestamp, channelName, data) = eventMessage
        val message = runCatching {
            ModerationMessage.parseModerationAction(id, timestamp, channelName, data)
        }.getOrElse {
            Log.d(TAG, "Failed to parse event sub moderation message: $it")
            return
        }

        applyModerationMessage(message)
    }

    private fun applyModerationMessage(message: ModerationMessage) {
        messages[message.channel]?.update { current ->
            when (message.action) {
                ModerationMessage.Action.Delete,
                ModerationMessage.Action.SharedDelete -> current.replaceWithTimeout(message, scrollBackLength, messageProcessor::onMessageRemoved)

                else                                  -> current.replaceOrAddModerationMessage(message, scrollBackLength, messageProcessor::onMessageRemoved)
            }
        }
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
        messages[eventMessage.channelName]?.update { current ->
            current.addAndLimit(ChatItem(automodMsg, importance = ChatImportance.SYSTEM), scrollBackLength, messageProcessor::onMessageRemoved)
        }
    }

    private fun handleAutomodUpdate(eventMessage: AutomodUpdate) {
        knownAutomodHeldIds.remove(eventMessage.data.messageId)
        val newStatus = when (eventMessage.data.status) {
            AutomodMessageStatus.Approved -> AutomodMessage.Status.Approved
            AutomodMessageStatus.Denied   -> AutomodMessage.Status.Denied
            AutomodMessageStatus.Expired  -> AutomodMessage.Status.Expired
        }
        updateAutomodMessageStatus(eventMessage.channelName, eventMessage.data.messageId, newStatus)
    }

    val notificationsFlow: SharedFlow<List<ChatItem>> = _notificationsFlow.asSharedFlow()
    val channelMentionCount: SharedFlow<Map<UserName, Int>> = _channelMentionCount.asSharedFlow()
    val unreadMessagesMap: SharedFlow<Map<UserName, Boolean>> = _unreadMessagesMap.asSharedFlow()
    val hasMentions = channelMentionCount.map { it.any { channel -> channel.key != WhisperMessage.WHISPER_CHANNEL && channel.value > 0 } }
    val hasWhispers = channelMentionCount.map { it.getOrDefault(WhisperMessage.WHISPER_CHANNEL, 0) > 0 }
    val mentions: StateFlow<List<ChatItem>> = _mentions
    val whispers: StateFlow<List<ChatItem>> = _whispers
    val activeChannel: StateFlow<UserName?> = _activeChannel.asStateFlow()
    val channels: StateFlow<List<UserName>?> = _channels.asStateFlow()
    val chatLoadingFailures = _chatLoadingFailures.asStateFlow()

    fun getChat(channel: UserName): StateFlow<List<ChatItem>> = messages.getOrPut(channel) { MutableStateFlow(emptyList()) }
    fun getConnectionState(channel: UserName): StateFlow<ConnectionState> = connectionState.getOrPut(channel) { MutableStateFlow(ConnectionState.DISCONNECTED) }

    fun findMessage(messageId: String, channel: UserName?) = (channel?.let { messages[channel] } ?: whispers).value.find { it.message.id == messageId }?.message

    fun clearChatLoadingFailures() = _chatLoadingFailures.update { emptySet() }

    suspend fun loadRecentMessagesIfEnabled(channel: UserName) {
        when {
            chatSettingsDataStore.settings.first().loadMessageHistory -> loadRecentMessages(channel)
            else                                                      -> messages[channel]?.update { current ->
                val message = SystemMessageType.NoHistoryLoaded.toChatItem()
                listOf(message).addAndLimit(current, scrollBackLength, messageProcessor::onMessageRemoved, checkForDuplications = true)
            }
        }
    }

    suspend fun reparseAllEmotesAndBadges() = withContext(Dispatchers.Default) {
        messages.values.map { flow ->
            async {
                flow.update { messages ->
                    messages.map {
                        it.copy(
                            tag = it.tag + 1,
                            message = messageProcessor.reparseEmotesAndBadges(it.message),
                        )
                    }
                }
            }
        }.awaitAll()
    }

    fun setActiveChannel(channel: UserName?) {
        _activeChannel.value = channel
    }

    fun clearMentionCount(channel: UserName) = with(_channelMentionCount) {
        tryEmit(firstValue.apply { set(channel, 0) })
    }

    fun clearMentionCounts() = with(_channelMentionCount) {
        tryEmit(firstValue.apply { keys.forEach { if (it != WhisperMessage.WHISPER_CHANNEL) set(it, 0) } })
    }

    fun clearUnreadMessage(channel: UserName) {
        _unreadMessagesMap.assign(channel, false)
    }

    fun clear(channel: UserName) {
        messages[channel]?.value = emptyList()
    }

    fun closeAndReconnect() = scope.launch {
        val channels = channels.value.orEmpty()

        readConnection.close()
        writeConnection.close()
        pubSubManager.close()
        eventSubManager.close()
        userStateRepository.clear()
        connectAndJoin(channels)
    }

    fun reconnect(reconnectPubsub: Boolean = true) {
        readConnection.reconnect()
        writeConnection.reconnect()

        if (reconnectPubsub) {
            pubSubManager.reconnect()
            eventSubManager.reconnect()
        }
    }

    fun reconnectIfNecessary() {
        readConnection.reconnectIfNecessary()
        writeConnection.reconnectIfNecessary()
        pubSubManager.reconnectIfNecessary()
        eventSubManager.reconnectIfNecessary()
    }

    fun getLastMessage(): String? = activeChannel.value?.let { lastMessage[it]?.withoutInvisibleChar }

    fun fakeWhisperIfNecessary(input: String) {
        if (pubSubManager.connectedAndHasWhisperTopic) {
            return
        }
        // fake whisper handling
        val split = input.split(" ")
        if (split.size > 2 && (split[0] == "/w" || split[0] == ".w") && split[1].isNotBlank()) {
            val message = input.substring(4 + split[1].length)
            val emotes = emoteRepository.parse3rdPartyEmotes(message, WhisperMessage.WHISPER_CHANNEL, withTwitch = true)
            val userState = userStateRepository.userState.value
            val name = authDataStore.userName ?: return
            val displayName = userState.displayName ?: return
            val fakeMessage = WhisperMessage(
                userId = userState.userId,
                name = name,
                displayName = displayName,
                color = userState.color?.let(Color::parseColor) ?: Message.DEFAULT_COLOR,
                recipientId = null,
                recipientColor = Message.DEFAULT_COLOR,
                recipientName = split[1].toUserName(),
                recipientDisplayName = split[1].toDisplayName(),
                message = message,
                rawEmotes = "",
                rawBadges = "",
                emotes = emotes,
            )
            val fakeItem = ChatItem(fakeMessage, isMentionTab = true)
            _whispers.update {
                it.addAndLimit(fakeItem, scrollBackLength, messageProcessor::onMessageRemoved)
            }
        }
    }

    fun sendMessage(input: String, replyId: String? = null) {
        val channel = activeChannel.value ?: return
        val preparedMessage = prepareMessage(channel, input, replyId) ?: return
        writeConnection.sendMessage(preparedMessage)
    }

    fun connectAndJoin(channels: List<UserName> = dankChatPreferenceStore.channels) {
        if (!pubSubManager.connected) {
            pubSubManager.start()
        }

        if (!readConnection.connected) {
            connect()
            joinChannels(channels)
        }
    }

    fun joinChannel(channel: UserName, listenToPubSub: Boolean = true): List<UserName> {
        val channels = channels.value.orEmpty()
        if (channel in channels)
            return channels

        val updatedChannels = channels + channel
        _channels.value = updatedChannels

        createFlowsIfNecessary(channel)
        messages[channel]?.value = emptyList()


        readConnection.joinChannel(channel)

        if (listenToPubSub) {
            pubSubManager.addChannel(channel)
        }

        return updatedChannels
    }

    fun createFlowsIfNecessary(channel: UserName) {
        messages.putIfAbsent(channel, MutableStateFlow(emptyList()))
        connectionState.putIfAbsent(channel, MutableStateFlow(ConnectionState.DISCONNECTED))
        usersRepository.initChannel(channel)
        channelRepository.initRoomState(channel)

        with(_channelMentionCount) {
            if (!firstValue.contains(WhisperMessage.WHISPER_CHANNEL)) tryEmit(firstValue.apply { set(channel, 0) })
            if (!firstValue.contains(channel)) tryEmit(firstValue.apply { set(channel, 0) })
        }
    }

    fun updateChannels(updatedChannels: List<UserName>): List<UserName> {
        val currentChannels = channels.value.orEmpty()
        val removedChannels = currentChannels - updatedChannels.toSet()

        removedChannels.forEach {
            partChannel(it)
        }

        _channels.value = updatedChannels
        return removedChannels
    }

    fun appendLastMessage(channel: UserName, message: String) {
        lastMessage[channel] = message
    }

    private fun connect() {
        readConnection.connect()
        writeConnection.connect()
    }

    private fun joinChannels(channels: List<UserName>) {
        _channels.value = channels
        if (channels.isEmpty()) return

        channels.onEach {
            createFlowsIfNecessary(it)
            if (messages[it]?.value == null) {
                messages[it]?.value = emptyList()
            }
        }

        readConnection.joinChannels(channels)
    }

    private fun partChannel(channel: UserName): List<UserName> {
        val updatedChannels = channels.value.orEmpty() - channel
        _channels.value = updatedChannels

        removeChannelData(channel)
        readConnection.partChannel(channel)

        pubSubManager.removeChannel(channel)
        eventSubManager.removeChannel(channel)

        return updatedChannels
    }

    private fun removeChannelData(channel: UserName) {
        messages.remove(channel)
        connectionState.remove(channel)
        lastMessage.remove(channel)
        _channelMentionCount.clear(channel)
        usersRepository.removeChannel(channel)
        userStateRepository.removeChannel(channel)
        channelRepository.removeRoomState(channel)
        emoteRepository.removeChannel(channel)
        messageProcessor.cleanupMessageThreadsInChannel(channel)
    }

    private fun prepareMessage(channel: UserName, message: String, replyId: String?): String? {
        if (message.isBlank()) return null
        val trimmedMessage = message.trimEnd()
        val replyIdOrBlank = replyId?.let { "@reply-parent-msg-id=$it " }.orEmpty()

        val messageWithSuffix = if (lastMessage[channel].orEmpty() == trimmedMessage) {
            // Find first space to double (preferred — Twitch strips extra spaces server-side)
            // Skip the first space if message starts with / or . (Twitch command prefix)
            val startIndex = if (trimmedMessage.startsWith('/') || trimmedMessage.startsWith('.')) {
                trimmedMessage.indexOf(' ').let { if (it == -1) 0 else it + 1 }
            } else {
                0
            }
            val spaceIndex = trimmedMessage.indexOf(' ', startIndex)

            if (spaceIndex != -1) {
                // Double the space — invisible to viewers, different on the wire
                trimmedMessage.replaceRange(spaceIndex, spaceIndex + 1, "  ")
            } else {
                // No space to double, fall back to invisible char suffix
                "$trimmedMessage $INVISIBLE_CHAR"
            }
        } else {
            trimmedMessage
        }

        lastMessage[channel] = messageWithSuffix
        return "${replyIdOrBlank}PRIVMSG #$channel :$messageWithSuffix"
    }

    private suspend fun onMessage(msg: IrcMessage): List<ChatItem>? {
        when (msg.command) {
            "CLEARCHAT"       -> handleClearChat(msg)
            "CLEARMSG"        -> handleClearMsg(msg)
            "ROOMSTATE"       -> channelRepository.handleRoomState(msg)
            "USERSTATE"       -> userStateRepository.handleUserState(msg)
            "GLOBALUSERSTATE" -> userStateRepository.handleGlobalUserState(msg)
            "WHISPER"         -> handleWhisper(msg)
            else              -> handleMessage(msg)
        }
        return null
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
        connectionState.keys.forEach {
            connectionState[it]?.value = state
        }
        makeAndPostSystemMessage(state.toSystemMessageType())

    }

    private fun handleConnected(channel: UserName, isAnonymous: Boolean) {
        val state = when {
            isAnonymous -> ConnectionState.CONNECTED_NOT_LOGGED_IN
            else        -> ConnectionState.CONNECTED
        }
        makeAndPostSystemMessage(state.toSystemMessageType(), setOf(channel))
        connectionState[channel]?.value = state
    }

    private fun handleClearChat(msg: IrcMessage) {
        val parsed = runCatching {
            ModerationMessage.parseClearChat(msg)
        }.getOrElse {
            return
        }

        messages[parsed.channel]?.update { current ->
            current.replaceOrAddModerationMessage(parsed, scrollBackLength, messageProcessor::onMessageRemoved)
        }
    }

    private fun handleClearMsg(msg: IrcMessage) {
        val parsed = runCatching {
            ModerationMessage.parseClearMessage(msg)
        }.getOrElse {
            return
        }

        messages[parsed.channel]?.update { current ->
            current.replaceWithTimeout(parsed, scrollBackLength, messageProcessor::onMessageRemoved)
        }
    }

    private suspend fun handleWhisper(ircMessage: IrcMessage) {
        if (pubSubManager.connectedAndHasWhisperTopic) {
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
        _whispers.update { current ->
            current.addAndLimit(item, scrollBackLength, messageProcessor::onMessageRemoved)
        }
        _channelMentionCount.increment(WhisperMessage.WHISPER_CHANNEL, 1)
    }

    private suspend fun handleMessage(ircMessage: IrcMessage) {
        if (ircMessage.command == "NOTICE" && ircMessage.tags["msg-id"] in NoticeMessage.ROOM_STATE_CHANGE_MSG_IDS) {
            val channel = ircMessage.params[0].substring(1).toUserName()
            if (eventSubManager.connectedAndHasModerateTopic(channel)) {
                // we get better data from event sub, avoid showing this message
                return
            }
        }

        val userId = ircMessage.tags["user-id"]?.toUserId()
        if (messageProcessor.isUserBlocked(userId)) {
            return
        }

        val rewardId = ircMessage.tags["custom-reward-id"]?.takeIf { it.isNotEmpty() }
        val isAutomodApproval = rewardId != null && knownAutomodHeldIds.remove(rewardId)
        val additionalMessages = when {
            rewardId != null && !isAutomodApproval -> {
                val reward = rewardMutex.withLock {
                    knownRewards[rewardId]
                        ?.also {
                            Log.d(TAG, "Removing known reward $rewardId")
                            knownRewards.remove(rewardId)
                        }
                        ?: run {
                            Log.d(TAG, "Waiting for pubsub reward message with id $rewardId")
                            withTimeoutOrNull(PUBSUB_TIMEOUT) {
                                pubSubManager.messages
                                    .filterIsInstance<PubSubMessage.PointRedemption>()
                                    .first { it.data.reward.id == rewardId }
                            }?.also { knownRewards[rewardId] = it } // mark message as known so default collector does not handle it again
                        }
                }

                reward?.let {
                    val processed = messageProcessor.processReward(PointRedemptionMessage.parsePointReward(it.timestamp, it.data))
                    listOfNotNull(processed?.let(::ChatItem))
                }.orEmpty()
            }

            else                                   -> emptyList()
        }

        val message = runCatching {
            messageProcessor.processIrcMessage(ircMessage) { channel, id ->
                messages[channel]?.value?.find { it.message.id == id }?.message
            }
        }.getOrElse {
            Log.e(TAG, "Failed to parse message", it)
            return
        } ?: return

        if (message is NoticeMessage && usersRepository.isGlobalChannel(message.channel)) {
            messages.keys.forEach {
                messages[it]?.update { current ->
                    current.addAndLimit(ChatItem(message, importance = ChatImportance.SYSTEM), scrollBackLength, messageProcessor::onMessageRemoved)
                }
            }
            return
        }

        if (message is PrivMessage) {
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

        messages[channel]?.update { current ->
            current.addAndLimit(items = additionalMessages + items, scrollBackLength, messageProcessor::onMessageRemoved)
        }

        _notificationsFlow.tryEmit(items)
        val mentions = items
            .filter { it.message.highlights.hasMention() }
            .toMentionTabItems()

        if (mentions.isNotEmpty()) {
            _mentions.update { current ->
                current.addAndLimit(mentions, scrollBackLength, messageProcessor::onMessageRemoved)
            }
        }

        if (channel != activeChannel.value) {
            if (mentions.isNotEmpty()) {
                _channelMentionCount.increment(channel, mentions.size)
            }

            if (message is PrivMessage) {
                val isUnread = _unreadMessagesMap.firstValue[channel] == true
                if (!isUnread) {
                    _unreadMessagesMap.assign(channel, true)
                }
            }
        }
    }

    fun makeAndPostCustomSystemMessage(message: String, channel: UserName) {
        messages[channel]?.update {
            it.addSystemMessage(SystemMessageType.Custom(message), scrollBackLength, messageProcessor::onMessageRemoved)
        }
    }

    fun makeAndPostSystemMessage(type: SystemMessageType, channel: UserName) {
        messages[channel]?.update {
            it.addSystemMessage(type, scrollBackLength, messageProcessor::onMessageRemoved)
        }
    }

    private fun makeAndPostSystemMessage(type: SystemMessageType, channels: Set<UserName> = messages.keys) {
        channels.forEach { channel ->
            val flow = messages[channel] ?: return@forEach
            val current = flow.value
            flow.value = current.addSystemMessage(type, scrollBackLength, messageProcessor::onMessageRemoved) {
                scope.launch {
                    if (chatSettingsDataStore.settings.first().loadMessageHistoryOnReconnect) {
                        loadRecentMessages(channel, isReconnect = true)
                    }
                }
            }
        }
    }

    private fun ConnectionState.toSystemMessageType(): SystemMessageType = when (this) {
        ConnectionState.DISCONNECTED            -> SystemMessageType.Disconnected
        ConnectionState.CONNECTED,
        ConnectionState.CONNECTED_NOT_LOGGED_IN -> SystemMessageType.Connected
    }

    private suspend fun loadRecentMessages(channel: UserName, isReconnect: Boolean = false) {
        val messagesFlow = messages[channel] ?: return
        val result = recentMessagesHandler.load(
            channel = channel,
            isReconnect = isReconnect,
            messagesFlow = messagesFlow,
            scrollBackLength = scrollBackLength,
            onMessageRemoved = messageProcessor::onMessageRemoved,
            onLoadingFailure = { step, throwable -> _chatLoadingFailures.update { it + ChatLoadingFailure(step, throwable) } },
            postSystemMessage = ::makeAndPostSystemMessage,
        )

        if (result.mentionItems.isNotEmpty()) {
            _mentions.update { current ->
                (current + result.mentionItems)
                    .distinctBy { it.message.id }
                    .sortedBy { it.message.timestamp }
            }
        }
        usersRepository.updateUsers(channel, result.userSuggestions)
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

    fun updateAutomodMessageStatus(channel: UserName, heldMessageId: String, status: AutomodMessage.Status) {
        messages[channel]?.update { current ->
            current.map { item ->
                val msg = item.message
                when {
                    msg is AutomodMessage && msg.heldMessageId == heldMessageId ->
                        item.copy(tag = item.tag + 1, message = msg.copy(status = status))

                    else                                                        -> item
                }
            }
        }
    }

    companion object {
        private val TAG = ChatRepository::class.java.simpleName
        private val ESCAPE_TAG = 0x000E0002.codePointAsString

        private const val PUBSUB_TIMEOUT = 5000L

        val ESCAPE_TAG_REGEX = "(?<!$ESCAPE_TAG)$ESCAPE_TAG".toRegex()
        const val ZERO_WIDTH_JOINER = 0x200D.toChar().toString()
    }
}
