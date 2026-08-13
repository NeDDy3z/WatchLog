package com.neddy.watchlog.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.neddy.watchlog.BuildConfig
import com.neddy.watchlog.data.update.UpdateInfo

@Composable
fun UpdateAvailableDialog(
    info: UpdateInfo,
    onDownload: () -> Unit,
    onLater: () -> Unit,
    onNever: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onLater,
        containerColor = MaterialTheme.colorScheme.surface,
        icon = {
            Icon(
                Icons.Filled.SystemUpdate,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text("Update available", color = MaterialTheme.colorScheme.onSurface)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 260.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "WatchLog ${info.version} is out, you have ${BuildConfig.VERSION_NAME}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (info.notes.isNotBlank()) {
                    Text(
                        text = info.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "\"Never\" hides this version, you can still check manually in Settings.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDownload) {
                Text("Download", color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onNever) {
                    Text("Never", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = onLater) {
                    Text("Later", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    )
}
