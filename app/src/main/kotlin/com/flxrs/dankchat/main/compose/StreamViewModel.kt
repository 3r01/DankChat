package com.flxrs.dankchat.main.compose

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.repo.stream.StreamDataRepository
import com.flxrs.dankchat.main.stream.StreamWebView
import com.flxrs.dankchat.preferences.stream.StreamsSettingsDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class StreamViewModel(
    application: Application,
    private val streamDataRepository: StreamDataRepository,
    private val streamsSettingsDataStore: StreamsSettingsDataStore,
) : AndroidViewModel(application) {

    private val _currentStreamedChannel = MutableStateFlow<UserName?>(null)
    val currentStreamedChannel: StateFlow<UserName?> = _currentStreamedChannel.asStateFlow()

    private var lastStreamedChannel: UserName? = null
    var hasWebViewBeenAttached: Boolean = false

    @SuppressLint("StaticFieldLeak")
    private var cachedWebView: StreamWebView? = null

    fun getOrCreateWebView(): StreamWebView {
        val preventReloads = streamsSettingsDataStore.current().preventStreamReloads
        return if (preventReloads) {
            cachedWebView ?: StreamWebView(getApplication()).also { cachedWebView = it }
        } else {
            StreamWebView(getApplication())
        }
    }

    fun setStream(channel: UserName, webView: StreamWebView) {
        if (channel == lastStreamedChannel) return
        lastStreamedChannel = channel
        loadStream(channel, webView)
    }

    fun destroyWebView(webView: StreamWebView) {
        webView.stopLoading()
        webView.destroy()
        if (cachedWebView === webView) {
            cachedWebView = null
        }
        lastStreamedChannel = null
        hasWebViewBeenAttached = false
    }

    private fun loadStream(channel: UserName, webView: StreamWebView) {
        val url = "https://player.twitch.tv/?channel=$channel&enableExtensions=true&muted=false&parent=twitch.tv"
        webView.stopLoading()
        webView.loadUrl(url)
    }

    fun toggleStream(channel: UserName) {
        _currentStreamedChannel.update { if (it == channel) null else channel }
    }

    fun closeStream() {
        _currentStreamedChannel.value = null
    }

    override fun onCleared() {
        cachedWebView?.destroy()
        cachedWebView = null
        lastStreamedChannel = null
        super.onCleared()
    }
}
