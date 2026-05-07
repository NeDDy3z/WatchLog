package com.neddy.watchlog.data.local.dao

import androidx.room.*
import com.neddy.watchlog.data.local.entity.UserTagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserTagDao {

    @Query("SELECT * FROM user_tags WHERE mediaId = :mediaId")
    fun getTagsForMedia(mediaId: Long): Flow<List<UserTagEntity>>

    @Query("SELECT * FROM user_tags WHERE mediaId = :mediaId")
    suspend fun getTagsForMediaSync(mediaId: Long): List<UserTagEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: UserTagEntity): Long

    @Delete
    suspend fun deleteTag(tag: UserTagEntity)
}
