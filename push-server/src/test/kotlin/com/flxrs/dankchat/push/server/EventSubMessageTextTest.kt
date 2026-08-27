package com.flxrs.dankchat.push.server

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class EventSubMessageTextTest {
    @Test
    fun `action framing is removed`() {
        assertEquals("waves hello", normalizeEventSubMessageText("\u0001ACTION waves hello\u0001"))
    }

    @Test
    fun `normal messages are unchanged`() {
        assertEquals("ACTION waves hello", normalizeEventSubMessageText("ACTION waves hello"))
    }
}
