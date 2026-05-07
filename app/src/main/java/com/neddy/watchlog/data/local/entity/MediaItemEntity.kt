package com.neddy.watchlog.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media_items")
data class MediaItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val apiId: String? = null,
    val title: String,
    val description: String = "",
    val mediaType: String,
    val posterUrl: String? = null,
    val dateAdded: Long = System.currentTimeMillis()
)
