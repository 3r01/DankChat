package com.flxrs.dankchat.ui.chat.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.LocalPlatformContext
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.toUserName
import com.flxrs.dankchat.data.twitch.emote.ChatMessageEmote
import com.flxrs.dankchat.ui.chat.BadgeUi
import com.flxrs.dankchat.ui.chat.ChatMessageUiState
import com.flxrs.dankchat.ui.chat.appendWithLinks
import com.flxrs.dankchat.ui.chat.messages.common.MessageTextWithInlineContent
import com.flxrs.dankchat.ui.chat.messages.common.launchCustomTab
import com.flxrs.dankchat.ui.chat.messages.common.parseUserAnnotation
import com.flxrs.dankchat.ui.chat.messages.common.timestampSpanStyle
import com.flxrs.dankchat.ui.chat.rememberAdaptiveTextColor
import com.flxrs.dankchat.ui.chat.rememberBackgroundColor
import com.flxrs.dankchat.ui.chat.rememberNormalizedColor
import com.flxrs.dankchat.utils.resolve

/**
 * Renders a regular chat message with:
 * - Optional reply thread header
 * - Badges and username
 * - Message text with inline emotes
 * - Clickable username and emotes
 * - Long-press to copy message
 */
@Suppress("LambdaParameterEventTrailing")
@Composable
fun PrivMessageComposable(
    message: ChatMessageUiState.PrivMessageUi,
    fontSize: Float,
    onUserClick: (userId: String?, userName: String, displayName: String, channel: String?, badges: List<BadgeUi>, isLongPress: Boolean) -> Unit,
    onMessageLongClick: (messageId: String, channel: String?, fullMessage: String) -> Unit,
    onEmoteClick: (emotes: List<ChatMessageEmote>) -> Unit,
    onReplyClick: (rootMessageId: String, replyName: UserName) -> Unit,
    modifier: Modifier = Modifier,
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
                .indication(interactionSource, ripple())
                .padding(horizontal = 2.dp, vertical = 2.dp),
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
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }

        // Reply thread header
        if (message.thread != null) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { onReplyClick(message.thread.rootId, message.thread.userName.toUserName()) }
                        .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val replyColor = rememberAdaptiveTextColor(backgroundColor).copy(alpha = 0.6f)
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Reply,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = replyColor,
                )
                Text(
                    text = "Reply to @${message.thread.userName}: ${message.thread.message}",
                    fontSize = (fontSize * 0.9f).sp,
                    color = replyColor,
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
    onEmoteClick: (emotes: List<ChatMessageEmote>) -> Unit,
) {
    val context = LocalPlatformContext.current
    val defaultTextColor = rememberAdaptiveTextColor(backgroundColor)
    val nameColor = rememberNormalizedColor(message.rawNameColor, backgroundColor)
    val linkColor = MaterialTheme.colorScheme.primary

    // Build annotated string with text content
    val annotatedString =
        remember(message, defaultTextColor, nameColor, showChannelPrefix, linkColor) {
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
                        append(" ")
                    }
                }

                // Badges (using appendInlineContent for proper rendering)
                message.badges.forEach { badge ->
                    appendInlineContent("BADGE_${badge.position}", "[badge]")
                    append(" ") // Space between badges
                }

                // Username with click annotation (only if nameText is not empty)
                if (message.nameText.isNotEmpty()) {
                    withStyle(
                        SpanStyle(
                            fontWeight = FontWeight.Bold,
                            color = nameColor,
                        ),
                    ) {
                        pushStringAnnotation(
                            tag = "USER",
                            annotation = "${message.userId?.value ?: ""}|${message.userName.value}|${message.displayName.value}|${message.channel.value}",
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
                            val prevChar = if (currentPos > 0) message.message[currentPos - 1] else null
                            appendWithLinks(segment, linkColor, prevChar)
                        }

                        // Emote inline content
                        appendInlineContent("EMOTE_${emote.code}", emote.code)

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
                        val prevChar = if (currentPos > 0) message.message[currentPos - 1] else null
                        appendWithLinks(segment, linkColor, prevChar)
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
        interactionSource = interactionSource,
        onEmoteClick = onEmoteClick,
        onTextClick = { offset ->
            annotatedString
                .getStringAnnotations("USER", offset, offset)
                .firstOrNull()
                ?.let { annotation ->
                    parseUserAnnotation(annotation.item)?.let { user ->
                        onUserClick(user.userId, user.userName, user.displayName, user.channel.orEmpty(), message.badges, false)
                    }
                }

            annotatedString
                .getStringAnnotations("URL", offset, offset)
                .firstOrNull()
                ?.let { annotation ->
                    launchCustomTab(context, annotation.item)
                }
        },
        onTextLongClick = { offset ->
            val user =
                annotatedString
                    .getStringAnnotations("USER", offset, offset)
                    .firstOrNull()
                    ?.let { parseUserAnnotation(it.item) }

            when {
                user != null -> onUserClick(user.userId, user.userName, user.displayName, user.channel.orEmpty(), message.badges, true)
                else -> onMessageLongClick(message.id, message.channel.value, message.fullMessage)
            }
        },
    )
}
