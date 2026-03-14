package com.flxrs.dankchat.chat.compose.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.flxrs.dankchat.chat.compose.BadgeUi
import com.flxrs.dankchat.chat.compose.ChatMessageUiState
import com.flxrs.dankchat.chat.compose.EmoteUi

/**
 * Renders a regular chat message with:
 * - Optional reply thread header
 * - Badges and username
 * - Message text with inline emotes
 * - Clickable username and emotes
 * - Long-press to copy message
 */
@Composable
fun PrivMessageComposable(
    message: ChatMessageUiState.PrivMessageUi,
    fontSize: Float,
    modifier: Modifier = Modifier,
    onUserClick: (userId: String?, userName: String, displayName: String, channel: String?, isLongPress: Boolean) -> Unit = { _, _, _, _, _ -> },
    onMessageLongClick: (messageId: String, channel: String?, fullMessage: String) -> Unit = { _, _, _ -> },
    onEmoteClick: (emotes: List<Any>) -> Unit = {},
    onReplyClick: (rootMessageId: String) -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(Color(message.backgroundColor))
            .combinedClickable(
                onClick = {},
                onLongClick = {
                    onMessageLongClick(message.id, message.channel.value, message.fullMessage)
                }
            )
            .padding(horizontal = 8.dp)
    ) {
        // Reply thread header
        if (message.thread != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onReplyClick(message.thread.rootId) }
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Reply,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.Gray
                )
                Text(
                    text = "Reply to @${message.thread.userName}: ${message.thread.message}",
                    fontSize = (fontSize * 0.9f).sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Main message
        PrivMessageText(
            message = message,
            fontSize = fontSize,
            onUserClick = onUserClick,
            onEmoteClick = onEmoteClick
        )
    }
}

@Composable
private fun PrivMessageText(
    message: ChatMessageUiState.PrivMessageUi,
    fontSize: Float,
    onUserClick: (userId: String?, userName: String, displayName: String, channel: String?, isLongPress: Boolean) -> Unit,
    onEmoteClick: (emotes: List<Any>) -> Unit,
) {
    val annotatedString = remember(message) {
        buildAnnotatedString {
            // Timestamp
            if (message.timestamp.isNotEmpty()) {
                withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = (fontSize * 0.95f).sp,
                        letterSpacing = 0.05.sp
                    )
                ) {
                    append(message.timestamp)
                }
                append(" ")
            }

            // Badges (placeholders)
            message.badges.forEach { badge ->
                pushStringAnnotation(tag = "BADGE", annotation = badge.position.toString())
                append("⠀")
                pop()
                append(" ")
            }

            // Username
            if (message.nameText.isNotEmpty()) {
                pushStringAnnotation(tag = "USERNAME", annotation = message.userId?.value ?: "")
                withStyle(
                    SpanStyle(
                        color = Color(message.nameColor),
                        fontWeight = FontWeight.Bold
                    )
                ) {
                    append(message.nameText.removeSuffix(": ").removeSuffix(" "))
                }
                pop()

                if (message.isAction) {
                    append(" ")
                } else {
                    append(": ")
                }
            }

            // Message text with emotes
            val textColor = if (message.isAction) {
                Color(message.nameColor)
            } else {
                Color.White.copy(alpha = message.textAlpha)
            }

            withStyle(SpanStyle(color = textColor)) {
                var currentPos = 0
                message.emotes.sortedBy { it.position.first }.forEach { emote ->
                    // Text before emote
                    if (currentPos < emote.position.first) {
                        append(message.message.substring(currentPos, emote.position.first))
                    }

                    // Emote placeholder
                    pushStringAnnotation(tag = "EMOTE", annotation = emote.code)
                    append("⠀") // Invisible space for emote
                    pop()

                    currentPos = emote.position.last + 1
                }

                // Remaining text
                if (currentPos < message.message.length) {
                    append(message.message.substring(currentPos))
                }
            }
        }
    }

    // Inline content for badges and emotes
    val inlineContent = remember(message.badges, message.emotes, fontSize) {
        buildInlineContent(message.badges, message.emotes, fontSize, onEmoteClick)
    }

    BasicText(
        text = annotatedString,
        modifier = Modifier.fillMaxWidth(),
        inlineContent = inlineContent
    )
}

private fun buildInlineContent(
    badges: List<BadgeUi>,
    emotes: List<EmoteUi>,
    fontSize: Float,
    onEmoteClick: (emotes: List<Any>) -> Unit,
): Map<String, InlineTextContent> {
    val content = mutableMapOf<String, InlineTextContent>()

    // Badges
    badges.forEach { badge ->
        content["BADGE_${badge.position}"] = InlineTextContent(
            placeholder = Placeholder(
                width = (fontSize * 1.2f).sp,
                height = fontSize.sp,
                placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
            )
        ) {
            AsyncImage(
                model = badge.url,
                contentDescription = null,
                modifier = Modifier.size((fontSize * 1.2f).dp)
            )
        }
    }

    // Emotes
    emotes.forEach { emote ->
        content["EMOTE_${emote.code}"] = InlineTextContent(
            placeholder = Placeholder(
                width = (fontSize * 2f).sp,
                height = fontSize.sp,
                placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
            )
        ) {
            AsyncImage(
                model = emote.urls.firstOrNull(),
                contentDescription = emote.code,
                modifier = Modifier
                    .size((fontSize * 2f).dp)
                    .clickable { onEmoteClick(emote.emotes) }
            )
        }
    }

    return content
}
