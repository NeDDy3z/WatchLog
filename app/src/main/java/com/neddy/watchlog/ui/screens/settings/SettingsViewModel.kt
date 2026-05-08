package com.neddy.watchlog.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.neddy.watchlog.data.backup.AutoBackupScheduler
import com.neddy.watchlog.data.backup.DriveBackupRepository
import com.neddy.watchlog.data.preferences.AutoBackupFrequency
import com.neddy.watchlog.data.preferences.UserPreferencesRepository
import com.neddy.watchlog.data.preferences.WatchlistDisplayMode
import com.neddy.watchlog.data.repository.MediaRepository
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
