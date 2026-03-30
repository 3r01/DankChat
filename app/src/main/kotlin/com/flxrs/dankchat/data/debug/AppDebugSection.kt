package com.flxrs.dankchat.data.debug

import android.os.Debug
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

@Single
class AppDebugSection : DebugSection {
    override val order = 11
    override val baseTitle = "App"

    override fun entries(): Flow<DebugSectionSnapshot> {
        val ticker =
            flow {
                while (true) {
                    emit(Unit)
                    delay(2_000)
                }
            }
        return ticker.map {
            val runtime = Runtime.getRuntime()
            val heapUsed = runtime.totalMemory() - runtime.freeMemory()
            val heapMax = runtime.maxMemory()
            val nativeAllocated = Debug.getNativeHeapAllocatedSize()
            val nativeTotal = Debug.getNativeHeapSize()
            val totalAppMemory = heapUsed + nativeAllocated

            DebugSectionSnapshot(
                title = baseTitle,
                entries =
                    listOf(
                        DebugEntry("Total app memory", formatBytes(totalAppMemory)),
                        DebugEntry("JVM heap", "${formatBytes(heapUsed)} / ${formatBytes(heapMax)}"),
                        DebugEntry("Native heap", "${formatBytes(nativeAllocated)} / ${formatBytes(nativeTotal)}"),
                        DebugEntry("Threads", "${Thread.activeCount()}"),
                    ),
            )
        }
    }

    private fun formatBytes(bytes: Long): String {
        val mb = bytes / (1024.0 * 1024.0)
        return "%.1f MB".format(mb)
    }
}
