package com.flxrs.dankchat.chat.compose.messages

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
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil3.compose.LocalPlatformContext
import com.flxrs.dankchat.chat.compose.BadgeUi
import com.flxrs.dankchat.chat.compose.ChatMessageUiState
import com.flxrs.dankchat.chat.compose.EmoteScaling
import com.flxrs.dankchat.chat.compose.StackedEmote
import com.flxrs.dankchat.chat.compose.TextWithMeasuredInlineContent
import com.flxrs.dankchat.chat.compose.rememberAdaptiveTextColor
import com.flxrs.dankchat.chat.compose.rememberBackgroundColor
import com.flxrs.dankchat.chat.compose.rememberEmoteAnimationCoordinator
import com.flxrs.dankchat.data.twitch.emote.ChatMessageEmote

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
    showChannelPrefix: Boolean = false,
    animateGifs: Boolean = true,
    onUserClick: (userId: String?, userName: String, displayName: String, channel: String?, badges: List<BadgeUi>, isLongPress: Boolean) -> Unit,
    onMessageLongClick: (messageId: String, channel: String?, fullMessage: String) -> Unit,
    onEmoteClick: (emotes: List<ChatMessageEmote>) -> Unit,
    onReplyClick: (rootMessageId: String) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val backgroundColor = rememberBackgroundColor(message.lightBackgroundColor, message.darkBackgroundColor)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(backgroundColor)
            .indication(interactionSource, ripple())
            .padding(horizontal = 8.dp, vertical = 2.dp)
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
            showChannelPrefix = showChannelPrefix,
            animateGifs = animateGifs,
            interactionSource = interactionSource,
            onUserClick = onUserClick,
            onMessageLongClick = onMessageLongClick,
            onEmoteClick = onEmoteClick
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
    onUserClick: (userId: String?, userName: String, displayName: String, channel: String?, badges: List<BadgeUi>, isLongPress: Boolean) -> Unit,
    onMessageLongClick: (messageId: String, channel: String?, fullMessage: String) -> Unit,
    onEmoteClick: (emotes: List<ChatMessageEmote>) -> Unit,
) {
    val context = LocalPlatformContext.current
    val imageLoader = coil3.ImageLoader.Builder(context).build()
    val emoteCoordinator = rememberEmoteAnimationCoordinator(imageLoader)
    val backgroundColor = rememberBackgroundColor(message.lightBackgroundColor, message.darkBackgroundColor)
    val defaultTextColor = rememberAdaptiveTextColor(backgroundColor)
    val nameColor = rememberBackgroundColor(message.lightNameColor, message.darkNameColor)

    // Build annotated string with text content
    val annotatedString = remember(message, defaultTextColor, nameColor, showChannelPrefix) {
        buildAnnotatedString {
            // Channel prefix (for mention tab)
            if (showChannelPrefix) {
                withStyle(
                    SpanStyle(
                        fontWeight = FontWeight.Bold,
                        color = defaultTextColor
                    )
                ) {
                    append("#${message.channel.value} ")
                }
            }

            // Timestamp
            if (message.timestamp.isNotEmpty()) {
                withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = (fontSize * 0.95f).sp,
                        color = defaultTextColor,
                        letterSpacing = (-0.03).em
                    )
                ) {
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
                        color = nameColor
                    )
                ) {
                    pushStringAnnotation(
                        tag = "USER",
                        annotation = "${message.userId?.value ?: ""}|${message.userName.value}|${message.displayName.value}|${message.channel.value}"
                    )
                    append(message.nameText)
                    pop()
                }
            }

            // Message text with emotes
            val textColor = if (message.isAction) {
                nameColor
            } else {
                defaultTextColor.copy(alpha = message.textAlpha)
            }

            withStyle(SpanStyle(color = textColor)) {
                var currentPos = 0
                message.emotes.sortedBy { it.position.first }.forEach { emote ->
                    // Text before emote
                    if (currentPos < emote.position.first) {
                        append(message.message.substring(currentPos, emote.position.first))
                    }

                    // Emote inline content
                    appendInlineContent("EMOTE_${emote.code}", "[${emote.code}]")

                    // Add space after emote if next character exists and is not whitespace
                    val nextPos = emote.position.last + 1
                    if (nextPos < message.message.length && !message.message[nextPos].isWhitespace()) {
                        append(" ")
                    }

                    currentPos = emote.position.last + 1
                }

                // Remaining text
                if (currentPos < message.message.length) {
                    append(message.message.substring(currentPos))
                }
            }
        }
    }

    // Build inline content providers for SubcomposeLayout
    val badgeSize = EmoteScaling.getBadgeSize(fontSize)
    val inlineContentProviders: Map<String, @Composable () -> Unit> = remember(message.badges, message.emotes, fontSize) {
        buildMap<String, @Composable () -> Unit> {
            // Badge providers  
            message.badges.forEach { badge ->
                put("BADGE_${badge.position}") {
                    coil3.compose.AsyncImage(
                        model = badge.url,
                        contentDescription = badge.badge.type.name,
                        modifier = Modifier.size(badgeSize)
                    )
                }
            }

            // Emote providers
            message.emotes.forEach { emote ->
                put("EMOTE_${emote.code}") {
                    StackedEmote(
                        emote = emote,
                        fontSize = fontSize,
                        emoteCoordinator = emoteCoordinator,
                        animateGifs = animateGifs,
                        modifier = Modifier,
                        onClick = { onEmoteClick(emote.emotes) }
                    )
                }
            }
        }
    }

    // Use SubcomposeLayout to measure inline content, then render text
    TextWithMeasuredInlineContent(
        text = annotatedString,
        inlineContentProviders = inlineContentProviders,
        modifier = Modifier.fillMaxWidth(),
        interactionSource = interactionSource,
        onTextClick = { offset ->
            // Handle username clicks
            annotatedString.getStringAnnotations("USER", offset, offset)
                .firstOrNull()?.let { annotation ->
                    val parts = annotation.item.split("|")
                    if (parts.size == 4) {
                        val userId = parts[0].takeIf { it.isNotEmpty() }
                        val userName = parts[1]
                        val displayName = parts[2]
                        val channel = parts[3]
                        onUserClick(userId, userName, displayName, channel, message.badges, false)
                    }
                }
        },
        onTextLongClick = { offset ->
            // Handle username long-press
            val userAnnotation = annotatedString.getStringAnnotations("USER", offset, offset).firstOrNull()
            if (userAnnotation != null) {
                // Long-press on username
                val parts = userAnnotation.item.split("|")
                if (parts.size == 4) {
                    val userId = parts[0].takeIf { it.isNotEmpty() }
                    val userName = parts[1]
                    val displayName = parts[2]
                    val channel = parts[3]
                    onUserClick(userId, userName, displayName, channel, message.badges, true)
                }
            } else {
                // Long-press on regular text (not username) - trigger message long-press
                onMessageLongClick(message.id, message.channel.value, message.fullMessage)
            }
        }
    )
}
