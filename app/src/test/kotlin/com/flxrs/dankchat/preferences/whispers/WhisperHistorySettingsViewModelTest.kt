package com.flxrs.dankchat.preferences.whispers

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class WhisperHistorySettingsViewModelTest {
    @Test
    fun `normalizes token formats accepted by the UI`() {
        assertEquals("token", normalizeWebOAuthToken(" token "))
        assertEquals("token", normalizeWebOAuthToken("auth-token=token"))
        assertEquals("token", normalizeWebOAuthToken("OAuth:token"))
    }
}
