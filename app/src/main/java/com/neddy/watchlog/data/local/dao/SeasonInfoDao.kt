package com.neddy.watchlog.data.local.dao

import androidx.room.*
import com.neddy.watchlog.data.local.entity.SeasonInfoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SeasonInfoDao {

    @Query("SELECT * FROM season_info WHERE mediaId = :mediaId ORDER BY seasonNumber ASC")
    fun getSeasonsForMedia(mediaId: Long): Flow<List<SeasonInfoEntity>>

    @Query("SELECT * FROM season_info WHERE mediaId = :mediaId ORDER BY seasonNumber ASC")
    suspend fun getSeasonsForMediaSync(mediaId: Long): List<SeasonInfoEntity>

    @Query("SELECT mediaId, SUM(episodeCount) as total FROM season_info GROUP BY mediaId")
    fun getAllTotalEpisodes(): Flow<List<TotalEpisodesEntry>>

    @Query("SELECT COALESCE(SUM(episodeCount), 0) FROM season_info WHERE mediaId = :mediaId")
    suspend fun getTotalEpisodesForMedia(mediaId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSeasons(seasons: List<SeasonInfoEntity>)

    @Query("DELETE FROM season_info WHERE mediaId = :mediaId")
    suspend fun deleteSeasonsForMedia(mediaId: Long)
}

data class TotalEpisodesEntry(val mediaId: Long, val total: Int)
