package com.flxrs.dankchat.main.compose

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import com.flxrs.dankchat.R
import com.flxrs.dankchat.utils.compose.avoidRoundedCorners

@Composable
fun ChatInputLayout(
    inputText: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onEmoteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
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
                enabled = inputText.isNotBlank()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.send_hint)
                )
            }
        },
        value = inputText,
        onValueChange = onInputChange,
        modifier = modifier
            .fillMaxWidth()
            .avoidRoundedCorners(fallback = PaddingValues()),
        label = {
            // TODO
            Text(stringResource(R.string.hint_connected))
        },
        maxLines = 5,
        singleLine = false
    )
}
