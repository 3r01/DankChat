package com.flxrs.dankchat.ui.chat.messages.common

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
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
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
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
    val allDimensionsKnown = inlineContentProviders.keys.all { it in knownDimensions }

    if (allDimensionsKnown) {
        // Fast path: all dimensions cached, skip SubcomposeLayout overhead
        val inlineContent = remember(knownDimensions, text, density, inlineContentProviders) {
            buildInlineContentMap(knownDimensions, text, density, inlineContentProviders)
        }
        ClickableInlineText(
            text = text,
            style = style,
            inlineContent = inlineContent,
            modifier = modifier,
            onTextClick = onTextClick,
            onTextLongClick = onTextLongClick,
            interactionSource = interactionSource,
        )
    } else {
        // Slow path: SubcomposeLayout to measure unknown inline content dimensions
        SubcomposeMeasuredInlineText(
            text = text,
            inlineContentProviders = inlineContentProviders,
            knownDimensions = knownDimensions,
            density = density,
            style = style,
            modifier = modifier,
            onTextClick = onTextClick,
            onTextLongClick = onTextLongClick,
            interactionSource = interactionSource,
        )
    }
}

@Composable
private fun ClickableInlineText(
    text: AnnotatedString,
    style: TextStyle,
    inlineContent: Map<String, InlineTextContent>,
    onTextClick: ((Int) -> Unit)?,
    onTextLongClick: ((Int) -> Unit)?,
    interactionSource: MutableInteractionSource?,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val textLayoutResultRef = remember { mutableStateOf<TextLayoutResult?>(null) }

    BasicText(
        text = text,
        style = style,
        inlineContent = inlineContent,
        modifier =
            modifier.pointerInput(text, interactionSource) {
                detectTapGestures(
                    onPress = { offset ->
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
                                onTextClick?.invoke(layoutResult.getOffsetForPosition(offset))
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

@Composable
private fun SubcomposeMeasuredInlineText(
    text: AnnotatedString,
    inlineContentProviders: ImmutableMap<String, @Composable () -> Unit>,
    knownDimensions: ImmutableMap<String, EmoteDimensions>,
    density: Density,
    style: TextStyle,
    onTextClick: ((Int) -> Unit)?,
    onTextLongClick: ((Int) -> Unit)?,
    interactionSource: MutableInteractionSource?,
    modifier: Modifier = Modifier,
) {
    SubcomposeLayout(modifier = modifier) { constraints ->
        val measuredDimensions = mutableMapOf<String, EmoteDimensions>()
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

        val inlineContent = buildInlineContentMapFromDimensions(measuredDimensions, density, inlineContentProviders)

        val textMeasurables =
            subcompose("text") {
                ClickableInlineText(
                    text = text,
                    style = style,
                    inlineContent = inlineContent,
                    onTextClick = onTextClick,
                    onTextLongClick = onTextLongClick,
                    interactionSource = interactionSource,
                )
            }

        if (textMeasurables.isEmpty()) {
            return@SubcomposeLayout layout(0, 0) {}
        }

        val textPlaceable = textMeasurables.first().measure(constraints)

        layout(textPlaceable.width, textPlaceable.height) {
            textPlaceable.place(0, 0)
        }
    }
}

private fun buildInlineContentMap(
    knownDimensions: ImmutableMap<String, EmoteDimensions>,
    text: AnnotatedString,
    density: Density,
    providers: ImmutableMap<String, @Composable () -> Unit>,
): Map<String, InlineTextContent> {
    val allDimensions = buildMap {
        putAll(knownDimensions)
        // Resolve spacers from annotated string
        text
            .getStringAnnotations(INLINE_CONTENT_TAG, 0, text.length)
            .filter { isSpacerId(it.item) && it.item !in knownDimensions }
            .distinctBy { it.item }
            .forEach { annotation ->
                val widthPx = with(density) { spacerWidthDp(annotation.item).dp.toPx().toInt() }
                put(annotation.item, EmoteDimensions(annotation.item, widthPx, 1))
            }
    }
    return buildInlineContentMapFromDimensions(allDimensions, density, providers)
}

private fun buildInlineContentMapFromDimensions(
    dimensions: Map<String, EmoteDimensions>,
    density: Density,
    providers: ImmutableMap<String, @Composable () -> Unit>,
): Map<String, InlineTextContent> = dimensions.mapValues { (id, dims) ->
    InlineTextContent(
        placeholder =
            Placeholder(
                width = with(density) { dims.widthPx.toDp().toSp() },
                height = with(density) { dims.heightPx.toDp().toSp() },
                placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
            ),
    ) {
        providers[id]?.invoke()
    }
}

private const val INLINE_CONTENT_TAG = "androidx.compose.foundation.text.inlineContent"
