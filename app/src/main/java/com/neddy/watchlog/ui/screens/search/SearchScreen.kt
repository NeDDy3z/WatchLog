package com.neddy.watchlog.ui.screens.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.neddy.watchlog.R
import com.neddy.watchlog.data.local.entity.MediaWithProgress
import com.neddy.watchlog.data.preferences.SortOrder
import com.neddy.watchlog.data.preferences.WatchlistDisplayMode
import com.neddy.watchlog.ui.components.MediaItemCard
import com.neddy.watchlog.ui.theme.*

@Composable
fun SearchScreen(
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToAdd: () -> Unit,
    fabVisible: Boolean = true,
    viewModel: SearchViewModel = viewModel()
) {
    val query by viewModel.searchQuery.collectAsState()
    val filterType by viewModel.filterType.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val showWatched by viewModel.showWatched.collectAsState()
    val mediaList by viewModel.mediaList.collectAsState()
    val displayMode by viewModel.displayMode.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp)
                    .padding(top = 12.dp)
            ) {
                Text(
                    text = stringResource(R.string.watchlist),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = viewModel::updateSearchQuery,
                    placeholder = {
                        Text(stringResource(R.string.search_placeholder), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    leadingIcon = { Icon(Icons.Filled.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    trailingIcon = {
                        if (query.isNotBlank()) {
                            IconButton(onClick = viewModel::clearQuery) {
                                Icon(Icons.Filled.Clear, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Movie", "TV Show").forEach { type ->
                            val label = if (type == "Movie") stringResource(R.string.movies)
                                        else stringResource(R.string.tv_shows)
                            FilterChip(
                                selected = filterType == type,
                                onClick = { viewModel.setFilter(if (filterType == type) null else type) },
                                label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = filterType == type,
                                    borderColor = MaterialTheme.colorScheme.outlineVariant,
                                    selectedBorderColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                        if (query.isBlank()) {
                            FilterChip(
                                selected = showWatched,
                                onClick = viewModel::toggleShowWatched,
                                label = { Text("Watched", style = MaterialTheme.typography.labelMedium) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = showWatched,
                                    borderColor = MaterialTheme.colorScheme.outlineVariant,
                                    selectedBorderColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                    SortDropdown(
                        sortOrder = sortOrder,
                        onSortSelected = viewModel::setSortOrder
                    )
                }
                Spacer(Modifier.height(4.dp))
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = fabVisible,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(200))
            ) {
                FloatingActionButton(
                    onClick = onNavigateToAdd,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape
                ) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_media))
                }
            }
        }
    ) { innerPadding ->
        if (mediaList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (query.isNotBlank()) stringResource(R.string.no_results)
                           else stringResource(R.string.empty_watchlist),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(32.dp)
                )
            }
        } else {
            when (displayMode) {
                WatchlistDisplayMode.LIST -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(mediaList, key = { it.media.id }) { item ->
                        SwipeToDeleteCard(
                            item = item,
                            compact = false,
                            onDelete = { viewModel.deleteMedia(item.media.id) },
                            onClick = { onNavigateToDetail(item.media.id) }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }

                WatchlistDisplayMode.COMPACT_LIST -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(mediaList, key = { it.media.id }) { item ->
                        SwipeToDeleteCard(
                            item = item,
                            compact = true,
                            onDelete = { viewModel.deleteMedia(item.media.id) },
                            onClick = { onNavigateToDetail(item.media.id) }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }

                WatchlistDisplayMode.GRID -> LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(mediaList, key = { it.media.id }) { item ->
                        WatchlistGridCard(
                            item = item,
                            onClick = { onNavigateToDetail(item.media.id) }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun SortDropdown(sortOrder: String, onSortSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val currentLabel = when (sortOrder) {
        SortOrder.TITLE -> "A–Z"
        SortOrder.OLDER -> "Older"
        else -> "Newer"
    }

    Box {
        Row(
            modifier = Modifier
                .clickable { expanded = true }
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Sort,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = currentLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Icon(
                Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            listOf(
                Triple(SortOrder.DATE_ADDED, "Newer", Icons.Filled.AccessTime),
                Triple(SortOrder.OLDER, "Older", Icons.Filled.ArrowDownward),
                Triple(SortOrder.TITLE, "Title (A–Z)", Icons.Filled.SortByAlpha)
            ).forEach { (value, label, icon) ->
                val isSelected = sortOrder == value
                DropdownMenuItem(
                    text = {
                        Text(
                            label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    leadingIcon = {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    onClick = { onSortSelected(value); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun WatchlistGridCard(item: MediaWithProgress, onClick: () -> Unit) {
    val media = item.media
    val isMovie = media.mediaType == "Movie"
    val badgeColor = if (isMovie) MovieBadgeColor else TvBadgeColor
    val typeIcon = if (isMovie) Icons.Filled.Movie else Icons.Filled.Tv

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(typeIcon, null, tint = badgeColor, modifier = Modifier.size(32.dp))
                }
                if (media.posterUrl != null) {
                    AsyncImage(
                        model = media.posterUrl,
                        contentDescription = media.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Column(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = media.title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = badgeColor.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(typeIcon, null, tint = badgeColor, modifier = Modifier.size(10.dp))
                        Text(
                            text = if (isMovie) "Movie" else "TV Show",
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SwipeToDeleteCard(
    item: MediaWithProgress,
    compact: Boolean,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    var showConfirm by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                showConfirm = true
                true
            } else false
        }
    )

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = {
                showConfirm = false
                scope.launch { dismissState.reset() }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Remove \"${item.media.title}\"?", color = MaterialTheme.colorScheme.onSurface) },
            text = { Text("This will permanently remove it from your watchlist.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                TextButton(onClick = {
                    showConfirm = false
                    onDelete()
                }) { Text("Remove", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showConfirm = false
                    scope.launch { dismissState.reset() }
                }) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        )
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 2.dp)
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(end = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(24.dp))
            }
        },
        enableDismissFromStartToEnd = false
    ) {
        MediaItemCard(item = item, onClick = onClick, compact = compact)
    }
}
