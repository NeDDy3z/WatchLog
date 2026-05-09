package com.neddy.watchlog.ui.screens.manualadd

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neddy.watchlog.R
import com.neddy.watchlog.data.remote.TvdbSearchItem

@Composable
fun ManualAddScreen(
    onNavigateBack: () -> Unit,
    viewModel: ManualAddViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val suggestions by viewModel.searchSuggestions.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            SimpleTopBar(
                title = if (uiState.isEditing) stringResource(R.string.edit_media)
                        else stringResource(R.string.add_media),
                onNavigateBack = onNavigateBack
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .imePadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Button(
                    onClick = { viewModel.saveMedia(onSuccess = onNavigateBack) },
                    enabled = !uiState.isSaving && !(uiState.isDuplicate && !uiState.isEditing),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        disabledContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.save),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                WatchlogTextField(
                    value = uiState.title,
                    onValueChange = viewModel::updateTitle,
                    label = stringResource(R.string.title),
                    isError = uiState.titleError != null || (uiState.isDuplicate && !uiState.isEditing),
                    supportingText = when {
                        uiState.isDuplicate && !uiState.isEditing -> "Already in your watchlist"
                        else -> uiState.titleError
                    },
                    singleLine = true,
                    onFocusLost = viewModel::clearSuggestions
                )

                AnimatedVisibility(
                    visible = isSearching || suggestions.isNotEmpty(),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isSearching && suggestions.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                        suggestions.take(5).forEach { suggestion ->
                            SuggestionItem(
                                suggestion = suggestion,
                                onClick = { viewModel.selectSuggestion(suggestion) }
                            )
                        }
                    }
                }
            }

            WatchlogTextField(
                value = uiState.description,
                onValueChange = viewModel::updateDescription,
                label = stringResource(R.string.description),
                minLines = 3,
                maxLines = 5
            )

            MediaTypeSelector(
                selectedType = uiState.mediaType,
                onTypeSelected = viewModel::updateMediaType
            )

            AnimatedVisibility(
                visible = uiState.mediaType == "TV Show",
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                SeasonsSection(
                    entries = uiState.seasonEntries,
                    onAddSeason = viewModel::addSeason,
                    onRemoveSeason = viewModel::removeSeason,
                    onEpisodesChanged = viewModel::updateSeasonEpisodes
                )
            }

            WatchlogTextField(
                value = uiState.posterUrl,
                onValueChange = viewModel::updatePosterUrl,
                label = stringResource(R.string.poster_url),
                singleLine = true
            )

            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun SuggestionItem(suggestion: TvdbSearchItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = suggestion.name ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = buildString {
                    suggestion.year?.let { append("$it · ") }
                    append(if (suggestion.type == "series") "TV Show" else "Movie")
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SeasonsSection(
    entries: List<SeasonEntry>,
    onAddSeason: () -> Unit,
    onRemoveSeason: (Int) -> Unit,
    onEpisodesChanged: (Int, Int) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Seasons",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                FilledTonalIconButton(
                    onClick = onAddSeason,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Filled.Add, "Add season", modifier = Modifier.size(18.dp))
                }
            }

            if (entries.isEmpty()) {
                Text(
                    text = "No seasons added yet. Tap + to add one.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                entries.forEachIndexed { index, entry ->
                    SeasonRow(
                        entry = entry,
                        onEpisodesChanged = { eps -> onEpisodesChanged(index, eps) },
                        onRemove = { onRemoveSeason(index) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SeasonRow(
    entry: SeasonEntry,
    onEpisodesChanged: (Int) -> Unit,
    onRemove: () -> Unit
) {
    var episodesText by remember(entry.season) {
        mutableStateOf(entry.episodes.toString())
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Season ${entry.season}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(80.dp)
        )

        OutlinedTextField(
            value = episodesText,
            onValueChange = { raw ->
                episodesText = raw
                raw.toIntOrNull()?.let { onEpisodesChanged(it) }
            },
            placeholder = { Text("Episodes", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.weight(1f)
        )

        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "Remove season",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun SimpleTopBar(title: String, onNavigateBack: () -> Unit) {
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
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun WatchlogTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supportingText: String? = null,
    singleLine: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    onFocusLost: (() -> Unit)? = null
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            isError = isError,
            singleLine = singleLine,
            minLines = minLines,
            maxLines = maxLines,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                errorBorderColor = MaterialTheme.colorScheme.error,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                errorTextColor = MaterialTheme.colorScheme.onSurface,
                errorLabelColor = MaterialTheme.colorScheme.error
            ),
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (onFocusLost != null)
                        Modifier.onFocusChanged { if (!it.isFocused) onFocusLost() }
                    else Modifier
                )
        )
        if (supportingText != null) {
            Text(
                text = supportingText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 12.dp, top = 4.dp)
            )
        }
    }
}

@Composable
private fun MediaTypeSelector(
    selectedType: String,
    onTypeSelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.media_type_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf("Movie", "TV Show").forEach { type ->
                val label = if (type == "Movie") stringResource(R.string.movie) else stringResource(R.string.tv_show)
                val isSelected = selectedType == type
                FilterChip(
                    selected = isSelected,
                    onClick = { onTypeSelected(type) },
                    label = { Text(label, style = MaterialTheme.typography.labelLarge) },
                    modifier = Modifier.height(40.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = MaterialTheme.colorScheme.outlineVariant,
                        selectedBorderColor = MaterialTheme.colorScheme.primary,
                        borderWidth = 1.dp,
                        selectedBorderWidth = 1.5.dp
                    )
                )
            }
        }
    }
}
