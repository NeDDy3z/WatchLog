package com.neddy.watchlog.ui.screens.watchlist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.neddy.watchlog.data.local.entity.MediaWithProgress
import com.neddy.watchlog.data.repository.MediaRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class HomeCard(val item: MediaWithProgress, val percentage: Float)

class WatchlistViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MediaRepository.getInstance(application)

    val homeCards = combine(
        repository.getAllMediaWithProgress().map { it.take(4) },
        repository.getAllWatchedCounts().map { list -> list.associate { it.mediaId to it.count } },
        repository.getAllTotalEpisodes().map { list -> list.associate { it.mediaId to it.total } }
    ) { media, watchedMap, totalMap ->
        media.map { item ->
            val pct = if (item.media.mediaType == "Movie") {
                if (item.progress?.isFinished == true) 1.0f else 0.0f
            } else {
                val total = totalMap[item.media.id] ?: 0
                val watched = watchedMap[item.media.id] ?: 0
                if (total == 0) 0.0f else (watched.toFloat() / total).coerceIn(0f, 1f)
            }
            HomeCard(item, pct)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
