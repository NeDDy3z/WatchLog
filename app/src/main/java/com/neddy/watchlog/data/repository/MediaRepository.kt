package com.neddy.watchlog.data.repository

import android.content.Context
import com.neddy.watchlog.data.local.WatchlogDatabase
import com.neddy.watchlog.data.local.dao.MediaItemDao
import com.neddy.watchlog.data.local.dao.SeasonInfoDao
import com.neddy.watchlog.data.local.dao.TotalEpisodesEntry
import com.neddy.watchlog.data.local.dao.UserTagDao
import com.neddy.watchlog.data.local.dao.WatchProgressDao
import com.neddy.watchlog.data.local.dao.WatchedCountEntry
import com.neddy.watchlog.data.local.dao.WatchedEpisodeDao
import com.neddy.watchlog.data.local.entity.MediaItemEntity
import com.neddy.watchlog.data.local.entity.MediaWithProgress
import com.neddy.watchlog.data.local.entity.SeasonInfoEntity
import com.neddy.watchlog.data.local.entity.UserTagEntity
import com.neddy.watchlog.data.local.entity.WatchProgressEntity
import com.neddy.watchlog.data.local.entity.WatchedEpisodeEntity
import com.neddy.watchlog.data.backup.BackupItem
import com.neddy.watchlog.data.backup.BackupProgress
import com.neddy.watchlog.data.backup.BackupSeason
import com.neddy.watchlog.data.backup.BackupWatchedEpisode
import com.neddy.watchlog.data.backup.WatchlogBackup
import com.neddy.watchlog.data.preferences.SwipeWatchedScope
import kotlinx.coroutines.flow.Flow

/** What a [MediaRepository.markWatched] call actually changed. */
sealed interface MarkWatchedOutcome {
    data object WholeWatched : MarkWatchedOutcome
    data class SeasonWatched(val season: Int) : MarkWatchedOutcome
    data class EpisodeWatched(val season: Int, val episode: Int) : MarkWatchedOutcome
    data object AlreadyWatched : MarkWatchedOutcome
}

