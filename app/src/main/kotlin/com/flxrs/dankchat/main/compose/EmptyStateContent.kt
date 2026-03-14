package com.flxrs.dankchat.main.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.flxrs.dankchat.R

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EmptyStateContent(
    isLoggedIn: Boolean,
    onAddChannel: () -> Unit,
    onLogin: () -> Unit,
    onToggleAppBar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.no_channels_added), // You might need to add this string or use a literal/different string
                style = MaterialTheme.typography.headlineMedium
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Shortcut chips
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
            ) {
                AssistChip(
                    onClick = onAddChannel,
                    label = { Text(stringResource(R.string.add_channel)) },
                    leadingIcon = { Icon(Icons.Default.Add, null) }
                )
                
                if (!isLoggedIn) {
                    AssistChip(
                        onClick = onLogin,
                        label = { Text(stringResource(R.string.login)) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Login, null) }
                    )
                }
                
                AssistChip(
                    onClick = onToggleAppBar,
                    label = { Text(stringResource(R.string.toggle_app_bar)) }
                )
            }
        }
    }
}
