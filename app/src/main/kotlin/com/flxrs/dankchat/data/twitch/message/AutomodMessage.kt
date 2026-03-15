package com.flxrs.dankchat.data.twitch.message

import com.flxrs.dankchat.data.DisplayName
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.twitch.badge.Badge

data class AutomodMessage(
    override val timestamp: Long,
    override val id: String,
    override val highlights: Set<Highlight> = emptySet(),
    val channel: UserName,
    val heldMessageId: String,
    val userName: UserName,
    val userDisplayName: DisplayName,
    val messageText: String,
    val reason: String,
    val badges: List<Badge> = emptyList(),
    val color: Int = Message.DEFAULT_COLOR,
    val status: Status = Status.Pending,
) : Message() {

    enum class Status { Pending, Approved, Denied, Expired }
}