class MediaRepository private constructor(
    private val context: Context,
    private val mediaItemDao: MediaItemDao,
    private val watchProgressDao: WatchProgressDao,
    private val userTagDao: UserTagDao,
    private val seasonInfoDao: SeasonInfoDao,
    private val watchedEpisodeDao: WatchedEpisodeDao,
    private val reminderDao: com.neddy.watchlog.data.local.dao.MediaReminderDao
) {
    fun getAllMediaWithProgress(): Flow<List<MediaWithProgress>> =
        mediaItemDao.getAllMediaWithProgress()

    fun getMediaByTypeWithProgress(type: String): Flow<List<MediaWithProgress>> =
        mediaItemDao.getMediaByTypeWithProgress(type)

    fun searchMediaWithProgress(query: String): Flow<List<MediaWithProgress>> =
        mediaItemDao.searchMediaWithProgress(query)

    fun getMediaByIdAsFlow(id: Long): Flow<MediaItemEntity?> =
        mediaItemDao.getMediaByIdAsFlow(id)

    fun getProgressForMedia(mediaId: Long): Flow<WatchProgressEntity?> =
        watchProgressDao.getProgressForMedia(mediaId)

    fun getTagsForMedia(mediaId: Long): Flow<List<UserTagEntity>> =
        userTagDao.getTagsForMedia(mediaId)

    fun getReminderForMedia(mediaId: Long): Flow<com.neddy.watchlog.data.local.entity.MediaReminderEntity?> =
        reminderDao.getReminderForMedia(mediaId)

    suspend fun getReminderForMediaSync(mediaId: Long): com.neddy.watchlog.data.local.entity.MediaReminderEntity? =
        reminderDao.getReminderForMediaSync(mediaId)

    fun getSeasonsForMedia(mediaId: Long): Flow<List<SeasonInfoEntity>> =
        seasonInfoDao.getSeasonsForMedia(mediaId)

    fun getAllWatchedCounts(): Flow<List<WatchedCountEntry>> =
        watchedEpisodeDao.getAllWatchedCounts()

    fun getAllTotalEpisodes(): Flow<List<TotalEpisodesEntry>> =
        seasonInfoDao.getAllTotalEpisodes()

    suspend fun getMediaById(id: Long): MediaItemEntity? =
        mediaItemDao.getMediaById(id)

    suspend fun existsByTitle(title: String): Boolean =
        mediaItemDao.countByTitle(title) > 0

    fun getWatchedMoviesCount(): Flow<Int> = mediaItemDao.getWatchedCountByType("Movie")
    fun getWatchedTvShowsCount(): Flow<Int> = mediaItemDao.getWatchedCountByType("TV Show")

    suspend fun getSeasonsForMediaSync(mediaId: Long): List<SeasonInfoEntity> =
        seasonInfoDao.getSeasonsForMediaSync(mediaId)

    suspend fun insertMedia(media: MediaItemEntity): Long =
        mediaItemDao.insertMedia(media)

    suspend fun updateMedia(media: MediaItemEntity) =
        mediaItemDao.updateMedia(media)

    suspend fun deleteMediaById(id: Long) =
        run {
            clearReminder(id)
            mediaItemDao.deleteMediaById(id)
        }

    suspend fun saveProgress(progress: WatchProgressEntity) {
        val existing = watchProgressDao.getProgressForMediaSync(progress.mediaId)
        if (existing != null) {
            watchProgressDao.updateProgress(progress.copy(id = existing.id))
        } else {
            watchProgressDao.insertProgress(progress)
        }
    }

    suspend fun replaceSeasons(mediaId: Long, seasons: List<SeasonInfoEntity>) {
        seasonInfoDao.deleteSeasonsForMedia(mediaId)
        if (seasons.isNotEmpty()) {
            seasonInfoDao.insertSeasons(seasons)
        }
    }

    suspend fun insertTag(tag: UserTagEntity): Long =
        userTagDao.insertTag(tag)

    suspend fun deleteTag(tag: UserTagEntity) =
        userTagDao.deleteTag(tag)

    suspend fun setReminder(mediaId: Long, triggerAtMillis: Long) {
        reminderDao.upsertReminder(
            com.neddy.watchlog.data.local.entity.MediaReminderEntity(
                mediaId = mediaId,
                triggerAtMillis = triggerAtMillis
            )
        )
        com.neddy.watchlog.data.reminders.ReminderScheduler.schedule(context, mediaId, triggerAtMillis)
    }

    suspend fun clearReminder(mediaId: Long) {
        reminderDao.deleteReminderForMedia(mediaId)
        com.neddy.watchlog.data.reminders.ReminderScheduler.cancel(context, mediaId)
    }

    fun getWatchedEpisodesForMedia(mediaId: Long): Flow<List<WatchedEpisodeEntity>> =
        watchedEpisodeDao.getWatchedEpisodesForMedia(mediaId)

    suspend fun addWatchedEpisode(mediaId: Long, season: Int, episode: Int) {
        watchedEpisodeDao.insertWatchedEpisode(
            WatchedEpisodeEntity(mediaId = mediaId, seasonNumber = season, episodeNumber = episode)
        )
        touchProgress(mediaId)
    }

    suspend fun removeWatchedEpisode(mediaId: Long, season: Int, episode: Int) {
        watchedEpisodeDao.deleteWatchedEpisode(mediaId, season, episode)
        touchProgress(mediaId)
    }

    suspend fun markSeasonWatched(mediaId: Long, seasonNumber: Int, episodeCount: Int) {
        watchedEpisodeDao.insertWatchedEpisodes(
            (1..episodeCount).map { ep ->
                WatchedEpisodeEntity(mediaId = mediaId, seasonNumber = seasonNumber, episodeNumber = ep)
            }
        )
        touchProgress(mediaId)
    }

    suspend fun unmarkSeasonWatched(mediaId: Long, seasonNumber: Int) {
        watchedEpisodeDao.deleteWatchedEpisodesForSeason(mediaId, seasonNumber)
        touchProgress(mediaId)
    }

    /**
     * Marks a media item as watched according to [scope]. Movies (and TV shows without season
     * info) are always finished as a whole, the scope only applies to TV shows with seasons.
     */
    suspend fun markWatched(mediaId: Long, scope: SwipeWatchedScope): MarkWatchedOutcome {
        val media = mediaItemDao.getMediaById(mediaId) ?: return MarkWatchedOutcome.AlreadyWatched
        val seasons = if (media.mediaType == "TV Show") seasonInfoDao.getSeasonsForMediaSync(mediaId) else emptyList()

        if (seasons.isEmpty()) return finishWithoutEpisodes(mediaId)

        val watched = watchedEpisodeDao.getWatchedEpisodesForMediaSync(mediaId)
            .map { it.seasonNumber to it.episodeNumber }
            .toSet()

        return when (scope) {
            SwipeWatchedScope.WHOLE_SHOW -> {
                val missing = seasons.flatMap { season ->
                    (1..season.episodeCount).map { season.seasonNumber to it }
                }.filter { it !in watched }
                if (missing.isEmpty()) return MarkWatchedOutcome.AlreadyWatched
                watchedEpisodeDao.insertWatchedEpisodes(
                    missing.map { (s, e) -> WatchedEpisodeEntity(mediaId = mediaId, seasonNumber = s, episodeNumber = e) }
                )
                touchProgress(mediaId)
                MarkWatchedOutcome.WholeWatched
            }

            SwipeWatchedScope.NEXT_SEASON -> {
                val next = seasons.firstOrNull { season ->
                    (1..season.episodeCount).any { (season.seasonNumber to it) !in watched }
                } ?: return MarkWatchedOutcome.AlreadyWatched
                markSeasonWatched(mediaId, next.seasonNumber, next.episodeCount)
                MarkWatchedOutcome.SeasonWatched(next.seasonNumber)
            }

            SwipeWatchedScope.NEXT_EPISODE -> {
                val next = seasons.firstNotNullOfOrNull { season ->
                    (1..season.episodeCount)
                        .firstOrNull { (season.seasonNumber to it) !in watched }
                        ?.let { season.seasonNumber to it }
                } ?: return MarkWatchedOutcome.AlreadyWatched
                addWatchedEpisode(mediaId, next.first, next.second)
                MarkWatchedOutcome.EpisodeWatched(next.first, next.second)
            }
        }
    }

    private suspend fun finishWithoutEpisodes(mediaId: Long): MarkWatchedOutcome {
        val existing = watchProgressDao.getProgressForMediaSync(mediaId)
        if (existing?.isFinished == true) return MarkWatchedOutcome.AlreadyWatched
        saveProgress(
            WatchProgressEntity(
                id = existing?.id ?: 0,
                mediaId = mediaId,
                currentSeason = existing?.currentSeason,
                currentEpisode = existing?.currentEpisode,
                isFinished = true,
                lastWatchedDate = System.currentTimeMillis()
            )
        )
        return MarkWatchedOutcome.WholeWatched
    }

    suspend fun getFullBackup(): WatchlogBackup {
        val allMedia = mediaItemDao.getAllMediaSync()
        val items = allMedia.map { media ->
            val progress = watchProgressDao.getProgressForMediaSync(media.id)
            val seasons = seasonInfoDao.getSeasonsForMediaSync(media.id)
            val watched = watchedEpisodeDao.getWatchedEpisodesForMediaSync(media.id)
            val tags = userTagDao.getTagsForMediaSync(media.id)
            val reminder = reminderDao.getReminderForMediaSync(media.id)
            BackupItem(
                apiId = media.apiId,
                title = media.title,
                description = media.description,
                mediaType = media.mediaType,
                posterUrl = media.posterUrl,
                dateAdded = media.dateAdded,
                progress = progress?.let {
                    BackupProgress(it.isFinished, it.lastWatchedDate, it.currentSeason, it.currentEpisode)
                },
                seasons = seasons.map { BackupSeason(it.seasonNumber, it.episodeCount) },
                watchedEpisodes = watched.map { BackupWatchedEpisode(it.seasonNumber, it.episodeNumber) },
                tags = tags.map { it.tagName },
                reminderTriggerAtMillis = reminder?.triggerAtMillis
            )
        }
        return WatchlogBackup(exportedAt = System.currentTimeMillis(), items = items)
    }

    suspend fun restoreFromBackup(backup: WatchlogBackup) {
        clearAllReminders()
        mediaItemDao.deleteAllMedia()
        for (item in backup.items) {
            val newId = mediaItemDao.insertMedia(
                MediaItemEntity(
                    apiId = item.apiId,
                    title = item.title,
                    description = item.description,
                    mediaType = item.mediaType,
                    posterUrl = item.posterUrl,
                    dateAdded = item.dateAdded
                )
            )
            item.progress?.let { p ->
                watchProgressDao.insertProgress(
                    WatchProgressEntity(
                        mediaId = newId,
                        isFinished = p.isFinished,
                        lastWatchedDate = p.lastWatchedDate,
                        currentSeason = p.currentSeason,
                        currentEpisode = p.currentEpisode
                    )
                )
            }
            if (item.seasons.isNotEmpty()) {
                seasonInfoDao.insertSeasons(item.seasons.map { s ->
                    SeasonInfoEntity(mediaId = newId, seasonNumber = s.seasonNumber, episodeCount = s.episodeCount)
                })
            }
            if (item.watchedEpisodes.isNotEmpty()) {
                watchedEpisodeDao.insertWatchedEpisodes(item.watchedEpisodes.map { e ->
                    WatchedEpisodeEntity(mediaId = newId, seasonNumber = e.seasonNumber, episodeNumber = e.episodeNumber)
                })
            }
            for (tag in item.tags) {
                userTagDao.insertTag(UserTagEntity(mediaId = newId, tagName = tag))
            }
            item.reminderTriggerAtMillis?.takeIf { it > System.currentTimeMillis() }?.let {
                setReminder(newId, it)
            }
        }
    }

    private suspend fun clearAllReminders() {
        reminderDao.getAllRemindersSync().forEach { reminder ->
            com.neddy.watchlog.data.reminders.ReminderScheduler.cancel(context, reminder.mediaId)
        }
        reminderDao.deleteAllReminders()
    }

    private suspend fun touchProgress(mediaId: Long) {
        val existing = watchProgressDao.getProgressForMediaSync(mediaId)
        val media = mediaItemDao.getMediaById(mediaId)

        val isFinished = if (media?.mediaType == "TV Show") {
            val total = seasonInfoDao.getTotalEpisodesForMedia(mediaId)
            val watched = watchedEpisodeDao.getWatchedCountForMedia(mediaId)
            total > 0 && watched >= total
        } else {
            existing?.isFinished ?: false
        }

        if (existing != null) {
            watchProgressDao.updateProgress(
                existing.copy(lastWatchedDate = System.currentTimeMillis(), isFinished = isFinished)
            )
        } else {
            watchProgressDao.insertProgress(
                WatchProgressEntity(mediaId = mediaId, lastWatchedDate = System.currentTimeMillis(), isFinished = isFinished)
            )
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: MediaRepository? = null

        fun getInstance(context: Context): MediaRepository =
            INSTANCE ?: synchronized(this) {
                val db = WatchlogDatabase.getDatabase(context)
                MediaRepository(
                    context.applicationContext,
                    db.mediaItemDao(),
                    db.watchProgressDao(),
                    db.userTagDao(),
                    db.seasonInfoDao(),
                    db.watchedEpisodeDao(),
                    db.mediaReminderDao()
                ).also { INSTANCE = it }
            }
    }
}
