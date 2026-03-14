package com.flxrs.dankchat.main.compose.sheets

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.LocalPlatformContext
import coil3.imageLoader
import com.flxrs.dankchat.R
import com.flxrs.dankchat.chat.compose.BadgeUi
import com.flxrs.dankchat.chat.compose.ChatScreen
import com.flxrs.dankchat.chat.compose.LocalEmoteAnimationCoordinator
import com.flxrs.dankchat.chat.compose.rememberEmoteAnimationCoordinator
import com.flxrs.dankchat.chat.history.compose.MessageHistoryComposeViewModel
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.twitch.emote.ChatMessageEmote
import com.flxrs.dankchat.preferences.appearance.AppearanceSettingsDataStore
import kotlinx.coroutines.CancellationException
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun MessageHistorySheet(
    channel: UserName,
    initialFilter: String,
    appearanceSettingsDataStore: AppearanceSettingsDataStore,
    onDismiss: () -> Unit,
    onUserClick: (userId: String?, userName: String, displayName: String, channel: String?, badges: List<BadgeUi>, isLongPress: Boolean) -> Unit,
    onMessageLongClick: (messageId: String, channel: String?, fullMessage: String) -> Unit,
    onEmoteClick: (List<ChatMessageEmote>) -> Unit,
    onJumpToMessage: ((messageId: String, channel: UserName) -> Unit)? = null,
) {
    val viewModel: MessageHistoryComposeViewModel = koinViewModel(
        key = channel.value,
        parameters = { parametersOf(channel) },
    )

    LaunchedEffect(initialFilter) {
        if (initialFilter.isNotEmpty()) {
            viewModel.updateSearchQuery(initialFilter)
        }
    }

    val appearanceSettings by appearanceSettingsDataStore.settings.collectAsStateWithLifecycle(
        initialValue = appearanceSettingsDataStore.current()
    )
    val messages by viewModel.historyUiStates.collectAsStateWithLifecycle(initialValue = emptyList())
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    var backProgress by remember { mutableFloatStateOf(0f) }

    val density = LocalDensity.current
    val statusBarHeight = with(density) { WindowInsets.statusBars.getTop(density).toDp() }
    val toolbarTopPadding = statusBarHeight + 8.dp + 48.dp + 16.dp

    val ime = WindowInsets.ime
    val navBars = WindowInsets.navigationBars
    val navBarHeightDp = with(density) { navBars.getBottom(density).toDp() }
    val currentImeHeight = (ime.getBottom(density) - navBars.getBottom(density)).coerceAtLeast(0)
    val currentImeDp = with(density) { currentImeHeight.toDp() }

    var searchBarHeightPx by remember { mutableIntStateOf(0) }
    val searchBarHeightDp = with(density) { searchBarHeightPx.toDp() }

    PredictiveBackHandler { progress ->
        try {
            progress.collect { event ->
                backProgress = event.progress
            }
            onDismiss()
        } catch (e: CancellationException) {
            backProgress = 0f
        }
    }

    val context = LocalPlatformContext.current
    val emoteCoordinator = rememberEmoteAnimationCoordinator(context.imageLoader)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .graphicsLayer {
                val scale = 1f - (backProgress * 0.1f)
                scaleX = scale
                scaleY = scale
                alpha = 1f - backProgress
                translationY = backProgress * 100f
            }
    ) {
        CompositionLocalProvider(LocalEmoteAnimationCoordinator provides emoteCoordinator) {
            ChatScreen(
                messages = messages,
                fontSize = appearanceSettings.fontSize.toFloat(),
                modifier = Modifier.fillMaxSize(),
                onUserClick = onUserClick,
                onMessageLongClick = onMessageLongClick,
                onEmoteClick = onEmoteClick,
                onJumpToMessage = onJumpToMessage,
                contentPadding = PaddingValues(top = toolbarTopPadding, bottom = searchBarHeightDp + navBarHeightDp + currentImeDp),
            )
        }

        // Floating toolbar with gradient scrim - back pill + channel name pill
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        0f to MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        0.75f to MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        1f to MaterialTheme.colorScheme.surface.copy(alpha = 0f),
                    )
                )
                .padding(top = statusBarHeight + 8.dp)
                .padding(bottom = 16.dp)
                .padding(horizontal = 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                // Back navigation pill
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                }

                // Channel name pill
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .padding(horizontal = 16.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.message_history_title, channel.value),
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                }
            }
        }

        // Bottom search bar with upward gradient scrim
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = currentImeDp)
                .background(
                    brush = Brush.verticalGradient(
                        0f to MaterialTheme.colorScheme.surface.copy(alpha = 0f),
                        0.25f to MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        1f to MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    )
                )
                .navigationBarsPadding()
                .onGloballyPositioned { coordinates ->
                    searchBarHeightPx = coordinates.size.height
                }
                .padding(top = 16.dp, bottom = 8.dp)
                .padding(horizontal = 8.dp),
        ) {
            SearchToolbar(
                searchQuery = searchQuery,
                onSearchQueryChange = viewModel::updateSearchQuery,
            )
        }
    }
}

@Composable
private fun SearchToolbar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
) {
    var textState by remember(searchQuery) { mutableStateOf(searchQuery) }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(searchQuery) {
        if (textState != searchQuery) {
            textState = searchQuery
        }
    }

    val textFieldColors = TextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
    )

    TextField(
        value = textState,
        onValueChange = {
            textState = it
            onSearchQueryChange(it)
        },
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(stringResource(R.string.search_messages_hint)) },
        trailingIcon = {
            if (textState.isNotEmpty()) {
                IconButton(onClick = {
                    textState = ""
                    onSearchQueryChange("")
                }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = stringResource(R.string.dialog_dismiss),
                    )
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
        shape = MaterialTheme.shapes.extraLarge,
        colors = textFieldColors,
    )
}
