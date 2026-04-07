package com.flxrs.dankchat.ui.chat

/**
 * Assigns stable ordinals to messages by ID, so that a message's even/odd
 * status never changes when other messages are added or evicted from the list.
 *
 * Each display context (ViewModel) should use its own tracker instance.
 */
class CheckeredMessageTracker {
    private val ordinals = HashMap<String, Long>()
    private var nextOrdinal = 0L

    fun isAlternate(id: String): Boolean {
        val ordinal = ordinals.getOrPut(id) { nextOrdinal++ }
        return ordinal % 2 == 0L
    }
}
