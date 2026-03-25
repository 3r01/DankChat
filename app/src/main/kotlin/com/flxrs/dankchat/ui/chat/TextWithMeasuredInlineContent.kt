package com.flxrs.dankchat.ui.chat

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
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
 * @param knownDimensions Optional pre-known dimensions for inline content IDs, skipping measurement subcomposition
 * @param onTextClick Callback for click events with offset position
 * @param onTextLongClick Callback for long-click events with offset position
 * @param interactionSource Optional interaction source for ripple effects
 */
@Composable
fun TextWithMeasuredInlineContent(
    text: AnnotatedString,
    inlineContentProviders: Map<String, @Composable () -> Unit>,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    knownDimensions: Map<String, EmoteDimensions> = emptyMap(),
    onTextClick: ((Int) -> Unit)? = null,
    onTextLongClick: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val textLayoutResultRef = remember { mutableStateOf<TextLayoutResult?>(null) }

    SubcomposeLayout(modifier = modifier) { constraints ->
        // Phase 1: Measure inline content to get actual dimensions
        // Skip measurement for IDs with pre-known dimensions (from cache)
        val measuredDimensions = mutableMapOf<String, EmoteDimensions>()

        // Add all pre-known dimensions first
        measuredDimensions.putAll(knownDimensions)

        // Only measure items that don't have known dimensions
        inlineContentProviders.forEach { (id, provider) ->
            if (id !in knownDimensions) {
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
        }

        // Phase 2: Create InlineTextContent with measured/known dimensions
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

        val textMeasurables = subcompose("text") {
            BasicText(
                text = text,
                style = style,
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
                            textLayoutResultRef.value?.let { layoutResult ->
                                val line = layoutResult.getLineForVerticalPosition(offset.y)
                                val lineLeft = layoutResult.getLineLeft(line)
                                val lineRight = layoutResult.getLineRight(line)
                                if (offset.x in lineLeft..lineRight) {
                                    val position = layoutResult.getOffsetForPosition(offset)
                                    onTextClick?.invoke(position)
                                }
                            }
                        },
                        onLongPress = {
                            onTextLongClick?.invoke()
                        }
                    )
                },
                onTextLayout = { layoutResult ->
                    textLayoutResultRef.value = layoutResult
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
