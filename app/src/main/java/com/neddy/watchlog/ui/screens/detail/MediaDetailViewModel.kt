package com.neddy.watchlog.ui.screens.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.neddy.watchlog.data.local.entity.MediaItemEntity
import com.neddy.watchlog.data.local.entity.MediaReminderEntity
import com.neddy.watchlog.data.local.entity.SeasonInfoEntity
import com.neddy.watchlog.data.local.entity.UserTagEntity
import com.neddy.watchlog.data.local.entity.WatchProgressEntity
import com.neddy.watchlog.data.local.entity.WatchedEpisodeEntity
import com.neddy.watchlog.data.repository.MediaRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MediaDetailViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val repository = MediaRepository.getInstance(application)
    val mediaId: Long = checkNotNull(savedStateHandle["mediaId"])

    val media: StateFlow<MediaItemEntity?> = repository.getMediaByIdAsFlow(mediaId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val progress: StateFlow<WatchProgressEntity?> = repository.getProgressForMedia(mediaId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val tags: StateFlow<List<UserTagEntity>> = repository.getTagsForMedia(mediaId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val reminder: StateFlow<MediaReminderEntity?> = repository.getReminderForMedia(mediaId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val seasons: StateFlow<List<SeasonInfoEntity>> = repository.getSeasonsForMedia(mediaId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val watchedEpisodes: StateFlow<List<WatchedEpisodeEntity>> = repository.getWatchedEpisodesForMedia(mediaId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggleMovieFinished() {
        viewModelScope.launch {
            val current = progress.value
            val newFinished = !(current?.isFinished ?: false)
            repository.saveProgress(
                WatchProgressEntity(
                    id = current?.id ?: 0,
                    mediaId = mediaId,
                    currentSeason = null,
                    currentEpisode = null,
                    isFinished = newFinished,
                    lastWatchedDate = System.currentTimeMillis()
                )
            )
        }
    }

    fun toggleEpisodeWatched(season: Int, episode: Int) {
        viewModelScope.launch {
            val isWatched = watchedEpisodes.value.any {
                it.seasonNumber == season && it.episodeNumber == episode
            }
            if (isWatched) {
                repository.removeWatchedEpisode(mediaId, season, episode)
            } else {
                repository.addWatchedEpisode(mediaId, season, episode)
            }
        }
    }

    fun toggleSeasonWatched(seasonNumber: Int, episodeCount: Int) {
        viewModelScope.launch {
            val allWatched = (1..episodeCount).all { ep ->
                watchedEpisodes.value.any { it.seasonNumber == seasonNumber && it.episodeNumber == ep }
            }
            if (allWatched) {
                repository.unmarkSeasonWatched(mediaId, seasonNumber)
            } else {
                repository.markSeasonWatched(mediaId, seasonNumber, episodeCount)
            }
        }
    }

    fun deleteMedia(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteMediaById(mediaId)
            onDeleted()
        }
    }

    fun setReminder(triggerAtMillis: Long) {
        if (triggerAtMillis <= System.currentTimeMillis()) return
        viewModelScope.launch {
            repository.setReminder(mediaId, triggerAtMillis)
        }
    }

    fun clearReminder() {
        viewModelScope.launch { repository.clearReminder(mediaId) }
    }

    fun addTag(tagName: String) {
        if (tagName.isBlank()) return
        viewModelScope.launch {
            repository.insertTag(UserTagEntity(mediaId = mediaId, tagName = tagName.trim()))
        }
    }

    fun removeTag(tag: UserTagEntity) {
        viewModelScope.launch { repository.deleteTag(tag) }
    }
}
