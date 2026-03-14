package com.flxrs.dankchat.preferences.overview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.FiberNew
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.PreviewDynamicColors
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.flxrs.dankchat.BuildConfig
import com.flxrs.dankchat.R
import com.flxrs.dankchat.changelog.DankChatVersion
import com.flxrs.dankchat.main.MainFragment
import com.flxrs.dankchat.preferences.DankChatPreferenceStore
import com.flxrs.dankchat.preferences.components.NavigationBarSpacer
import com.flxrs.dankchat.preferences.components.PreferenceCategoryTitle
import com.flxrs.dankchat.preferences.components.PreferenceCategoryWithSummary
import com.flxrs.dankchat.preferences.components.PreferenceItem
import com.flxrs.dankchat.preferences.components.PreferenceSummary
import com.flxrs.dankchat.theme.DankChatTheme
import com.flxrs.dankchat.utils.compose.buildClickableAnnotation
import com.flxrs.dankchat.utils.compose.buildLinkAnnotation
import com.flxrs.dankchat.utils.extensions.navigateSafe
import com.google.android.material.transition.MaterialFadeThrough
import com.google.android.material.transition.MaterialSharedAxis
import org.koin.android.ext.android.inject

class OverviewSettingsFragment : Fragment() {

    private val navController: NavController by lazy { findNavController() }

    private val dankChatPreferences: DankChatPreferenceStore by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        exitTransition = MaterialFadeThrough()
        reenterTransition = MaterialFadeThrough()
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                DankChatTheme {
                    OverviewSettingsScreen(
                        isLoggedIn = dankChatPreferences.isLoggedIn,
                        hasChangelog = DankChatVersion.HAS_CHANGELOG,
                        onBackPressed = { navController.popBackStack() },
                        onNavigateRequested = { navigateSafe(it) },
                        onLogoutRequested = {
                            with(navController) {
                                previousBackStackEntry?.savedStateHandle?.set(MainFragment.LOGOUT_REQUEST_KEY, true)
                                popBackStack()
                            }
                        },
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        postponeEnterTransition()
        (view.parent as? View)?.doOnPreDraw { startPostponedEnterTransition() }
    }
}

