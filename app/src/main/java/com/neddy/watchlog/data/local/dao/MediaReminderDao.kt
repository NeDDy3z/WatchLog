package com.neddy.watchlog.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.neddy.watchlog.data.local.entity.MediaReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaReminderDao {
    @Query("SELECT * FROM media_reminders WHERE mediaId = :mediaId LIMIT 1")
    fun getReminderForMedia(mediaId: Long): Flow<MediaReminderEntity?>

    @Query("SELECT * FROM media_reminders WHERE mediaId = :mediaId LIMIT 1")
    suspend fun getReminderForMediaSync(mediaId: Long): MediaReminderEntity?

    @Query("SELECT * FROM media_reminders")
    suspend fun getAllRemindersSync(): List<MediaReminderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReminder(reminder: MediaReminderEntity)

    @Query("DELETE FROM media_reminders WHERE mediaId = :mediaId")
    suspend fun deleteReminderForMedia(mediaId: Long)

    @Query("DELETE FROM media_reminders")
    suspend fun deleteAllReminders()
}

