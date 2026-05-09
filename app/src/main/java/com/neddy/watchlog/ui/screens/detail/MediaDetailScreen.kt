package com.neddy.watchlog.ui.screens.detail

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.SubcomposeAsyncImage
import com.neddy.watchlog.R
import com.neddy.watchlog.data.local.entity.SeasonInfoEntity
import com.neddy.watchlog.data.local.entity.MediaReminderEntity
import com.neddy.watchlog.data.local.entity.UserTagEntity
import com.neddy.watchlog.data.local.entity.WatchProgressEntity
import com.neddy.watchlog.data.local.entity.WatchedEpisodeEntity
import com.neddy.watchlog.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.*

@Composable
fun MediaDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    viewModel: MediaDetailViewModel = viewModel()
) {
    val media by viewModel.media.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val tags by viewModel.tags.collectAsState()
    val reminder by viewModel.reminder.collectAsState()
    val seasons by viewModel.seasons.collectAsState()
    val watchedEpisodes by viewModel.watchedEpisodes.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (media == null) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    val m = media!!
    val isMovie = m.mediaType == "Movie"
    val context = LocalContext.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            DetailTopBar(
                title = m.title,
                onNavigateBack = onNavigateBack,
                onEdit = { onNavigateToEdit(viewModel.mediaId) },
                onDelete = { showDeleteDialog = true }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PosterSection(posterUrl = m.posterUrl, title = m.title, mediaType = m.mediaType)
            MetaSection(media = m)
            ProgressSection(
                progress = progress,
                isMovie = isMovie,
                seasons = seasons,
                watchedEpisodes = watchedEpisodes,
                onToggleMovieFinished = { viewModel.toggleMovieFinished() },
                onToggleEpisode = { season, ep -> viewModel.toggleEpisodeWatched(season, ep) },
                onToggleSeason = { season, count -> viewModel.toggleSeasonWatched(season, count) }
            )
            ReminderSection(
                reminder = reminder,
                onSetReminder = viewModel::setReminder,
                onClearReminder = viewModel::clearReminder,
                context = context
            )
            TagsSection(
                tags = tags,
                onAddTag = viewModel::addTag,
                onRemoveTag = viewModel::removeTag
            )
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete ${m.title}?", color = MaterialTheme.colorScheme.onSurface) },
            text = { Text("This will remove the item and all progress data.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.deleteMedia { onNavigateBack() }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderSection(
    reminder: MediaReminderEntity?,
    onSetReminder: (Long) -> Unit,
    onClearReminder: () -> Unit,
    context: android.content.Context
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pendingDateMillis by remember { mutableStateOf<Long?>(null) }
    var pendingTriggerAtMillis by remember { mutableStateOf<Long?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val pending = pendingTriggerAtMillis
        pendingTriggerAtMillis = null
        if (granted && pending != null) {
            onSetReminder(pending)
        } else if (!granted) {
            Toast.makeText(
                context,
                context.getString(R.string.reminder_permission_denied),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val initialTrigger = reminder?.triggerAtMillis ?: (System.currentTimeMillis() + 30 * 60_000L)
    val displayText = reminder
        ?.takeIf { it.triggerAtMillis > System.currentTimeMillis() }
        ?.let { formatDateTime(it.triggerAtMillis) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.reminder), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)

            if (displayText != null) {
                Text(
                    text = stringResource(R.string.reminder_scheduled_for, displayText),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(onClick = onClearReminder) {
                    Icon(Icons.Filled.Close, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.clear_reminder))
                }
            } else {
                Text(
                    text = stringResource(R.string.reminder_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(onClick = {
                pendingDateMillis = null
                showDatePicker = true
            }) {
                Icon(Icons.Filled.Alarm, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.set_reminder))
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialTrigger)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val selectedDate = datePickerState.selectedDateMillis ?: return@TextButton
                    pendingDateMillis = selectedDate
                    showDatePicker = false
                    showTimePicker = true
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val baseMillis = pendingDateMillis ?: initialTrigger
        val calendar = Calendar.getInstance().apply { timeInMillis = baseMillis }
        val timePickerState = rememberTimePickerState(
            initialHour = calendar.get(Calendar.HOUR_OF_DAY),
            initialMinute = calendar.get(Calendar.MINUTE),
            is24Hour = true
        )

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(R.string.reminder_choose_time), color = MaterialTheme.colorScheme.onSurface) },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    val selectedDate = pendingDateMillis ?: return@TextButton
                    val combined = Calendar.getInstance().apply {
                        timeInMillis = selectedDate
                        set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        set(Calendar.MINUTE, timePickerState.minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis

                    showTimePicker = false
                    pendingDateMillis = null

                    if (combined <= System.currentTimeMillis()) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.reminder_future_required),
                            Toast.LENGTH_SHORT
                        ).show()
                        return@TextButton
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
                    ) {
                        pendingTriggerAtMillis = combined
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        onSetReminder(combined)
                    }
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showTimePicker = false
                    pendingDateMillis = null
                }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
private fun DetailTopBar(
    title: String,
    onNavigateBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .height(56.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onNavigateBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), tint = MaterialTheme.colorScheme.onSurface)
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onEdit) {
            Icon(Icons.Filled.Edit, "Edit", tint = MaterialTheme.colorScheme.onSurface)
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun PosterSection(posterUrl: String?, title: String, mediaType: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        if (posterUrl != null) {
            SubcomposeAsyncImage(
                model = posterUrl,
                contentDescription = stringResource(R.string.poster_content_desc, title),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(120.dp)
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp)),
                loading = {
                    Box(
                        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                },
                error = {
                    Box(
                        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Movie, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(48.dp))
                    }
                }
            )
        } else {
            Box(
                Modifier
                    .width(120.dp)
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Movie, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(48.dp))
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val badgeColor = if (mediaType == "Movie") MovieBadgeColor else TvBadgeColor
            val typeIcon = if (mediaType == "Movie") Icons.Filled.Movie else Icons.Filled.Tv
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = badgeColor.copy(alpha = 0.15f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(typeIcon, null, tint = badgeColor, modifier = Modifier.size(14.dp))
                    Text(mediaType, style = MaterialTheme.typography.labelMedium, color = badgeColor)
                }
            }
        }
    }
}

