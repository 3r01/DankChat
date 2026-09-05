package com.flxrs.dankchat.ui.main

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.View
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.flxrs.dankchat.data.api.twitchgql.TwitchWebGqlClient
import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.compose.koinInject

private const val TWITCH_ORIGIN = "https://www.twitch.tv/"
private val TWITCH_HOSTS = setOf("www.twitch.tv", "m.twitch.tv")

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun TwitchWebGqlHost(modifier: Modifier = Modifier) {
    val client: TwitchWebGqlClient = koinInject()
    val viewRef = remember { arrayOfNulls<WebView>(1) }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                isFocusable = false
                isFocusableInTouchMode = false
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient =
                    object : WebViewClient() {
                        override fun onPageStarted(
                            view: WebView,
                            url: String?,
                            favicon: Bitmap?,
                        ) {
                            logger.info { "Twitch GQL page started (url=$url)" }
                            client.attach(view)
                        }

                        override fun onPageFinished(
                            view: WebView,
                            url: String?,
                        ) {
                            logger.info { "Twitch GQL page finished (url=$url)" }
                            client.markReady(view)
                        }

                        override fun onPageCommitVisible(
                            view: WebView,
                            url: String?,
                        ) {
                            logger.info { "Twitch GQL page committed (url=$url)" }
                            client.markReady(view)
                        }

                        override fun onReceivedError(
                            view: WebView,
                            request: WebResourceRequest,
                            error: WebResourceError,
                        ) {
                            if (request.isForMainFrame) {
                                logger.error { "Twitch GQL page failed (${error.errorCode}: ${error.description})" }
                            }
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            request: WebResourceRequest,
                        ): Boolean = request.isForMainFrame && request.url.host !in TWITCH_HOSTS
                    }
                viewRef[0] = this
                client.attach(this)
                loadUrl(TWITCH_ORIGIN)
            }
        },
        modifier = modifier.size(1.dp).alpha(0.01f),
    )

    DisposableEffect(Unit) {
        onDispose {
            viewRef[0]?.let { view ->
                client.detach(view)
                view.stopLoading()
                view.destroy()
            }
        }
    }
}

private val logger = KotlinLogging.logger("TwitchWebGqlHost")
