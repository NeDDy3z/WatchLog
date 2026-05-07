package com.neddy.watchlog.data.local.dao

import androidx.room.*
import com.neddy.watchlog.data.local.entity.WatchProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchProgressDao {

    @Query("SELECT * FROM watch_progress WHERE mediaId = :mediaId LIMIT 1")
    fun getProgressForMedia(mediaId: Long): Flow<WatchProgressEntity?>

    @Query("SELECT * FROM watch_progress WHERE mediaId = :mediaId LIMIT 1")
    suspend fun getProgressForMediaSync(mediaId: Long): WatchProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: WatchProgressEntity): Long

    @Update
    suspend fun updateProgress(progress: WatchProgressEntity)
}
