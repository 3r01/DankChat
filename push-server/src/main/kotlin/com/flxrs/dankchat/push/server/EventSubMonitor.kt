package com.flxrs.dankchat.push.server

import java.util.concurrent.atomic.AtomicReference

class EventSubMonitor {
    private val state = AtomicReference(EventSubHealth())

    fun snapshot(): EventSubHealth = state.get()

    fun markConnecting() {
        state.updateAndGet { current ->
            current.copy(
                connected = false,
                subscriptionCount = 0,
            )
        }
    }

    fun markConnected(subscriptionCount: Int) {
        val now = System.currentTimeMillis()
        state.updateAndGet { current ->
            current.copy(
                connected = true,
                subscriptionCount = subscriptionCount,
                lastConnectedAt = now,
                lastActivityAt = now,
                lastFailure = null,
            )
        }
    }

    fun markActivity() {
        val now = System.currentTimeMillis()
        state.updateAndGet { current -> current.copy(lastActivityAt = now) }
    }

    fun markDisconnected(cause: Throwable?) {
        state.updateAndGet { current ->
            current.copy(
                connected = false,
                subscriptionCount = 0,
                lastFailureAt = cause?.let { System.currentTimeMillis() } ?: current.lastFailureAt,
                lastFailure = cause?.let { it::class.simpleName ?: "UnknownFailure" },
            )
        }
    }
}

data class EventSubHealth(
    val connected: Boolean = false,
    val subscriptionCount: Int = 0,
    val lastConnectedAt: Long? = null,
    val lastActivityAt: Long? = null,
    val lastFailureAt: Long? = null,
    val lastFailure: String? = null,
)
