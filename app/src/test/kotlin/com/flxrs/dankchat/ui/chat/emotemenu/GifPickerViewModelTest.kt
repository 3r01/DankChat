package com.flxrs.dankchat.ui.chat.emotemenu

import com.flxrs.dankchat.data.api.twitchgql.TwitchGifApiClient
import com.flxrs.dankchat.data.api.twitchgql.TwitchGifConfig
import com.flxrs.dankchat.data.api.twitchgql.TwitchGifPickerItem
import com.flxrs.dankchat.data.api.twitchgql.TwitchGifPickerPage
import com.flxrs.dankchat.data.api.twitchgql.TwitchGifSendResult
import com.flxrs.dankchat.data.auth.AuthDataStore
import com.flxrs.dankchat.data.auth.AuthSettings
import com.flxrs.dankchat.data.repo.channel.Channel
import com.flxrs.dankchat.data.repo.channel.ChannelRepository
import com.flxrs.dankchat.data.repo.chat.ChatChannelProvider
import com.flxrs.dankchat.data.repo.emote.GifPickerDataStore
import com.flxrs.dankchat.data.repo.emote.GifPickerLibrary
import com.flxrs.dankchat.data.toDisplayName
import com.flxrs.dankchat.data.toUserId
import com.flxrs.dankchat.data.toUserName
import com.flxrs.dankchat.preferences.whispers.WhisperHistorySettings
import com.flxrs.dankchat.preferences.whispers.WhisperHistorySettingsDataStore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
internal class GifPickerViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val api = mockk<TwitchGifApiClient>()
    private val savedGifs = mockk<GifPickerDataStore>(relaxed = true)
    private lateinit var viewModel: GifPickerViewModel

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(dispatcher)
        val channels = mockk<ChatChannelProvider>()
        val repository = mockk<ChannelRepository>()
        val auth = mockk<AuthDataStore>()
        val settings = mockk<WhisperHistorySettingsDataStore>()
        every { channels.activeChannel } returns MutableStateFlow("forsen".toUserName())
        every { auth.settings } returns MutableStateFlow(AuthSettings(userId = "1", isLoggedIn = true))
        every { settings.settings } returns MutableStateFlow(WhisperHistorySettings(mapOf("1" to "test-token")))
        coEvery { repository.getChannel(any<com.flxrs.dankchat.data.UserName>()) } returns
            Channel("1".toUserId(), "forsen".toUserName(), "forsen".toDisplayName(), null)
        coEvery { api.getConfig(any(), any()) } returns Result.success(TwitchGifConfig(true, true, true))
        coEvery { api.getGifs(any(), "", 0) } returns Result.success(TwitchGifPickerPage(listOf(gif("first")), 24))
        every { savedGifs.library } returns MutableStateFlow(GifPickerLibrary())
        viewModel = GifPickerViewModel(api, repository, channels, auth, settings, mockk(relaxed = true), savedGifs)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `pagination deduplicates requests and entries and stops at the last page`() = runTest(dispatcher) {
        val response = CompletableDeferred<Result<TwitchGifPickerPage>>()
        coEvery { api.getGifs(any(), "", 24) } coAnswers { response.await() }
        viewModel.loadMore()
        viewModel.loadMore()
        assertTrue((viewModel.state.value as GifPickerState.Ready).isLoadingMore)
        response.complete(Result.success(TwitchGifPickerPage(listOf(gif("first"), gif("second")), null)))
        viewModel.loadMore()
        assertEquals(listOf("first", "second"), (viewModel.state.value as GifPickerState.Ready).gifs.map { it.id })
        coVerify(exactly = 1) { api.getGifs(any(), "", 24) }
    }

    @Test
    fun `failed page preserves results and can retry`() = runTest(dispatcher) {
        coEvery { api.getGifs(any(), "", 24) } returns Result.failure(IllegalStateException("Offline"))
        viewModel.loadMore()
        val failed = viewModel.state.value as GifPickerState.Ready
        assertEquals(listOf("first"), failed.gifs.map { it.id })
        assertNotNull(failed.pageError)
        coEvery { api.getGifs(any(), "", 24) } returns Result.success(TwitchGifPickerPage(listOf(gif("second")), null))
        viewModel.loadMore()
        assertEquals(2, (viewModel.state.value as GifPickerState.Ready).gifs.size)
    }

    @Test
    fun `changing search cancels the old page and restarts at zero`() = runTest(dispatcher) {
        val oldPage = CompletableDeferred<Result<TwitchGifPickerPage>>()
        coEvery { api.getGifs(any(), "", 24) } coAnswers { oldPage.await() }
        coEvery { api.getGifs(any(), "forsen", 0) } returns Result.success(TwitchGifPickerPage(listOf(gif("new")), null))
        viewModel.loadMore()
        viewModel.search("forsen")
        advanceUntilIdle()
        oldPage.complete(Result.success(TwitchGifPickerPage(listOf(gif("stale")), null)))
        assertEquals(listOf("new"), (viewModel.state.value as GifPickerState.Ready).gifs.map { it.id })
    }

    private fun gif(id: String) = TwitchGifPickerItem(id, id, "https://example.com/$id.gif", "https://example.com/$id.webp", 200, 200, null)

    @Test
    fun `only successful sends are recorded in recents`() = runTest(dispatcher) {
        coEvery { api.sendGif(any(), any(), any()) } returns Result.failure(IllegalStateException("Offline"))
        viewModel.send(gif("first")) {}
        coVerify(exactly = 0) { savedGifs.recordSent(any()) }
        coEvery { api.sendGif(any(), any(), any()) } returns Result.success(TwitchGifSendResult(0))
        viewModel.send(gif("first")) {}
        coVerify(exactly = 1) { savedGifs.recordSent(gif("first")) }
    }
}
