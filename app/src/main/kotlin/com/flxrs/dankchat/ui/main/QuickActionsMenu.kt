package com.flxrs.dankchat.ui.main

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider
import com.flxrs.dankchat.R
import com.flxrs.dankchat.preferences.appearance.InputAction
import com.flxrs.dankchat.ui.main.input.TourOverlayState
import kotlinx.collections.immutable.ImmutableList

/**
 * Inline overflow menu for input actions that don't fit in the quick actions bar.
 * Renders as a Surface with rounded top corners, designed to sit directly above
 * the input Surface in a Column layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickActionsMenu(
    surfaceColor: Color,
    visibleActions: ImmutableList<InputAction>,
    enabled: Boolean,
    hasLastMessage: Boolean,
    isStreamActive: Boolean,
    hasStreamData: Boolean,
    isFullscreen: Boolean,
    isModerator: Boolean,
    tourState: TourOverlayState,
    onActionClick: (InputAction) -> Unit,
    onConfigureClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        color = surfaceColor,
        modifier = modifier,
    ) {
        Column(modifier = Modifier.width(IntrinsicSize.Max)) {
            for (action in InputAction.entries) {
                if (action in visibleActions) continue
                val overflowItem = getOverflowItem(
                    action = action,
                    isStreamActive = isStreamActive,
                    hasStreamData = hasStreamData,
                    isFullscreen = isFullscreen,
                    isModerator = isModerator,
                )
                if (overflowItem != null) {
                    val actionEnabled = isActionEnabled(action, enabled, hasLastMessage)
                    DropdownMenuItem(
                        text = { Text(stringResource(overflowItem.labelRes)) },
                        onClick = { onActionClick(action) },
                        enabled = actionEnabled,
                        leadingIcon = {
                            Icon(
                                imageVector = overflowItem.icon,
                                contentDescription = null,
                            )
                        },
                    )
                }
            }

            HorizontalDivider()

            val configureItem: @Composable () -> Unit = {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.input_action_configure)) },
                    onClick = {
                        when {
                            tourState.configureActionsTooltipState != null -> tourState.onAdvance?.invoke()
                            else                                           -> onConfigureClick()
                        }
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                        )
                    },
                )
            }
            when {
                tourState.configureActionsTooltipState != null -> {
                    TooltipBox(
                        positionProvider = rememberStartAlignedTooltipPositionProvider(),
                        tooltip = {
                            EndCaretTourTooltip(
                                text = stringResource(R.string.tour_configure_actions),
                                onAction = { tourState.onAdvance?.invoke() },
                                onSkip = { tourState.onSkip?.invoke() },
                            )
                        },
                        state = tourState.configureActionsTooltipState,
                        onDismissRequest = {},
                        focusable = true,
                        hasAction = true,
                    ) {
                        configureItem()
                    }
                }

                else                                           -> configureItem()
            }
        }
    }
}

@Immutable
private data class OverflowItem(
    val labelRes: Int,
    val icon: ImageVector,
)

private fun getOverflowItem(
    action: InputAction,
    isStreamActive: Boolean,
    hasStreamData: Boolean,
    isFullscreen: Boolean,
    isModerator: Boolean,
): OverflowItem? = when (action) {
    InputAction.Search      -> OverflowItem(
        labelRes = R.string.input_action_search,
        icon = Icons.Default.Search,
    )

    InputAction.LastMessage -> OverflowItem(
        labelRes = R.string.input_action_last_message,
        icon = Icons.Default.History,
    )

    InputAction.Stream      -> when {
        hasStreamData || isStreamActive -> OverflowItem(
            labelRes = if (isStreamActive) R.string.menu_hide_stream else R.string.menu_show_stream,
            icon = if (isStreamActive) Icons.Default.VideocamOff else Icons.Default.Videocam,
        )

        else                            -> null
    }

    InputAction.ModActions  -> when {
        isModerator -> OverflowItem(
            labelRes = R.string.menu_mod_actions,
            icon = Icons.Default.Shield,
        )

        else        -> null
    }

    InputAction.Fullscreen  -> OverflowItem(
        labelRes = if (isFullscreen) R.string.menu_exit_fullscreen else R.string.menu_fullscreen,
        icon = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
    )

    InputAction.HideInput   -> OverflowItem(
        labelRes = R.string.menu_hide_input,
        icon = Icons.Default.VisibilityOff,
    )

    InputAction.Debug       -> OverflowItem(
        labelRes = R.string.input_action_debug,
        icon = Icons.Default.BugReport,
    )
}

private fun isActionEnabled(action: InputAction, inputEnabled: Boolean, hasLastMessage: Boolean): Boolean = when (action) {
    InputAction.Search, InputAction.Fullscreen, InputAction.HideInput, InputAction.Debug -> true
    InputAction.LastMessage                                                              -> inputEnabled && hasLastMessage
    InputAction.Stream, InputAction.ModActions                                           -> inputEnabled
}

/**
 * Tour tooltip positioned to the start of its anchor, with a right-pointing caret on the end side.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EndCaretTourTooltip(
    text: String,
    onAction: () -> Unit,
    onSkip: () -> Unit,
) {
    val containerColor = MaterialTheme.colorScheme.secondaryContainer
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = containerColor,
            shadowElevation = 2.dp,
            tonalElevation = 2.dp,
            modifier = Modifier.widthIn(max = 220.dp),
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 12.dp, bottom = 8.dp)
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.align(Alignment.End),
                ) {
                    TextButton(onClick = onSkip) {
                        Text(stringResource(R.string.tour_skip))
                    }
                    TextButton(onClick = onAction) {
                        Text(stringResource(R.string.tour_next))
                    }
                }
            }
        }
        Canvas(modifier = Modifier.size(width = 12.dp, height = 24.dp)) {
            val path = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, size.height / 2f)
                lineTo(0f, size.height)
                close()
            }
            drawPath(path, containerColor)
        }
    }
}

/**
 * Positions the tooltip to the start (left in LTR) of the anchor, vertically centered.
 * Falls back to above-positioning if there's not enough horizontal space.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun rememberStartAlignedTooltipPositionProvider(
    spacingBetweenTooltipAndAnchor: Dp = 4.dp,
): PopupPositionProvider {
    val spacingPx = with(LocalDensity.current) { spacingBetweenTooltipAndAnchor.roundToPx() }
    return remember(spacingPx) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ): IntOffset {
                val startX = anchorBounds.left - popupContentSize.width - spacingPx
                return if (startX >= 0) {
                    // Fits to the start — vertically center on anchor
                    val y = anchorBounds.top + (anchorBounds.height - popupContentSize.height) / 2
                    IntOffset(
                        startX,
                        y.coerceIn(0, (windowSize.height - popupContentSize.height).coerceAtLeast(0)),
                    )
                } else {
                    // Not enough space — fall back to above, horizontally end-aligned with anchor
                    val x = (anchorBounds.right - popupContentSize.width)
                        .coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))
                    val y = (anchorBounds.top - popupContentSize.height - spacingPx)
                        .coerceIn(0, (windowSize.height - popupContentSize.height).coerceAtLeast(0))
                    IntOffset(x, y)
                }
            }
        }
    }
}
