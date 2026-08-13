package com.neddy.watchlog.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
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
        val AUTO_BACKUP_FREQUENCY = stringPreferencesKey("auto_backup_frequency")
        val LAST_BACKUP_AT = longPreferencesKey("last_backup_at")
        val AUTO_BACKUP_ACCESS_TOKEN = stringPreferencesKey("auto_backup_access_token")
        val SWIPE_LEFT_ACTION = stringPreferencesKey("swipe_left_action")
        val SWIPE_RIGHT_ACTION = stringPreferencesKey("swipe_right_action")
        val SWIPE_WATCHED_SCOPE = stringPreferencesKey("swipe_watched_scope")
        val UPDATE_CHECK_ENABLED = booleanPreferencesKey("update_check_enabled")
        val SKIPPED_UPDATE_VERSION = stringPreferencesKey("skipped_update_version")
        val LAST_UPDATE_CHECK_AT = longPreferencesKey("last_update_check_at")

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

    val autoBackupFrequency: Flow<AutoBackupFrequency> = context.dataStore.data.map { prefs ->
        AutoBackupFrequency.entries.find { it.name == prefs[AUTO_BACKUP_FREQUENCY] }
            ?: AutoBackupFrequency.OFF
    }

    val lastBackupAt: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[LAST_BACKUP_AT] ?: 0L
    }

    val autoBackupAccessToken: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[AUTO_BACKUP_ACCESS_TOKEN] ?: ""
    }

    val swipeLeftAction: Flow<SwipeAction> = context.dataStore.data.map { prefs ->
        SwipeAction.entries.find { it.name == prefs[SWIPE_LEFT_ACTION] } ?: SwipeAction.DELETE
    }

    val swipeRightAction: Flow<SwipeAction> = context.dataStore.data.map { prefs ->
        SwipeAction.entries.find { it.name == prefs[SWIPE_RIGHT_ACTION] } ?: SwipeAction.MARK_WATCHED
    }

    val swipeWatchedScope: Flow<SwipeWatchedScope> = context.dataStore.data.map { prefs ->
        SwipeWatchedScope.entries.find { it.name == prefs[SWIPE_WATCHED_SCOPE] } ?: SwipeWatchedScope.WHOLE_SHOW
    }

    val updateCheckEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[UPDATE_CHECK_ENABLED] ?: true
    }

    val skippedUpdateVersion: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[SKIPPED_UPDATE_VERSION] ?: ""
    }

    val lastUpdateCheckAt: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[LAST_UPDATE_CHECK_AT] ?: 0L
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

    suspend fun setAutoBackupFrequency(frequency: AutoBackupFrequency) {
        context.dataStore.edit { it[AUTO_BACKUP_FREQUENCY] = frequency.name }
    }

    suspend fun setLastBackupAt(millis: Long) {
        context.dataStore.edit { it[LAST_BACKUP_AT] = millis }
    }

    suspend fun setAutoBackupAccessToken(token: String) {
        context.dataStore.edit { it[AUTO_BACKUP_ACCESS_TOKEN] = token }
    }

    suspend fun setSwipeLeftAction(action: SwipeAction) {
        context.dataStore.edit { it[SWIPE_LEFT_ACTION] = action.name }
    }

    suspend fun setSwipeRightAction(action: SwipeAction) {
        context.dataStore.edit { it[SWIPE_RIGHT_ACTION] = action.name }
    }

    suspend fun setSwipeWatchedScope(scope: SwipeWatchedScope) {
        context.dataStore.edit { it[SWIPE_WATCHED_SCOPE] = scope.name }
    }

    suspend fun setUpdateCheckEnabled(enabled: Boolean) {
        context.dataStore.edit { it[UPDATE_CHECK_ENABLED] = enabled }
    }

    suspend fun setSkippedUpdateVersion(version: String) {
        context.dataStore.edit { it[SKIPPED_UPDATE_VERSION] = version }
    }

    suspend fun setLastUpdateCheckAt(millis: Long) {
        context.dataStore.edit { it[LAST_UPDATE_CHECK_AT] = millis }
    }
}

object SortOrder {
    const val DATE_ADDED = "date_added"
    const val OLDER = "older"
    const val TITLE = "title"
}

@androidx.annotation.Keep
enum class WatchlistDisplayMode { LIST, COMPACT_LIST, GRID }

@androidx.annotation.Keep
enum class AutoBackupFrequency { OFF, DAILY, WEEKLY, MONTHLY }

/** What a swipe in a given direction does on a watchlist card. */
@androidx.annotation.Keep
enum class SwipeAction { NONE, MARK_WATCHED, DELETE }

/** How much of a TV show a "mark as watched" swipe covers. Movies are always finished whole. */
@androidx.annotation.Keep
enum class SwipeWatchedScope { WHOLE_SHOW, NEXT_SEASON, NEXT_EPISODE }
