package com.flxrs.dankchat.push.server

import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class ParticipationTrackerTest {
    @Test
    fun `tracks threads after current user participates`() {
        val tracker = ParticipationTracker()

        assertFalse(tracker.participated("root", currentUserSentMessage = false, replyTargetsCurrentUser = false))
        tracker.participated("root", currentUserSentMessage = true, replyTargetsCurrentUser = false)
        assertTrue(tracker.participated("root", currentUserSentMessage = false, replyTargetsCurrentUser = false))
    }

    @Test
    fun `direct reply is participating immediately`() {
        assertTrue(ParticipationTracker().participated("root", currentUserSentMessage = false, replyTargetsCurrentUser = true))
    }
}
