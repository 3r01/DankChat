package com.flxrs.dankchat.ui.chat.messages

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.drawable.Drawable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.asDrawable
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.api.seventv.SevenTVPaint
import com.flxrs.dankchat.data.toUserName
import com.flxrs.dankchat.ui.chat.BadgeUi
import com.flxrs.dankchat.ui.chat.ChatMessageUiState
import com.flxrs.dankchat.ui.chat.emote.EmoteSheetData
import com.flxrs.dankchat.ui.chat.emote.LocalEmoteAnimationCoordinator
import com.flxrs.dankchat.ui.chat.messages.common.MENTIONED_USER_ANNOTATION_TAG
import com.flxrs.dankchat.ui.chat.messages.common.MessageTextWithInlineContent
import com.flxrs.dankchat.ui.chat.messages.common.ResolvedUsernameMention
import com.flxrs.dankchat.ui.chat.messages.common.appendInlineSpacer
import com.flxrs.dankchat.ui.chat.messages.common.appendWithLinks
import com.flxrs.dankchat.ui.chat.messages.common.launchCustomTab
import com.flxrs.dankchat.ui.chat.messages.common.parseUserAnnotation
import com.flxrs.dankchat.ui.chat.messages.common.rememberAdaptiveLinkColor
import com.flxrs.dankchat.ui.chat.messages.common.rememberAdaptiveTextColor
import com.flxrs.dankchat.ui.chat.messages.common.rememberBackgroundColor
import com.flxrs.dankchat.ui.chat.messages.common.rememberNormalizedColor
import com.flxrs.dankchat.ui.chat.messages.common.timestampSpanStyle
import com.flxrs.dankchat.utils.resolve
import android.graphics.Shader.TileMode as AndroidTileMode
import coil3.size.Size as CoilSize

/**
 * Renders a regular chat message with:
 * - Optional reply thread header
 * - Badges and username
 * - Message text with inline emotes
 * - Clickable username and emotes
 * - Long-press to copy message
 */
@OptIn(ExperimentalFoundationApi::class)
@Suppress("LambdaParameterEventTrailing")
@Composable
fun PrivMessageComposable(
    message: ChatMessageUiState.PrivMessageUi,
    fontSize: Float,
    onUserClick: (userId: String?, userName: String, displayName: String, channel: String?, badges: List<BadgeUi>, isLongPress: Boolean) -> Unit,
    onMessageLongClick: (messageId: String, channel: String?, fullMessage: String) -> Unit,
    onEmoteClick: (emotes: List<EmoteSheetData>) -> Unit,
    onReplyClick: (rootMessageId: String, replyName: UserName) -> Unit,
    modifier: Modifier = Modifier,
    onTap: (() -> Unit)? = null,
    highlightShape: Shape = RectangleShape,
    showChannelPrefix: Boolean = false,
    animateGifs: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val backgroundColor = rememberBackgroundColor(message.lightBackgroundColor, message.darkBackgroundColor)

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .alpha(message.textAlpha)
                .background(backgroundColor, highlightShape)
                .then(
                    if (onTap != null) {
                        Modifier.clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = onTap,
                        )
                    } else {
                        Modifier
                    },
                ).indication(interactionSource, ripple())
                .padding(horizontal = 6.dp, vertical = 3.dp),
    ) {
        // Highlight type header (First Time Chat, Elevated Chat)
        if (message.highlightHeader != null) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val headerColor = rememberAdaptiveTextColor(backgroundColor).copy(alpha = 0.6f)
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Chat,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = headerColor,
                )
                Text(
                    text = message.highlightHeader.resolve(),
                    fontSize = (fontSize * 0.9f).sp,
                    fontWeight = FontWeight.Medium,
                    color = headerColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .weight(1f, fill = false),
                )
                if (message.highlightHeaderImageUrl != null && message.highlightHeaderCost != null) {
                    AsyncImage(
                        model = message.highlightHeaderImageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .size(14.dp),
                        alpha = 0.6f,
                    )
                    val costText = buildString {
                        append(message.highlightHeaderCost)
                        if (message.highlightHeaderCostSuffix != null) {
                            append(" ${message.highlightHeaderCostSuffix}")
                        }
                    }
                    Text(
                        text = costText,
                        fontSize = (fontSize * 0.9f).sp,
                        fontWeight = FontWeight.Medium,
                        color = headerColor,
                        modifier = Modifier.padding(start = 2.dp),
                    )
                }
            }
        }

        // Reply thread header
        if (message.thread != null) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { onReplyClick(message.thread.rootId, message.thread.userName.toUserName()) },
                            onLongClick = { onMessageLongClick(message.id, message.channel.value, message.fullMessage) },
                        ).padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val replyColor = rememberAdaptiveTextColor(backgroundColor).copy(alpha = 0.6f)
                val replyNameColor = rememberNormalizedColor(message.thread.rawNameColor, backgroundColor)
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Reply,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = replyColor,
                )
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = replyColor)) {
                            append("Reply to ")
                        }
                        withStyle(SpanStyle(color = replyNameColor)) {
                            append("@${message.thread.userName}: ")
                        }
                        withStyle(SpanStyle(color = replyColor)) {
                            append(message.thread.message)
                        }
                    },
                    fontSize = (fontSize * 0.9f).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // Main message
        PrivMessageText(
            message = message,
            fontSize = fontSize,
            showChannelPrefix = showChannelPrefix,
            animateGifs = animateGifs,
            interactionSource = interactionSource,
            backgroundColor = backgroundColor,
            onUserClick = onUserClick,
            onMessageLongClick = onMessageLongClick,
            onEmoteClick = onEmoteClick,
            onTap = onTap,
        )
    }
}

