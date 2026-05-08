package com.neddy.watchlog.data.backup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.neddy.watchlog.data.preferences.UserPreferencesRepository
import com.neddy.watchlog.data.repository.MediaRepository
import kotlinx.coroutines.flow.first

class AutoBackupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = UserPreferencesRepository.getInstance(applicationContext)
        val token = prefs.autoBackupAccessToken.first()
        if (token.isBlank()) return Result.failure()

        val mediaRepository = MediaRepository.getInstance(applicationContext)
        val driveBackupRepository = DriveBackupRepository.getInstance(applicationContext)
        val result = driveBackupRepository.backup(token, mediaRepository)

        return result.fold(
            onSuccess = {
                prefs.setLastBackupAt(System.currentTimeMillis())
                Result.success()
            },
            onFailure = { Result.retry() }
        )
    }
}
