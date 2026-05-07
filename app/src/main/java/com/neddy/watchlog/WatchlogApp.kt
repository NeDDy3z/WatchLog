package com.neddy.watchlog

import android.app.Application
import com.neddy.watchlog.data.local.WatchlogDatabase
import com.neddy.watchlog.data.preferences.UserPreferencesRepository
import com.neddy.watchlog.data.repository.MediaRepository

class WatchlogApp : Application() {

    val database by lazy { WatchlogDatabase.getDatabase(this) }
    val mediaRepository by lazy { MediaRepository.getInstance(this) }
    val preferencesRepository by lazy { UserPreferencesRepository.getInstance(this) }
}
