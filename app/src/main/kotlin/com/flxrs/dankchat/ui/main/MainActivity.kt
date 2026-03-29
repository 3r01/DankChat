package com.flxrs.dankchat.ui.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.flxrs.dankchat.BuildConfig
import com.flxrs.dankchat.DankChatViewModel
import com.flxrs.dankchat.R
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.api.ApiException
import com.flxrs.dankchat.data.notification.NotificationService
import com.flxrs.dankchat.data.repo.data.DataRepository
import com.flxrs.dankchat.data.repo.data.ServiceEvent
import com.flxrs.dankchat.preferences.DankChatPreferenceStore
import com.flxrs.dankchat.preferences.about.AboutScreen
import com.flxrs.dankchat.preferences.appearance.AppearanceSettingsScreen
import com.flxrs.dankchat.preferences.chat.ChatSettingsScreen
import com.flxrs.dankchat.preferences.chat.commands.CustomCommandsScreen
import com.flxrs.dankchat.preferences.chat.userdisplay.UserDisplayScreen
import com.flxrs.dankchat.preferences.developer.DeveloperSettingsScreen
import com.flxrs.dankchat.preferences.notifications.NotificationsSettingsScreen
import com.flxrs.dankchat.preferences.notifications.highlights.HighlightsScreen
import com.flxrs.dankchat.preferences.notifications.ignores.IgnoresScreen
import com.flxrs.dankchat.preferences.overview.OverviewSettingsScreen
import com.flxrs.dankchat.preferences.overview.SettingsNavigation
import com.flxrs.dankchat.preferences.stream.StreamsSettingsScreen
import com.flxrs.dankchat.preferences.tools.ToolsSettingsScreen
import com.flxrs.dankchat.preferences.tools.tts.TTSUserIgnoreListScreen
import com.flxrs.dankchat.preferences.tools.upload.ImageUploaderScreen
import com.flxrs.dankchat.ui.changelog.ChangelogScreen
import com.flxrs.dankchat.ui.login.LoginScreen
import com.flxrs.dankchat.ui.onboarding.OnboardingDataStore
import com.flxrs.dankchat.ui.onboarding.OnboardingScreen
import com.flxrs.dankchat.ui.theme.DankChatTheme
import com.flxrs.dankchat.utils.createMediaFile
import com.flxrs.dankchat.utils.extensions.hasPermission
import com.flxrs.dankchat.utils.extensions.isAtLeastTiramisu
import com.flxrs.dankchat.utils.extensions.isInSupportedPictureInPictureMode
import com.flxrs.dankchat.utils.extensions.keepScreenOn
import com.flxrs.dankchat.utils.extensions.parcelable
import com.flxrs.dankchat.utils.removeExifAttributes
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.DynamicColorsOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private val viewModel: DankChatViewModel by viewModel()
    private val dankChatPreferenceStore: DankChatPreferenceStore by inject()
    private val mainEventBus: MainEventBus by inject()
    private val onboardingDataStore: OnboardingDataStore by inject()
    private val dataRepository: DataRepository by inject()
    private val pendingChannelsToClear = mutableListOf<UserName>()
    private var currentMediaUri: Uri = Uri.EMPTY

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        startService()
    }

    private val requestImageCapture = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) handleCaptureRequest(imageCapture = true)
    }

    private val requestVideoCapture = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) handleCaptureRequest(imageCapture = false)
    }

    private val requestGalleryMedia = registerForActivityResult(PickVisualMedia()) { uri ->
        uri ?: return@registerForActivityResult
        val contentResolver = contentResolver
        val mimeType = contentResolver.getType(uri)
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
        if (extension == null) {
            lifecycleScope.launch { mainEventBus.emitEvent(MainEvent.UploadFailed(getString(R.string.snackbar_upload_failed), createMediaFile(this@MainActivity), false)) }
            return@registerForActivityResult
        }

        val copy = createMediaFile(this, extension)
        try {
            contentResolver.openInputStream(uri)?.use { input -> copy.outputStream().use { input.copyTo(it) } }
            if (copy.extension == "jpg" || copy.extension == "jpeg") {
                copy.removeExifAttributes()
            }
            uploadMedia(copy, imageCapture = false)
        } catch (_: Throwable) {
            copy.delete()
            lifecycleScope.launch { mainEventBus.emitEvent(MainEvent.UploadFailed(null, copy, false)) }
        }
    }

    private val twitchServiceConnection = TwitchServiceConnection()
    var notificationService: NotificationService? = null
    var isBound = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val isTrueDarkModeEnabled = viewModel.isTrueDarkModeEnabled
        val isDynamicColorAvailable = DynamicColors.isDynamicColorAvailable()
        when {
            isTrueDarkModeEnabled && isDynamicColorAvailable -> {
                val dynamicColorsOptions = DynamicColorsOptions.Builder()
                    .setThemeOverlay(R.style.AppTheme_TrueDarkOverlay)
                    .build()
                DynamicColors.applyToActivityIfAvailable(this, dynamicColorsOptions)
                // TODO check if still neded in future material alphas
                theme.applyStyle(R.style.AppTheme_TrueDarkOverlay, true)
                window.peekDecorView()?.context?.theme?.applyStyle(R.style.AppTheme_TrueDarkOverlay, true)
            }

            isTrueDarkModeEnabled                            -> {
                theme.applyStyle(R.style.AppTheme_TrueDarkTheme, true)
                window.peekDecorView()?.context?.theme?.applyStyle(R.style.AppTheme_TrueDarkTheme, true)
            }

            else                                             -> DynamicColors.applyToActivityIfAvailable(this)
        }

        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false

        super.onCreate(savedInstanceState)

        setupComposeUi()
        intent.parcelable<UserName>(OPEN_CHANNEL_KEY)?.let { channel ->
            lifecycleScope.launch { mainEventBus.emitEvent(MainEvent.OpenChannel(channel)) }
        }

        viewModel.checkLogin()
        viewModel.serviceEvents
            .flowWithLifecycle(lifecycle, minActiveState = Lifecycle.State.CREATED)
            .onEach {
                Log.i(TAG, "Received service event: $it")
                when (it) {
                    ServiceEvent.Shutdown -> handleShutDown()
                }
            }
            .launchIn(lifecycleScope)

        viewModel.keepScreenOn
            .flowWithLifecycle(lifecycle, minActiveState = Lifecycle.State.CREATED)
            .onEach {
                Log.i(TAG, "Setting FLAG_KEEP_SCREEN_ON to $it")
                keepScreenOn(it)
            }
            .launchIn(lifecycleScope)
    }

    private fun setupComposeUi() {
        setContent {
            DankChatTheme {
                val navController = rememberNavController()
                val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle(
                    initialValue = dankChatPreferenceStore.isLoggedIn
                )

                val onboardingCompleted = onboardingDataStore.current().hasCompletedOnboarding

                NavHost(
                    navController = navController,
                    startDestination = if (onboardingCompleted) Main else Onboarding,
                ) {
                    composable<Onboarding>(
                        enterTransition = { fadeIn(animationSpec = tween(220, delayMillis = 90)) },
                        exitTransition = { fadeOut(animationSpec = tween(90)) },
                        popEnterTransition = { fadeIn(animationSpec = tween(220, delayMillis = 90)) },
                        popExitTransition = { fadeOut(animationSpec = tween(90)) }
                    ) {
                        OnboardingScreen(
                            onNavigateToLogin = {
                                navController.navigate(Login)
                            },
                            onComplete = {
                                navController.navigate(Main) {
                                    popUpTo(Onboarding) { inclusive = true }
                                }
                            },
                        )
                    }
                    composable<Main>(
                        exitTransition = { scaleOut(targetScale = 0.92f, animationSpec = tween(300)) + fadeOut(animationSpec = tween(200)) },
                        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)) },
                    ) {
                        MainScreen(
                            navController = navController,
                            isLoggedIn = isLoggedIn,
                            onNavigateToSettings = {
                                navController.navigate(Settings)
                            },
                            onLogin = {
                                navController.navigate(Login)
                            },
                            onRelogin = {
                                navController.navigate(Login)
                            },
                            onLogout = {
                                viewModel.clearDataForLogout()
                            },
                            onOpenChannel = {
                                val channel = viewModel.activeChannel.value ?: return@MainScreen
                                val url = "https://twitch.tv/$channel"
                                Intent(Intent.ACTION_VIEW).also {
                                    it.data = url.toUri()
                                    startActivity(it)
                                }
                            },
                            onReportChannel = {
                                val channel = viewModel.activeChannel.value ?: return@MainScreen
                                val url = "https://twitch.tv/$channel/report"
                                Intent(Intent.ACTION_VIEW).also {
                                    it.data = url.toUri()
                                    startActivity(it)
                                }
                            },
                            onOpenUrl = { url: String ->
                                Intent(Intent.ACTION_VIEW).also {
                                    it.data = url.toUri()
                                    startActivity(it)
                                }
                            },
                            onReloadEmotes = {
                                // Handled in MainScreen with ViewModel
                            },
                            onReconnect = {
                                // Handled in MainScreen with ViewModel
                            },
                            onCaptureImage = {
                                startCameraCapture(captureVideo = false)
                            },
                            onCaptureVideo = {
                                startCameraCapture(captureVideo = true)
                            },
                            onChooseMedia = {
                                requestGalleryMedia.launch(PickVisualMediaRequest(PickVisualMedia.ImageAndVideo))
                            }
                        )
                    }
                    composable<Login>(
                        enterTransition = { fadeIn(animationSpec = tween(220, delayMillis = 90)) },
                        exitTransition = { fadeOut(animationSpec = tween(90)) },
                        popEnterTransition = { fadeIn(animationSpec = tween(220, delayMillis = 90)) },
                        popExitTransition = { fadeOut(animationSpec = tween(90)) }
                    ) {
                        LoginScreen(
                            onLoginSuccess = { navController.popBackStack() },
                            onCancel = { navController.popBackStack() }
                        )
                    }
                    composable<Settings>(
                        enterTransition = { slideInHorizontally(initialOffsetX = { it / 3 }, animationSpec = tween(300)) + fadeIn(animationSpec = tween(200)) },
                        exitTransition = { scaleOut(targetScale = 0.92f, animationSpec = tween(300)) + fadeOut(animationSpec = tween(200)) },
                        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)) },
                        popExitTransition = { scaleOut(targetScale = 0.92f, animationSpec = tween(300)) + fadeOut(animationSpec = tween(200)) },
                    ) {
                        OverviewSettingsScreen(
                            isLoggedIn = isLoggedIn,
                            hasChangelog = com.flxrs.dankchat.ui.changelog.DankChatVersion.HAS_CHANGELOG,
                            onBackPressed = { navController.popBackStack() },
                            onLogoutRequested = {
                                lifecycleScope.launch {
                                    mainEventBus.emitEvent(MainEvent.LogOutRequested)
                                    navController.popBackStack()
                                }
                            },
                            onNavigateRequested = { destination ->
                                when (destination) {
                                    SettingsNavigation.Appearance -> navController.navigate(AppearanceSettings)
                                    SettingsNavigation.Notifications -> navController.navigate(NotificationsSettings)
                                    SettingsNavigation.Chat -> navController.navigate(ChatSettings)
                                    SettingsNavigation.Streams -> navController.navigate(StreamsSettings)
                                    SettingsNavigation.Tools -> navController.navigate(ToolsSettings)
                                    SettingsNavigation.Developer -> navController.navigate(DeveloperSettings)
                                    SettingsNavigation.Changelog -> navController.navigate(ChangelogSettings)
                                    SettingsNavigation.About -> navController.navigate(AboutSettings)
                                }
                            }
                        )
                    }

                    val subEnter: @JvmSuppressWildcards AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
                        slideInHorizontally(initialOffsetX = { it / 3 }, animationSpec = tween(300)) + fadeIn(animationSpec = tween(200))
                    }
                    val subExit: @JvmSuppressWildcards AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
                        scaleOut(targetScale = 0.92f, animationSpec = tween(300)) + fadeOut(animationSpec = tween(200))
                    }
                    val subPopEnter: @JvmSuppressWildcards AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
                        slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300))
                    }
                    val subPopExit: @JvmSuppressWildcards AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
                        scaleOut(targetScale = 0.92f, animationSpec = tween(300)) + fadeOut(animationSpec = tween(200))
                    }

                    composable<AppearanceSettings>(
                        enterTransition = subEnter,
                        exitTransition = subExit,
                        popEnterTransition = subPopEnter,
                        popExitTransition = subPopExit
                    ) {
                        AppearanceSettingsScreen(
                            onBackPressed = { navController.popBackStack() }
                        )
                    }
                    composable<NotificationsSettings>(
                        enterTransition = subEnter,
                        exitTransition = subExit,
                        popEnterTransition = subPopEnter,
                        popExitTransition = subPopExit
                    ) {
                        NotificationsSettingsScreen(
                            onNavToHighlights = { navController.navigate(HighlightsSettings) },
                            onNavToIgnores = { navController.navigate(IgnoresSettings) },
                            onNavBack = { navController.popBackStack() }
                        )
                    }
                    composable<HighlightsSettings>(
                        enterTransition = subEnter,
                        exitTransition = subExit,
                        popEnterTransition = subPopEnter,
                        popExitTransition = subPopExit
                    ) {
                        HighlightsScreen(
                            onNavBack = { navController.popBackStack() }
                        )
                    }
                    composable<IgnoresSettings>(
                        enterTransition = subEnter,
                        exitTransition = subExit,
                        popEnterTransition = subPopEnter,
                        popExitTransition = subPopExit
                    ) {
                        IgnoresScreen(
                            onNavBack = { navController.popBackStack() }
                        )
                    }
                    composable<ChatSettings>(
                        enterTransition = subEnter,
                        exitTransition = subExit,
                        popEnterTransition = subPopEnter,
                        popExitTransition = subPopExit
                    ) {
                        ChatSettingsScreen(
                            onNavToCommands = { navController.navigate(CustomCommandsSettings) },
                            onNavToUserDisplays = { navController.navigate(UserDisplaySettings) },
                            onNavBack = { navController.popBackStack() }
                        )
                    }
                    composable<CustomCommandsSettings>(
                        enterTransition = subEnter,
                        exitTransition = subExit,
                        popEnterTransition = subPopEnter,
                        popExitTransition = subPopExit
                    ) {
                        CustomCommandsScreen(
                            onNavBack = { navController.popBackStack() }
                        )
                    }
                    composable<UserDisplaySettings>(
                        enterTransition = subEnter,
                        exitTransition = subExit,
                        popEnterTransition = subPopEnter,
                        popExitTransition = subPopExit
                    ) {
                        UserDisplayScreen(
                            onNavBack = { navController.popBackStack() }
                        )
                    }
                    composable<StreamsSettings>(
                        enterTransition = subEnter,
                        exitTransition = subExit,
                        popEnterTransition = subPopEnter,
                        popExitTransition = subPopExit
                    ) {
                        StreamsSettingsScreen(
                            onBackPressed = { navController.popBackStack() }
                        )
                    }
                    composable<ToolsSettings>(
                        enterTransition = subEnter,
                        exitTransition = subExit,
                        popEnterTransition = subPopEnter,
                        popExitTransition = subPopExit
                    ) {
                        ToolsSettingsScreen(
                            onNavToImageUploader = { navController.navigate(ImageUploaderSettings) },
                            onNavToTTSUserIgnoreList = { navController.navigate(TTSUserIgnoreListSettings) },
                            onNavBack = { navController.popBackStack() }
                        )
                    }
                    composable<ImageUploaderSettings>(
                        enterTransition = subEnter,
                        exitTransition = subExit,
                        popEnterTransition = subPopEnter,
                        popExitTransition = subPopExit
                    ) {
                        ImageUploaderScreen(
                            onNavBack = { navController.popBackStack() }
                        )
                    }
                    composable<TTSUserIgnoreListSettings>(
                        enterTransition = subEnter,
                        exitTransition = subExit,
                        popEnterTransition = subPopEnter,
                        popExitTransition = subPopExit
                    ) {
                        TTSUserIgnoreListScreen(
                            onNavBack = { navController.popBackStack() }
                        )
                    }
                    composable<DeveloperSettings>(
                        enterTransition = subEnter,
                        exitTransition = subExit,
                        popEnterTransition = subPopEnter,
                        popExitTransition = subPopExit
                    ) {
                        DeveloperSettingsScreen(
                            onBackPressed = { navController.popBackStack() }
                        )
                    }
                    composable<ChangelogSettings>(
                        enterTransition = subEnter,
                        exitTransition = subExit,
                        popEnterTransition = subPopEnter,
                        popExitTransition = subPopExit
                    ) {
                        ChangelogScreen(
                            onBackPressed = { navController.popBackStack() }
                        )
                    }
                    composable<AboutSettings>(
                        enterTransition = subEnter,
                        exitTransition = subExit,
                        popEnterTransition = subPopEnter,
                        popExitTransition = subPopExit
                    ) {
                        AboutScreen(
                            onBackPressed = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        if (!isChangingConfigurations && !isInSupportedPictureInPictureMode) {
            handleShutDown()
        }
    }

    @SuppressLint("InlinedApi")
    override fun onStart() {
        super.onStart()

        val hasCompletedOnboarding = onboardingDataStore.current().hasCompletedOnboarding
        val needsNotificationPermission = hasCompletedOnboarding && isAtLeastTiramisu && !hasPermission(Manifest.permission.POST_NOTIFICATIONS)
        when {
            needsNotificationPermission -> requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            else                        -> startService()
        }
    }

    private fun startService() {
        if (!isBound) Intent(this, NotificationService::class.java).also {
            try {
                isBound = true
                ContextCompat.startForegroundService(this, it)
                bindService(it, twitchServiceConnection, BIND_AUTO_CREATE)
            } catch (t: Throwable) {
                Log.e(TAG, Log.getStackTraceString(t))
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (isBound) {
            if (!isChangingConfigurations) {
                notificationService?.enableNotifications()
            }

            isBound = false
            try {
                unbindService(twitchServiceConnection)
            } catch (t: Throwable) {
                Log.e(TAG, Log.getStackTraceString(t))
            }
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: android.content.res.Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        mainEventBus.setInPipMode(isInPictureInPictureMode)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val channelExtra = intent.parcelable<UserName>(OPEN_CHANNEL_KEY) ?: return
        lifecycleScope.launch { mainEventBus.emitEvent(MainEvent.OpenChannel(channelExtra)) }
    }

    fun clearNotificationsOfChannel(channel: UserName) = when {
        isBound && notificationService != null -> notificationService?.setActiveChannel(channel)
        else                                   -> pendingChannelsToClear += channel
    }

    private fun handleShutDown() {
        stopService(Intent(this, NotificationService::class.java))
        finish()
        android.os.Process.killProcess(android.os.Process.myPid())
    }

    private fun startCameraCapture(captureVideo: Boolean = false) {
        val (action, extension) = when {
            captureVideo -> MediaStore.ACTION_VIDEO_CAPTURE to "mp4"
            else         -> MediaStore.ACTION_IMAGE_CAPTURE to "jpg"
        }
        Intent(action).also { captureIntent ->
            captureIntent.resolveActivity(packageManager)?.also {
                try {
                    createMediaFile(this, extension).apply { currentMediaUri = toUri() }
                } catch (_: IOException) {
                    null
                }?.also {
                    val uri = FileProvider.getUriForFile(this, "${BuildConfig.APPLICATION_ID}.fileprovider", it)
                    captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
                    when {
                        captureVideo -> requestVideoCapture.launch(captureIntent)
                        else         -> requestImageCapture.launch(captureIntent)
                    }
                }
            }
        }
    }

    private fun handleCaptureRequest(imageCapture: Boolean) {
        if (currentMediaUri == Uri.EMPTY) return
        var mediaFile: java.io.File? = null

        try {
            mediaFile = currentMediaUri.toFile()
            currentMediaUri = Uri.EMPTY
            uploadMedia(mediaFile, imageCapture)
        } catch (_: IOException) {
            currentMediaUri = Uri.EMPTY
            mediaFile?.delete()
            lifecycleScope.launch { mainEventBus.emitEvent(MainEvent.UploadFailed(null, mediaFile ?: return@launch, imageCapture)) }
        }
    }

    private fun uploadMedia(file: java.io.File, imageCapture: Boolean) {
        lifecycleScope.launch {
            mainEventBus.emitEvent(MainEvent.UploadLoading)
            withContext(Dispatchers.IO) {
                if (imageCapture) {
                    runCatching { file.removeExifAttributes() }
                }
            }
            val result = withContext(Dispatchers.IO) { dataRepository.uploadMedia(file) }
            result.fold(
                onSuccess = { url ->
                    file.delete()
                    mainEventBus.emitEvent(MainEvent.UploadSuccess(url))
                },
                onFailure = { throwable ->
                    val message = when (throwable) {
                        is ApiException -> "${throwable.status} ${throwable.message}"
                        else            -> throwable.message
                    }
                    mainEventBus.emitEvent(MainEvent.UploadFailed(message, file, imageCapture))
                }
            )
        }
    }

    private inner class TwitchServiceConnection : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as NotificationService.LocalBinder
            notificationService = binder.service
            isBound = true

            if (pendingChannelsToClear.isNotEmpty()) {
                pendingChannelsToClear.forEach { notificationService?.setActiveChannel(it) }
                pendingChannelsToClear.clear()
            }

            binder.service.checkForNotification()
        }

        override fun onServiceDisconnected(className: ComponentName?) {
            notificationService = null
            isBound = false
        }
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
        const val OPEN_CHANNEL_KEY = "open_channel"
    }
}
