package com.flxrs.dankchat.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flxrs.dankchat.preferences.DankChatPreferenceStore
import com.flxrs.dankchat.preferences.chat.ChatSettingsDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.android.annotation.KoinViewModel

data class OnboardingState(
    val initialPage: Int = 0,
    val currentPage: Int = 0,
    val loginCompleted: Boolean = false,
    val messageHistoryDecided: Boolean = false,
    val messageHistoryEnabled: Boolean = true,
)

@KoinViewModel
class OnboardingViewModel(
    private val onboardingDataStore: OnboardingDataStore,
    private val dankChatPreferenceStore: DankChatPreferenceStore,
    private val chatSettingsDataStore: ChatSettingsDataStore,
) : ViewModel() {

    private val _state: MutableStateFlow<OnboardingState>
    val state: StateFlow<OnboardingState>

    init {
        val savedPage = runBlocking { onboardingDataStore.current().onboardingPage }
        val isLoggedIn = dankChatPreferenceStore.isLoggedIn
        _state = MutableStateFlow(
            OnboardingState(
                initialPage = savedPage,
                currentPage = savedPage,
                loginCompleted = isLoggedIn,
                // If we're past the history page, the decision was already made in a previous session
                messageHistoryDecided = savedPage > 2,
            )
        )
        state = _state.asStateFlow()
    }

    fun setCurrentPage(page: Int) {
        _state.update { it.copy(currentPage = page) }
        viewModelScope.launch {
            onboardingDataStore.update { it.copy(onboardingPage = page) }
        }
    }

    fun onLoginCompleted() {
        _state.update { it.copy(loginCompleted = true) }
    }

    fun onMessageHistoryDecision(enabled: Boolean) {
        _state.update { it.copy(messageHistoryDecided = true, messageHistoryEnabled = enabled) }
    }

    suspend fun completeOnboarding() {
        val historyEnabled = _state.value.messageHistoryEnabled
        dankChatPreferenceStore.hasMessageHistoryAcknowledged = true
        chatSettingsDataStore.update { it.copy(loadMessageHistory = historyEnabled) }
        onboardingDataStore.update { it.copy(hasCompletedOnboarding = true, onboardingPage = 0) }
    }
}
