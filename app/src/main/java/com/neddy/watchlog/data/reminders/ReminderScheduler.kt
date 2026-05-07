package com.neddy.watchlog.data.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.Manifest
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.neddy.watchlog.R
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    const val CHANNEL_ID = "watchlog_reminders"
    const val EXTRA_MEDIA_ID = "reminder_media_id"
    const val EXTRA_TRIGGER_AT = "reminder_trigger_at"

    private fun workName(mediaId: Long) = "watchlog_reminder_$mediaId"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = ContextCompat.getSystemService(context, NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.reminder_channel_description)
        }
        manager.createNotificationChannel(channel)
    }

    fun schedule(context: Context, mediaId: Long, triggerAtMillis: Long) {
        val delay = (triggerAtMillis - System.currentTimeMillis()).coerceAtLeast(0L)
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf(
                    EXTRA_MEDIA_ID to mediaId,
                    EXTRA_TRIGGER_AT to triggerAtMillis
                )
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            workName(mediaId),
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancel(context: Context, mediaId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork(workName(mediaId))
    }
}

fun showReminderNotification(context: Context, mediaTitle: String, mediaId: Long) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
    ) {
        return
    }
    ReminderScheduler.ensureChannel(context)
    val builder = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle(context.getString(R.string.reminder_notification_title, mediaTitle))
        .setContentText(context.getString(R.string.reminder_notification_text))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .setContentIntent(ReminderPendingIntent.create(context, mediaId))

    NotificationManagerCompat.from(context).notify(mediaId.toInt(), builder.build())
}

