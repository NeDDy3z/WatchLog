package com.neddy.watchlog.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media_reminders")
data class MediaReminderEntity(
    @PrimaryKey val mediaId: Long,
    val triggerAtMillis: Long
)

