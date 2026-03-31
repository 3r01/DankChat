package com.flxrs.dankchat.ui.main.stream

import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.doOnAttach
import com.flxrs.dankchat.R
import com.flxrs.dankchat.data.UserName
import org.koin.compose.viewmodel.koinViewModel

@Suppress("LambdaParameterEventTrailing")
@Composable
fun StreamView(
    channel: UserName,
    onClose: () -> Unit,
    onAudioOnly: () -> Unit,
    modifier: Modifier = Modifier,
    isInPipMode: Boolean = false,
    fillPane: Boolean = false,
) {
    val streamViewModel: StreamViewModel = koinViewModel()
    // Track whether the WebView has been attached to a window before.
    // First open: load URL while detached, attach after page loads (avoids white SurfaceView flash).
    // Subsequent opens: attach immediately, load URL while attached (video surface already initialized).
    var hasBeenAttached by remember { mutableStateOf(streamViewModel.hasWebViewBeenAttached) }
    var isPageLoaded by remember { mutableStateOf(hasBeenAttached) }
    val webView =
        remember {
            streamViewModel.getOrCreateWebView().also { wv ->
                wv.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                wv.webViewClient =
                    StreamComposeWebViewClient(
                        onPageFinished = { isPageLoaded = true },
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
            if (streamViewModel.streamState.value.currentStream == null) {
                streamViewModel.destroyWebView(webView)
            }
        }
    }

    Box(
        modifier =
            modifier
                .then(if (isInPipMode || fillPane) Modifier else Modifier.statusBarsPadding())
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface),
    ) {
        val webViewModifier =
            when {
                isInPipMode || fillPane -> {
                    Modifier.fillMaxSize()
                }

                else -> {
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                }
            }

        if (isPageLoaded) {
            AndroidView(
                factory = { _ ->
                    (webView.parent as? ViewGroup)?.removeView(webView)
                    webView.layoutParams =
                        ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                    if (!hasBeenAttached) {
                        hasBeenAttached = true
                        streamViewModel.hasWebViewBeenAttached = true
                    } else {
                        // Resume playback after config change — the Twitch player pauses
                        // when the WebView detaches from the old window during Activity recreation.
                        webView.doOnAttach { view ->
                            view.postDelayed({
                                (view as? WebView)?.evaluateJavascript("document.querySelector('video')?.play()", null)
                            }, 100)
                        }
                    }
                    webView
                },
                update = { _ ->
                    streamViewModel.setStream(channel, webView)
                },
                modifier =
                    webViewModifier.graphicsLayer {
                        compositingStrategy = CompositingStrategy.Offscreen
                    },
            )
        } else {
            Box(modifier = webViewModifier)
        }

        if (!isInPipMode) {
            Row(
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(8.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement
                    .spacedBy(6.dp),
            ) {
                StreamOverlayButton(
                    icon = Icons.Default.Headphones,
                    contentDescription = stringResource(R.string.menu_audio_only),
                    onClick = onAudioOnly,
                )
                StreamOverlayButton(
                    icon = Icons.Default.Close,
                    contentDescription = stringResource(R.string.dialog_dismiss),
                    onClick = onClose,
                )
            }
        }
    }
}

@Composable
private fun StreamOverlayButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .size(28.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                    shape = CircleShape,
                ).clickable(onClick = onClick),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(18.dp),
        )
    }
}

private class StreamComposeWebViewClient(
    private val onPageFinished: () -> Unit,
) : WebViewClient() {
    override fun onPageFinished(
        view: WebView?,
        url: String?,
    ) {
        if (url != null && url != BLANK_URL) {
            onPageFinished()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun shouldOverrideUrlLoading(
        view: WebView?,
        url: String?,
    ): Boolean {
        if (url.isNullOrBlank()) return true
        return ALLOWED_PATHS.none { url.startsWith(it) }
    }

    override fun shouldOverrideUrlLoading(
        view: WebView?,
        request: WebResourceRequest?,
    ): Boolean {
        val url = request?.url?.toString()
        if (url.isNullOrBlank()) return true
        return ALLOWED_PATHS.none { url.startsWith(it) }
    }

    companion object {
        private const val BLANK_URL = "about:blank"
        private val ALLOWED_PATHS =
            listOf(
                BLANK_URL,
                "https://id.twitch.tv/",
                "https://www.twitch.tv/passport-callback",
                "https://player.twitch.tv/",
            )
    }
}
