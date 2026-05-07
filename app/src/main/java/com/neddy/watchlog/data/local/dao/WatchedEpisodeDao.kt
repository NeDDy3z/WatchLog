package com.neddy.watchlog.data.local.dao

import androidx.room.*
import com.neddy.watchlog.data.local.entity.WatchedEpisodeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchedEpisodeDao {

    @Query("SELECT * FROM watched_episodes WHERE mediaId = :mediaId ORDER BY seasonNumber, episodeNumber")
    fun getWatchedEpisodesForMedia(mediaId: Long): Flow<List<WatchedEpisodeEntity>>

    @Query("SELECT * FROM watched_episodes WHERE mediaId = :mediaId ORDER BY seasonNumber, episodeNumber")
    suspend fun getWatchedEpisodesForMediaSync(mediaId: Long): List<WatchedEpisodeEntity>

    @Query("SELECT mediaId, COUNT(*) as count FROM watched_episodes GROUP BY mediaId")
    fun getAllWatchedCounts(): Flow<List<WatchedCountEntry>>

    @Query("SELECT COUNT(*) FROM watched_episodes WHERE mediaId = :mediaId")
    suspend fun getWatchedCountForMedia(mediaId: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWatchedEpisode(episode: WatchedEpisodeEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWatchedEpisodes(episodes: List<WatchedEpisodeEntity>)

    @Query("DELETE FROM watched_episodes WHERE mediaId = :mediaId AND seasonNumber = :season AND episodeNumber = :episode")
    suspend fun deleteWatchedEpisode(mediaId: Long, season: Int, episode: Int)

    @Query("DELETE FROM watched_episodes WHERE mediaId = :mediaId AND seasonNumber = :season")
    suspend fun deleteWatchedEpisodesForSeason(mediaId: Long, season: Int)

    @Query("DELETE FROM watched_episodes WHERE mediaId = :mediaId")
    suspend fun deleteAllForMedia(mediaId: Long)
}

data class WatchedCountEntry(val mediaId: Long, val count: Int)
