package com.flxrs.dankchat.ui.chat

import android.app.Application
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertSame

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ChatScrollStatesTest {
    @Test
    fun `different circular pages for the same channel share scroll state`() {
        val states = ChatScrollStates()
        val forsen = states.forKey("forsen")
        forsen.shouldAutoScroll = false

        assertSame(forsen, states.forKey("forsen"))
        assertFalse(states.forKey("forsen").shouldAutoScroll)
        assertNotSame(forsen, states.forKey("Iore"))
    }

    @Test
    fun `removing a channel resets only its scroll state`() {
        val states = ChatScrollStates()
        val forsen = states.forKey("forsen")
        val iore = states.forKey("Iore")

        states.retain(setOf("forsen"))

        assertSame(forsen, states.forKey("forsen"))
        assertNotSame(iore, states.forKey("Iore"))
    }

    @Test
    fun `saved scroll position includes its offset and auto scroll state`() {
        val original = ChatScrollState(firstVisibleItemIndex = 12, firstVisibleItemScrollOffset = 34, shouldAutoScroll = false)

        val restored = ChatScrollState.fromSaveable(original.toSaveable())

        assertEquals(12, restored.listState.firstVisibleItemIndex)
        assertEquals(34, restored.listState.firstVisibleItemScrollOffset)
        assertFalse(restored.shouldAutoScroll)
    }
}
