package com.flxrs.dankchat.chat.user

import android.os.Parcelable
import com.flxrs.dankchat.data.DisplayName
import com.flxrs.dankchat.data.UserId
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.twitch.badge.Badge
import kotlinx.parcelize.Parcelize

@Parcelize
data class UserPopupStateParams(
    val targetUserId: UserId,
    val targetUserName: UserName,
    val targetDisplayName: DisplayName,
    val channel: UserName?,
    val isWhisperPopup: Boolean = false,
    val badges: List<Badge> = emptyList(),
) : Parcelable
