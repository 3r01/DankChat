package com.flxrs.dankchat.ui.login

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class LoginCookieTest {
    @Test
    fun `extracts the Twitch web token from a cookie header`() {
        assertEquals(
            "web-token",
            "unique_id=value; auth-token=web-token; persistent=value".extractCookieValue("auth-token"),
        )
    }

    @Test
    fun `does not confuse similarly named cookies`() {
        assertNull("other-auth-token=value; auth-token-extra=value".extractCookieValue("auth-token"))
    }
}
