package com.flxrs.dankchat.chat.compose.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import com.flxrs.dankchat.chat.compose.BadgeUi
import com.flxrs.dankchat.chat.compose.ChatMessageUiState
import com.flxrs.dankchat.chat.compose.appendWithLinks
import com.flxrs.dankchat.chat.compose.messages.common.MessageTextWithInlineContent
import com.flxrs.dankchat.chat.compose.messages.common.launchCustomTab
import com.flxrs.dankchat.chat.compose.messages.common.timestampSpanStyle
import com.flxrs.dankchat.chat.compose.rememberAdaptiveTextColor
import com.flxrs.dankchat.chat.compose.rememberBackgroundColor
import com.flxrs.dankchat.chat.compose.rememberNormalizedColor
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.twitch.emote.ChatMessageEmote

/**
 * Renders a whisper message (private message between users)
 */
@Composable
fun WhisperMessageComposable(
    message: ChatMessageUiState.WhisperMessageUi,
    fontSize: Float,
    modifier: Modifier = Modifier,
    animateGifs: Boolean = true,
    onUserClick: (userId: String?, userName: String, displayName: String, badges: List<BadgeUi>, isLongPress: Boolean) -> Unit,
    onMessageLongClick: (messageId: String, fullMessage: String) -> Unit,
    onEmoteClick: (emotes: List<ChatMessageEmote>) -> Unit,
    onWhisperReply: ((userName: UserName) -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val backgroundColor = rememberBackgroundColor(message.lightBackgroundColor, message.darkBackgroundColor)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .alpha(message.textAlpha)
            .background(backgroundColor)
            .indication(interactionSource, ripple())
            .padding(horizontal = 2.dp, vertical = 2.dp)
    ) {
        Box(modifier = Modifier.weight(1f)) {
            WhisperMessageText(
                message = message,
                fontSize = fontSize,
                animateGifs = animateGifs,
                backgroundColor = backgroundColor,
                onUserClick = onUserClick,
                onMessageLongClick = onMessageLongClick,
                onEmoteClick = onEmoteClick
            )
        }
        if (onWhisperReply != null) {
            IconButton(
                onClick = { onWhisperReply(message.replyTargetName) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Reply,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun WhisperMessageText(
    message: ChatMessageUiState.WhisperMessageUi,
    fontSize: Float,
    animateGifs: Boolean,
    backgroundColor: Color,
    onUserClick: (userId: String?, userName: String, displayName: String, badges: List<BadgeUi>, isLongPress: Boolean) -> Unit,
    onMessageLongClick: (messageId: String, fullMessage: String) -> Unit,
    onEmoteClick: (emotes: List<ChatMessageEmote>) -> Unit,
) {
    val context = LocalPlatformContext.current
    val defaultTextColor = rememberAdaptiveTextColor(backgroundColor)
    val senderColor = rememberNormalizedColor(message.rawSenderColor, backgroundColor)
    val recipientColor = rememberNormalizedColor(message.rawRecipientColor, backgroundColor)
    val linkColor = MaterialTheme.colorScheme.primary

    // Build annotated string with text content
    val annotatedString = remember(message, defaultTextColor, senderColor, recipientColor, linkColor) {
        buildAnnotatedString {
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

            // Sender username with click annotation
            withStyle(
                SpanStyle(
                    fontWeight = FontWeight.Bold,
                    color = senderColor
                )
            ) {
                pushStringAnnotation(
                    tag = "USER",
                    annotation = "${message.userId.value}|${message.userName.value}|${message.displayName.value}"
                )
                append(message.senderName)
                pop()
            }
            withStyle(SpanStyle(color = defaultTextColor)) {
                append(" -> ")
            }

            // Recipient
            withStyle(
                SpanStyle(
                    fontWeight = FontWeight.Bold,
                    color = recipientColor
                )
            ) {
                append(message.recipientName)
            }
            withStyle(SpanStyle(color = defaultTextColor)) {
                append(": ")
            }

            // Message text with emotes
            withStyle(SpanStyle(color = defaultTextColor)) {
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
        onEmoteClick = onEmoteClick,
        onTextClick = { offset ->
            annotatedString.getStringAnnotations("USER", offset, offset)
                .firstOrNull()?.let { annotation ->
                    val parts = annotation.item.split("|")
                    if (parts.size == 3) {
                        val userId = parts[0].takeIf { it.isNotEmpty() }
                        val userName = parts[1]
                        val displayName = parts[2]
                        onUserClick(userId, userName, displayName, message.badges, false)
                    }
                }

            annotatedString.getStringAnnotations("URL", offset, offset)
                .firstOrNull()?.let { annotation ->
                    launchCustomTab(context, annotation.item)
                }
        },
        onTextLongClick = {
            onMessageLongClick(message.id, message.fullMessage)
        },
    )
}

/**
 * Renders a channel point redemption message
 */
@Composable
fun PointRedemptionMessageComposable(
    message: ChatMessageUiState.PointRedemptionMessageUi,
    fontSize: Float,
    modifier: Modifier = Modifier,
    highlightShape: Shape = RectangleShape,
) {
    val backgroundColor = rememberBackgroundColor(message.lightBackgroundColor, message.darkBackgroundColor)
    val timestampColor = rememberAdaptiveTextColor(backgroundColor)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .alpha(message.textAlpha)
            .background(backgroundColor, highlightShape)
            .padding(horizontal = 2.dp, vertical = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            val annotatedString = remember(message, timestampColor) {
                buildAnnotatedString {
                    // Timestamp
                    if (message.timestamp.isNotEmpty()) {
                        withStyle(timestampSpanStyle(fontSize, timestampColor)) {
                            append(message.timestamp)
                        }
                        append(" ")
                    }

                    when {
                        message.requiresUserInput -> {
                            append("Redeemed ")
                        }

                        message.nameText != null  -> {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(message.nameText)
                            }
                            append(" redeemed ")
                        }
                    }

                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(message.title)
                    }
                    append("  ")
                }
            }

            BasicText(
                text = annotatedString,
                style = TextStyle(fontSize = fontSize.sp),
                modifier = Modifier.weight(1f)
            )

            AsyncImage(
                model = message.rewardImageUrl,
                contentDescription = message.title,
                modifier = Modifier.size((fontSize * 1.5f).dp)
            )

            BasicText(
                text = " ${message.cost}",
                style = TextStyle(fontSize = fontSize.sp),
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}
