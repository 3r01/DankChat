package com.flxrs.dankchat.ui.main.sheet

import android.content.ClipData
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flxrs.dankchat.data.debug.DebugEntry
import com.flxrs.dankchat.utils.compose.BottomSheetNestedScrollConnection
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugInfoSheet(
    viewModel: DebugInfoViewModel,
    sheetState: SheetState,
    onDismiss: () -> Unit,
) {
    val sections by viewModel.sections.collectAsStateWithLifecycle()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        contentWindowInsets = { WindowInsets.statusBars },
    ) {
        val navBarPadding = WindowInsets.navigationBars.asPaddingValues()
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .nestedScroll(BottomSheetNestedScrollConnection)
                .padding(horizontal = 16.dp),
            contentPadding = navBarPadding,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            sections.forEachIndexed { index, section ->
                item(key = "header_${section.title}") {
                    Column {
                        if (index > 0) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        }
                        Text(
                            text = section.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }

                items(section.entries, key = { "${section.title}_${it.label}" }) { entry ->
                    DebugEntryRow(entry)
                }
            }
        }
    }
}

@Composable
private fun DebugEntryRow(entry: DebugEntry) {
    val clipboardManager = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val copyModifier = when {
        entry.copyValue != null -> Modifier.clickable {
            scope.launch {
                clipboardManager.setClipEntry(ClipEntry(ClipData.newPlainText(entry.label, entry.copyValue)))
            }
        }

        else                    -> Modifier
    }
    Row(
        modifier = copyModifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = entry.label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = FontFamily.Monospace,
        )
        Text(
            text = entry.value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace,
        )
    }
}
