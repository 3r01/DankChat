package com.flxrs.dankchat.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.util.LruCache
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.content.LocusIdCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import coil3.asDrawable
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.size.Size
import com.flxrs.dankchat.R
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.auth.AuthDataStore
import com.flxrs.dankchat.data.repo.channel.ChannelRepository
import com.flxrs.dankchat.data.toUserId
import com.flxrs.dankchat.data.toUserName
import com.flxrs.dankchat.push.PushMessage
import com.flxrs.dankchat.push.PushMessageKind
import com.flxrs.dankchat.ui.main.MainActivity
import com.flxrs.dankchat.utils.AppLifecycleListener
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import org.koin.core.annotation.Single

private val remoteNotificationLogger = KotlinLogging.logger("RemotePushNotificationManager")

@Single
class RemotePushNotificationManager(
    private val context: Context,
    private val authDataStore: AuthDataStore,
    private val channelRepository: ChannelRepository,
    private val appLifecycleListener: AppLifecycleListener,
    private val store: RemoteNotificationStore,
) {
    private val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val icons = LruCache<String, Bitmap>(MAX_NOTIFICATION_ICONS)
    private val appIcon by lazy {
        context.applicationInfo.loadIcon(context.packageManager).toBitmap(ICON_SIZE, ICON_SIZE, Bitmap.Config.ARGB_8888)
    }

    suspend fun show(message: PushMessage) {
        if (appLifecycleListener.appState.value == AppLifecycleListener.AppLifecycle.Foreground) return
        createNotificationChannel()
        val history = store.add(message)
        when (message.kind) {
            PushMessageKind.Mention -> showMention(message, history)
            PushMessageKind.Whisper -> showWhisper(message, history)
        }
    }

    suspend fun clear(channel: UserName) {
        if (channel == UserName.EMPTY) {
            store
                .clearWhispers()
                .map { it.senderUserName.lowercase() }
                .distinct()
                .forEach { manager.cancel(whisperTag(it), WHISPER_ID) }
        } else {
            val removed = store.clearChannel(channel.value)
            removed.forEach { manager.cancel(messageTag(it.messageId), MESSAGE_ID) }
            manager.cancel(channelSummaryTag(channel.value), CHANNEL_SUMMARY_ID)
        }
    }

    suspend fun handleDismiss(intent: Intent) {
        when (intent.action) {
            DISMISS_MESSAGE -> {
                val messageId = intent.getStringExtra(EXTRA_MESSAGE_ID) ?: return
                val channel = intent.getStringExtra(EXTRA_CHANNEL) ?: return
                val state = store.remove(messageId)
                updateChannelSummary(channel, state.messages.filterMentions(channel))
            }

            DISMISS_CHANNEL -> intent.getStringExtra(EXTRA_CHANNEL)?.let { clear(it.toUserName()) }

            DISMISS_WHISPER -> intent.getStringExtra(EXTRA_SENDER)?.let { sender ->
                store.clearWhispers(sender)
                manager.cancel(whisperTag(sender), WHISPER_ID)
            }
        }
    }

    private suspend fun showMention(
        message: PushMessage,
        history: List<PushMessage>,
    ) {
        val channel = message.channelName ?: return
        val senderBitmap = senderIcon(message)
        val sender = message.senderPerson(senderBitmap)
        val conversationIcon = channelIcon(channel)
        val shortcutId = channelShortcutId(channel)
        publishShortcut(shortcutId, "#$channel", history.map { it.senderPerson() }.distinctBy { it.key }, openChannelIntent(channel), conversationIcon)

        val style = NotificationCompat
            .MessagingStyle(currentUserPerson())
            .setConversationTitle("#$channel")
            .setGroupConversation(true)
            .addMessage(message.text, message.timestamp, sender)
        val notification = NotificationCompat
            .Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.notification_mention, message.senderUserName, channel))
            .setContentText(message.text)
            .setContentIntent(openChannelPendingIntent(message, includeMessage = true))
            .setDeleteIntent(dismissMessagePendingIntent(message))
            .setSmallIcon(R.drawable.ic_notification_icon)
            .setLargeIcon(senderBitmap)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setGroup(channelGroup(channel))
            .setShortcutId(shortcutId)
            .setLocusId(LocusIdCompat(shortcutId))
            .setStyle(style)
            .setAutoCancel(true)
            .build()
        manager.notify(messageTag(message.messageId), MESSAGE_ID, notification)
        updateChannelSummary(channel, history, conversationIcon)
    }

    private suspend fun updateChannelSummary(
        channel: String,
        history: List<PushMessage>,
        suppliedIcon: Bitmap? = null,
    ) {
        if (history.isEmpty()) {
            manager.cancel(channelSummaryTag(channel), CHANNEL_SUMMARY_ID)
            return
        }
        val latest = history.last()
        val icon = suppliedIcon ?: channelIcon(channel)
        val style = NotificationCompat.MessagingStyle(currentUserPerson()).setConversationTitle("#$channel").setGroupConversation(true)
        history.takeLast(NotificationCompat.MessagingStyle.MAXIMUM_RETAINED_MESSAGES).forEach { message ->
            style.addMessage(message.text, message.timestamp, message.senderPerson(senderIcon(message)))
        }
        val shortcutId = channelShortcutId(channel)
        val summary = NotificationCompat
            .Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.notification_channel_mentions, channel))
            .setContentText(context.getString(R.string.notification_message_with_sender, latest.senderLabel(), latest.text))
            .setContentIntent(openChannelPendingIntent(latest, includeMessage = false))
            .setDeleteIntent(dismissChannelPendingIntent(channel))
            .setSmallIcon(R.drawable.ic_notification_icon)
            .setLargeIcon(icon)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setGroup(channelGroup(channel))
            .setGroupSummary(true)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
            .setOnlyAlertOnce(true)
            .setShortcutId(shortcutId)
            .setLocusId(LocusIdCompat(shortcutId))
            .setStyle(style)
            .setAutoCancel(true)
            .build()
        manager.notify(channelSummaryTag(channel), CHANNEL_SUMMARY_ID, summary)
    }

    private suspend fun showWhisper(
        message: PushMessage,
        history: List<PushMessage>,
    ) {
        val senderKey = message.senderUserName.lowercase()
        val icon = senderIcon(message)
        val sender = message.senderPerson(icon)
        val shortcutId = whisperShortcutId(message)
        val title = context.getString(R.string.notification_whisper_conversation_title, message.senderDisplayName)
        publishShortcut(shortcutId, title, listOf(sender), openWhisperIntent(message.senderUserName), icon)
        val style = NotificationCompat.MessagingStyle(currentUserPerson())
        history.takeLast(NotificationCompat.MessagingStyle.MAXIMUM_RETAINED_MESSAGES).forEach {
            style.addMessage(it.text, it.timestamp, it.senderPerson(if (it.senderUserId == message.senderUserId) icon else null))
        }
        val notification = NotificationCompat
            .Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.notification_whisper_mention, message.senderUserName))
            .setContentText(message.text)
            .setSubText(context.getString(R.string.whispers))
            .setContentIntent(openWhisperPendingIntent(message.senderUserName))
            .setDeleteIntent(dismissWhisperPendingIntent(senderKey))
            .setSmallIcon(R.drawable.ic_notification_icon)
            .setLargeIcon(icon)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setShortcutId(shortcutId)
            .setLocusId(LocusIdCompat(shortcutId))
            .setStyle(style)
            .setAutoCancel(true)
            .build()
        manager.notify(whisperTag(senderKey), WHISPER_ID, notification)
    }

    private fun createNotificationChannel() {
        manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Mentions", NotificationManager.IMPORTANCE_DEFAULT))
    }

    private fun publishShortcut(
        id: String,
        label: String,
        people: List<Person>,
        intent: Intent,
        icon: Bitmap,
    ) {
        runCatching {
            ShortcutManagerCompat.pushDynamicShortcut(
                context,
                ShortcutInfoCompat
                    .Builder(context, id)
                    .setShortLabel(label)
                    .setLongLabel(label)
                    .setIcon(IconCompat.createWithBitmap(icon))
                    .setIntent(intent)
                    .setPersons(people.toTypedArray())
                    .setIsConversation()
                    .build(),
            )
        }.onFailure { remoteNotificationLogger.warn(it) { "Failed to publish remote notification shortcut" } }
    }

    private suspend fun channelIcon(channel: String) = loadIcon("channel:$channel") {
        channelRepository.getUserDtoByName(channel.toUserName())?.avatarUrl
    }

    private suspend fun senderIcon(message: PushMessage) = loadIcon("user:${message.senderUserId}") {
        channelRepository.getUserDto(message.senderUserId.toUserId())?.avatarUrl
            ?: channelRepository.getUserDtoByName(message.senderUserName.toUserName())?.avatarUrl
    }

    private suspend fun loadIcon(
        key: String,
        url: suspend () -> String?,
    ): Bitmap {
        icons[key]?.let { return it }
        val imageUrl = try {
            url()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
        if (imageUrl.isNullOrBlank()) return appIcon
        return try {
            val request = ImageRequest
                .Builder(context)
                .data(imageUrl)
                .allowHardware(false)
                .size(Size(ICON_SIZE, ICON_SIZE))
                .build()
            context.imageLoader
                .execute(request)
                .image
                ?.asDrawable(context.resources)
                ?.toBitmap(ICON_SIZE, ICON_SIZE, Bitmap.Config.ARGB_8888)
                ?.also { icons.put(key, it) } ?: appIcon
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            appIcon
        }
    }

    private suspend fun currentUserPerson(): Person {
        val id = authDataStore.userIdString?.value
        val name = authDataStore.displayName?.value ?: authDataStore.userName?.value ?: context.getString(R.string.app_name)
        val icon = id?.let { loadIcon("user:$it") { channelRepository.getUserDto(it.toUserId())?.avatarUrl } }
        return Person
            .Builder()
            .setName(name)
            .setKey(id ?: name)
            .apply { icon?.let { setIcon(IconCompat.createWithBitmap(it)) } }
            .build()
    }

    private fun PushMessage.senderPerson(icon: Bitmap? = null) = Person
        .Builder()
        .setName(senderLabel())
        .setKey(senderUserId)
        .apply { icon?.let { setIcon(IconCompat.createWithBitmap(it)) } }
        .build()

    private fun PushMessage.senderLabel() = if (senderDisplayName.equals(senderUserName, ignoreCase = true)) senderUserName else "$senderDisplayName ($senderUserName)"

    private fun openChannelPendingIntent(
        message: PushMessage,
        includeMessage: Boolean,
    ) = PendingIntent.getActivity(
        context,
        message.messageId.hashCode(),
        openChannelIntent(message.channelName.orEmpty()).apply { if (includeMessage) putExtra(MainActivity.OPEN_MESSAGE_KEY, message.messageId) },
        PENDING_INTENT_FLAGS,
    )

    private fun openWhisperPendingIntent(sender: String) = PendingIntent.getActivity(context, sender.hashCode(), openWhisperIntent(sender), PENDING_INTENT_FLAGS)

    private fun openChannelIntent(channel: String) = Intent(context, MainActivity::class.java).apply {
        action = Intent.ACTION_VIEW
        putExtra(MainActivity.OPEN_CHANNEL_KEY, channel)
    }

    private fun openWhisperIntent(sender: String) = openChannelIntent("").apply { putExtra(MainActivity.OPEN_WHISPER_TARGET_KEY, sender) }

    private fun dismissMessagePendingIntent(message: PushMessage) = dismissPendingIntent(DISMISS_MESSAGE, message.messageId.hashCode()) {
        putExtra(EXTRA_MESSAGE_ID, message.messageId)
        putExtra(EXTRA_CHANNEL, message.channelName)
    }

    private fun dismissChannelPendingIntent(channel: String) = dismissPendingIntent(DISMISS_CHANNEL, channel.hashCode()) { putExtra(EXTRA_CHANNEL, channel) }

    private fun dismissWhisperPendingIntent(sender: String) = dismissPendingIntent(DISMISS_WHISPER, sender.hashCode()) { putExtra(EXTRA_SENDER, sender) }

    private fun dismissPendingIntent(
        actionName: String,
        requestCode: Int,
        configure: Intent.() -> Unit,
    ) = PendingIntent.getBroadcast(
        context,
        requestCode,
        Intent(context, RemoteNotificationDismissReceiver::class.java).apply {
            action = actionName
            configure()
        },
        PENDING_INTENT_FLAGS,
    )

    private fun List<PushMessage>.filterMentions(channel: String) = filter {
        it.kind == PushMessageKind.Mention && it.channelName.equals(channel, ignoreCase = true)
    }.takeLast(NotificationCompat.MessagingStyle.MAXIMUM_RETAINED_MESSAGES)

    private fun messageTag(id: String) = "remote_message_$id"

    private fun channelSummaryTag(channel: String) = "remote_channel_summary_${channel.lowercase()}"

    private fun whisperTag(sender: String) = "remote_whisper_${sender.lowercase()}"

    private fun channelGroup(channel: String) = "remote_channel_${channel.lowercase()}"

    private fun channelShortcutId(channel: String) = "channel_${channel.lowercase()}"

    private fun whisperShortcutId(message: PushMessage) = "whisper_${message.senderUserId}"

    private companion object {
        const val CHANNEL_ID = "com.flxrs.dankchat.very_dank_id"
        const val MESSAGE_ID = 34567
        const val CHANNEL_SUMMARY_ID = 12345
        const val WHISPER_ID = 23456
        const val ICON_SIZE = 128
        const val MAX_NOTIFICATION_ICONS = 100
        const val DISMISS_MESSAGE = "com.flxrs.dankchat.remote.DISMISS_MESSAGE"
        const val DISMISS_CHANNEL = "com.flxrs.dankchat.remote.DISMISS_CHANNEL"
        const val DISMISS_WHISPER = "com.flxrs.dankchat.remote.DISMISS_WHISPER"
        const val EXTRA_MESSAGE_ID = "message_id"
        const val EXTRA_CHANNEL = "channel"
        const val EXTRA_SENDER = "sender"
        val PENDING_INTENT_FLAGS = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    }
}
