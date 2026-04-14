package com.flxrs.dankchat.ui.main.input

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.flxrs.dankchat.preferences.appearance.InputAction
import com.flxrs.dankchat.utils.compose.rememberRoundedCornerHorizontalPadding
import com.flxrs.dankchat.utils.resolve
import kotlinx.collections.immutable.ImmutableList

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ChatBottomBar(
    showInput: Boolean,
    textFieldState: TextFieldState,
    uiState: ChatInputUiState,
    callbacks: ChatInputCallbacks,
    isUploading: Boolean,
    isLoading: Boolean,
    isFullscreen: Boolean,
    isModerator: Boolean,
    isStreamActive: Boolean,
    isAudioOnly: Boolean,
    hasStreamData: Boolean,
    isSheetOpen: Boolean,
    inputActions: ImmutableList<InputAction>,
    onInputHeightChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    debugMode: Boolean = false,
    overflowExpanded: Boolean = false,
    onOverflowExpandedChange: (Boolean) -> Unit = {},
    onHelperTextHeightChange: (Int) -> Unit = {},
    isInSplitLayout: Boolean = false,
    instantHide: Boolean = false,
    tourState: TourOverlayState = TourOverlayState(),
    isRepeatedSendEnabled: Boolean = false,
) {
    val inputVisibleState = remember { MutableTransitionState(showInput) }
    inputVisibleState.targetState = showInput
    val inputFullyHidden = !inputVisibleState.targetState && inputVisibleState.isIdle

    Column(modifier = modifier.fillMaxWidth()) {
        AnimatedVisibility(
            visibleState = inputVisibleState,
            enter = EnterTransition.None,
            exit =
                when {
                    instantHide -> ExitTransition.None
                    else -> slideOutVertically(targetOffsetY = { it })
                },
        ) {
            ChatInputLayout(
                textFieldState = textFieldState,
                uiState = uiState,
                callbacks = callbacks,
                isSheetOpen = isSheetOpen,
                isUploading = isUploading,
                isLoading = isLoading,
                isFullscreen = isFullscreen,
                isModerator = isModerator,
                isStreamActive = isStreamActive,
                isAudioOnly = isAudioOnly,
                hasStreamData = hasStreamData,
                inputActions = inputActions,
                debugMode = debugMode,
                overflowExpanded = overflowExpanded,
                onOverflowExpandedChange = onOverflowExpandedChange,
                tourState = tourState,
                isRepeatedSendEnabled = isRepeatedSendEnabled,
                modifier =
                    Modifier.onSizeChanged { size ->
                        onInputHeightChange(size.height)
                    },
            )
        }

        // Sticky helper text + nav bar spacer — wait for exit animation to finish
        if (inputFullyHidden && !isSheetOpen) {
            val helperTextState = uiState.helperText
            if (!helperTextState.isEmpty) {
                val horizontalPadding =
                    when {
                        isFullscreen && isInSplitLayout -> {
                            val rcPadding = rememberRoundedCornerHorizontalPadding(fallback = 16.dp)
                            val direction = LocalLayoutDirection.current
                            PaddingValues(start = 16.dp, end = rcPadding.calculateEndPadding(direction))
                        }

                        isFullscreen -> {
                            rememberRoundedCornerHorizontalPadding(fallback = 16.dp)
                        }

                        else -> {
                            PaddingValues(horizontal = 16.dp)
                        }
                    }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.85f),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .onSizeChanged { onHelperTextHeightChange(it.height) },
                ) {
                    ExpandableHelperText(
                        helperText = helperTextState,
                        modifier =
                            Modifier
                                .navigationBarsPadding()
                                .padding(horizontalPadding)
                                .padding(vertical = 6.dp),
                    )
                }
            }
        }
    }
}
