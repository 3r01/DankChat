package com.flxrs.dankchat.login.compose

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flxrs.dankchat.R
import com.flxrs.dankchat.login.LoginViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onCancel: () -> Unit,
) {
    val viewModel: LoginViewModel = koinViewModel()
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event.successful) {
                onLoginSuccess()
            } else {
                // TODO: Show error? Legacy just navigates up mostly.
                // onCancel() 
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.login)) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.dialog_cancel))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        @SuppressLint("SetJavaScriptEnabled")
                        settings.javaScriptEnabled = true
                        settings.setSupportZoom(true)
                        
                        clearCache(true)
                        clearFormData()
                        
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                isLoading = true
                            }
                            
                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                            }

                            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                val urlString = url ?: ""
                                val fragment = urlString.toUri().fragment ?: return false
                                viewModel.parseToken(fragment)
                                return true // Consume
                            }

                            @RequiresApi(Build.VERSION_CODES.N)
                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val fragment = request?.url?.fragment ?: return false
                                viewModel.parseToken(fragment)
                                return true // Consume
                            }
                            
                            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                                Log.e("LoginScreen", "Error: ${error?.description}")
                                isLoading = false
                            }
                        }
                        
                        loadUrl(viewModel.loginUrl)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
    
    BackHandler {
        onCancel()
    }
}
