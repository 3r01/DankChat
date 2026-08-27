package com.flxrs.dankchat.push.server

class ParticipationTracker(
    private val capacity: Int = 2_000,
) {
    private val roots = LinkedHashSet<String>()

    @Synchronized
    fun participated(
        rootMessageId: String,
        currentUserSentMessage: Boolean,
        replyTargetsCurrentUser: Boolean,
    ): Boolean {
        val participated = replyTargetsCurrentUser || rootMessageId in roots
        if (currentUserSentMessage) {
            roots.remove(rootMessageId)
            roots.add(rootMessageId)
            while (roots.size > capacity) {
                roots.remove(roots.first())
            }
        }
        return participated
    }
}
