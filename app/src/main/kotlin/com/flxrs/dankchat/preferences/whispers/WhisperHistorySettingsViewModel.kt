package com.flxrs.dankchat.preferences.whispers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flxrs.dankchat.data.auth.AuthDataStore
import com.flxrs.dankchat.domain.WhisperHistoryCoordinator
import com.flxrs.dankchat.domain.WhisperHistoryStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
import kotlin.time.Duration.Companion.seconds

@KoinViewModel
class WhisperHistorySettingsViewModel(
    private val authDataStore: AuthDataStore,
    private val settingsDataStore: WhisperHistorySettingsDataStore,
    coordinator: WhisperHistoryCoordinator,
) : ViewModel() {
    private val invalidToken = MutableStateFlow(false)

    val state =
        combine(
            authDataStore.settings,
            settingsDataStore.settings,
            coordinator.status,
            invalidToken,
        ) { auth, settings, status, invalid ->
            val userId = auth.userId.takeIf { auth.isLoggedIn }
            WhisperHistorySettingsState(
                userName = auth.userName.takeIf { userId != null },
                hasSavedToken = userId != null && !settings.webOAuthTokens[userId].isNullOrBlank(),
                status = status,
                invalidToken = invalid,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5.seconds),
            initialValue = currentState(),
        )

    fun saveToken(rawToken: String): Boolean {
        val token = normalizeWebOAuthToken(rawToken)
        val userId = authDataStore.userIdString?.value
        if (token.isBlank() || userId == null) {
            invalidToken.value = true
            return false
        }
        invalidToken.value = false
        viewModelScope.launch { settingsDataStore.saveToken(userId, token) }
        return true
    }

    fun clearToken() {
        val userId = authDataStore.userIdString?.value ?: return
        invalidToken.value = false
        viewModelScope.launch { settingsDataStore.clearToken(userId) }
    }

    private fun currentState(): WhisperHistorySettingsState {
        val userId = authDataStore.userIdString?.value
        return WhisperHistorySettingsState(
            userName = authDataStore.userName?.value,
            hasSavedToken = userId != null && !settingsDataStore.tokenFor(userId).isNullOrBlank(),
            status = WhisperHistoryStatus.Disabled,
            invalidToken = false,
        )
    }
}

data class WhisperHistorySettingsState(
    val userName: String?,
    val hasSavedToken: Boolean,
    val status: WhisperHistoryStatus,
    val invalidToken: Boolean,
)

internal fun normalizeWebOAuthToken(rawToken: String): String {
    var token = rawToken.trim()
    if (token.startsWith("auth-token=", ignoreCase = true)) token = token.substring("auth-token=".length)
    if (token.startsWith("oauth:", ignoreCase = true)) token = token.substring("oauth:".length)
    return token.trim()
}
