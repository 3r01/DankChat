package com.flxrs.dankchat.ui.main.input

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.flxrs.dankchat.preferences.appearance.InputAction
import com.flxrs.dankchat.utils.compose.rememberRoundedCornerHorizontalPadding
import kotlinx.collections.immutable.ImmutableList

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ChatBottomBar(
    showInput: Boolean,
    textFieldState: TextFieldState,
    inputState: ChatInputUiState,
    isUploading: Boolean,
    isLoading: Boolean,
    isFullscreen: Boolean,
    isModerator: Boolean,
    isStreamActive: Boolean,
    hasStreamData: Boolean,
    isSheetOpen: Boolean,
    inputActions: ImmutableList<InputAction>,
    characterCounter: CharacterCounterState = CharacterCounterState.Hidden,
    onSend: () -> Unit,
    onLastMessageClick: () -> Unit,
    onEmoteClick: () -> Unit,
    onOverlayDismiss: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onToggleInput: () -> Unit,
    onToggleStream: () -> Unit,
    onModActions: () -> Unit,
    onSearchClick: () -> Unit,
    onDebugInfoClick: () -> Unit = {},
    debugMode: Boolean = false,
    onNewWhisper: (() -> Unit)?,
    onInputActionsChanged: (ImmutableList<InputAction>) -> Unit,
    overflowExpanded: Boolean = false,
    onOverflowExpandedChanged: (Boolean) -> Unit = {},
    onInputHeightChanged: (Int) -> Unit,
    onHelperTextHeightChanged: (Int) -> Unit = {},
    isInSplitLayout: Boolean = false,
    instantHide: Boolean = false,
    tourState: TourOverlayState = TourOverlayState(),
    isRepeatedSendEnabled: Boolean = false,
    onRepeatedSendChanged: (Boolean) -> Unit = {},
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        AnimatedVisibility(
            visible = showInput,
            enter = EnterTransition.None,
            exit = when {
                instantHide -> ExitTransition.None
                else        -> slideOutVertically(targetOffsetY = { it })
            },
        ) {
            ChatInputLayout(
                textFieldState = textFieldState,
                inputState = inputState.inputState,
                enabled = inputState.enabled,
                hasLastMessage = inputState.hasLastMessage,
                canSend = inputState.canSend,
                isEmoteMenuOpen = inputState.isEmoteMenuOpen,
                helperText = if (isSheetOpen) null else inputState.helperText,
                isUploading = isUploading,
                isLoading = isLoading,
                isFullscreen = isFullscreen,
                isModerator = isModerator,
                isStreamActive = isStreamActive,
                hasStreamData = hasStreamData,
                inputActions = inputActions,
                characterCounter = characterCounter,
                onSend = onSend,
                onLastMessageClick = onLastMessageClick,
                onEmoteClick = onEmoteClick,
                overlay = inputState.overlay,
                onOverlayDismiss = onOverlayDismiss,
                onToggleFullscreen = onToggleFullscreen,
                onToggleInput = onToggleInput,
                onToggleStream = onToggleStream,
                onModActions = onModActions,
                onInputActionsChanged = onInputActionsChanged,
                onSearchClick = onSearchClick,
                onDebugInfoClick = onDebugInfoClick,
                debugMode = debugMode,
                onNewWhisper = onNewWhisper,
                overflowExpanded = overflowExpanded,
                onOverflowExpandedChanged = onOverflowExpandedChanged,
                showQuickActions = !isSheetOpen,
                tourState = tourState,
                isRepeatedSendEnabled = isRepeatedSendEnabled,
                onRepeatedSendChanged = onRepeatedSendChanged,
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    onInputHeightChanged(coordinates.size.height)
                }
            )
        }

        // Sticky helper text + nav bar spacer when input is hidden
        if (!showInput && !isSheetOpen) {
            val helperText = inputState.helperText
            if (!helperText.isNullOrEmpty()) {
                val horizontalPadding = when {
                    isFullscreen && isInSplitLayout -> {
                        val rcPadding = rememberRoundedCornerHorizontalPadding(fallback = 16.dp)
                        val direction = LocalLayoutDirection.current
                        PaddingValues(start = 16.dp, end = rcPadding.calculateEndPadding(direction))
                    }

                    isFullscreen                    -> rememberRoundedCornerHorizontalPadding(fallback = 16.dp)
                    else                            -> PaddingValues(horizontal = 16.dp)
                }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { onHelperTextHeightChanged(it.size.height) }
                ) {
                    Text(
                        text = helperText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier
                            .navigationBarsPadding()
                            .fillMaxWidth()
                            .padding(horizontalPadding)
                            .padding(vertical = 6.dp)
                            .basicMarquee(),
                        textAlign = TextAlign.Start
                    )
                }
            }
        }
    }
}
