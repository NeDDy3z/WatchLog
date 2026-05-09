package com.neddy.watchlog.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.neddy.watchlog.R
import com.neddy.watchlog.data.local.entity.MediaWithProgress
import com.neddy.watchlog.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MediaItemCard(
    item: MediaWithProgress,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val media = item.media
    val progress = item.progress
    val isMovie = media.mediaType == "Movie"
    val badgeColor = if (isMovie) MovieBadgeColor else TvBadgeColor
    val typeIcon = if (isMovie) Icons.Filled.Movie else Icons.Filled.Tv

    val rowHeight = if (compact) 42.dp else 128.dp
    val titleTypography = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium
    val lines = if (compact) 1 else 2

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.height(rowHeight),
        ) {
            PosterImage(
                url = media.posterUrl,
                title = media.title,
                modifier = Modifier
                    .width(80.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = media.title,
                        style = titleTypography,
                        color = MaterialTheme.colorScheme.onSurface,
                        minLines = lines,
                        maxLines = lines,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    TypeBadge(label = media.mediaType, color = badgeColor, icon = typeIcon)
                }

                if (!compact) {
                    Text(
                        text = media.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        minLines = lines,
                        maxLines = lines,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Spacer(Modifier.height(2.dp))

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ProgressChip(progress = progress, isMovie = isMovie)
                        Text(
                            text = formatDate(media.dateAdded),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PosterImage(url: String?, title: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Filled.Movie,
            null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(28.dp)
        )
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = stringResource(R.string.poster_content_desc, title),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun TypeBadge(
    label: String,
    color: androidx.compose.ui.graphics.Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(11.dp))
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = color)
        }
    }
}

@Composable
private fun ProgressChip(
    progress: com.neddy.watchlog.data.local.entity.WatchProgressEntity?,
    isMovie: Boolean
) {
    when {
        progress == null -> {
            StatusPill(text = "Not started", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        progress.isFinished -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(Icons.Filled.CheckCircle, null, tint = FinishedColor, modifier = Modifier.size(14.dp))
                Text(
                    text = "Finished",
                    style = MaterialTheme.typography.labelSmall,
                    color = FinishedColor
                )
            }
        }
        !isMovie && progress.currentSeason != null -> {
            StatusPill(
                text = stringResource(
                    R.string.s_ep,
                    progress.currentSeason,
                    progress.currentEpisode ?: 1
                ),
                color = MaterialTheme.colorScheme.primary
            )
        }
        else -> {
            StatusPill(text = "Watching", color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun StatusPill(text: String, color: androidx.compose.ui.graphics.Color) {
    Text(text = text, style = MaterialTheme.typography.labelSmall, color = color)
}

private fun formatDate(millis: Long): String {
    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return sdf.format(Date(millis))
}
