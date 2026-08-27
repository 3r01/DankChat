package com.flxrs.dankchat.push

import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class PushRuleEvaluatorTest {
    @Test
    fun `matches username and display name at word boundaries`() {
        val evaluator = evaluator()

        assertTrue(evaluator.shouldNotify(candidate("hello qbit")))
        assertTrue(evaluator.shouldNotify(candidate("hello Q Bit")))
        assertFalse(evaluator.shouldNotify(candidate("hello qbits")))
    }

    @Test
    fun `does not notify for own mention or shared chat duplicate`() {
        val evaluator = evaluator()

        assertFalse(evaluator.shouldNotify(candidate("qbit", sender = "qbit")))
        assertFalse(evaluator.shouldNotify(candidate("qbit", shared = true)))
    }

    @Test
    fun `matches participated replies and configured message user and badge rules`() {
        val evaluator = evaluator()

        assertTrue(evaluator.shouldNotify(candidate("ordinary", participatedReply = true)))
        assertTrue(evaluator.shouldNotify(candidate("a PING appeared")))
        assertTrue(evaluator.shouldNotify(candidate("ordinary", sender = "friend")))
        assertTrue(evaluator.shouldNotify(candidate("ordinary", badges = listOf("moderator/1"))))
    }

    @Test
    fun `blacklist takes precedence over every highlight`() {
        val evaluator = evaluator()

        assertFalse(
            evaluator.shouldNotify(
                candidate(
                    text = "qbit ping",
                    sender = "blocked_user",
                    badges = listOf("moderator/1"),
                    participatedReply = true,
                ),
            ),
        )
    }

    @Test
    fun `invalid regular expressions are ignored`() {
        val configuration =
            configuration().copy(
                rules =
                    configuration().rules.copy(
                        messageHighlights = listOf(MessageHighlightRule("[", isRegex = true, isCaseSensitive = false)),
                    ),
            )

        assertFalse(PushRuleEvaluator(configuration).shouldNotify(candidate("anything")))
    }

    private fun evaluator() = PushRuleEvaluator(configuration())

    private fun configuration() =
        PushConfiguration(
            revision = 1,
            twitchUserId = "1",
            userName = "qbit",
            displayName = "Q Bit",
            notifyWhispers = true,
            channels = listOf(PushChannel("2", "forsen")),
            rules =
                PushNotificationRules(
                    notifyOnUsername = true,
                    notifyOnParticipatedReply = true,
                    messageHighlights = listOf(MessageHighlightRule("ping", isRegex = false, isCaseSensitive = false)),
                    userHighlights = listOf("friend"),
                    badgeHighlights = listOf("moderator"),
                    blacklistedUsers = listOf(BlacklistedUserRule("blocked_.*", isRegex = true)),
                ),
        )

    private fun candidate(
        text: String,
        sender: String = "sender",
        badges: List<String> = emptyList(),
        participatedReply: Boolean = false,
        shared: Boolean = false,
    ) = ChatMessageCandidate(
        senderUserName = sender,
        text = text,
        badges = badges,
        participatedReply = participatedReply,
        isSharedChatDuplicate = shared,
    )
}