@Composable
private fun PrivMessageText(
    message: ChatMessageUiState.PrivMessageUi,
    fontSize: Float,
    showChannelPrefix: Boolean,
    animateGifs: Boolean,
    interactionSource: MutableInteractionSource,
    backgroundColor: Color,
    onUserClick: (userId: String?, userName: String, displayName: String, channel: String?, badges: List<BadgeUi>, isLongPress: Boolean) -> Unit,
    onMessageLongClick: (messageId: String, channel: String?, fullMessage: String) -> Unit,
    onEmoteClick: (emotes: List<EmoteSheetData>) -> Unit,
    onTap: (() -> Unit)?,
) {
    val context = LocalPlatformContext.current
    val defaultTextColor = rememberAdaptiveTextColor(backgroundColor)
    val nameColor = rememberNormalizedColor(message.rawNameColor, backgroundColor)
    val nameBrush = rememberSevenTVPaintBrush(message.namePaint, nameColor, message.animateNamePaint)
    val linkColor = rememberAdaptiveLinkColor(backgroundColor)
    val usernameMentions =
        message.usernameMentions.map { mention ->
            ResolvedUsernameMention(
                start = mention.start,
                end = mention.end,
                color = mention.rawColor?.let { rememberNormalizedColor(it, backgroundColor) },
                isBold = mention.isBold,
                userAnnotation = "|${mention.userName.value}|${mention.displayName.value}|${message.channel.value}",
            )
        }

    // Build annotated string with text content. Keyed on the content-affecting fields only,
    // so layout-only copies (rounded corners, divider) don't rebuild the string.
    val annotatedString =
        remember(
            message.id,
            message.timestamp,
            message.badges,
            message.nameText,
            message.message,
            message.emotes,
            usernameMentions,
            message.isAction,
            defaultTextColor,
            nameColor,
            nameBrush,
            showChannelPrefix,
            linkColor,
            fontSize,
        ) {
            buildAnnotatedString {
                // Channel prefix (for mention tab)
                if (showChannelPrefix) {
                    withStyle(
                        SpanStyle(
                            fontWeight = FontWeight.Bold,
                            color = defaultTextColor,
                        ),
                    ) {
                        append("#${message.channel.value} ")
                    }
                }

                // Timestamp
                if (message.timestamp.isNotEmpty()) {
                    withStyle(timestampSpanStyle(fontSize, defaultTextColor)) {
                        append(message.timestamp)
                    }
                    appendInlineSpacer(6.dp)
                }

                // Badges (using appendInlineContent for proper rendering)
                message.badges.forEach { badge ->
                    appendInlineContent("BADGE_${badge.position}", "[badge]")
                    append(" ") // Space between badges
                }

                // Username with click annotation (only if nameText is not empty)
                if (message.nameText.isNotEmpty()) {
                    val nameStyle =
                        nameBrush?.let { SpanStyle(brush = it, fontWeight = FontWeight.Bold) }
                            ?: SpanStyle(fontWeight = FontWeight.Bold, color = nameColor)
                    withStyle(nameStyle) {
                        pushStringAnnotation(
                            tag = "USER",
                            annotation = "${message.userId?.value.orEmpty()}|${message.userName.value}|${message.displayName.value}|${message.channel.value}",
                        )
                        append(message.nameText)
                        pop()
                    }
                }

                // Message text with emotes
                val textColor =
                    if (message.isAction) {
                        nameColor
                    } else {
                        defaultTextColor
                    }

                withStyle(SpanStyle(color = textColor)) {
                    var currentPos = 0
                    message.emotes.sortedBy { it.position.first }.forEach { emote ->
                        // Text before emote
                        if (currentPos < emote.position.first) {
                            val segment = message.message.substring(currentPos, emote.position.first)
                            appendWithLinks(
                                text = segment,
                                segmentStart = currentPos,
                                links = message.links,
                                linkColor = linkColor,
                                usernameMentions = usernameMentions,
                            )
                        }

                        // Emote inline content
                        appendInlineContent("EMOTE_${emote.position}", emote.code)

                        // Cheer amount text
                        if (emote.cheerAmount != null) {
                            withStyle(
                                SpanStyle(
                                    color = emote.cheerColor ?: textColor,
                                    fontWeight = FontWeight.Bold,
                                ),
                            ) {
                                append(emote.cheerAmount.toString())
                            }
                        }

                        // Add space after emote if next character exists and is not whitespace
                        val nextPos = emote.position.last + 1
                        if (nextPos < message.message.length && !message.message[nextPos].isWhitespace()) {
                            append(" ")
                        }

                        currentPos = emote.position.last + 1
                    }

                    // Remaining text
                    if (currentPos < message.message.length) {
                        val segment = message.message.substring(currentPos)
                        appendWithLinks(
                            text = segment,
                            segmentStart = currentPos,
                            links = message.links,
                            linkColor = linkColor,
                            usernameMentions = usernameMentions,
                        )
                    }
                }
            }
        }

    val density = LocalDensity.current
    val paintShadowTexts =
        remember(annotatedString, message.namePaint?.shadows, nameBrush, nameColor, density) {
            val userRange = annotatedString.getStringAnnotations("USER", 0, annotatedString.length).firstOrNull()
            if (userRange == null) {
                emptyList()
            } else {
                message.namePaint
                    ?.shadows
                    .orEmpty()
                    .map { shadow ->
                        buildAnnotatedString {
                            append(annotatedString)
                            val textShadow =
                                Shadow(
                                    color = shadow.rgba.toComposeColor(),
                                    offset = Offset(shadow.xOffset * density.density, shadow.yOffset * density.density),
                                    blurRadius = (shadow.radius * density.density).coerceAtLeast(0f),
                                )
                            addStyle(
                                nameBrush?.let { SpanStyle(brush = it, fontWeight = FontWeight.Bold, shadow = textShadow) }
                                    ?: SpanStyle(color = nameColor, fontWeight = FontWeight.Bold, shadow = textShadow),
                                start = userRange.start,
                                end = userRange.end,
                            )
                        }
                    }
            }
        }

    MessageTextWithInlineContent(
        annotatedString = annotatedString,
        badges = message.badges,
        emotes = message.emotes,
        fontSize = fontSize,
        animateGifs = animateGifs,
        isAsciiArt = message.isAsciiArt,
        interactionSource = interactionSource,
        backgroundTexts = paintShadowTexts,
        onEmoteClick = onEmoteClick,
        onBackgroundClick = {
            onTap?.invoke()
        },
        onTextClick = { offset ->
            val sender = annotatedString.getStringAnnotations("USER", offset, offset).firstOrNull()
            val mentionedUser = annotatedString.getStringAnnotations(MENTIONED_USER_ANNOTATION_TAG, offset, offset).firstOrNull()
            val user = sender ?: mentionedUser
            val url = annotatedString.getStringAnnotations("URL", offset, offset).firstOrNull()
            when {
                user != null -> parseUserAnnotation(user.item)?.let {
                    val badges = if (sender != null) message.badges else emptyList()
                    onUserClick(it.userId, it.userName, it.displayName, it.channel.orEmpty(), badges, false)
                }

                url != null -> launchCustomTab(context, url.item)

                else -> onTap?.invoke()
            }
        },
        onTextLongClick = { offset ->
            val sender = annotatedString.getStringAnnotations("USER", offset, offset).firstOrNull()
            val mentionedUser = annotatedString.getStringAnnotations(MENTIONED_USER_ANNOTATION_TAG, offset, offset).firstOrNull()
            val user = sender ?: mentionedUser

            when {
                user != null -> parseUserAnnotation(user.item)?.let {
                    val badges = if (sender != null) message.badges else emptyList()
                    onUserClick(it.userId, it.userName, it.displayName, it.channel.orEmpty(), badges, true)
                }

                else -> onMessageLongClick(message.id, message.channel.value, message.fullMessage)
            }
        },
    )
}

