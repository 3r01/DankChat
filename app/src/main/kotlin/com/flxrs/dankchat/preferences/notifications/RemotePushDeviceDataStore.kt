package com.flxrs.dankchat.preferences.notifications

import android.content.Context
import com.flxrs.dankchat.di.DispatchersProvider
import com.flxrs.dankchat.utils.datastore.createDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.Single

@Single
class RemotePushDeviceDataStore(
    context: Context,
    dispatchersProvider: DispatchersProvider,
) {
    private val dataStore =
        createDataStore(
            fileName = "remote_push_device",
            context = context,
            defaultValue = RemotePushDevice(),
            serializer = RemotePushDevice.serializer(),
            scope = CoroutineScope(dispatchersProvider.io + SupervisorJob()),
        )

    val device = dataStore.data

    suspend fun current() = device.first()

    suspend fun setFirebaseInstallationId(id: String) {
        dataStore.updateData { RemotePushDevice(id) }
    }
}
