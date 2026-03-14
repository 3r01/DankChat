package com.flxrs.dankchat.main.compose

import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.flxrs.dankchat.R
import com.flxrs.dankchat.data.UserName

@Composable
fun StreamView(
    channel: UserName,
    streamViewModel: StreamViewModel,
    isInPipMode: Boolean = false,
    fillPane: Boolean = false,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Track whether the WebView has been attached to a window before.
    // First open: load URL while detached, attach after page loads (avoids white SurfaceView flash).
    // Subsequent opens: attach immediately, load URL while attached (video surface already initialized).
    var hasBeenAttached by remember { mutableStateOf(streamViewModel.hasWebViewBeenAttached) }
    var isPageLoaded by remember { mutableStateOf(hasBeenAttached) }
    val webView = remember {
        streamViewModel.getOrCreateWebView().also { wv ->
            wv.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            wv.webViewClient = StreamComposeWebViewClient(
                onPageFinished = { isPageLoaded = true }
            )
        }
    }

    // For first open: load URL on detached WebView
    if (!hasBeenAttached) {
        DisposableEffect(channel) {
            streamViewModel.setStream(channel, webView)
            onDispose { }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            (webView.parent as? ViewGroup)?.removeView(webView)
            // Active close (channel set to null) → destroy WebView
            // Config change (channel still set) → just detach, keep alive for reuse
            if (streamViewModel.currentStreamedChannel.value == null) {
                streamViewModel.destroyWebView(webView)
            }
        }
    }

    Box(
        modifier = modifier
            .then(if (isInPipMode || fillPane) Modifier else Modifier.statusBarsPadding())
            .fillMaxWidth()
            .background(Color.Black)
    ) {
        val webViewModifier = when {
            isInPipMode || fillPane -> Modifier.fillMaxSize()
            else -> Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        }

        if (isPageLoaded) {
            AndroidView(
                factory = { _ ->
                    (webView.parent as? ViewGroup)?.removeView(webView)
                    webView.layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    if (!hasBeenAttached) {
                        hasBeenAttached = true
                        streamViewModel.hasWebViewBeenAttached = true
                    }
                    webView
                },
                update = { _ ->
                    // For subsequent opens: load URL while attached
                    streamViewModel.setStream(channel, webView)
                },
                modifier = webViewModifier
            )
        } else {
            Box(modifier = webViewModifier)
        }

        if (!isInPipMode) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(8.dp)
                    .size(36.dp)
                    .background(
                        color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.dialog_dismiss),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

private class StreamComposeWebViewClient(
    private val onPageFinished: () -> Unit,
) : WebViewClient() {

    override fun onPageFinished(view: WebView?, url: String?) {
        if (url != null && url != BLANK_URL) {
            onPageFinished()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
        if (url.isNullOrBlank()) return true
        return ALLOWED_PATHS.none { url.startsWith(it) }
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url?.toString()
        if (url.isNullOrBlank()) return true
        return ALLOWED_PATHS.none { url.startsWith(it) }
    }

    companion object {
        private const val BLANK_URL = "about:blank"
        private val ALLOWED_PATHS = listOf(
            BLANK_URL,
            "https://id.twitch.tv/",
            "https://www.twitch.tv/passport-callback",
            "https://player.twitch.tv/",
        )
    }
}
