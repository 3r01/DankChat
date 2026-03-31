package com.flxrs.dankchat.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.getSystemService
import androidx.media.app.NotificationCompat.MediaStyle
import com.flxrs.dankchat.R
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.repo.chat.ChatNotificationRepository
import com.flxrs.dankchat.data.repo.data.DataRepository
import com.flxrs.dankchat.preferences.notifications.NotificationsSettingsDataStore
import com.flxrs.dankchat.ui.main.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import kotlin.concurrent.atomics.AtomicInt
import kotlin.coroutines.CoroutineContext

class NotificationService :
    Service(),
    CoroutineScope {
    private val binder = LocalBinder()
    private val manager: NotificationManager by lazy { getSystemService(NOTIFICATION_SERVICE) as NotificationManager }

    private var notificationsEnabled = false

    private var notificationsJob: Job? = null
    private val notifications = mutableMapOf<UserName, MutableList<Int>>()
    private val notifiedMessageIds = LinkedHashSet<String>()

    private val chatNotificationRepository: ChatNotificationRepository by inject()
    private val dataRepository: DataRepository by inject()
    private val notificationsSettingsDataStore: NotificationsSettingsDataStore by inject()

    // minSdk 30 guarantees PendingIntent.FLAG_IMMUTABLE support (API 23+)
    private val pendingIntentFlag: Int = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

    private var shouldNotifyOnMention = false

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + Job()

    inner class LocalBinder(
        val service: NotificationService = this@NotificationService,
    ) : Binder()

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        coroutineContext.cancelChildren()
        manager.cancelAll()

        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
        super.onDestroy()
    }

    override fun onCreate() {
        super.onCreate()
        // minSdk 30 guarantees notification channel support (API 26+)
        val name = getString(R.string.app_name)
        val channel =
            NotificationChannel(CHANNEL_ID_LOW, name, NotificationManager.IMPORTANCE_LOW).apply {
                enableVibration(false)
                enableLights(false)
                setShowBadge(false)
            }

        val mentionChannel = NotificationChannel(CHANNEL_ID_DEFAULT, "Mentions", NotificationManager.IMPORTANCE_DEFAULT)
        manager.createNotificationChannel(mentionChannel)
        manager.createNotificationChannel(channel)

        notificationsSettingsDataStore.showNotifications
            .onEach { notificationsEnabled = it }
            .launchIn(this)
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        when (intent?.action) {
            STOP_COMMAND -> launch { dataRepository.sendShutdownCommand() }
            else -> startForeground()
        }

        return START_NOT_STICKY
    }

    override fun onTimeout(
        startId: Int,
        fgsType: Int,
    ) {
        Log.w(TAG, "Stopping foreground service due to 6h timeout restriction..")
        stopSelf()
    }

    fun clearNotificationsForChannel(channel: UserName) {
        val ids = notifications.remove(channel)
        ids?.forEach { manager.cancel(it) }

        if (notifications.isEmpty()) {
            manager.cancel(SUMMARY_NOTIFICATION_ID)
            manager.cancelAll()
        }
    }

    fun enableNotifications() {
        shouldNotifyOnMention = true
    }

    private fun startForeground() {
        val title = getString(R.string.notification_title)
        val message = getString(R.string.notification_message)

        val pendingStartActivityIntent =
            Intent(this, MainActivity::class.java).let {
                PendingIntent.getActivity(this, NOTIFICATION_START_INTENT_CODE, it, pendingIntentFlag)
            }

        val pendingStopIntent =
            Intent(this, NotificationService::class.java).let {
                it.action = STOP_COMMAND
                PendingIntent.getService(this, NOTIFICATION_STOP_INTENT_CODE, it, pendingIntentFlag)
            }

        val notification =
            NotificationCompat
                .Builder(this, CHANNEL_ID_LOW)
                .setSound(null)
                .setVibrate(null)
                .setContentTitle(title)
                .setContentText(message)
                .addAction(R.drawable.ic_clear, getString(R.string.notification_stop), pendingStopIntent)
                .setStyle(MediaStyle().setShowActionsInCompactView(0)) // minSdk 30 guarantees MediaStyle support
                .setContentIntent(pendingStartActivityIntent)
                .setSmallIcon(R.drawable.ic_notification_icon)
                .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    fun checkForNotification() {
        shouldNotifyOnMention = false
        notifiedMessageIds.clear()

        notificationsJob?.cancel()
        notificationsJob =
            launch {
                chatNotificationRepository.messageUpdates.collect { items ->
                    if (!shouldNotifyOnMention || !notificationsEnabled) {
                        return@collect
                    }

                    items.forEach { (message) ->
                        if (!notifiedMessageIds.add(message.id)) {
                            return@forEach
                        }
                        if (notifiedMessageIds.size > MAX_NOTIFIED_IDS) {
                            val iterator = notifiedMessageIds.iterator()
                            iterator.next()
                            iterator.remove()
                        }
                        message.toNotificationData()?.createMentionNotification()
                    }
                }
            }
    }

    private fun NotificationData.createMentionNotification() {
        val pendingStartActivityIntent =
            Intent(this@NotificationService, MainActivity::class.java).let {
                it.putExtra(MainActivity.OPEN_CHANNEL_KEY, channel)
                PendingIntent.getActivity(this@NotificationService, notificationIntentCode.fetchAndAdd(1), it, pendingIntentFlag)
            }

        val summary =
            NotificationCompat
                .Builder(this@NotificationService, CHANNEL_ID_DEFAULT)
                .setContentTitle(getString(R.string.notification_new_mentions))
                .setContentText("")
                .setSmallIcon(R.drawable.ic_notification_icon)
                .setGroup(MENTION_GROUP)
                .setGroupSummary(true)
                .setAutoCancel(true)
                .build()

        val title =
            when {
                isWhisper -> getString(R.string.notification_whisper_mention, name)
                isNotify -> getString(R.string.notification_notify_mention, channel)
                else -> getString(R.string.notification_mention, name, channel)
            }

        val notification =
            NotificationCompat
                .Builder(this@NotificationService, CHANNEL_ID_DEFAULT)
                .setContentTitle(title)
                .setContentText(message)
                .setContentIntent(pendingStartActivityIntent)
                .setSmallIcon(R.drawable.ic_notification_icon)
                .setGroup(MENTION_GROUP)
                .build()

        val id = notificationId.fetchAndAdd(1)
        notifications.getOrPut(channel) { mutableListOf() } += id

        manager.notify(id, notification)
        manager.notify(SUMMARY_NOTIFICATION_ID, summary)
    }

    companion object {
        private val TAG = NotificationService::class.simpleName

        private const val CHANNEL_ID_LOW = "com.flxrs.dankchat.dank_id"
        private const val CHANNEL_ID_DEFAULT = "com.flxrs.dankchat.very_dank_id"
        private const val NOTIFICATION_ID = 77777
        private const val NOTIFICATION_START_INTENT_CODE = 66666
        private const val NOTIFICATION_STOP_INTENT_CODE = 55555
        private const val SUMMARY_NOTIFICATION_ID = 12345
        private const val MENTION_GROUP = "dank_group"
        private const val STOP_COMMAND = "STOP_DANKING"

        private const val MAX_NOTIFIED_IDS = 500

        private val notificationId = AtomicInt(42)
        private val notificationIntentCode = AtomicInt(420)
    }
}