@Composable
private fun rememberSevenTVPaintBrush(
    paint: SevenTVPaint?,
    baseColor: Color,
    animate: Boolean,
): Brush? {
    val context = LocalPlatformContext.current
    val animationCoordinator = LocalEmoteAnimationCoordinator.current
    val imageUrl = paint?.imageUrl?.takeIf { paint.function.equals("URL", ignoreCase = true) }?.withHttpsScheme()
    var drawable by remember(imageUrl) { mutableStateOf(imageUrl?.let(animationCoordinator::getCached)) }
    var frame by remember(imageUrl) { mutableIntStateOf(0) }

    LaunchedEffect(imageUrl) {
        drawable = imageUrl?.let(animationCoordinator::getCached)
        if (imageUrl == null) return@LaunchedEffect
        if (drawable != null) return@LaunchedEffect
        val result =
            context.imageLoader.execute(
                ImageRequest
                    .Builder(context)
                    .data(imageUrl)
                    .size(CoilSize.ORIGINAL)
                    .build(),
            ) as? SuccessResult
        val loaded = result?.image?.asDrawable(context.resources) ?: return@LaunchedEffect
        animationCoordinator.putInCache(imageUrl, loaded)
        drawable = loaded
    }

    DisposableEffect(drawable, animate) {
        val currentDrawable = drawable
        if (currentDrawable == null) {
            onDispose { }
        } else {
            val invalidationListener: () -> Unit = { frame += 1 }
            animationCoordinator.registerInvalidationListener(currentDrawable, invalidationListener, animate)
            onDispose { animationCoordinator.unregisterInvalidationListener(currentDrawable, invalidationListener) }
        }
    }

    val image = remember(drawable, frame, baseColor) { drawable?.renderOver(baseColor) }
    return remember(paint, baseColor, image) { paint?.toBrush(baseColor, image) }
}

