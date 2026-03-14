package com.flxrs.dankchat.preferences.appearance

import android.app.Activity
import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.LocalActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.findNavController
import com.flxrs.dankchat.R
import com.flxrs.dankchat.preferences.appearance.AppearanceSettingsInteraction.AutoDisableInput
import com.flxrs.dankchat.preferences.appearance.AppearanceSettingsInteraction.CheckeredMessages
import com.flxrs.dankchat.preferences.appearance.AppearanceSettingsInteraction.FontSize
import com.flxrs.dankchat.preferences.appearance.AppearanceSettingsInteraction.KeepScreenOn
import com.flxrs.dankchat.preferences.appearance.AppearanceSettingsInteraction.LineSeparator
import com.flxrs.dankchat.preferences.appearance.AppearanceSettingsInteraction.ShowChangelogs
import com.flxrs.dankchat.preferences.appearance.AppearanceSettingsInteraction.ShowChips
import com.flxrs.dankchat.preferences.appearance.AppearanceSettingsInteraction.ShowInput
import com.flxrs.dankchat.preferences.appearance.AppearanceSettingsInteraction.Theme
import com.flxrs.dankchat.preferences.appearance.AppearanceSettingsInteraction.TrueDarkTheme
import com.flxrs.dankchat.preferences.components.NavigationBarSpacer
import com.flxrs.dankchat.preferences.components.PreferenceCategory
import com.flxrs.dankchat.preferences.components.PreferenceListDialog
import com.flxrs.dankchat.preferences.components.SliderPreferenceItem
import com.flxrs.dankchat.preferences.components.SwitchPreferenceItem
import com.flxrs.dankchat.theme.DankChatTheme
import com.google.android.material.transition.MaterialFadeThrough
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

class AppearanceSettingsFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialFadeThrough()
        returnTransition = MaterialFadeThrough()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        postponeEnterTransition()
        (view.parent as? View)?.doOnPreDraw { startPostponedEnterTransition() }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                DankChatTheme {
                    AppearanceSettingsScreen(
                        onBackPressed = { findNavController().popBackStack() }
                    )
                }
            }
        }
    }
}
