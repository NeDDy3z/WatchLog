package com.neddy.watchlog.data.reminders

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.neddy.watchlog.data.repository.MediaRepository

class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val mediaId = inputData.getLong(ReminderScheduler.EXTRA_MEDIA_ID, -1L)
        val triggerAtMillis = inputData.getLong(ReminderScheduler.EXTRA_TRIGGER_AT, -1L)
        if (mediaId == -1L || triggerAtMillis == -1L) return Result.failure()

        val repository = MediaRepository.getInstance(applicationContext)
        val media = repository.getMediaById(mediaId) ?: return Result.success()
        val reminder = repository.getReminderForMediaSync(mediaId) ?: return Result.success()

        if (reminder.triggerAtMillis != triggerAtMillis) return Result.success()
        if (System.currentTimeMillis() < triggerAtMillis) return Result.retry()

        showReminderNotification(applicationContext, media.title, mediaId)
        return Result.success()
    }
}

