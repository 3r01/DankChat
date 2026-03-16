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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
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
import com.flxrs.dankchat.main.compose.SuggestionDropdown
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException

@Composable
fun MessageHistorySheet(
    viewModel: MessageHistoryComposeViewModel,
    channel: UserName,
    initialFilter: String,
    onDismiss: () -> Unit,
    onUserClick: (userId: String?, userName: String, displayName: String, channel: String?, badges: List<BadgeUi>, isLongPress: Boolean) -> Unit,
    onMessageLongClick: (messageId: String, channel: String?, fullMessage: String) -> Unit,
    onEmoteClick: (List<ChatMessageEmote>) -> Unit,
    onJumpToMessage: ((messageId: String, channel: UserName) -> Unit)? = null,
) {

    LaunchedEffect(viewModel, initialFilter) {
        viewModel.setInitialQuery(initialFilter)
    }

    val displaySettings by viewModel.chatDisplaySettings.collectAsStateWithLifecycle()
    val messages by viewModel.historyUiStates.collectAsStateWithLifecycle(initialValue = emptyList())
    val filterSuggestions by viewModel.filterSuggestions.collectAsStateWithLifecycle()

    val sheetBackgroundColor = lerp(
        MaterialTheme.colorScheme.surfaceContainer,
        MaterialTheme.colorScheme.surfaceContainerHigh,
        fraction = 0.75f,
    )
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
            .background(sheetBackgroundColor)
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
                fontSize = displaySettings.fontSize,
                showLineSeparator = displaySettings.showLineSeparator,
                animateGifs = displaySettings.animateGifs,
                modifier = Modifier.fillMaxSize(),
                onUserClick = onUserClick,
                onMessageLongClick = onMessageLongClick,
                onEmoteClick = onEmoteClick,
                onJumpToMessage = onJumpToMessage,
                contentPadding = PaddingValues(top = toolbarTopPadding, bottom = searchBarHeightDp + navBarHeightDp + currentImeDp),
                containerColor = sheetBackgroundColor,
            )
        }

        // Floating toolbar with gradient scrim - back pill + channel name pill
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        0f to sheetBackgroundColor.copy(alpha = 0.7f),
                        0.75f to sheetBackgroundColor.copy(alpha = 0.7f),
                        1f to sheetBackgroundColor.copy(alpha = 0f),
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

        // Filter suggestions above search bar
        SuggestionDropdown(
            suggestions = filterSuggestions.toImmutableList(),
            onSuggestionClick = { suggestion -> viewModel.applySuggestion(suggestion) },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = searchBarHeightDp + navBarHeightDp + currentImeDp + 8.dp)
                .padding(horizontal = 8.dp),
        )

        // Floating search bar pill
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = currentImeDp)
                .navigationBarsPadding()
                .onGloballyPositioned { coordinates ->
                    searchBarHeightPx = coordinates.size.height
                }
                .padding(bottom = 8.dp)
                .padding(horizontal = 8.dp),
        ) {
            SearchToolbar(
                state = viewModel.searchFieldState,
            )
        }
    }
}

@Composable
private fun SearchToolbar(
    state: TextFieldState,
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    val textFieldColors = TextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
    )

    TextField(
        state = state,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(stringResource(R.string.search_messages_hint)) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
            )
        },
        trailingIcon = {
            if (state.text.isNotEmpty()) {
                IconButton(onClick = { state.clearText() }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = stringResource(R.string.dialog_dismiss),
                    )
                }
            }
        },
        lineLimits = TextFieldLineLimits.SingleLine,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        onKeyboardAction = { keyboardController?.hide() },
        shape = MaterialTheme.shapes.extraLarge,
        colors = textFieldColors,
    )
}
