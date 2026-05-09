package com.neddy.watchlog.ui.screens.settings

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.neddy.watchlog.BuildConfig
import com.neddy.watchlog.R
import com.neddy.watchlog.data.preferences.AutoBackupFrequency
import com.neddy.watchlog.data.preferences.WatchlistDisplayMode
import com.neddy.watchlog.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val defaultEpisodes by viewModel.defaultEpisodesPerSeason.collectAsState()
    val displayMode by viewModel.displayMode.collectAsState()
    val showWatchedDefault by viewModel.showWatchedDefault.collectAsState()
    val watchedMovies by viewModel.watchedMoviesCount.collectAsState()
    val watchedTvShows by viewModel.watchedTvShowsCount.collectAsState()
    val backupState by viewModel.backupState.collectAsState()
    val autoBackupFrequency by viewModel.autoBackupFrequency.collectAsState()
    val lastBackupAt by viewModel.lastBackupAt.collectAsState()

    val uriHandler = LocalUriHandler.current
    val gitHubLink = "https://github.com/NeDDy3z/watchlog"

    val snackbarHostState = remember { SnackbarHostState() }
    var showRestoreConfirm by remember { mutableStateOf(false) }

    val driveScope = Scope("https://www.googleapis.com/auth/drive.appdata")
    val authorizationClient = remember { Identity.getAuthorizationClient(context) }

    val authLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                val authResult = authorizationClient.getAuthorizationResultFromIntent(result.data)
                val token = authResult.accessToken
                if (token != null) {
                    when (viewModel.pendingAction) {
                        BackupAction.BACKUP -> viewModel.startBackup(token)
                        BackupAction.RESTORE -> viewModel.startRestore(token)
                        null -> {}
                    }
                } else {
                    viewModel.setBackupError("Authorization failed: no token received")
                }
            } catch (e: Exception) {
                viewModel.setBackupError("Authorization failed: ${e.message}")
            }
        } else {
            viewModel.setBackupError("Authorization cancelled")
        }
        viewModel.pendingAction = null
    }

    fun requestDriveAccess(action: BackupAction) {
        viewModel.pendingAction = action
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(driveScope))
            .build()
        authorizationClient.authorize(request)
            .addOnSuccessListener { authResult ->
                if (authResult.hasResolution()) {
                    authLauncher.launch(
                        IntentSenderRequest.Builder(authResult.pendingIntent!!.intentSender).build()
                    )
                } else {
                    val token = authResult.accessToken
                    if (token != null) {
                        when (action) {
                            BackupAction.BACKUP -> viewModel.startBackup(token)
                            BackupAction.RESTORE -> viewModel.startRestore(token)
                        }
                    } else {
                        viewModel.setBackupError("Authorization failed: no token received")
                    }
                    viewModel.pendingAction = null
                }
            }
            .addOnFailureListener { e ->
                viewModel.setBackupError("Authorization failed: ${e.message}")
                viewModel.pendingAction = null
            }
    }

    LaunchedEffect(backupState) {
        when (val s = backupState) {
            is BackupState.Success -> {
                snackbarHostState.showSnackbar(s.message)
                viewModel.clearBackupState()
            }
            is BackupState.Error -> {
                snackbarHostState.showSnackbar(s.message)
                viewModel.clearBackupState()
            }
            else -> {}
        }
    }

    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Restore Watchlist?", color = MaterialTheme.colorScheme.onSurface) },
            text = { Text("This will replace all local data with the backup from Google Drive.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                TextButton(onClick = {
                    showRestoreConfirm = false
                    requestDriveAccess(BackupAction.RESTORE)
                }) { Text("Restore", color = MaterialTheme.colorScheme.primary) }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.settings),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(24.dp))

            SettingsSectionHeader("Defaults")
            Spacer(Modifier.height(8.dp))

            SettingsCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CounterSettingRow(
                        icon = Icons.Filled.Tv,
                        title = "Default episodes per season",
                        subtitle = "Applied when adding a new season",
                        count = defaultEpisodes,
                        onDecrement = { viewModel.setDefaultEpisodesPerSeason(defaultEpisodes - 1) },
                        onIncrement = { viewModel.setDefaultEpisodesPerSeason(defaultEpisodes + 1) }
                    )
                    ToggleSettingRow(
                        icon = Icons.Filled.Visibility,
                        title = "Show watched by default",
                        subtitle = "Include finished items in the watchlist",
                        checked = showWatchedDefault,
                        onCheckedChange = { viewModel.setShowWatchedDefault(it) }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            SettingsSectionHeader("Display")
            Spacer(Modifier.height(8.dp))

            SettingsCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Watchlist layout",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        listOf(
                            WatchlistDisplayMode.LIST to "List",
                            WatchlistDisplayMode.COMPACT_LIST to "Compact",
                            WatchlistDisplayMode.GRID to "Grid"
                        ).forEachIndexed { index, (mode, label) ->
                            SegmentedButton(
                                shape = SegmentedButtonDefaults.itemShape(index, 3),
                                onClick = { viewModel.setDisplayMode(mode) },
                                selected = displayMode == mode,
                                colors = SegmentedButtonDefaults.colors(
                                    activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    inactiveContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Text(label, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            SettingsSectionHeader("Backup & Restore")
            Spacer(Modifier.height(8.dp))

            val isLoading = backupState is BackupState.Loading

            BackupSection(
                autoBackupFrequency = autoBackupFrequency,
                onAutoBackupSelected = viewModel::setAutoBackupFrequency,
                lastBackupAt = lastBackupAt,
                isLoading = isLoading,
                onBackupClick = { requestDriveAccess(BackupAction.BACKUP) },
                onRestoreClick = { showRestoreConfirm = true }
            )

            Spacer(Modifier.height(20.dp))
            SettingsSectionHeader("Statistics")
            Spacer(Modifier.height(8.dp))

            SettingsCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatRow(
                        icon = Icons.Filled.Movie,
                        label = "Movies watched",
                        count = watchedMovies,
                        color = MovieBadgeColor
                    )
                    StatRow(
                        icon = Icons.Filled.Tv,
                        label = "TV shows watched",
                        count = watchedTvShows,
                        color = TvBadgeColor
                    )
                }
            }

            Spacer(Modifier.height(40.dp))

            Text(
                text = "WatchLog v${BuildConfig.VERSION_NAME} - Erik Vaněk",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clickable { uriHandler.openUri(gitHubLink) }
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            content = content
        )
    }
}