private fun Drawable.renderOver(baseColor: Color): Bitmap {
    val width = intrinsicWidth.coerceAtLeast(1)
    val height = intrinsicHeight.coerceAtLeast(1)
    return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { target ->
        Canvas(target).apply {
            drawColor(baseColor.toArgb())
            setBounds(0, 0, width, height)
            draw(this)
        }
    }
}

private fun SevenTVPaint.toBrush(
    baseColor: Color,
    image: Bitmap?,
): Brush? {
    if (function.equals("URL", ignoreCase = true)) {
        return image?.let(::UrlPaintBrush) ?: SolidPaintBrush(baseColor)
    }
    val parsedStops =
        stops
            .sortedBy(SevenTVPaint.Stop::position)
            .map { stop -> stop.position.coerceIn(0f, 1f) to stop.rgba.toComposeColor().compositeOver(baseColor) }
            .withStrictlyIncreasingPositions()
    if (parsedStops.isEmpty()) {
        return color?.toComposeColor()?.compositeOver(baseColor)?.let(::SolidPaintBrush)
    }

    return when (function.uppercase()) {
        "LINEAR_GRADIENT", "LINEAR-GRADIENT" -> LinearPaintBrush(parsedStops, repeat, angle)
        "RADIAL_GRADIENT", "RADIAL-GRADIENT" -> RadialPaintBrush(parsedStops, repeat)
        else -> null
    }
}

