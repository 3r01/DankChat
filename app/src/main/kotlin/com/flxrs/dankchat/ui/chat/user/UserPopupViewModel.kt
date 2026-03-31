package com.flxrs.dankchat.ui.chat.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flxrs.dankchat.data.UserId
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.api.helix.dto.UserDto
import com.flxrs.dankchat.data.api.helix.dto.UserFollowsDto
import com.flxrs.dankchat.data.repo.IgnoresRepository
import com.flxrs.dankchat.data.repo.channel.ChannelRepository
import com.flxrs.dankchat.data.repo.chat.UserStateRepository
import com.flxrs.dankchat.data.repo.data.DataRepository
import com.flxrs.dankchat.preferences.DankChatPreferenceStore
import com.flxrs.dankchat.utils.DateTimeUtils.asParsedZonedDateTime
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam

@KoinViewModel
class UserPopupViewModel(
    @InjectedParam private val params: UserPopupStateParams,
    private val dataRepository: DataRepository,
    private val ignoresRepository: IgnoresRepository,
    private val channelRepository: ChannelRepository,
    private val userStateRepository: UserStateRepository,
    private val preferenceStore: DankChatPreferenceStore,
) : ViewModel() {
    private val _userPopupState = MutableStateFlow<UserPopupState>(UserPopupState.Loading(params.targetUserName, params.targetDisplayName))
    val userPopupState: StateFlow<UserPopupState> = _userPopupState.asStateFlow()
    val isOwnUser: Boolean get() = preferenceStore.userIdString == params.targetUserId

    init {
        loadData()
    }

    fun blockUser() = updateStateWith { targetUserId, targetUsername ->
        ignoresRepository.addUserBlock(targetUserId, targetUsername)
    }

    fun unblockUser() = updateStateWith { targetUserId, targetUsername ->
        ignoresRepository.removeUserBlock(targetUserId, targetUsername)
    }

    private inline fun updateStateWith(crossinline block: suspend (targetUserId: UserId, targetUsername: UserName) -> Unit) = viewModelScope.launch {
        if (!preferenceStore.isLoggedIn) {
            return@launch
        }

        val result = runCatching { block(params.targetUserId, params.targetUserName) }
        when {
            result.isFailure -> _userPopupState.value = UserPopupState.Error(result.exceptionOrNull())
            else -> loadData()
        }
    }

    private fun loadData() = viewModelScope.launch {
        _userPopupState.value = UserPopupState.Loading(params.targetUserName, params.targetDisplayName)
        val currentUserId = preferenceStore.userIdString
        if (!preferenceStore.isLoggedIn || currentUserId == null) {
            _userPopupState.value = UserPopupState.Error()
            return@launch
        }

        val targetUserId = params.targetUserId
        val result =
            runCatching {
                val channelId = params.channel?.let { channelRepository.getChannel(it)?.id }
                val isBlocked = ignoresRepository.isUserBlocked(targetUserId)
                val canLoadFollows = channelId != targetUserId && (currentUserId == channelId || userStateRepository.isModeratorInChannel(params.channel))

                val channelUserFollows =
                    async {
                        channelId?.takeIf { canLoadFollows }?.let { dataRepository.getChannelFollowers(channelId, targetUserId) }
                    }
                val user =
                    async {
                        dataRepository.getUser(targetUserId)
                    }

                mapToState(
                    user = user.await(),
                    showFollowing = canLoadFollows,
                    channelUserFollows = channelUserFollows.await(),
                    isBlocked = isBlocked,
                )
            }

        val state = result.getOrElse { UserPopupState.Error(it) }
        _userPopupState.value = state
    }

    private fun mapToState(
        user: UserDto?,
        showFollowing: Boolean,
        channelUserFollows: UserFollowsDto?,
        isBlocked: Boolean,
    ): UserPopupState {
        user ?: return UserPopupState.Error()

        return UserPopupState.Success(
            userId = user.id,
            userName = user.name,
            displayName = user.displayName,
            avatarUrl = user.avatarUrl,
            created = user.createdAt.asParsedZonedDateTime(),
            showFollowingSince = showFollowing,
            followingSince =
                channelUserFollows
                    ?.data
                    ?.firstOrNull()
                    ?.followedAt
                    ?.asParsedZonedDateTime(),
            isBlocked = isBlocked,
        )
    }
}
