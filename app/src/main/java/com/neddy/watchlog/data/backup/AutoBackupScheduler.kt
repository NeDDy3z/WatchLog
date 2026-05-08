package com.neddy.watchlog.data.backup

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.neddy.watchlog.data.preferences.AutoBackupFrequency
import java.util.concurrent.TimeUnit

object AutoBackupScheduler {
    private const val WORK_NAME = "watchlog_auto_backup"

    fun schedule(context: Context, frequency: AutoBackupFrequency) {
        val workManager = WorkManager.getInstance(context)
        if (frequency == AutoBackupFrequency.OFF) {
            workManager.cancelUniqueWork(WORK_NAME)
            return
        }

        val repeatIntervalDays = when (frequency) {
            AutoBackupFrequency.DAILY -> 1L
            AutoBackupFrequency.WEEKLY -> 7L
            AutoBackupFrequency.MONTHLY -> 30L
            AutoBackupFrequency.OFF -> 1L
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(repeatIntervalDays, TimeUnit.DAYS)
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}