@Composable
private fun BackupSection(
    autoBackupFrequency: AutoBackupFrequency,
    onAutoBackupSelected: (AutoBackupFrequency) -> Unit,
    lastBackupAt: Long,
    isLoading: Boolean,
    onBackupClick: () -> Unit,
    onRestoreClick: () -> Unit
) {
    SettingsCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BackupActionRow(
                icon = Icons.Filled.CloudUpload,
                title = "Backup to Drive",
                subtitle = "Save watchlist to Google Drive",
                buttonLabel = "Backup",
                loading = isLoading,
                onClick = onBackupClick
            )
            BackupActionRow(
                icon = Icons.Filled.CloudDownload,
                title = "Restore from Drive",
                subtitle = "Replace local data with backup",
                buttonLabel = "Restore",
                loading = isLoading,
                onClick = onRestoreClick
            )
            AutoBackupRow(
                icon = Icons.Filled.AccessTime,
                title = "Auto backup",
                lastBackupAt = lastBackupAt,
                selected = autoBackupFrequency,
                onSelected = onAutoBackupSelected
            )
        }
    }
}

@Composable
private fun BackupActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    buttonLabel: String,
    loading: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Box(
            modifier = Modifier.size(width = 80.dp, height = 40.dp),
            contentAlignment = Alignment.Center
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp
                )
            } else {
                TextButton(onClick = onClick) {
                    Text(buttonLabel, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun AutoBackupRow(
    icon: ImageVector,
    title: String,
    lastBackupAt: Long,
    selected: AutoBackupFrequency,
    onSelected: (AutoBackupFrequency) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val subtitle = if (lastBackupAt <= 0L) {
        "Last backup: Never"
    } else {
        "Last backup: ${formatBackupDate(lastBackupAt)}"
    }
    val currentLabel = when (selected) {
        AutoBackupFrequency.OFF -> "Off"
        AutoBackupFrequency.DAILY -> "Daily"
        AutoBackupFrequency.WEEKLY -> "Weekly"
        AutoBackupFrequency.MONTHLY -> "Monthly"
    }

    Row(
        modifier = Modifier.fillMaxWidth().height(40.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Box {
            TextButton(onClick = { expanded = true }) {
                Text(currentLabel, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                Icon(
                    Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                listOf(
                    AutoBackupFrequency.OFF to "Off",
                    AutoBackupFrequency.DAILY to "Daily",
                    AutoBackupFrequency.WEEKLY to "Weekly",
                    AutoBackupFrequency.MONTHLY to "Monthly"
                ).forEach { (value, label) ->
                    val isSelected = value == selected
                    DropdownMenuItem(
                        text = {
                            Text(
                                label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            onSelected(value)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

private fun formatBackupDate(millis: Long): String {
    val adjustedMillis = millis + 60_000L
    val sdf = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(adjustedMillis))
}

@Composable
private fun StatRow(
    icon: ImageVector,
    label: String,
    count: Int,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            Text(label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        }
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = color
        )
    }
}

@Composable
private fun CounterSettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    count: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(40.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = onDecrement,
                enabled = count > 1,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Filled.Remove,
                    contentDescription = "Decrease",
                    tint = if (count > 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(32.dp)
            )
            IconButton(
                onClick = onIncrement,
                enabled = count < 99,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "Increase",
                    tint = if (count < 99) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun ToggleSettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(40.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimaryContainer,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.height(24.dp)
        )
    }
}
