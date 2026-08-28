package com.flxrs.dankchat.data.notification

import com.flxrs.dankchat.preferences.notifications.RemotePushDeviceDataStore
import com.flxrs.dankchat.push.PushMessage
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.koin.android.ext.android.inject

class RemotePushMessagingService : FirebaseMessagingService() {
    private val deviceDataStore: RemotePushDeviceDataStore by inject()
    private val notificationManager: RemotePushNotificationManager by inject()

    override fun onRegistered(installationId: String) {
        runBlocking(Dispatchers.IO) { deviceDataStore.setFirebaseInstallationId(installationId) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val payload = message.data[PAYLOAD_KEY] ?: return
        val pushMessage = runCatching { Json.decodeFromString<PushMessage>(payload) }.getOrNull() ?: return
        runBlocking(Dispatchers.IO) { notificationManager.show(pushMessage) }
    }

    companion object {
        private const val PAYLOAD_KEY = "dankchat_push"
    }
}
