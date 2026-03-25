package com.flxrs.dankchat.chat.compose.messages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.flxrs.dankchat.R
import com.flxrs.dankchat.chat.compose.ChatMessageUiState
import com.flxrs.dankchat.chat.compose.ChatMessageUiState.AutomodMessageUi.AutomodMessageStatus
import com.flxrs.dankchat.chat.compose.EmoteDimensions
import com.flxrs.dankchat.chat.compose.messages.common.BadgeInlineContent
import com.flxrs.dankchat.chat.compose.EmoteScaling
import com.flxrs.dankchat.chat.compose.TextWithMeasuredInlineContent
import com.flxrs.dankchat.chat.compose.rememberNormalizedColor
import com.flxrs.dankchat.chat.compose.resolve
import com.flxrs.dankchat.data.UserName

private val AutoModBlue = Color(0xFF448AFF)

private const val ALLOW_TAG = "ALLOW"
private const val DENY_TAG = "DENY"

@Composable
fun AutomodMessageComposable(
    message: ChatMessageUiState.AutomodMessageUi,
    fontSize: Float,
    onAllow: (heldMessageId: String, channel: UserName) -> Unit,
    onDeny: (heldMessageId: String, channel: UserName) -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val textColor = MaterialTheme.colorScheme.onSurface
    val timestampColor = MaterialTheme.colorScheme.onSurface
    val allowColor = MaterialTheme.colorScheme.primary
    val denyColor = MaterialTheme.colorScheme.error
    val textSize = fontSize.sp
    val isPending = message.status == AutomodMessageStatus.Pending
    val nameColor = rememberNormalizedColor(message.rawNameColor, backgroundColor)

    // Resolve strings
    val headerText = stringResource(R.string.automod_header, message.reason.resolve())
    val allowText = stringResource(R.string.automod_allow)
    val denyText = stringResource(R.string.automod_deny)
    val approvedText = stringResource(R.string.automod_status_approved)
    val deniedText = stringResource(R.string.automod_status_denied)
    val expiredText = stringResource(R.string.automod_status_expired)

    // Header line: [badge] "AutoMod: Held a message for reason: {reason}. Allow will post it in chat. Allow  Deny"
    val headerString = remember(message, textColor, timestampColor, allowColor, denyColor, textSize, headerText, allowText, denyText, approvedText, deniedText, expiredText) {
        buildAnnotatedString {
            // Timestamp
            if (message.timestamp.isNotEmpty()) {
                withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = textSize * 0.95f,
                        color = timestampColor,
                        letterSpacing = (-0.03).em,
                    )
                ) {
                    append(message.timestamp)
                    append(" ")
                }
            }

            // Badges
            message.badges.forEach { badge ->
                appendInlineContent("BADGE_${badge.position}", "[badge]")
                append(" ")
            }

            // "AutoMod: " in blue bold
            withStyle(SpanStyle(color = AutoModBlue, fontWeight = FontWeight.Bold)) {
                append("AutoMod: ")
            }

            // Reason text
            withStyle(SpanStyle(color = textColor)) {
                append("$headerText ")
            }

            // Allow / Deny buttons or status text
            when (message.status) {
                AutomodMessageStatus.Pending -> {
                    pushStringAnnotation(tag = ALLOW_TAG, annotation = message.heldMessageId)
                    withStyle(SpanStyle(color = allowColor, fontWeight = FontWeight.Bold)) {
                        append(allowText)
                    }
                    pop()

                    pushStringAnnotation(tag = DENY_TAG, annotation = message.heldMessageId)
                    withStyle(SpanStyle(color = denyColor, fontWeight = FontWeight.Bold)) {
                        append("  $denyText")
                    }
                    pop()
                }

                AutomodMessageStatus.Approved -> {
                    withStyle(SpanStyle(color = allowColor.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)) {
                        append(approvedText)
                    }
                }

                AutomodMessageStatus.Denied -> {
                    withStyle(SpanStyle(color = denyColor.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)) {
                        append(deniedText)
                    }
                }

                AutomodMessageStatus.Expired -> {
                    withStyle(SpanStyle(color = textColor.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)) {
                        append(expiredText)
                    }
                }
            }
        }
    }

    // Body line: "timestamp {displayName}: {message}"
    val bodyString = remember(message, textColor, nameColor, timestampColor, textSize) {
        buildAnnotatedString {
            // Timestamp for alignment
            if (message.timestamp.isNotEmpty()) {
                withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = textSize * 0.95f,
                        color = timestampColor,
                        letterSpacing = (-0.03).em,
                    )
                ) {
                    append(message.timestamp)
                    append(" ")
                }
            }

            // Username in bold with user color
            withStyle(SpanStyle(color = nameColor, fontWeight = FontWeight.Bold)) {
                append("${message.userDisplayName}: ")
            }

            // Message text
            withStyle(SpanStyle(color = textColor)) {
                append(message.messageText)
            }
        }
    }

    // Badge inline content providers (same pattern as PrivMessage)
    val badgeSize = EmoteScaling.getBadgeSize(fontSize)
    val inlineContentProviders: Map<String, @Composable () -> Unit> = remember(message.badges, fontSize) {
        buildMap {
            message.badges.forEach { badge ->
                put("BADGE_${badge.position}") {
                    BadgeInlineContent(badge = badge, size = badgeSize)
                }
            }
        }
    }

    val density = LocalDensity.current
    val knownDimensions = remember(message.badges, fontSize) {
        buildMap {
            val badgeSizePx = with(density) { badgeSize.toPx().toInt() }
            message.badges.forEach { badge ->
                put("BADGE_${badge.position}", EmoteDimensions("BADGE_${badge.position}", badgeSizePx, badgeSizePx))
            }
        }
    }

    val resolvedAlpha = when (message.status) {
        AutomodMessageStatus.Pending -> 1f
        else                         -> 0.5f
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .alpha(resolvedAlpha)
            .drawBehind { drawRect(backgroundColor) }
            .padding(horizontal = 2.dp, vertical = 2.dp)
    ) {
        // Header line with badge inline content
        TextWithMeasuredInlineContent(
            text = headerString,
            inlineContentProviders = inlineContentProviders,
            style = TextStyle(fontSize = textSize),
            knownDimensions = knownDimensions,
            modifier = Modifier.fillMaxWidth(),
            onTextClick = { offset ->
                if (isPending) {
                    headerString.getStringAnnotations(ALLOW_TAG, offset, offset)
                        .firstOrNull()?.let {
                            onAllow(message.heldMessageId, message.channel)
                        }
                    headerString.getStringAnnotations(DENY_TAG, offset, offset)
                        .firstOrNull()?.let {
                            onDeny(message.heldMessageId, message.channel)
                        }
                }
            },
        )

        // Body line (no inline content needed)
        TextWithMeasuredInlineContent(
            text = bodyString,
            inlineContentProviders = emptyMap(),
            style = TextStyle(fontSize = textSize),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
