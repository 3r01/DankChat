package com.flxrs.dankchat.chat.compose

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * Data class to hold measured emote dimensions
 */
data class EmoteDimensions(
    val id: String,
    val widthPx: Int,
    val heightPx: Int
)

/**
 * Renders text with inline images (badges, emotes) using SubcomposeLayout.
 * 
 * This solves the fundamental problem with InlineTextContent: we need to know
 * the size of images before creating Placeholder objects, but images load asynchronously.
 * 
 * SubcomposeLayout allows us to:
 * 1. First measure all inline images to get their actual dimensions
 * 2. Create InlineTextContent with correct Placeholder sizes
 * 3. Finally compose the text with properly sized placeholders
 * 
 * This maintains natural text flow (like TextView) while supporting variable-sized
 * inline content (like ImageSpans with different drawable sizes).
 * 
 * @param text The AnnotatedString with annotations marking where inline content goes
 * @param inlineContentProviders Map of content IDs to composables that will be measured
 * @param modifier Modifier for the text
 * @param onTextClick Callback for click events with offset position
 * @param onTextLongClick Callback for long-click events with offset position
 * @param interactionSource Optional interaction source for ripple effects
 */
@Composable
fun TextWithMeasuredInlineContent(
    text: AnnotatedString,
    inlineContentProviders: Map<String, @Composable () -> Unit>,
    modifier: Modifier = Modifier,
    onTextClick: ((Int) -> Unit)? = null,
    onTextLongClick: ((Int) -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    
    SubcomposeLayout(modifier = modifier) { constraints ->
        // Phase 1: Measure all inline content to get actual dimensions
        val measuredDimensions = mutableMapOf<String, EmoteDimensions>()
        
        inlineContentProviders.forEach { (id, provider) ->
            val measurables = subcompose("measure_$id", provider)
            if (measurables.isNotEmpty()) {
                // Measure with unbounded constraints to get natural size
                val placeable = measurables.first().measure(
                    Constraints(
                        maxWidth = constraints.maxWidth,
                        maxHeight = Constraints.Infinity
                    )
                )
                measuredDimensions[id] = EmoteDimensions(
                    id = id,
                    widthPx = placeable.width,
                    heightPx = placeable.height
                )
            }
        }
        
        // Phase 2: Create InlineTextContent with measured dimensions
        val inlineContent = measuredDimensions.mapValues { (id, dimensions) ->
            InlineTextContent(
                placeholder = Placeholder(
                    width = with(density) { dimensions.widthPx.toDp() }.value.sp,
                    height = with(density) { dimensions.heightPx.toDp() }.value.sp,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                )
            ) {
                // Render the actual content (re-compose with same provider)
                inlineContentProviders[id]?.invoke()
            }
        }
        
        // Phase 3: Compose the text with correct inline content
        var textLayoutResult: androidx.compose.ui.text.TextLayoutResult? = null
        
        val textMeasurables = subcompose("text") {
            BasicText(
                text = text,
                inlineContent = inlineContent,
                modifier = Modifier.pointerInput(text, interactionSource) {
                    detectTapGestures(
                        onPress = { offset ->
                            // Emit press interaction for ripple effect
                            interactionSource?.let { source ->
                                val press = PressInteraction.Press(offset)
                                coroutineScope.launch {
                                    source.emit(press)
                                    tryAwaitRelease()
                                    source.emit(PressInteraction.Release(press))
                                }
                            }
                        },
                        onTap = { offset ->
                            textLayoutResult?.let { layoutResult ->
                                // Precision check: make sure the click is actually on text
                                val isYWithinBounds = offset.y >= 0 && offset.y <= layoutResult.size.height
                                if (isYWithinBounds) {
                                    val line = layoutResult.getLineForVerticalPosition(offset.y)
                                    val isXWithinBounds = offset.x >= layoutResult.getLineLeft(line) && offset.x <= layoutResult.getLineRight(line)
                                    if (isXWithinBounds) {
                                        val position = layoutResult.getOffsetForPosition(offset)
                                        onTextClick?.invoke(position)
                                    }
                                }
                            }
                        },
                        onLongPress = { offset ->
                            textLayoutResult?.let { layoutResult ->
                                // Precision check: make sure the click is actually on text
                                val isYWithinBounds = offset.y >= 0 && offset.y <= layoutResult.size.height
                                if (isYWithinBounds) {
                                    val line = layoutResult.getLineForVerticalPosition(offset.y)
                                    val isXWithinBounds = offset.x >= layoutResult.getLineLeft(line) && offset.x <= layoutResult.getLineRight(line)
                                    if (isXWithinBounds) {
                                        val position = layoutResult.getOffsetForPosition(offset)
                                        onTextLongClick?.invoke(position)
                                    }
                                }
                            }
                        }
                    )
                },
                onTextLayout = { layoutResult ->
                    textLayoutResult = layoutResult
                }
            )
        }
        
        if (textMeasurables.isEmpty()) {
            return@SubcomposeLayout layout(0, 0) {}
        }
        
        // Phase 4: Measure and layout the text
        val textPlaceable = textMeasurables.first().measure(constraints)
        
        layout(textPlaceable.width, textPlaceable.height) {
            textPlaceable.place(0, 0)
        }
    }
}

/**
 * Simpler version that just wraps BasicText with measured inline content.
 * Use this when you already have the dimensions or don't need click handling.
 */
@Composable
fun MeasuredInlineText(
    text: AnnotatedString,
    inlineContent: Map<String, InlineTextContent>,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        BasicText(
            text = text,
            inlineContent = inlineContent
        )
    }
}
