package com.flxrs.dankchat.ui.chat.messages.common

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.launch

data class EmoteDimensions(
    val id: String,
    val widthPx: Int,
    val heightPx: Int,
)

@Composable
fun TextWithMeasuredInlineContent(
    text: AnnotatedString,
    inlineContentProviders: ImmutableMap<String, @Composable () -> Unit>,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    knownDimensions: ImmutableMap<String, EmoteDimensions> = persistentMapOf(),
    onTextClick: ((Int) -> Unit)? = null,
    onTextLongClick: ((Int) -> Unit)? = null,
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

        // Resolve spacer inline content from annotated string annotations
        text
            .getStringAnnotations(INLINE_CONTENT_TAG, 0, text.length)
            .filter { isSpacerId(it.item) && it.item !in measuredDimensions }
            .distinctBy { it.item }
            .forEach { annotation ->
                val widthPx = spacerWidthDp(annotation.item).dp.toPx().toInt()
                measuredDimensions[annotation.item] = EmoteDimensions(annotation.item, widthPx, 1)
            }

        // Only measure items that don't have known dimensions
        inlineContentProviders.forEach { (id, provider) ->
            if (id !in knownDimensions) {
                val measurables = subcompose("measure_$id", provider)
                if (measurables.isNotEmpty()) {
                    // Measure with unbounded constraints to get natural size
                    val placeable =
                        measurables.first().measure(
                            Constraints(
                                maxWidth = constraints.maxWidth,
                                maxHeight = Constraints.Infinity,
                            ),
                        )
                    measuredDimensions[id] =
                        EmoteDimensions(
                            id = id,
                            widthPx = placeable.width,
                            heightPx = placeable.height,
                        )
                }
            }
        }

        // Phase 2: Create InlineTextContent with measured/known dimensions
        val inlineContent =
            measuredDimensions.mapValues { (id, dimensions) ->
                InlineTextContent(
                    placeholder =
                        Placeholder(
                            width = with(density) { dimensions.widthPx.toDp() }.value.sp,
                            height = with(density) { dimensions.heightPx.toDp() }.value.sp,
                            placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
                        ),
                ) {
                    // Render the actual content (re-compose with same provider)
                    inlineContentProviders[id]?.invoke()
                }
            }

        // Phase 3: Compose the text with correct inline content

        val textMeasurables =
            subcompose("text") {
                BasicText(
                    text = text,
                    style = style,
                    inlineContent = inlineContent,
                    modifier =
                        Modifier.pointerInput(text, interactionSource) {
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
                                onLongPress = { offset ->
                                    val layoutResult = textLayoutResultRef.value
                                    if (layoutResult != null) {
                                        val line = layoutResult.getLineForVerticalPosition(offset.y)
                                        val lineLeft = layoutResult.getLineLeft(line)
                                        val lineRight = layoutResult.getLineRight(line)
                                        if (offset.x in lineLeft..lineRight) {
                                            onTextLongClick?.invoke(layoutResult.getOffsetForPosition(offset))
                                        } else {
                                            onTextLongClick?.invoke(-1)
                                        }
                                    } else {
                                        onTextLongClick?.invoke(-1)
                                    }
                                },
                            )
                        },
                    onTextLayout = { layoutResult ->
                        textLayoutResultRef.value = layoutResult
                    },
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

private const val INLINE_CONTENT_TAG = "androidx.compose.foundation.text.inlineContent"
