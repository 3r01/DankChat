package com.flxrs.dankchat.data.api.seventv.eventapi

import com.flxrs.dankchat.data.api.seventv.dto.SevenTVPaintDto
import com.flxrs.dankchat.data.api.seventv.eventapi.dto.CosmeticCreateDispatchData
import com.flxrs.dankchat.data.api.seventv.eventapi.dto.DataMessage
import com.flxrs.dankchat.data.api.seventv.eventapi.dto.DispatchMessage
import com.flxrs.dankchat.data.api.seventv.eventapi.dto.EmoteSetCreateDispatchData
import com.flxrs.dankchat.data.api.seventv.eventapi.dto.EntitlementCreateDispatchData
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

internal class SevenTVEventSnapshotTest {
    private val json =
        Json {
            explicitNulls = false
            ignoreUnknownKeys = true
        }

    @Test
    fun `actorless cosmetic entitlement and emote set events decode`() {
        val cosmetic = decodeDispatch(
            """{"op":0,"d":{"type":"cosmetic.create","body":{"id":"paint-id","object":{"kind":"PAINT","data":{"id":"paint-id","name":"Image paint","function":"URL","image_url":"https://cdn.7tv.app/paint.webp","shadows":[{"x_offset":1.0,"y_offset":2.0,"radius":3.0,"color":4294967295}]}}}}}""",
        )
        val entitlement = decodeDispatch(
            """{"op":0,"d":{"type":"entitlement.create","body":{"id":"entitlement-id","object":{"kind":"PAINT","ref_id":"paint-id","user":{"connections":[{"id":"22484632","platform":"TWITCH","username":"forsen"}]}}}}}""",
        )
        val emoteSet = decodeDispatch(
            """{"op":0,"d":{"type":"emote_set.create","body":{"id":"personal-set","object":{"id":"personal-set","flags":4}}}}""",
        )

        val cosmeticBody = assertIs<CosmeticCreateDispatchData>(cosmetic.d).body
        assertNull(cosmeticBody.actor)
        val paint = json.decodeFromJsonElement<SevenTVPaintDto>(cosmeticBody.`object`.data)
        assertEquals("https://cdn.7tv.app/paint.webp", paint.imageUrl)
        assertEquals(1, paint.shadows.size)
        assertNull(assertIs<EntitlementCreateDispatchData>(entitlement.d).body.actor)
        assertEquals("paint-id", assertIs<EntitlementCreateDispatchData>(entitlement.d).body.`object`.refId)
        assertEquals("personal-set", assertIs<EmoteSetCreateDispatchData>(emoteSet.d).body.`object`.id)
    }

    private fun decodeDispatch(value: String) = assertIs<DispatchMessage>(json.decodeFromString<DataMessage>(value))
}
