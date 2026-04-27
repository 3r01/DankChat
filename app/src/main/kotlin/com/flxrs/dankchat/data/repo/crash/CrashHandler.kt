package com.flxrs.dankchat.data.repo.crash

import android.content.Context
import android.os.Build
import androidx.datastore.core.DataStore
import com.flxrs.dankchat.BuildConfig
import com.flxrs.dankchat.preferences.developer.DeveloperSettingsDataStore
import com.flxrs.dankchat.utils.datastore.createDataStore
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import org.koin.core.annotation.Single
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Instant

private val logger = KotlinLogging.logger("CrashHandler")

@Single(createdAtStart = true)
class CrashHandler(
    context: Context,
    private val developerSettingsDataStore: DeveloperSettingsDataStore,
) : Thread.UncaughtExceptionHandler {
    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    @Suppress("InjectDispatcher")
    val dataStore: DataStore<CrashData> = createDataStore(
        fileName = DATA_STORE_FILE,
        context = context,
        defaultValue = CrashData(),
        serializer = CrashData.serializer(),
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    )

    init {
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(
        thread: Thread,
        throwable: Throwable,
    ) {
        // Always log to file via logback
        logger.error(throwable) { "Uncaught exception on ${thread.name}" }

        // Only persist crash report data when debug mode is enabled
        if (developerSettingsDataStore.current().debugMode) {
            try {
                persistCrash(thread, throwable)
            } catch (_: Throwable) {
                // Best effort — must never throw from the crash handler
            }
        }

        defaultHandler?.uncaughtException(thread, throwable)
    }

    private fun persistCrash(
        thread: Thread,
        throwable: Throwable,
    ) {
        val timestamp = System.currentTimeMillis()
        val stackTrace = StringWriter().also { throwable.printStackTrace(PrintWriter(it)) }.toString()

        var rootCause: Throwable = throwable
        while (rootCause.cause != null) {
            rootCause = rootCause.cause ?: break
        }
        val exceptionHeader = "${rootCause.javaClass.name}: ${rootCause.message.orEmpty()}"

        val fingerprint = computeFingerprint(throwable)
        val entry = CrashEntry(
            id = timestamp,
            fingerprint = fingerprint,
            timestamp = Instant.ofEpochMilli(timestamp).toString(),
            version = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            androidInfo = "${Build.VERSION.SDK_INT} (${Build.VERSION.RELEASE})",
            device = "${Build.MANUFACTURER} ${Build.MODEL}",
            threadName = thread.name,
            exceptionHeader = exceptionHeader,
            stackTrace = stackTrace,
        )

        runBlocking {
            dataStore.updateData { current ->
                val isNew = fingerprint !in current.knownFingerprints
                current.copy(
                    crashes = (listOf(entry) + current.crashes).take(MAX_CRASHES),
                    hasUnshownCrash = isNew,
                    knownFingerprints = current.knownFingerprints + fingerprint,
                )
            }
        }
    }

    private fun computeFingerprint(throwable: Throwable): String {
        val frames = throwable.stackTrace.take(FINGERPRINT_FRAME_COUNT).joinToString("|") { it.toString() }
        val key = "${throwable.javaClass.name}:${throwable.message}|$frames"
        return key.hashCode().toString()
    }

    companion object {
        private const val DATA_STORE_FILE = "crash_data"
        private const val FINGERPRINT_FRAME_COUNT = 5
        private const val MAX_CRASHES = 10
    }
}
