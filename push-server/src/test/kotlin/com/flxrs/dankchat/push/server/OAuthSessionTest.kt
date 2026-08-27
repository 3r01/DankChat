package com.flxrs.dankchat.push.server

import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

internal class OAuthSessionTest {
    @Test
    fun `state is single use`() {
        val session = OAuthSession()
        val state = session.begin()

        assertTrue(session.consume(state))
        assertFalse(session.consume(state))
    }

    @Test
    fun `beginning again invalidates prior state`() {
        val session = OAuthSession()
        val first = session.begin()
        val second = session.begin()

        assertNotEquals(first, second)
        assertFalse(session.consume(first))
    }
}
