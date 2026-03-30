package com.flxrs.dankchat.ui.main.dialog

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.flxrs.dankchat.R
import com.flxrs.dankchat.utils.compose.ConfirmationBottomSheet

@Composable
fun ConfirmationDialog(
    title: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissText: String = stringResource(R.string.dialog_cancel),
) {
    ConfirmationBottomSheet(
        title = title,
        confirmText = confirmText,
        dismissText = dismissText,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}
