package com.neddy.watchlog.data.backup

data class WatchlogBackup(
    val version: Int = 2,
    val exportedAt: Long = System.currentTimeMillis(),
    val items: List<BackupItem> = emptyList()
)

data class BackupItem(
    val apiId: String?,
    val title: String,
    val description: String,
    val mediaType: String,
    val posterUrl: String?,
    val dateAdded: Long,
    val progress: BackupProgress?,
    val seasons: List<BackupSeason>,
    val watchedEpisodes: List<BackupWatchedEpisode>,
    val tags: List<String>,
    val reminderTriggerAtMillis: Long? = null
)

data class BackupProgress(
    val isFinished: Boolean,
    val lastWatchedDate: Long,
    val currentSeason: Int?,
    val currentEpisode: Int?
)

data class BackupSeason(val seasonNumber: Int, val episodeCount: Int)

data class BackupWatchedEpisode(val seasonNumber: Int, val episodeNumber: Int)
