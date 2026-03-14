package com.flxrs.dankchat.chat.emotemenu

import androidx.compose.runtime.Immutable

@Immutable
data class EmoteMenuTabItem(val type: EmoteMenuTab, val items: List<EmoteItem>)
