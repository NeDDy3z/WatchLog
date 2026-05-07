package com.neddy.watchlog.data.local.dao

import androidx.room.*
import com.neddy.watchlog.data.local.entity.MediaItemEntity
import com.neddy.watchlog.data.local.entity.MediaWithProgress
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaItemDao {

    @Transaction
    @Query("SELECT * FROM media_items ORDER BY dateAdded DESC")
    fun getAllMediaWithProgress(): Flow<List<MediaWithProgress>>

    @Transaction
    @Query("SELECT * FROM media_items WHERE mediaType = :type ORDER BY dateAdded DESC")
    fun getMediaByTypeWithProgress(type: String): Flow<List<MediaWithProgress>>

    @Transaction
    @Query("""
        SELECT * FROM media_items
        WHERE LOWER(title) LIKE '%' || LOWER(:query) || '%'
           OR EXISTS (
               SELECT 1
               FROM user_tags
               WHERE user_tags.mediaId = media_items.id
                 AND LOWER(user_tags.tagName) LIKE '%' || LOWER(:query) || '%'
           )
        ORDER BY dateAdded DESC
    """)
    fun searchMediaWithProgress(query: String): Flow<List<MediaWithProgress>>

    @Query("SELECT * FROM media_items WHERE id = :id")
    fun getMediaByIdAsFlow(id: Long): Flow<MediaItemEntity?>

    @Query("SELECT * FROM media_items WHERE id = :id")
    suspend fun getMediaById(id: Long): MediaItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(media: MediaItemEntity): Long

    @Update
    suspend fun updateMedia(media: MediaItemEntity)

    @Query("DELETE FROM media_items WHERE id = :id")
    suspend fun deleteMediaById(id: Long)

    @Query("SELECT COUNT(*) FROM media_items WHERE LOWER(TRIM(title)) = LOWER(TRIM(:title))")
    suspend fun countByTitle(title: String): Int

    @Query("SELECT * FROM media_items ORDER BY dateAdded DESC")
    suspend fun getAllMediaSync(): List<MediaItemEntity>

    @Query("DELETE FROM media_items")
    suspend fun deleteAllMedia()

    @Query("""
        SELECT COUNT(*) FROM media_items
        INNER JOIN watch_progress ON media_items.id = watch_progress.mediaId
        WHERE media_items.mediaType = :type AND watch_progress.isFinished = 1
    """)
    fun getWatchedCountByType(type: String): Flow<Int>
}
