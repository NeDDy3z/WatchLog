package com.neddy.watchlog.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.neddy.watchlog.data.local.dao.MediaItemDao
import com.neddy.watchlog.data.local.dao.MediaReminderDao
import com.neddy.watchlog.data.local.dao.SeasonInfoDao
import com.neddy.watchlog.data.local.dao.UserTagDao
import com.neddy.watchlog.data.local.dao.WatchProgressDao
import com.neddy.watchlog.data.local.dao.WatchedEpisodeDao
import com.neddy.watchlog.data.local.entity.MediaItemEntity
import com.neddy.watchlog.data.local.entity.MediaReminderEntity
import com.neddy.watchlog.data.local.entity.SeasonInfoEntity
import com.neddy.watchlog.data.local.entity.UserTagEntity
import com.neddy.watchlog.data.local.entity.WatchProgressEntity
import com.neddy.watchlog.data.local.entity.WatchedEpisodeEntity

@Database(
    entities = [
        MediaItemEntity::class,
        MediaReminderEntity::class,
        WatchProgressEntity::class,
        UserTagEntity::class,
        SeasonInfoEntity::class,
        WatchedEpisodeEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class WatchlogDatabase : RoomDatabase() {

    abstract fun mediaItemDao(): MediaItemDao
    abstract fun mediaReminderDao(): MediaReminderDao
    abstract fun watchProgressDao(): WatchProgressDao
    abstract fun userTagDao(): UserTagDao
    abstract fun seasonInfoDao(): SeasonInfoDao
    abstract fun watchedEpisodeDao(): WatchedEpisodeDao

    companion object {
        @Volatile
        private var INSTANCE: WatchlogDatabase? = null

        fun getDatabase(context: Context): WatchlogDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    WatchlogDatabase::class.java,
                    "watchlog_database"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
