package com.flxrs.dankchat.main.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.flxrs.dankchat.R
import com.flxrs.dankchat.utils.compose.avoidRoundedCorners

@Composable
fun ChatInputLayout(
    textFieldState: TextFieldState,
    canSend: Boolean,
    onSend: () -> Unit,
    onEmoteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Input field with TextFieldState
    OutlinedTextField(
        state = textFieldState,
        leadingIcon = {
            IconButton(onClick = onEmoteClick) {
                Icon(
                    imageVector = Icons.Default.EmojiEmotions,
                    contentDescription = stringResource(R.string.emote_menu_hint)
                )
            }
        },
        trailingIcon = {
            IconButton(
                onClick = onSend,
                enabled = canSend
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.send_hint)
                )
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .avoidRoundedCorners(fallback = PaddingValues()),
        label = {
            Text(stringResource(R.string.send_hint))
        },
        lineLimits = androidx.compose.foundation.text.input.TextFieldLineLimits.MultiLine(
            minHeightInLines = 1,
            maxHeightInLines = 5
        )
    )
}
