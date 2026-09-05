package com.flxrs.dankchat.data.api.twitchgql

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.flxrs.dankchat.di.DispatchersProvider
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.koin.core.annotation.Single
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds

@Single
class TwitchWebGqlClient(
    private val json: Json,
    private val dispatchersProvider: DispatchersProvider,
) {
    private val ready = MutableStateFlow(false)
    private val pending = ConcurrentHashMap<String, CompletableDeferred<TwitchWebGqlResponse>>()
    private val bridge = ResultBridge()
    private var webView: WebView? = null

    @SuppressLint("SetJavaScriptEnabled")
    fun attach(view: WebView) {
        logger.info { "Attaching Twitch GQL WebView (url=${view.url})" }
        webView = view
        ready.value = false
        view.settings.javaScriptEnabled = true
        view.settings.domStorageEnabled = true
        view.addJavascriptInterface(bridge, BRIDGE_NAME)
    }

    fun markReady(view: WebView) {
        if (webView === view && view.url.isTwitchWebUrl()) {
            logger.info { "Twitch GQL WebView ready (url=${view.url})" }
            ready.value = true
        }
    }

    fun detach(view: WebView) {
        if (webView !== view) return
        logger.info { "Detaching Twitch GQL WebView" }
        ready.value = false
        webView = null
        view.removeJavascriptInterface(BRIDGE_NAME)
        pending.values.forEach { it.completeExceptionally(TwitchWebGqlException.Unavailable) }
        pending.clear()
    }

    suspend fun execute(
        webOAuthToken: String,
        body: JsonObject,
    ): TwitchWebGqlResponse = withTimeout(REQUEST_TIMEOUT) {
        logger.info { "Waiting for Twitch GQL WebView" }
        ready.filter { it }.first()
        val nonce = UUID.randomUUID().toString()
        val result = CompletableDeferred<TwitchWebGqlResponse>()
        pending[nonce] = result
        try {
            val request =
                buildJsonObject {
                    put("nonce", JsonPrimitive(nonce))
                    put("authorization", JsonPrimitive("OAuth $webOAuthToken"))
                    put("body", body)
                }
            val encoded = json.encodeToString(JsonObject.serializer(), request)
            withContext(dispatchersProvider.main) {
                val activeView = webView ?: throw TwitchWebGqlException.Unavailable
                if (!activeView.url.isTwitchWebUrl()) throw TwitchWebGqlException.Unavailable
                logger.info { "Executing Twitch GQL request" }
                activeView.evaluateJavascript(createExecutionScript(encoded), null)
            }
            result.await()
        } finally {
            pending.remove(nonce)
        }
    }

    private inner class ResultBridge {
        @JavascriptInterface
        fun postMessage(message: String) {
            val value = runCatching { json.parseToJsonElement(message) as? JsonObject }.getOrNull() ?: return
            val nonce = value["nonce"]?.jsonPrimitive?.contentOrNull ?: return
            val deferred = pending[nonce] ?: return
            val error = value["error"]?.jsonPrimitive?.contentOrNull
            if (error != null) {
                logger.warn { "Twitch GQL request failed in WebView: $error" }
                deferred.completeExceptionally(TwitchWebGqlException.RequestFailed(error))
                return
            }
            val status = value["status"]?.jsonPrimitive?.intOrNull ?: 0
            val body = value["body"]?.jsonPrimitive?.contentOrNull.orEmpty()
            logger.info { "Twitch GQL request completed (status=$status)" }
            deferred.complete(TwitchWebGqlResponse(status, body))
        }
    }

    private companion object {
        const val BRIDGE_NAME = "DankChatGqlBridge"
        val REQUEST_TIMEOUT = 45.seconds
        val TWITCH_HOSTS = setOf("www.twitch.tv", "m.twitch.tv")
    }

    private fun String?.isTwitchWebUrl(): Boolean {
        val uri = this?.let(Uri::parse) ?: return false
        return uri.scheme == "https" && uri.host in TWITCH_HOSTS
    }
}

