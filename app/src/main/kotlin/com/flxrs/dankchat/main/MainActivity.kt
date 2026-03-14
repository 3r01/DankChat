package com.flxrs.dankchat.main

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.Context
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
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat.Type
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.doOnAttach
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.findNavController
import androidx.navigation.toRoute
import com.flxrs.dankchat.BuildConfig
import com.flxrs.dankchat.DankChatViewModel
import com.flxrs.dankchat.R
import com.flxrs.dankchat.ValidationResult
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.notification.NotificationService
import com.flxrs.dankchat.data.repo.data.ServiceEvent
import com.flxrs.dankchat.databinding.MainActivityBinding
import com.flxrs.dankchat.login.compose.LoginScreen
import com.flxrs.dankchat.main.compose.MainScreen
import com.flxrs.dankchat.main.compose.MainEventBus
import com.flxrs.dankchat.preferences.DankChatPreferenceStore
import com.flxrs.dankchat.preferences.about.AboutScreen
import com.flxrs.dankchat.preferences.appearance.AppearanceSettingsDataStore
import com.flxrs.dankchat.preferences.appearance.AppearanceSettingsScreen
import com.flxrs.dankchat.preferences.chat.ChatSettingsScreen
import com.flxrs.dankchat.preferences.chat.commands.CustomCommandsScreen
import com.flxrs.dankchat.preferences.chat.userdisplay.UserDisplayScreen
import com.flxrs.dankchat.preferences.developer.DeveloperSettingsDataStore
import com.flxrs.dankchat.preferences.developer.DeveloperSettingsScreen
import com.flxrs.dankchat.preferences.notifications.NotificationsSettingsScreen
import com.flxrs.dankchat.preferences.notifications.highlights.HighlightsScreen
import com.flxrs.dankchat.preferences.notifications.ignores.IgnoresScreen
import com.flxrs.dankchat.preferences.overview.OverviewSettingsScreen
import com.flxrs.dankchat.preferences.stream.StreamsSettingsScreen
import com.flxrs.dankchat.preferences.tools.ToolsSettingsScreen
import com.flxrs.dankchat.preferences.tools.tts.TTSUserIgnoreListScreen
import com.flxrs.dankchat.preferences.tools.upload.ImageUploaderScreen
import com.flxrs.dankchat.theme.DankChatTheme
import com.flxrs.dankchat.data.api.ApiException
import com.flxrs.dankchat.data.repo.data.DataRepository
import com.flxrs.dankchat.utils.createMediaFile
import com.flxrs.dankchat.utils.removeExifAttributes
import com.flxrs.dankchat.utils.extensions.hasPermission
import com.flxrs.dankchat.utils.extensions.isAtLeastTiramisu
import com.flxrs.dankchat.utils.extensions.isInSupportedPictureInPictureMode
import com.flxrs.dankchat.utils.extensions.keepScreenOn
import com.flxrs.dankchat.utils.extensions.parcelable
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.DynamicColorsOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.compose.viewmodel.koinViewModel
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private val viewModel: DankChatViewModel by viewModel()
    private val developerSettingsDataStore: DeveloperSettingsDataStore by inject()
    private val dankChatPreferenceStore: DankChatPreferenceStore by inject()
    private val mainEventBus: MainEventBus by inject()
    private val dataRepository: DataRepository by inject()
    private val pendingChannelsToClear = mutableListOf<UserName>()
    private var navController: NavController? = null
    private var bindingRef: MainActivityBinding? = null
    private val binding get() = bindingRef
    private var currentMediaUri: Uri = Uri.EMPTY

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        // just start the service, we don't care if the permission has been granted or not xd
        startService()
    }

    private val requestImageCapture = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) handleCaptureRequest(imageCapture = true)
    }

    private val requestVideoCapture = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) handleCaptureRequest(imageCapture = false)
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
    var channelToOpen: UserName? = null

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

        super.onCreate(savedInstanceState)

        // Check if we should use Compose UI
        val useComposeUi = developerSettingsDataStore.current().useComposeChatUi

        if (useComposeUi) {
            setupComposeUi()
        } else {
            setupFragmentUi()
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

    private fun setupFragmentUi() {
        bindingRef = MainActivityBinding.inflate(layoutInflater)
        setContentView(binding!!.root)
        navController = findNavController(R.id.main_content)
    }

    private fun setupComposeUi() {
        lifecycleScope.launch {
            viewModel.validationResult.collect { result ->
                when (result) {
                    is ValidationResult.User -> mainEventBus.emitEvent(MainEvent.LoginValidated(result.username))
                    is ValidationResult.IncompleteScopes -> mainEventBus.emitEvent(MainEvent.LoginOutdated(result.username))
                    ValidationResult.TokenInvalid -> mainEventBus.emitEvent(MainEvent.LoginTokenInvalid)
                    ValidationResult.Failure -> mainEventBus.emitEvent(MainEvent.LoginValidationFailed)
                }
            }
        }
        setContent {
            DankChatTheme {
                val navController = rememberNavController()
                val developerSettings by developerSettingsDataStore.settings.collectAsStateWithLifecycle(
                    initialValue = developerSettingsDataStore.current()
                )
                val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle(
                    initialValue = dankChatPreferenceStore.isLoggedIn
                )

                NavHost(
                    navController = navController,
                    startDestination = Main
                ) {
                    composable<Main>(
                        enterTransition = { slideInHorizontally(initialOffsetX = { -it }) },
                        exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) },
                        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) },
                        popExitTransition = { slideOutHorizontally(targetOffsetX = { -it }) }
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
                            navController = navController,
                            onLoginSuccess = { navController.popBackStack() },
                            onCancel = { navController.popBackStack() }
                        )
                    }
                    composable<Settings>(
                        enterTransition = {
                            if (initialState.destination.route?.contains("Main") == true) {
                                slideInHorizontally(initialOffsetX = { it })
                            } else {
                                fadeIn(animationSpec = tween(220, delayMillis = 90))
                            }
                        },
                        exitTransition = {
                            if (targetState.destination.route?.contains("Main") == true) {
                                slideOutHorizontally(targetOffsetX = { it })
                            } else {
                                fadeOut(animationSpec = tween(90))
                            }
                        },
                        popEnterTransition = {
                            if (initialState.destination.route?.contains("Main") == true) {
                                slideInHorizontally(initialOffsetX = { it })
                            } else {
                                fadeIn(animationSpec = tween(220, delayMillis = 90))
                            }
                        },
                        popExitTransition = {
                            if (targetState.destination.route?.contains("Main") == true) {
                                slideOutHorizontally(targetOffsetX = { it })
                            } else {
                                fadeOut(animationSpec = tween(90))
                            }
                        }
                    ) {
                        OverviewSettingsScreen(
                            isLoggedIn = isLoggedIn,
                            hasChangelog = com.flxrs.dankchat.changelog.DankChatVersion.HAS_CHANGELOG,
                            onBackPressed = { navController.popBackStack() },
                            onLogoutRequested = {
                                lifecycleScope.launch {
                                    mainEventBus.emitEvent(MainEvent.LogOutRequested)
                                    navController.popBackStack()
                                }
                            },
                            onNavigateRequested = { destinationId ->
                                when (destinationId) {
                                    R.id.action_overviewSettingsFragment_to_appearanceSettingsFragment -> navController.navigate(AppearanceSettings)
                                    R.id.action_overviewSettingsFragment_to_notificationsSettingsFragment -> navController.navigate(NotificationsSettings)
                                    R.id.action_overviewSettingsFragment_to_chatSettingsFragment -> navController.navigate(ChatSettings)
                                    R.id.action_overviewSettingsFragment_to_streamsSettingsFragment -> navController.navigate(StreamsSettings)
                                    R.id.action_overviewSettingsFragment_to_toolsSettingsFragment -> navController.navigate(ToolsSettings)
                                    R.id.action_overviewSettingsFragment_to_developerSettingsFragment -> navController.navigate(DeveloperSettings)
                                    R.id.action_overviewSettingsFragment_to_changelogSheetFragment -> navController.navigate(ChangelogSettings)
                                    R.id.action_overviewSettingsFragment_to_aboutFragment -> navController.navigate(AboutSettings)
                                }
                            }
                        )
                    }

                    val settingsEnterTransition: AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>.() -> EnterTransition = {
                        fadeIn(animationSpec = tween(220, delayMillis = 90))
                    }
                    val settingsExitTransition: AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>.() -> ExitTransition = {
                        fadeOut(animationSpec = tween(90))
                    }

                    composable<AppearanceSettings>(
                        enterTransition = settingsEnterTransition,
                        exitTransition = settingsExitTransition,
                        popEnterTransition = settingsEnterTransition,
                        popExitTransition = settingsExitTransition
                    ) {
                        AppearanceSettingsScreen(
                            onBackPressed = { navController.popBackStack() }
                        )
                    }
                    composable<NotificationsSettings>(
                        enterTransition = settingsEnterTransition,
                        exitTransition = settingsExitTransition,
                        popEnterTransition = settingsEnterTransition,
                        popExitTransition = settingsExitTransition
                    ) {
                        NotificationsSettingsScreen(
                            onNavToHighlights = { navController.navigate(HighlightsSettings) },
                            onNavToIgnores = { navController.navigate(IgnoresSettings) },
                            onNavBack = { navController.popBackStack() }
                        )
                    }
                    composable<HighlightsSettings>(
                        enterTransition = settingsEnterTransition,
                        exitTransition = settingsExitTransition,
                        popEnterTransition = settingsEnterTransition,
                        popExitTransition = settingsExitTransition
                    ) {
                        HighlightsScreen(
                            onNavBack = { navController.popBackStack() }
                        )
                    }
                    composable<IgnoresSettings>(
                        enterTransition = settingsEnterTransition,
                        exitTransition = settingsExitTransition,
                        popEnterTransition = settingsEnterTransition,
                        popExitTransition = settingsExitTransition
                    ) {
                        IgnoresScreen(
                            onNavBack = { navController.popBackStack() }
                        )
                    }
                    composable<ChatSettings>(
                        enterTransition = settingsEnterTransition,
                        exitTransition = settingsExitTransition,
                        popEnterTransition = settingsEnterTransition,
                        popExitTransition = settingsExitTransition
                    ) {
                        ChatSettingsScreen(
                            onNavToCommands = { navController.navigate(CustomCommandsSettings) },
                            onNavToUserDisplays = { navController.navigate(UserDisplaySettings) },
                            onNavBack = { navController.popBackStack() }
                        )
                    }
                    composable<CustomCommandsSettings>(
                        enterTransition = settingsEnterTransition,
                        exitTransition = settingsExitTransition,
                        popEnterTransition = settingsEnterTransition,
                        popExitTransition = settingsExitTransition
                    ) {
                        CustomCommandsScreen(
                            onNavBack = { navController.popBackStack() }
                        )
                    }
                    composable<UserDisplaySettings>(
                        enterTransition = settingsEnterTransition,
                        exitTransition = settingsExitTransition,
                        popEnterTransition = settingsEnterTransition,
                        popExitTransition = settingsExitTransition
                    ) {
                        UserDisplayScreen(
                            onNavBack = { navController.popBackStack() }
                        )
                    }
                    composable<StreamsSettings>(
                        enterTransition = settingsEnterTransition,
                        exitTransition = settingsExitTransition,
                        popEnterTransition = settingsEnterTransition,
                        popExitTransition = settingsExitTransition
                    ) {
                        StreamsSettingsScreen(
                            onBackPressed = { navController.popBackStack() }
                        )
                    }
                    composable<ToolsSettings>(
                        enterTransition = settingsEnterTransition,
                        exitTransition = settingsExitTransition,
                        popEnterTransition = settingsEnterTransition,
                        popExitTransition = settingsExitTransition
                    ) {
                        ToolsSettingsScreen(
                            onNavToImageUploader = { navController.navigate(ImageUploaderSettings) },
                            onNavToTTSUserIgnoreList = { navController.navigate(TTSUserIgnoreListSettings) },
                            onNavBack = { navController.popBackStack() }
                        )
                    }
                    composable<ImageUploaderSettings>(
                        enterTransition = settingsEnterTransition,
                        exitTransition = settingsExitTransition,
                        popEnterTransition = settingsEnterTransition,
                        popExitTransition = settingsExitTransition
                    ) {
                        ImageUploaderScreen(
                            onNavBack = { navController.popBackStack() }
                        )
                    }
                    composable<TTSUserIgnoreListSettings>(
                        enterTransition = settingsEnterTransition,
                        exitTransition = settingsExitTransition,
                        popEnterTransition = settingsEnterTransition,
                        popExitTransition = settingsExitTransition
                    ) {
                        TTSUserIgnoreListScreen(
                            onNavBack = { navController.popBackStack() }
                        )
                    }
                    composable<DeveloperSettings>(
                        enterTransition = settingsEnterTransition,
                        exitTransition = settingsExitTransition,
                        popEnterTransition = settingsEnterTransition,
                        popExitTransition = settingsExitTransition
                    ) {
                        DeveloperSettingsScreen(
                            onBackPressed = { navController.popBackStack() }
                        )
                    }
                    composable<ChangelogSettings>(
                        enterTransition = settingsEnterTransition,
                        exitTransition = settingsExitTransition,
                        popEnterTransition = settingsEnterTransition,
                        popExitTransition = settingsExitTransition
                    ) {
                        com.flxrs.dankchat.changelog.ChangelogScreen(
                            onBackPressed = { navController.popBackStack() }
                        )
                    }
                    composable<AboutSettings>(
                        enterTransition = settingsEnterTransition,
                        exitTransition = settingsExitTransition,
                        popEnterTransition = settingsEnterTransition,
                        popExitTransition = settingsExitTransition
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
        bindingRef = null

        if (!isChangingConfigurations && !isInSupportedPictureInPictureMode) {
            handleShutDown()
        }
    }

    @SuppressLint("InlinedApi")
    override fun onStart() {
        super.onStart()
        val needsNotificationPermission = isAtLeastTiramisu && hasPermission(Manifest.permission.POST_NOTIFICATIONS)
        when {
            needsNotificationPermission -> requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            // start service without notification permission
            else                        -> startService()
        }
    }

    private fun startService() {
        if (!isBound) Intent(this, NotificationService::class.java).also {
            try {
                isBound = true
                ContextCompat.startForegroundService(this, it)
                bindService(it, twitchServiceConnection, Context.BIND_AUTO_CREATE)
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

    override fun onSupportNavigateUp(): Boolean {
        return navController?.navigateUp() ?: false || super.onSupportNavigateUp()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val channelExtra = intent.parcelable<UserName>(OPEN_CHANNEL_KEY)
        channelToOpen = channelExtra
    }

    fun clearNotificationsOfChannel(channel: UserName) = when {
        isBound && notificationService != null -> notificationService?.setActiveChannel(channel)
        else                                   -> pendingChannelsToClear += channel
    }

    fun setFullScreen(enabled: Boolean, changeActionBarVisibility: Boolean = true) {
        val rootView = binding?.root ?: return
        rootView.doOnAttach {
            val windowInsetsController = WindowCompat.getInsetsController(window, it)
            when {
                enabled -> {
                    // minSdk 30 guarantees multi-window support (API 24+)
                    if (!isInMultiWindowMode) {
                        with(windowInsetsController) {
                            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                            hide(Type.systemBars())
                        }
                    }
                    if (changeActionBarVisibility) {
                        supportActionBar?.hide()
                    }
                }

                else    -> {
                    windowInsetsController.show(Type.systemBars())
                    if (changeActionBarVisibility) {
                        supportActionBar?.show()
                    }
                }
            }
            it.requestApplyInsets()
        }
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

            viewModel.init(tryReconnect = !isChangingConfigurations)
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
