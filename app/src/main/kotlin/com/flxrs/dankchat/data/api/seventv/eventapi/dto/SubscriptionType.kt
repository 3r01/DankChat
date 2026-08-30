package com.flxrs.dankchat.data.api.seventv.eventapi.dto

enum class SubscriptionType(
    val type: String,
) {
    UserUpdates(type = "user.update"),
    EmoteSetUpdates(type = "emote_set.update"),
    EmoteSets(type = "emote_set.*"),
    CosmeticCreates(type = "cosmetic.create"),
    EntitlementCreates(type = "entitlement.create"),
    EntitlementDeletes(type = "entitlement.delete"),
}
