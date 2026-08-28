package com.flxrs.dankchat.push.server

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.time.Duration

@OptIn(ExperimentalCoroutinesApi::class)
internal class RestartingWorkerTest {
    @Test
    fun `session cancellation restarts while parent remains active`() =
        runTest {
            var attempts = 0
            val failures = mutableListOf<Throwable>()
            val job =
                launch {
                    runRestartingWorker(Duration.ZERO, failures::add) {
                        if (++attempts == 1) throw CancellationException("session closed")
                        awaitCancellation()
                    }
                }

            runCurrent()

            assertEquals(2, attempts)
            assertEquals(1, failures.size)
            job.cancelAndJoin()
        }

    @Test
    fun `parent cancellation stops without being treated as a failure`() =
        runTest {
            val failures = mutableListOf<Throwable>()
            val job = launch { runRestartingWorker(Duration.ZERO, failures::add) { awaitCancellation() } }
            runCurrent()

            job.cancelAndJoin()

            assertEquals(emptyList(), failures)
        }
}
