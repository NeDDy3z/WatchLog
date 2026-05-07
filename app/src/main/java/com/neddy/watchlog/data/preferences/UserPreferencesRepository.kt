package com.neddy.watchlog.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "watchlog_prefs")

class UserPreferencesRepository(private val context: Context) {

    companion object {
        val SORT_ORDER = stringPreferencesKey("sort_order")
        val DEFAULT_FILTER = stringPreferencesKey("default_filter")
        val DEFAULT_EPISODES_PER_SEASON = intPreferencesKey("default_episodes")
        val WATCHLIST_DISPLAY_MODE = stringPreferencesKey("watchlist_display_mode")
        val SHOW_WATCHED_DEFAULT = booleanPreferencesKey("show_watched_default")

        @Volatile
        private var INSTANCE: UserPreferencesRepository? = null

        fun getInstance(context: Context): UserPreferencesRepository =
            INSTANCE ?: synchronized(this) {
                UserPreferencesRepository(context.applicationContext).also { INSTANCE = it }
            }
    }

    val sortOrder: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[SORT_ORDER] ?: SortOrder.DATE_ADDED
    }

    val defaultEpisodesPerSeason: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[DEFAULT_EPISODES_PER_SEASON] ?: 10
    }

    val watchlistDisplayMode: Flow<WatchlistDisplayMode> = context.dataStore.data.map { prefs ->
        WatchlistDisplayMode.entries.find { it.name == prefs[WATCHLIST_DISPLAY_MODE] }
            ?: WatchlistDisplayMode.LIST
    }

    val showWatchedDefault: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[SHOW_WATCHED_DEFAULT] ?: false
    }

    suspend fun setSortOrder(order: String) {
        context.dataStore.edit { it[SORT_ORDER] = order }
    }

    suspend fun setDefaultEpisodesPerSeason(count: Int) {
        context.dataStore.edit { it[DEFAULT_EPISODES_PER_SEASON] = count.coerceIn(1, 99) }
    }

    suspend fun setWatchlistDisplayMode(mode: WatchlistDisplayMode) {
        context.dataStore.edit { it[WATCHLIST_DISPLAY_MODE] = mode.name }
    }

    suspend fun setShowWatchedDefault(show: Boolean) {
        context.dataStore.edit { it[SHOW_WATCHED_DEFAULT] = show }
    }
}

object SortOrder {
    const val DATE_ADDED = "date_added"
    const val OLDER = "older"
    const val TITLE = "title"
}

enum class WatchlistDisplayMode { LIST, COMPACT_LIST, GRID }
