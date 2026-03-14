package com.flxrs.dankchat.chat.search

object ChatSearchFilterParser {

    fun parse(query: String): List<ChatSearchFilter> {
        if (query.isBlank()) return emptyList()

        return query.trim().split("\\s+".toRegex()).mapNotNull { token ->
            parseToken(token)
        }
    }

    private fun parseToken(token: String): ChatSearchFilter? {
        if (token.isBlank()) return null

        val (negate, raw) = extractNegation(token)
        val colonIndex = raw.indexOf(':')

        if (colonIndex > 0) {
            val prefix = raw.substring(0, colonIndex).lowercase()
            val value = raw.substring(colonIndex + 1)

            when (prefix) {
                "from" -> {
                    if (value.isNotEmpty()) {
                        return ChatSearchFilter.Author(name = value, negate = negate)
                    }
                }
                "has" -> {
                    return when (value.lowercase()) {
                        "link" -> ChatSearchFilter.HasLink(negate = negate)
                        "emote" -> ChatSearchFilter.HasEmote(emoteName = null, negate = negate)
                        else -> {
                            if (value.isNotEmpty()) {
                                ChatSearchFilter.HasEmote(emoteName = value, negate = negate)
                            } else {
                                null
                            }
                        }
                    }
                }
                "badge" -> {
                    if (value.isNotEmpty()) {
                        return ChatSearchFilter.BadgeFilter(badgeName = value.lowercase(), negate = negate)
                    }
                }
            }
        }

        return ChatSearchFilter.Text(query = raw, negate = negate)
    }

    private fun extractNegation(token: String): Pair<Boolean, String> {
        return when {
            token.startsWith('!') || token.startsWith('-') -> true to token.substring(1)
            else -> false to token
        }
    }
}
