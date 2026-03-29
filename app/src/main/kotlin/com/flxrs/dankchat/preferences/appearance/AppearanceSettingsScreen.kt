package com.flxrs.dankchat.preferences.appearance

import android.app.Activity
import android.content.Context
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
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flxrs.dankchat.R
import com.flxrs.dankchat.preferences.appearance.AppearanceSettingsInteraction.AutoDisableInput
import com.flxrs.dankchat.preferences.appearance.AppearanceSettingsInteraction.CheckeredMessages
import com.flxrs.dankchat.preferences.appearance.AppearanceSettingsInteraction.FontSize
import com.flxrs.dankchat.preferences.appearance.AppearanceSettingsInteraction.KeepScreenOn
import com.flxrs.dankchat.preferences.appearance.AppearanceSettingsInteraction.LineSeparator
import com.flxrs.dankchat.preferences.appearance.AppearanceSettingsInteraction.ShowCharacterCounter
import com.flxrs.dankchat.preferences.appearance.AppearanceSettingsInteraction.Theme
import com.flxrs.dankchat.preferences.appearance.AppearanceSettingsInteraction.TrueDarkTheme
import com.flxrs.dankchat.preferences.components.NavigationBarSpacer
import com.flxrs.dankchat.preferences.components.PreferenceCategory
import com.flxrs.dankchat.preferences.components.PreferenceListDialog
import com.flxrs.dankchat.preferences.components.SliderPreferenceItem
import com.flxrs.dankchat.preferences.components.SwitchPreferenceItem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

@Composable
fun AppearanceSettingsScreen(
    onBack: () -> Unit,
) {
    val viewModel = koinViewModel<AppearanceSettingsViewModel>()
    val uiState = viewModel.settings.collectAsStateWithLifecycle().value

    AppearanceSettingsContent(
        settings = uiState.settings,
        onInteraction = { viewModel.onInteraction(it) },
        onSuspendingInteraction = { viewModel.onSuspendingInteraction(it) },
        onBack = onBack
    )
}

@Composable
private fun AppearanceSettingsContent(
    settings: AppearanceSettings,
    onInteraction: (AppearanceSettingsInteraction) -> Unit,
    onSuspendingInteraction: suspend (AppearanceSettingsInteraction) -> Unit,
    onBack: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.exclude(WindowInsets.navigationBars),
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                scrollBehavior = scrollBehavior,
                title = { Text(stringResource(R.string.preference_appearance_header)) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        content = { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back)) },
                    )
                }
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            ThemeCategory(
                theme = settings.theme,
                trueDarkTheme = settings.trueDarkTheme,
                onInteraction = onSuspendingInteraction,
            )
            HorizontalDivider(thickness = Dp.Hairline)
            DisplayCategory(
                fontSize = settings.fontSize,
                keepScreenOn = settings.keepScreenOn,
                lineSeparator = settings.lineSeparator,
                checkeredMessages = settings.checkeredMessages,
                onInteraction = onInteraction,
            )
            HorizontalDivider(thickness = Dp.Hairline)
            ComponentsCategory(
                autoDisableInput = settings.autoDisableInput,
                showCharacterCounter = settings.showCharacterCounter,
                onInteraction = onInteraction,
            )
            NavigationBarSpacer()
        }
    }
}

@Composable
private fun ComponentsCategory(
    autoDisableInput: Boolean,
    showCharacterCounter: Boolean,
    onInteraction: (AppearanceSettingsInteraction) -> Unit,
) {
    PreferenceCategory(
        title = stringResource(R.string.preference_components_group_title),
    ) {
        SwitchPreferenceItem(
            title = stringResource(R.string.preference_auto_disable_input_title),
            isChecked = autoDisableInput,
            onClick = { onInteraction(AutoDisableInput(it)) },
        )
        SwitchPreferenceItem(
            title = stringResource(R.string.preference_show_character_counter_title),
            summary = stringResource(R.string.preference_show_character_counter_summary),
            isChecked = showCharacterCounter,
            onClick = { onInteraction(ShowCharacterCounter(it)) },
        )
    }
}

