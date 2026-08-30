package com.flxrs.dankchat.data.api.seventv.eventapi

import com.flxrs.dankchat.data.DisplayName
import com.flxrs.dankchat.data.api.seventv.dto.SevenTVBadgeDto
import com.flxrs.dankchat.data.api.seventv.dto.SevenTVEmoteDto
import com.flxrs.dankchat.data.api.seventv.dto.SevenTVPaintDto

sealed interface SevenTVEventMessage {
    data class PersonalEmoteSetCreated(
        val emoteSetId: String,
    ) : SevenTVEventMessage

    data class EmoteSetUpdated(
        val emoteSetId: String,
        val actorName: DisplayName,
        val added: List<SevenTVEmoteDto>,
        val removed: List<RemovedEmote>,
        val updated: List<UpdatedEmote>,
    ) : SevenTVEventMessage {
        data class UpdatedEmote(
            val id: String,
            val name: String,
            val oldName: String,
        )

        data class RemovedEmote(
            val id: String,
            val name: String,
        )
    }

    data class UserUpdated(
        val actorName: DisplayName,
        val connectionIndex: Int,
        val emoteSetId: String,
        val oldEmoteSetId: String,
    ) : SevenTVEventMessage

    sealed interface CosmeticCreated : SevenTVEventMessage {
        data class Badge(
            val badge: SevenTVBadgeDto,
        ) : CosmeticCreated

        data class Paint(
            val paint: SevenTVPaintDto,
        ) : CosmeticCreated
    }

    data class EntitlementChanged(
        val added: Boolean,
        val kind: String,
        val refId: String,
        val users: List<User>,
    ) : SevenTVEventMessage {
        data class User(
            val id: com.flxrs.dankchat.data.UserId,
            val name: com.flxrs.dankchat.data.UserName,
        )
    }
}
