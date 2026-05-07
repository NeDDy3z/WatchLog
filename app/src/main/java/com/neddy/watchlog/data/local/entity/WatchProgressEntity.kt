package com.neddy.watchlog.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "watch_progress",
    foreignKeys = [
        ForeignKey(
            entity = MediaItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["mediaId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class WatchProgressEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(index = true) val mediaId: Long,
    val currentSeason: Int? = null,
    val currentEpisode: Int? = null,
    val isFinished: Boolean = false,
    val lastWatchedDate: Long = System.currentTimeMillis()
)
