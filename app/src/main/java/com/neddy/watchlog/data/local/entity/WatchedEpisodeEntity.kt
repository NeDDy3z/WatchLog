package com.neddy.watchlog.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "watched_episodes",
    foreignKeys = [ForeignKey(
        entity = MediaItemEntity::class,
        parentColumns = ["id"],
        childColumns = ["mediaId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [
        Index("mediaId"),
        Index(value = ["mediaId", "seasonNumber", "episodeNumber"], unique = true)
    ]
)
data class WatchedEpisodeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo val mediaId: Long,
    val seasonNumber: Int,
    val episodeNumber: Int
)
