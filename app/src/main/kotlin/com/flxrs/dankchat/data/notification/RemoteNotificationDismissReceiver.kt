package com.flxrs.dankchat.data.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.flxrs.dankchat.di.DispatchersProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class RemoteNotificationDismissReceiver :
    BroadcastReceiver(),
    KoinComponent {
    private val notifications: RemotePushNotificationManager by inject()
    private val dispatchersProvider: DispatchersProvider by inject()

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val result = goAsync()
        CoroutineScope(SupervisorJob() + dispatchersProvider.io).launch {
            try {
                notifications.handleDismiss(intent)
            } finally {
                result.finish()
            }
        }
    }
}
