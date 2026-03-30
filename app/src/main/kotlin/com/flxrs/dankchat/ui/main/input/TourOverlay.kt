package com.flxrs.dankchat.ui.main.input

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TooltipScope
import androidx.compose.material3.TooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.flxrs.dankchat.R

@OptIn(ExperimentalMaterial3Api::class)
@Immutable
data class TourOverlayState(
    val inputActionsTooltipState: TooltipState? = null,
    val overflowMenuTooltipState: TooltipState? = null,
    val configureActionsTooltipState: TooltipState? = null,
    val swipeGestureTooltipState: TooltipState? = null,
    val forceOverflowOpen: Boolean = false,
    val isTourActive: Boolean = false,
    val onAdvance: (() -> Unit)? = null,
    val onSkip: (() -> Unit)? = null,
)

@Suppress("ContentSlotReused")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OptionalTourTooltip(
    tooltipState: TooltipState?,
    text: String,
    onAdvance: (() -> Unit)?,
    onSkip: (() -> Unit)?,
    focusable: Boolean = false,
    content: @Composable () -> Unit,
) {
    if (tooltipState != null) {
        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
            tooltip = {
                TourTooltip(
                    text = text,
                    onAction = { onAdvance?.invoke() },
                    onSkip = { onSkip?.invoke() },
                )
            },
            state = tooltipState,
            onDismissRequest = {},
            focusable = focusable,
            hasAction = true,
        ) {
            content()
        }
    } else {
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TooltipScope.TourTooltip(
    text: String,
    onAction: () -> Unit,
    onSkip: () -> Unit,
    isLast: Boolean = false,
) {
    val tourColors =
        TooltipDefaults.richTooltipColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            actionContentColor = MaterialTheme.colorScheme.secondary,
        )
    RichTooltip(
        colors = tourColors,
        caretShape = TooltipDefaults.caretShape(caretSize = DpSize(24.dp, 12.dp)),
        action = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onSkip) {
                    Text(stringResource(R.string.tour_skip))
                }
                TextButton(onClick = onAction) {
                    Text(stringResource(if (isLast) R.string.tour_got_it else R.string.tour_next))
                }
            }
        },
    ) {
        Text(text)
    }
}
