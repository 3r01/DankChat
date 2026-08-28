package com.flxrs.dankchat.push

class PushRuleEvaluator(
    private val configuration: PushConfiguration,
) {
    private val currentUserPatterns =
        buildList {
            add(configuration.userName)
            configuration.displayName
                ?.takeUnless { it.equals(configuration.userName, ignoreCase = true) }
                ?.let(::add)
        }.map { value -> "(?<!\\w)${Regex.escape(value)}(?!\\w)".toRegex(RegexOption.IGNORE_CASE) }

    private val messageRules = configuration.rules.messageHighlights.mapNotNull { it.toRegexOrNull() }
    private val blacklistedUsers = configuration.rules.blacklistedUsers.mapNotNull { it.toRegexOrNull() }

    fun shouldNotify(candidate: ChatMessageCandidate): Boolean {
        if (candidate.isSharedChatDuplicate || isBlacklisted(candidate.senderUserName)) {
            return false
        }

        val rules = configuration.rules
        return (rules.notifyOnUsername && !candidate.senderUserName.equals(configuration.userName, ignoreCase = true) && currentUserPatterns.any(candidate.text::contains)) ||
            (rules.notifyOnParticipatedReply && candidate.participatedReply && !candidate.senderUserName.equals(configuration.userName, ignoreCase = true)) ||
            messageRules.any(candidate.text::contains) ||
            rules.userHighlights.any { candidate.senderUserName.equals(it, ignoreCase = true) } ||
            rules.badgeHighlights.any { highlighted -> candidate.badges.any { badge -> badgeMatches(highlighted, badge) } }
    }

    private fun isBlacklisted(sender: String): Boolean = blacklistedUsers.any(sender::matches)

    private fun badgeMatches(
        highlighted: String,
        actual: String,
    ): Boolean =
        when {
            highlighted.contains('/') -> actual == highlighted
            else -> actual.startsWith("$highlighted/")
        }

    private fun MessageHighlightRule.toRegexOrNull(): Regex? =
        runCatching {
            val options = if (isCaseSensitive) emptySet() else setOf(RegexOption.IGNORE_CASE)
            if (isRegex) {
                pattern.toRegex(options)
            } else {
                "(?<!\\w)${Regex.escape(pattern)}(?!\\w)".toRegex(options)
            }
        }.getOrNull()

    private fun BlacklistedUserRule.toRegexOrNull(): Regex? =
        runCatching {
            if (isRegex) pattern.toRegex(RegexOption.IGNORE_CASE) else Regex("^${Regex.escape(pattern)}$", RegexOption.IGNORE_CASE)
        }.getOrNull()
}
