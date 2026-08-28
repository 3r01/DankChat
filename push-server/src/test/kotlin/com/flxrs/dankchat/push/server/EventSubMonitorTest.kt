package com.flxrs.dankchat.push.server

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class EventSubMonitorTest {
    @Test
    fun `connected state records subscriptions and activity`() {
        val monitor = EventSubMonitor()

        monitor.markConnected(subscriptionCount = 4)

        val health = monitor.snapshot()
        assertTrue(health.connected)
        assertEquals(4, health.subscriptionCount)
        assertNotNull(health.lastConnectedAt)
        assertNotNull(health.lastActivityAt)
        assertNull(health.lastFailure)
    }

    @Test
    fun `failure marks connection unhealthy and preserves failure details`() {
        val monitor = EventSubMonitor()
        monitor.markConnected(subscriptionCount = 4)

        monitor.markDisconnected(IllegalStateException("connection closed"))

        val health = monitor.snapshot()
        assertFalse(health.connected)
        assertEquals(0, health.subscriptionCount)
        assertNotNull(health.lastFailureAt)
        assertEquals("IllegalStateException", health.lastFailure)
    }

    @Test
    fun `reconnection clears the previous failure`() {
        val monitor = EventSubMonitor()
        monitor.markDisconnected(IllegalStateException("connection closed"))

        monitor.markConnected(subscriptionCount = 4)

        assertNull(monitor.snapshot().lastFailure)
    }
}
