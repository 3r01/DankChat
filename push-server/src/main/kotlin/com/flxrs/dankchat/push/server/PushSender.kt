package com.flxrs.dankchat.push.server

import com.flxrs.dankchat.push.PushMessage
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.AndroidConfig
import com.google.firebase.messaging.AndroidConfig.Priority
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.nio.file.Files

interface PushSender {
    suspend fun send(message: PushMessage)
}

class FirebasePushSender(
    private val config: ServerConfig,
    private val stateStore: StateStore,
    private val json: Json = Json,
) : PushSender {
    private val messaging: FirebaseMessaging by lazy {
        val credentials = Files.newInputStream(config.firebaseCredentials).use(GoogleCredentials::fromStream)
        val options = FirebaseOptions.builder().setCredentials(credentials).build()
        val app = FirebaseApp.initializeApp(options, FIREBASE_APP_NAME)
        FirebaseMessaging.getInstance(app)
    }

    override suspend fun send(message: PushMessage) {
        stateStore.state.value.devices.forEach { installationId ->
            try {
                sendToDevice(installationId, message)
            } catch (e: FirebaseMessagingException) {
                if (e.messagingErrorCode?.name == "UNREGISTERED") {
                    stateStore.removeDevice(installationId)
                } else {
                    throw e
                }
            }
        }
    }

    private suspend fun sendToDevice(
        installationId: String,
        message: PushMessage,
    ) = withContext(Dispatchers.IO) {
        val data = mapOf(PAYLOAD_KEY to json.encodeToString(PushMessage.serializer(), message))
        val request =
            Message
                .builder()
                .setFid(installationId)
                .putAllData(data)
                .setAndroidConfig(AndroidConfig.builder().setPriority(Priority.HIGH).build())
                .build()
        messaging.send(request)
    }

    companion object {
        const val PAYLOAD_KEY = "dankchat_push"
        private const val FIREBASE_APP_NAME = "dankchat-push-server"
    }
}
