package com.flxrs.dankchat.ui.chat.emote

import com.flxrs.dankchat.R
import com.flxrs.dankchat.data.twitch.emote.EmoteType
import com.flxrs.dankchat.data.twitch.emote.GenericEmote

private const val SEVEN_TV_BASE_LINK = "https://7tv.app/emotes/"
private const val FFZ_BASE_LINK = "https://www.frankerfacez.com/emoticon/"
private const val BTTV_BASE_LINK = "https://betterttv.com/emotes/"
private const val TWITCH_BASE_LINK = "https://chatvau.lt/emote/twitch/"

fun GenericEmote.toEmoteInfoItem(): EmoteInfoItem {
    val type = emoteType
    return EmoteInfoItem(
        id = id,
        name = code,
        imageUrl = url,
        baseName = when (type) {
            is EmoteType.ChannelSevenTVEmote -> type.baseName
            is EmoteType.GlobalSevenTVEmote -> type.baseName
            else -> null
        },
        creatorName = when (type) {
            is EmoteType.ChannelSevenTVEmote -> type.creator
            is EmoteType.GlobalSevenTVEmote -> type.creator
            is EmoteType.ChannelBTTVEmote -> type.creator
            is EmoteType.ChannelFFZEmote -> type.creator
            is EmoteType.GlobalFFZEmote -> type.creator
            else -> null
        },
        providerUrl = when (type) {
            is EmoteType.ChannelSevenTVEmote,
            is EmoteType.GlobalSevenTVEmote,
            -> "$SEVEN_TV_BASE_LINK$id"

            is EmoteType.ChannelBTTVEmote,
            EmoteType.GlobalBTTVEmote,
            -> "$BTTV_BASE_LINK$id"

            is EmoteType.ChannelFFZEmote,
            is EmoteType.GlobalFFZEmote,
            -> "$FFZ_BASE_LINK$id-$code"

            else -> "$TWITCH_BASE_LINK$id"
        },
        isZeroWidth = isOverlayEmote,
        emoteType = when (type) {
            is EmoteType.ChannelBTTVEmote -> if (type.isShared) R.string.emote_sheet_bttv_shared_emote else R.string.emote_sheet_bttv_channel_emote
            is EmoteType.ChannelFFZEmote -> R.string.emote_sheet_ffz_channel_emote
            is EmoteType.ChannelSevenTVEmote -> R.string.emote_sheet_seventv_channel_emote
            EmoteType.GlobalBTTVEmote -> R.string.emote_sheet_bttv_global_emote
            is EmoteType.GlobalFFZEmote -> R.string.emote_sheet_ffz_global_emote
            is EmoteType.GlobalSevenTVEmote -> R.string.emote_sheet_seventv_global_emote
            else -> R.string.emote_sheet_twitch_emote
        },
    )
}
