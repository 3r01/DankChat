package com.flxrs.dankchat.ui.main.dialog

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.flxrs.dankchat.R
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.utils.compose.InputBottomSheet

@Composable
fun AddChannelDialog(
    onDismiss: () -> Unit,
    onAddChannel: (UserName) -> Unit,
) {
    InputBottomSheet(
        title = stringResource(R.string.add_channel),
        hint = stringResource(R.string.add_channel_hint),
        onConfirm = { name ->
            onAddChannel(UserName(name))
            onDismiss()
        },
        onDismiss = onDismiss,
    )
}
