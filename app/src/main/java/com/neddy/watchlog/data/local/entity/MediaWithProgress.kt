package com.neddy.watchlog.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class MediaWithProgress(
    @Embedded val media: MediaItemEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "mediaId"
    )
    val progress: WatchProgressEntity?
)
