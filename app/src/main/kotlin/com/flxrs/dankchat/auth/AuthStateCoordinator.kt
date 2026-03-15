package com.flxrs.dankchat.auth

import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebStorage
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.api.ApiException
import com.flxrs.dankchat.data.api.auth.AuthApiClient
import com.flxrs.dankchat.data.repo.IgnoresRepository
import com.flxrs.dankchat.data.repo.chat.ChatRepository
import com.flxrs.dankchat.data.repo.chat.UserStateRepository
import com.flxrs.dankchat.data.repo.emote.EmoteRepository
import com.flxrs.dankchat.data.repo.emote.EmoteUsageRepository
import com.flxrs.dankchat.di.DispatchersProvider
import com.flxrs.dankchat.domain.ChannelDataCoordinator
import com.flxrs.dankchat.utils.extensions.withoutOAuthPrefix
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single

sealed interface AuthEvent {
    data class LoggedIn(val userName: UserName) : AuthEvent
    data class ScopesOutdated(val userName: UserName) : AuthEvent
    data object TokenInvalid : AuthEvent
    data object ValidationFailed : AuthEvent
}

@Single
class AuthStateCoordinator(
    private val authDataStore: AuthDataStore,
    private val chatRepository: ChatRepository,
    private val channelDataCoordinator: ChannelDataCoordinator,
    private val emoteRepository: EmoteRepository,
    private val authApiClient: AuthApiClient,
    private val ignoresRepository: IgnoresRepository,
    private val userStateRepository: UserStateRepository,
    private val emoteUsageRepository: EmoteUsageRepository,
    dispatchersProvider: DispatchersProvider,
) {

    private val scope = CoroutineScope(SupervisorJob() + dispatchersProvider.io)
    private val _events = Channel<AuthEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        // React to login state changes — handles both login and logout.
        // distinctUntilChangedBy on isLoggedIn+oAuthKey solves re-login (new token = new oAuthKey).
        // drop(1) skips initial emission (startup connection handled by DankChatViewModel.init).
        scope.launch {
            authDataStore.settings
                .distinctUntilChangedBy { it.isLoggedIn to it.oAuthKey }
                .drop(1)
                .collect { settings ->
                    when {
                        settings.isLoggedIn -> {
                            chatRepository.reconnect()
                            channelDataCoordinator.reloadGlobalData()
                            settings.userName?.let { name ->
                                _events.send(AuthEvent.LoggedIn(UserName(name)))
                            }
                        }

                        else                -> {
                            channelDataCoordinator.cancelGlobalLoading()
                            emoteRepository.clearTwitchEmotes()
                            chatRepository.closeAndReconnect()
                        }
                    }
                }
        }
    }

    suspend fun validateOnStartup() {
        if (!authDataStore.isLoggedIn) return

        val token = authDataStore.oAuthKey?.withoutOAuthPrefix ?: return
        val result = authApiClient.validateUser(token).fold(
            onSuccess = { validateDto ->
                // Update username from validation response
                authDataStore.update { it.copy(userName = validateDto.login.value) }
                when {
                    authApiClient.validateScopes(validateDto.scopes.orEmpty()) -> AuthEvent.LoggedIn(validateDto.login)
                    else                                                       -> AuthEvent.ScopesOutdated(validateDto.login)
                }
            },
            onFailure = { throwable ->
                when {
                    throwable is ApiException && throwable.status == HttpStatusCode.Unauthorized -> {
                        authDataStore.clearLogin()
                        AuthEvent.TokenInvalid
                    }

                    else                                                                         -> {
                        Log.e(TAG, "Failed to validate token: ${throwable.message}")
                        AuthEvent.ValidationFailed
                    }
                }
            }
        )
        _events.send(result)
    }

    fun logout() {
        scope.launch {
            CookieManager.getInstance().removeAllCookies(null)
            WebStorage.getInstance().deleteAllData()

            userStateRepository.clear()
            ignoresRepository.clearIgnores()
            emoteUsageRepository.clearUsages()

            // Setting isLoggedIn = false triggers the settings observer which handles
            // clearing twitch emotes and reconnecting anonymously.
            authDataStore.clearLogin()
        }
    }

    companion object {
        private val TAG = AuthStateCoordinator::class.java.simpleName
    }
}
