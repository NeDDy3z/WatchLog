package com.neddy.watchlog.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.neddy.watchlog.data.backup.AutoBackupScheduler
import com.neddy.watchlog.data.backup.DriveBackupRepository
import com.neddy.watchlog.data.preferences.AutoBackupFrequency
import com.neddy.watchlog.data.preferences.SwipeAction
import com.neddy.watchlog.data.preferences.SwipeWatchedScope
import com.neddy.watchlog.data.preferences.UserPreferencesRepository
import com.neddy.watchlog.data.preferences.WatchlistDisplayMode
import com.neddy.watchlog.data.repository.MediaRepository
import com.neddy.watchlog.data.update.UpdateRepository
import com.neddy.watchlog.data.update.UpdateState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class BackupAction { BACKUP, RESTORE }

sealed class BackupState {
    object Idle : BackupState()
    object Loading : BackupState()
    data class Success(val message: String) : BackupState()
    data class Error(val message: String) : BackupState()
}

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsRepository = UserPreferencesRepository.getInstance(application)
    private val mediaRepository = MediaRepository.getInstance(application)
    private val driveBackupRepository = DriveBackupRepository.getInstance(application)
    private val updateRepository = UpdateRepository.getInstance(application)

    val watchedMoviesCount: StateFlow<Int> = mediaRepository.getWatchedMoviesCount().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), 0
    )

    val watchedTvShowsCount: StateFlow<Int> = mediaRepository.getWatchedTvShowsCount().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), 0
    )

    val defaultEpisodesPerSeason: StateFlow<Int> = prefsRepository.defaultEpisodesPerSeason.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), 10
    )

    val displayMode: StateFlow<WatchlistDisplayMode> = prefsRepository.watchlistDisplayMode.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), WatchlistDisplayMode.LIST
    )

    val showWatchedDefault: StateFlow<Boolean> = prefsRepository.showWatchedDefault.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), false
    )

    val autoBackupFrequency: StateFlow<AutoBackupFrequency> = prefsRepository.autoBackupFrequency.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), AutoBackupFrequency.OFF
    )

    val lastBackupAt: StateFlow<Long> = prefsRepository.lastBackupAt.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L
    )

    val swipeLeftAction: StateFlow<SwipeAction> = prefsRepository.swipeLeftAction.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), SwipeAction.DELETE
    )

    val swipeRightAction: StateFlow<SwipeAction> = prefsRepository.swipeRightAction.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), SwipeAction.MARK_WATCHED
    )

    val swipeWatchedScope: StateFlow<SwipeWatchedScope> = prefsRepository.swipeWatchedScope.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), SwipeWatchedScope.WHOLE_SHOW
    )

    val updateCheckEnabled: StateFlow<Boolean> = prefsRepository.updateCheckEnabled.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), true
    )

    val lastUpdateCheckAt: StateFlow<Long> = prefsRepository.lastUpdateCheckAt.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L
    )

    val updateState: StateFlow<UpdateState> = updateRepository.state

    private val _backupState = MutableStateFlow<BackupState>(BackupState.Idle)
    val backupState: StateFlow<BackupState> = _backupState.asStateFlow()

    var pendingAction: BackupAction? = null

    fun setDefaultEpisodesPerSeason(count: Int) {
        viewModelScope.launch { prefsRepository.setDefaultEpisodesPerSeason(count) }
    }

    fun setDisplayMode(mode: WatchlistDisplayMode) {
        viewModelScope.launch { prefsRepository.setWatchlistDisplayMode(mode) }
    }

    fun setShowWatchedDefault(show: Boolean) {
        viewModelScope.launch { prefsRepository.setShowWatchedDefault(show) }
    }

    fun setSwipeLeftAction(action: SwipeAction) {
        viewModelScope.launch { prefsRepository.setSwipeLeftAction(action) }
    }

    fun setSwipeRightAction(action: SwipeAction) {
        viewModelScope.launch { prefsRepository.setSwipeRightAction(action) }
    }

    fun setSwipeWatchedScope(scope: SwipeWatchedScope) {
        viewModelScope.launch { prefsRepository.setSwipeWatchedScope(scope) }
    }

    fun setUpdateCheckEnabled(enabled: Boolean) {
        viewModelScope.launch { prefsRepository.setUpdateCheckEnabled(enabled) }
    }

    fun checkForUpdates() {
        viewModelScope.launch { updateRepository.checkNow() }
    }

    fun clearUpdateState() {
        updateRepository.dismiss()
    }

    fun setAutoBackupFrequency(frequency: AutoBackupFrequency) {
        viewModelScope.launch {
            prefsRepository.setAutoBackupFrequency(frequency)
            AutoBackupScheduler.schedule(getApplication(), frequency)
        }
    }

    fun startBackup(accessToken: String) {
        viewModelScope.launch {
            _backupState.value = BackupState.Loading
            prefsRepository.setAutoBackupAccessToken(accessToken)
            val result = driveBackupRepository.backup(accessToken, mediaRepository)
            _backupState.value = result.fold(
                onSuccess = {
                    prefsRepository.setLastBackupAt(System.currentTimeMillis())
                    BackupState.Success("Backup saved to Google Drive")
                },
                onFailure = { BackupState.Error(it.message ?: "Backup failed") }
            )
        }
    }

    fun startRestore(accessToken: String) {
        viewModelScope.launch {
            _backupState.value = BackupState.Loading
            prefsRepository.setAutoBackupAccessToken(accessToken)
            val result = driveBackupRepository.restore(accessToken, mediaRepository)
            _backupState.value = result.fold(
                onSuccess = { BackupState.Success("Watchlist restored successfully") },
                onFailure = { BackupState.Error(it.message ?: "Restore failed") }
            )
        }
    }

    fun setBackupError(message: String) {
        _backupState.value = BackupState.Error(message)
    }

    fun clearBackupState() {
        _backupState.value = BackupState.Idle
    }
}
