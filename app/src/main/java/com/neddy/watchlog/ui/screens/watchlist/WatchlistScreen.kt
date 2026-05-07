package com.neddy.watchlog.ui.screens.watchlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.neddy.watchlog.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun WatchlistScreen(
    onNavigateToDetail: (Long) -> Unit,
    viewModel: WatchlistViewModel = viewModel()
) {
    val homeCards by viewModel.homeCards.collectAsState()
    val density = LocalDensity.current
    var stableHeightPx by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(12.dp))

        Text(
            text = "WatchLog",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Recently Added",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .onSizeChanged { size ->
                    if (stableHeightPx == 0 || size.height < stableHeightPx) {
                        stableHeightPx = size.height
                    }
                }
        ) {
            if (homeCards.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.Movie,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            "Nothing added yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                val gridItems = remember(homeCards) {
                    val padded = homeCards.take(4).toMutableList<HomeCard?>()
                    while (padded.size < 4) padded.add(null)
                    padded
                }

                val gridHeightModifier = if (stableHeightPx > 0)
                    Modifier.height(with(density) { stableHeightPx.toDp() })
                else
                    Modifier.fillMaxHeight()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(gridHeightModifier)
                        .padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    gridItems.chunked(2).forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            row.forEach { card ->
                                if (card != null) {
                                    MediaGridCard(
                                        item = card,
                                        onClick = { onNavigateToDetail(card.item.media.id) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                    )
                                } else {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaGridCard(
    item: HomeCard,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val badgeColor = if (item.item.media.mediaType == "Movie") MovieBadgeColor else TvBadgeColor

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 12.dp),
                text = item.item.media.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
                maxLines = 3
            )
            if (item.item.media.posterUrl != null) {
                AsyncImage(
                    model = item.item.media.posterUrl,
                    contentDescription = item.item.media.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        if (item.percentage > 0f) {
            val isComplete = item.percentage >= 1.0f
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .background(
                        if (isComplete) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.88f)
                        else MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.62f),
                        RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${(item.percentage * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.inverseOnSurface
                )
            }
        }
    }
}
