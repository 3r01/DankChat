package com.flxrs.dankchat.ui.main.input

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
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
    Column(modifier = modifier.fillMaxWidth()) {
        AnimatedVisibility(
            visible = showInput,
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
                hasStreamData = hasStreamData,
                inputActions = inputActions,
                debugMode = debugMode,
                overflowExpanded = overflowExpanded,
                onOverflowExpandedChange = onOverflowExpandedChange,
                tourState = tourState,
                isRepeatedSendEnabled = isRepeatedSendEnabled,
                modifier =
                    Modifier.onGloballyPositioned { coordinates ->
                        onInputHeightChange(coordinates.size.height)
                    },
            )
        }

        // Sticky helper text + nav bar spacer when input is hidden
        if (!showInput && !isSheetOpen) {
            val helperTextState = uiState.helperText
            val resolvedRoomState = helperTextState.roomStateParts.map { it.resolve() }
            val roomStateText = resolvedRoomState.joinToString(separator = ", ")
            val streamInfoText = helperTextState.streamInfo
            val combinedText = listOfNotNull(roomStateText.ifEmpty { null }, streamInfoText).joinToString(separator = " - ")
            if (combinedText.isNotEmpty()) {
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
                val textMeasurer = rememberTextMeasurer()
                val style = MaterialTheme.typography.labelSmall
                val density = LocalDensity.current
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.85f),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { onHelperTextHeightChange(it.size.height) },
                ) {
                    BoxWithConstraints(
                        modifier =
                            Modifier
                                .navigationBarsPadding()
                                .fillMaxWidth()
                                .padding(horizontalPadding)
                                .padding(vertical = 6.dp)
                                .animateContentSize(),
                    ) {
                        val maxWidthPx = with(density) { maxWidth.roundToPx() }
                        val fitsOnOneLine =
                            remember(combinedText, style, maxWidthPx) {
                                textMeasurer.measure(combinedText, style).size.width <= maxWidthPx
                            }
                        when {
                            fitsOnOneLine || streamInfoText == null || roomStateText.isEmpty() -> {
                                Text(
                                    text = combinedText,
                                    style = style,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    modifier = Modifier.fillMaxWidth().basicMarquee(iterations = Int.MAX_VALUE),
                                )
                            }

                            else -> {
                                Column {
                                    Text(
                                        text = roomStateText,
                                        style = style,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        modifier = Modifier.fillMaxWidth().basicMarquee(iterations = Int.MAX_VALUE),
                                    )
                                    Text(
                                        text = streamInfoText,
                                        style = style,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        modifier = Modifier.fillMaxWidth().basicMarquee(iterations = Int.MAX_VALUE),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
