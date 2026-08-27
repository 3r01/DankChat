package com.flxrs.dankchat.push.server

import com.flxrs.dankchat.push.PushConfiguration
import com.flxrs.dankchat.push.PushNotificationRules
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class StateStoreTest {
    @TempDir
    lateinit var directory: Path

    @Test
    fun `persists state and rejects stale configuration revisions`() =
        kotlinx.coroutines.test.runTest {
            val path = directory.resolve("state.json")
            val store = StateStore(path)

            assertTrue(store.updateConfiguration(configuration(2)))
            assertFalse(store.updateConfiguration(configuration(1)))
            store.addDevice("device")
            store.updateTwitchTokens(TwitchTokens("user", "access", "refresh"))

            val restored = StateStore(path).state.value
            assertEquals(2, restored.configuration?.revision)
            assertEquals(setOf("device"), restored.devices)
            assertEquals("refresh", restored.twitchTokens?.refreshToken)
        }

    private fun configuration(revision: Long) =
        PushConfiguration(
            revision = revision,
            twitchUserId = "1",
            userName = "user",
            notifyWhispers = true,
            channels = emptyList(),
            rules = PushNotificationRules(false, false, emptyList(), emptyList(), emptyList(), emptyList()),
        )
}