@Composable
private fun DisplayCategory(
    fontSize: Int,
    keepScreenOn: Boolean,
    lineSeparator: Boolean,
    checkeredMessages: Boolean,
    onInteraction: (AppearanceSettingsInteraction) -> Unit,
) {
    PreferenceCategory(
        title = stringResource(R.string.preference_display_group_title),
    ) {
        val context = LocalContext.current
        var value by remember(fontSize) { mutableFloatStateOf(fontSize.toFloat()) }
        val summary = remember(value) { getFontSizeSummary(value.roundToInt(), context) }
        SliderPreferenceItem(
            title = stringResource(R.string.preference_font_size_title),
            value = value,
            range = 10f..40f,
            onDrag = { value = it },
            onDragFinish = { onInteraction(FontSize(value.roundToInt())) },
            summary = summary,
        )

        SwitchPreferenceItem(
            title = stringResource(R.string.preference_keep_screen_on_title),
            isChecked = keepScreenOn,
            onClick = { onInteraction(KeepScreenOn(it)) },
        )
        SwitchPreferenceItem(
            title = stringResource(R.string.preference_line_separator_title),
            isChecked = lineSeparator,
            onClick = { onInteraction(LineSeparator(it)) },
        )
        SwitchPreferenceItem(
            title = stringResource(R.string.preference_checkered_lines_title),
            summary = stringResource(R.string.preference_checkered_lines_summary),
            isChecked = checkeredMessages,
            onClick = { onInteraction(CheckeredMessages(it)) },
        )
    }
}

@Composable
private fun ThemeCategory(
    theme: ThemePreference,
    trueDarkTheme: Boolean,
    onInteraction: suspend (AppearanceSettingsInteraction) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val themeState = rememberThemeState(theme, trueDarkTheme, isSystemInDarkTheme())
    PreferenceCategory(
        title = stringResource(R.string.preference_theme_title),
    ) {
        val activity = LocalActivity.current
        PreferenceListDialog(
            title = stringResource(R.string.preference_theme_title),
            summary = themeState.summary,
            isEnabled = themeState.themeSwitcherEnabled,
            values = themeState.values,
            entries = themeState.entries,
            selected = themeState.preference,
            onChange ={
                scope.launch {
                    activity ?: return@launch
                    onInteraction(Theme(it))
                    setDarkMode(it, activity)
                }
            }
        )
        SwitchPreferenceItem(
            title = stringResource(R.string.preference_true_dark_theme_title),
            summary = stringResource(R.string.preference_true_dark_theme_summary),
            isChecked = themeState.trueDarkPreference,
            isEnabled = themeState.trueDarkEnabled,
            onClick = {
                scope.launch {
                    activity ?: return@launch
                    onInteraction(TrueDarkTheme(it))
                    ActivityCompat.recreate(activity)
                }
            }
        )
    }
}

@Immutable
data class ThemeState(
    val preference: ThemePreference,
    val summary: String,
    val trueDarkPreference: Boolean,
    val values: ImmutableList<ThemePreference>,
    val entries: ImmutableList<String>,
    val themeSwitcherEnabled: Boolean,
    val trueDarkEnabled: Boolean,
)

@Composable
@Stable
private fun rememberThemeState(theme: ThemePreference, trueDark: Boolean, systemDarkMode: Boolean): ThemeState {
    LocalContext.current
    val defaultEntries = stringArrayResource(R.array.theme_entries).toImmutableList()
    // minSdk 30 always supports light mode and system dark mode
    stringResource(R.string.preference_dark_theme_entry_title)
    stringResource(R.string.preference_light_theme_entry_title)

    val (entries, values) = remember {
        defaultEntries to ThemePreference.entries.toImmutableList()
    }

    return remember(theme, trueDark) {
        val selected = if (theme in values) theme else ThemePreference.Dark
        val trueDarkEnabled = selected == ThemePreference.Dark || (selected == ThemePreference.System && systemDarkMode)
        ThemeState(
            preference = selected,
            summary = entries[values.indexOf(selected)],
            trueDarkPreference = trueDarkEnabled && trueDark,
            values = values,
            entries = entries,
            themeSwitcherEnabled = true,
            trueDarkEnabled = trueDarkEnabled,
        )
    }
}

private fun getFontSizeSummary(value: Int, context: Context): String {
    return when {
        value < 13 -> context.getString(R.string.preference_font_size_summary_very_small)
        value in 13..17 -> context.getString(R.string.preference_font_size_summary_small)
        value in 18..22 -> context.getString(R.string.preference_font_size_summary_large)
        else -> context.getString(R.string.preference_font_size_summary_very_large)
    }
}

private fun setDarkMode(themePreference: ThemePreference, activity: Activity) {
    AppCompatDelegate.setDefaultNightMode(
        when (themePreference) {
            ThemePreference.System -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            ThemePreference.Dark   -> AppCompatDelegate.MODE_NIGHT_YES
            else                   -> AppCompatDelegate.MODE_NIGHT_NO
        }
    )
    ActivityCompat.recreate(activity)
}