private class UrlPaintBrush(
    private val image: Bitmap,
) : ShaderBrush() {
    override fun createShader(size: Size): Shader = BitmapShader(image, AndroidTileMode.CLAMP, AndroidTileMode.CLAMP).apply {
        setLocalMatrix(
            Matrix().apply {
                setScale(size.width / image.width.coerceAtLeast(1), size.height / image.height.coerceAtLeast(1))
            },
        )
    }
}

private class SolidPaintBrush(
    private val color: Color,
) : ShaderBrush() {
    override fun createShader(size: Size): Shader = LinearGradientShader(
        from = Offset.Zero,
        to = Offset(size.width.coerceAtLeast(1f), 0f),
        colors = listOf(color, color),
    )
}

private class LinearPaintBrush(
    private val stops: List<Pair<Float, Color>>,
    private val repeat: Boolean,
    private val angle: Float,
) : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        val radians = Math.toRadians(angle.toDouble())
        val direction = Offset(kotlin.math.sin(radians).toFloat(), -kotlin.math.cos(radians).toFloat())
        val halfLength = (kotlin.math.abs(direction.x) * size.width + kotlin.math.abs(direction.y) * size.height) / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        val fullStart = center - direction * halfLength
        val fullEnd = center + direction * halfLength
        val firstPosition = stops.first().first
        val lastPosition = stops.last().first
        val canRepeat = repeat && lastPosition > firstPosition
        val start = if (canRepeat) lerp(fullStart, fullEnd, firstPosition) else fullStart
        val end = if (canRepeat) lerp(fullStart, fullEnd, lastPosition) else fullEnd

        return LinearGradientShader(
            from = start,
            to = end,
            colors = stops.map { it.second },
            colorStops = stops.normalizedPositions(canRepeat),
            tileMode = if (canRepeat) TileMode.Repeated else TileMode.Clamp,
        )
    }
}

private class RadialPaintBrush(
    private val stops: List<Pair<Float, Color>>,
    private val repeat: Boolean,
) : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        val lastPosition = stops.last().first
        val canRepeat = repeat && lastPosition > stops.first().first
        val radius = (maxOf(size.width, size.height) / 2f) * if (canRepeat) lastPosition else 1f
        return RadialGradientShader(
            center = Offset(size.width / 2f, size.height / 2f),
            radius = radius.coerceAtLeast(1f),
            colors = stops.map { it.second },
            colorStops = stops.normalizedPositions(canRepeat),
            tileMode = if (canRepeat) TileMode.Repeated else TileMode.Clamp,
        )
    }
}

private fun List<Pair<Float, Color>>.withStrictlyIncreasingPositions(): List<Pair<Float, Color>> {
    var previous = -1f
    return map { (position, color) ->
        val adjusted = if (position <= previous) (previous + PAINT_STOP_EPSILON).coerceAtMost(1f) else position
        previous = adjusted
        adjusted to color
    }
}

private fun List<Pair<Float, Color>>.normalizedPositions(repeat: Boolean): List<Float> {
    if (!repeat) return map { it.first }
    val start = first().first
    val length = last().first - start
    return map { (position) -> (position - start) / length }
}

private fun lerp(
    start: Offset,
    end: Offset,
    fraction: Float,
): Offset = start + (end - start) * fraction

private fun Long.toComposeColor(): Color {
    val rgba = this and 0xFFFFFFFFL
    val argb = ((rgba and 0xFFL) shl 24) or (rgba ushr 8)
    return Color(argb.toInt())
}

private fun String.withHttpsScheme(): String = if (startsWith("//")) "https:$this" else this

private const val PAINT_STOP_EPSILON = 0.0000001f
