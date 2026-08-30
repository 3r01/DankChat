package com.flxrs.dankchat.data.api.seventv.eventapi.dto

import androidx.annotation.Keep
import com.flxrs.dankchat.data.DisplayName
import com.flxrs.dankchat.data.api.seventv.dto.SevenTVEmoteDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
@SerialName("0")
data class DispatchMessage(
    override val d: DispatchData,
) : DataMessage

@Serializable
@JsonClassDiscriminator(discriminator = "type")
sealed interface DispatchData : Data {
    val body: ChangeMapData
}

interface ChangeMapData {
    val id: String
}

@Serializable
data class Actor(
    @SerialName("display_name") val displayName: DisplayName,
)

@Serializable
@SerialName("emote_set.update")
data class EmoteSetDispatchData(
    override val body: EmoteSetChangeMapData,
) : DispatchData

@Serializable
@SerialName("emote_set.create")
data class EmoteSetCreateDispatchData(
    override val body: EmoteSetCreateChangeMapData,
) : DispatchData

@Serializable
data class EmoteSetCreateChangeMapData(
    override val id: String,
    val actor: Actor? = null,
    val `object`: EmoteSetCreateObject,
) : ChangeMapData

@Serializable
data class EmoteSetCreateObject(
    val id: String,
    val flags: Int = 0,
) {
    val isPersonalOrCommercial: Boolean get() = flags and (PERSONAL_FLAG or COMMERCIAL_FLAG) != 0

    companion object {
        private const val PERSONAL_FLAG = 1 shl 2
        private const val COMMERCIAL_FLAG = 1 shl 3
    }
}

@Serializable
data class EmoteSetChangeMapData(
    override val id: String,
    val actor: Actor? = null,
    val pushed: List<EmoteChangeField>?,
    val pulled: List<EmoteChangeField>?,
    val updated: List<EmoteChangeField>?,
) : ChangeMapData

@Serializable
@JsonClassDiscriminator("key")
sealed interface ChangeField

@Serializable
@SerialName("emotes")
data class EmoteChangeField(
    val value: SevenTVEmoteDto?,
    @SerialName("old_value") val oldValue: SevenTVEmoteDto?,
) : ChangeField

@Serializable
@SerialName("user.update")
data class UserDispatchData(
    override val body: UserChangeMapData,
) : DispatchData

@Serializable
data class UserChangeMapData(
    override val id: String,
    val actor: Actor? = null,
    val updated: List<UserChangeFields>?,
) : ChangeMapData

@Serializable
@SerialName("connections")
data class UserChangeFields(
    val value: List<UserChangeField>?,
    val index: Int,
) : ChangeField

@Serializable
sealed interface UserChangeField : ChangeField

@Serializable
@SerialName("emote_set")
data class EmoteSetChangeField(
    val value: EmoteSet,
    @SerialName("old_value") val oldValue: EmoteSet,
) : UserChangeField

@Keep
@Serializable
@SerialName("emote_set_id")
data object EmoteSetIdChangeField : UserChangeField

@Serializable
data class EmoteSet(
    val id: String,
)

@Serializable
@SerialName("cosmetic.create")
data class CosmeticCreateDispatchData(
    override val body: CosmeticChangeMapData,
) : DispatchData

@Serializable
data class CosmeticChangeMapData(
    override val id: String,
    val actor: Actor? = null,
    val `object`: CosmeticObject,
) : ChangeMapData

@Serializable
data class CosmeticObject(
    val kind: String,
    val data: kotlinx.serialization.json.JsonObject,
)

@Serializable
@SerialName("entitlement.create")
data class EntitlementCreateDispatchData(
    override val body: EntitlementChangeMapData,
) : DispatchData

@Serializable
@SerialName("entitlement.delete")
data class EntitlementDeleteDispatchData(
    override val body: EntitlementChangeMapData,
) : DispatchData

@Serializable
data class EntitlementChangeMapData(
    override val id: String,
    val actor: Actor? = null,
    val `object`: EntitlementObject,
) : ChangeMapData

@Serializable
data class EntitlementObject(
    val kind: String,
    @SerialName("ref_id") val refId: String,
    val user: EntitlementUser,
)

@Serializable
data class EntitlementUser(
    val connections: List<EntitlementConnection>,
)

@Serializable
data class EntitlementConnection(
    val id: String,
    val platform: String,
    val username: String,
)
