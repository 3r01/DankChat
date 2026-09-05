package com.flxrs.dankchat.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flxrs.dankchat.data.api.auth.AuthApiClient
import com.flxrs.dankchat.data.api.auth.dto.ValidateDto
import com.flxrs.dankchat.data.auth.AuthDataStore
import com.flxrs.dankchat.preferences.whispers.WhisperHistorySettingsDataStore
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

private val logger = KotlinLogging.logger("LoginViewModel")

@KoinViewModel
class LoginViewModel(
    private val authApiClient: AuthApiClient,
    private val authDataStore: AuthDataStore,
    private val whisperHistorySettingsDataStore: WhisperHistorySettingsDataStore,
) : ViewModel() {
    data class TokenParseEvent(
        val successful: Boolean,
    )

    private val eventChannel = Channel<TokenParseEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    val loginUrl = AuthApiClient.LOGIN_URL

    fun parseToken(
        fragment: String,
        webOAuthToken: String? = null,
    ) = viewModelScope.launch {
        if (!fragment.startsWith("access_token")) {
            eventChannel.send(TokenParseEvent(successful = false))
            return@launch
        }

        val token =
            fragment
                .substringAfter("access_token=")
                .substringBefore("&scope=")

        val result =
            authApiClient.validateUser(token).fold(
                onSuccess = { saveLoginDetails(token, webOAuthToken, it) },
                onFailure = {
                    logger.error { "Failed to validate token: ${it.message}" }
                    TokenParseEvent(successful = false)
                },
            )
        eventChannel.send(result)
    }

    private suspend fun saveLoginDetails(
        oAuth: String,
        webOAuthToken: String?,
        validateDto: ValidateDto,
    ): TokenParseEvent {
        authDataStore.login(
            oAuthKey = oAuth,
            userName = validateDto.login.value.lowercase(),
            userId = validateDto.userId.value,
            clientId = validateDto.clientId,
        )
        if (!webOAuthToken.isNullOrBlank()) {
            whisperHistorySettingsDataStore.saveToken(validateDto.userId.value, webOAuthToken)
        }
        return TokenParseEvent(successful = true)
    }
}