private val logger = KotlinLogging.logger("TwitchWebGqlClient")

data class TwitchWebGqlResponse(
    val status: Int,
    val body: String,
)

sealed class TwitchWebGqlException(
    message: String,
) : Exception(message) {
    data object Unavailable : TwitchWebGqlException("Twitch web session is unavailable")

    class RequestFailed(
        message: String,
    ) : TwitchWebGqlException(message)
}

private fun createExecutionScript(request: String): String =
    """
    (() => {
      const request = $request;
      const bridge = window.$BRIDGE_NAME;
      const post = value => bridge.postMessage(JSON.stringify({ nonce: request.nonce, ...value }));
      const newId = () => (crypto.randomUUID?.() || `${'$'}{Date.now()}${'$'}{Math.random()}`).replaceAll('-', '').replace('.', '');
      const state = window.__dankChatGqlState ||= {
        deviceId: newId(), sessionId: newId(), clientVersion: null,
        integrity: null, integrityRefreshAt: 0, authorization: null,
        integrityPromise: null,
      };
      const clientId = 'kimne78kx3ncx6brgo4mv6wki5h1ko';
      const baseHeaders = async () => {
        if (!state.clientVersion) {
          state.clientVersion = window.__twilightBuildID || null;
          if (!state.clientVersion) {
            const html = await (await fetch('https://www.twitch.tv/', { credentials: 'include' })).text();
            state.clientVersion = html.match(/__twilightBuildID\s*=\s*["']([^"']+)/)?.[1] || '';
          }
        }
        return {
          'Client-ID': clientId,
          'Authorization': request.authorization,
          'Client-Version': state.clientVersion,
          'X-Device-ID': state.deviceId,
          'Client-Session-ID': state.sessionId,
          'Client-Request-ID': newId(),
        };
      };
      const clearIntegrity = () => {
        state.integrity = null;
        state.integrityRefreshAt = 0;
        state.integrityPromise = null;
      };
      const waitForKpsdk = async () => {
        const deadline = Date.now() + 15000;
        while (!window.KPSDK?.configure && Date.now() < deadline) {
          await new Promise(resolve => setTimeout(resolve, 100));
        }
      };
      const integrity = async () => {
        if (state.authorization !== request.authorization) {
          state.authorization = request.authorization;
          clearIntegrity();
        }
        if (state.integrity && Date.now() < state.integrityRefreshAt) return state.integrity;
        if (state.integrityPromise) return state.integrityPromise;
        state.integrityPromise = (async () => {
          await waitForKpsdk();
          const response = await fetch('https://gql.twitch.tv/integrity', {
            method: 'POST', mode: 'cors', credentials: 'omit', headers: await baseHeaders(),
          });
          const value = await response.json();
          if (!response.ok || !value.token) throw new Error(`Integrity request failed (${ '$' }{response.status})`);
          const now = Date.now();
          state.integrity = value.token;
          state.integrityRefreshAt = now + Math.max(0, (Number(value.expiration) - now) * 0.9);
          return state.integrity;
        })();
        try { return await state.integrityPromise; } finally { state.integrityPromise = null; }
      };
      const execute = async retry => {
        const headers = await baseHeaders();
        headers['Client-Integrity'] = await integrity();
        headers['Accept'] = 'application/json';
        headers['Content-Type'] = 'application/json';
        const response = await fetch('https://gql.twitch.tv/gql', {
          method: 'POST', mode: 'cors', credentials: 'omit', headers,
          body: JSON.stringify(request.body),
        });
        const body = await response.text();
        if (retry && body.toLowerCase().includes('failed integrity check')) {
          state.clientVersion = null;
          clearIntegrity();
          return execute(false);
        }
        post({ status: response.status, body });
      };
      execute(true).catch(error => post({ error: String(error?.message || error) }));
    })();
    """.trimIndent()

private const val BRIDGE_NAME = "DankChatGqlBridge"