@Composable
private fun MetaSection(media: com.neddy.watchlog.data.local.entity.MediaItemEntity) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (media.description.isNotBlank()) {
                Text("Description", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(media.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            MetaRow(label = stringResource(R.string.date_added), value = formatDate(media.dateAdded))
        }
    }
}

@Composable
private fun ProgressSection(
    progress: WatchProgressEntity?,
    isMovie: Boolean,
    seasons: List<SeasonInfoEntity>,
    watchedEpisodes: List<WatchedEpisodeEntity>,
    onToggleMovieFinished: () -> Unit,
    onToggleEpisode: (Int, Int) -> Unit,
    onToggleSeason: (Int, Int) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.progress), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)

            if (isMovie) {
                val isFinished = progress?.isFinished ?: false
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleMovieFinished() }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Checkbox(
                        checked = isFinished,
                        onCheckedChange = { onToggleMovieFinished() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary,
                            checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                            uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Text(
                        text = if (isFinished) stringResource(R.string.finished) else "Mark as watched",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isFinished) FinishedColor else MaterialTheme.colorScheme.onSurface
                    )
                }
                if (progress != null) {
                    MetaRow(stringResource(R.string.last_watched), formatDate(progress.lastWatchedDate))
                }
            } else {
                if (seasons.isEmpty()) {
                    Text(
                        "No seasons configured. Edit this entry to add seasons.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    TvShowProgressSection(
                        seasons = seasons,
                        watchedEpisodes = watchedEpisodes,
                        onToggleEpisode = onToggleEpisode,
                        onToggleSeason = onToggleSeason
                    )
                }
                if (progress != null && watchedEpisodes.isNotEmpty()) {
                    MetaRow(stringResource(R.string.last_watched), formatDate(progress.lastWatchedDate))
                }
            }
        }
    }
}

@Composable
private fun TvShowProgressSection(
    seasons: List<SeasonInfoEntity>,
    watchedEpisodes: List<WatchedEpisodeEntity>,
    onToggleEpisode: (Int, Int) -> Unit,
    onToggleSeason: (Int, Int) -> Unit
) {
    val watchedSet = remember(watchedEpisodes) {
        watchedEpisodes.map { it.seasonNumber to it.episodeNumber }.toHashSet()
    }
    var expandedSeasons by remember { mutableStateOf(emptySet<Int>()) }

    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        seasons.forEachIndexed { index, season ->
            val total = season.episodeCount
            val watchedCount = (1..total).count { ep -> (season.seasonNumber to ep) in watchedSet }
            val allWatched = total > 0 && watchedCount == total
            val isExpanded = season.seasonNumber in expandedSeasons

            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = allWatched,
                        onCheckedChange = { onToggleSeason(season.seasonNumber, total) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = FinishedColor,
                            checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                            uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.size(36.dp)
                    )
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                expandedSeasons = if (isExpanded)
                                    expandedSeasons - season.seasonNumber
                                else
                                    expandedSeasons + season.seasonNumber
                            }
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                "Season ${season.seasonNumber}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (allWatched) FinishedColor else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "$watchedCount/$total",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                AnimatedVisibility(
                    visible = isExpanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .padding(start = 8.dp, bottom = 6.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        for (ep in 1..total) {
                            val isWatched = (season.seasonNumber to ep) in watchedSet
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onToggleEpisode(season.seasonNumber, ep) }
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Checkbox(
                                    checked = isWatched,
                                    onCheckedChange = { onToggleEpisode(season.seasonNumber, ep) },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = MaterialTheme.colorScheme.primary,
                                        checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                                        uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                                Text(
                                    "Episode $ep",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isWatched) FinishedColor else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                if (index < seasons.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                }
            }
        }
    }
}

@Composable
private fun TagsSection(
    tags: List<UserTagEntity>,
    onAddTag: (String) -> Unit,
    onRemoveTag: (UserTagEntity) -> Unit
) {
    var tagInput by remember { mutableStateOf("") }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Tags", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)

            if (tags.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(tags, key = { it.id }) { tag ->
                        InputChip(
                            selected = false,
                            onClick = { onRemoveTag(tag) },
                            label = { Text(tag.tagName, style = MaterialTheme.typography.labelMedium) },
                            trailingIcon = {
                                Icon(
                                    Icons.Filled.Close,
                                    "Remove tag",
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = InputChipDefaults.inputChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                trailingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = tagInput,
                    onValueChange = { tagInput = it },
                    placeholder = { Text("Add tag…", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        if (tagInput.isNotBlank()) {
                            onAddTag(tagInput)
                            tagInput = ""
                        }
                    },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(Icons.Filled.Add, "Add tag")
                }
            }
        }
    }
}

@Composable
private fun MetaRow(label: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
    }
}

private fun formatDate(millis: Long): String =
    SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()).format(Date(millis))

private fun formatDateTime(millis: Long): String =
    SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()).format(Date(millis))

