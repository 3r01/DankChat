package com.flxrs.dankchat.chat.compose.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.flxrs.dankchat.chat.compose.ChatMessageUiState

/**
 * Renders a whisper message (private message between users)
 */
@Composable
fun WhisperMessageComposable(
    message: ChatMessageUiState.WhisperMessageUi,
    fontSize: Float,
    modifier: Modifier = Modifier,
    onUserClick: (userId: String?, userName: String, displayName: String, isLongPress: Boolean) -> Unit = { _, _, _, _ -> },
    onMessageLongClick: (messageId: String, fullMessage: String) -> Unit = { _, _ -> },
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(Color(message.backgroundColor))
            .combinedClickable(
                onClick = {},
                onLongClick = {
                    onMessageLongClick(message.id, message.fullMessage)
                }
            )
            .padding(horizontal = 8.dp)
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

                // Badges (simplified for now)
                message.badges.forEach { _ ->
                    append("⠀ ")
                }

                // Sender
                withStyle(
                    SpanStyle(
                        color = Color(message.senderColor),
                        fontWeight = FontWeight.Bold
                    )
                ) {
                    append(message.senderName)
                }
                append(" -> ")

                // Recipient
                withStyle(
                    SpanStyle(
                        color = Color(message.recipientColor),
                        fontWeight = FontWeight.Bold
                    )
                ) {
                    append(message.recipientName)
                }
                append(": ")

                // Message
                withStyle(SpanStyle(color = Color.White.copy(alpha = message.textAlpha))) {
                    append(message.message)
                }
            }
        }

        BasicText(
            text = annotatedString,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Renders a channel point redemption message
 */
@Composable
fun PointRedemptionMessageComposable(
    message: ChatMessageUiState.PointRedemptionMessageUi,
    fontSize: Float,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(Color(message.backgroundColor))
            .padding(horizontal = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
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

                    when {
                        message.requiresUserInput -> {
                            append("Redeemed ")
                        }
                        message.nameText != null -> {
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
                modifier = Modifier.weight(1f)
            )

            AsyncImage(
                model = message.rewardImageUrl,
                contentDescription = message.title,
                modifier = Modifier.size((fontSize * 1.5f).dp)
            )

            BasicText(
                text = " ${message.cost}",
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}
