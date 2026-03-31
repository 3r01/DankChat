package com.flxrs.dankchat.data.api.dankchat

import io.ktor.client.HttpClient
import io.ktor.client.request.get

class DankChatApi(
    private val ktorClient: HttpClient,
) {
    suspend fun getDankChatBadges() = ktorClient.get("badges")
}
