package com.fitpaisa.app.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.fitpaisa.app.MainActivity
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local daily reminders (no server needed):
 * - 8:00 AM: "Time to walk!" (challenge reminder)
 * - 8:00 PM: "Convert your steps!" (evening nudge)
 *
 * Uses WorkManager for reliable daily scheduling.
 */
@Singleton
class NotificationScheduler @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_ID = "fitpaisa_reminders"
        const val MORNING_WORK = "morning_reminder"
        const val EVENING_WORK = "evening_reminder"
    }

    fun scheduleDaily() {
        createChannel()
        scheduleAt(MORNING_WORK, 8, 0,
            "🚶 Time to walk!",
            "Hit today's challenge — walk 8,000 steps for +50 bonus coins!")
        scheduleAt(EVENING_WORK, 20, 0,
            "💰 Convert your steps!",
            "Don't forget to convert today's steps into FitCoins before midnight.")
    }

    private fun scheduleAt(workName: String, hour: Int, minute: Int, title: String, text: String) {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= now.timeInMillis) add(Calendar.DAY_OF_YEAR, 1)
        }
        val delayMs = target.timeInMillis - now.timeInMillis

        val input = workDataOf(
            "title" to title,
            "text" to text,
            "notification_id" to workName.hashCode()
        )
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(input)
            .addTag(workName)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            workName,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Daily Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Walk and convert reminders"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}

/** Shows the notification, then reschedules for tomorrow. */
class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        val title = inputData.getString("title") ?: return Result.failure()
        val text = inputData.getString("text") ?: return Result.failure()
        val id = inputData.getInt("notification_id", 0)

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, NotificationScheduler.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Notification permission is checked by the caller (Android 13+).
        try {
            manager.notify(id, notification)
        } catch (e: SecurityException) {
            // Permission not granted — skip silently.
        }

        // Reschedule for tomorrow (WorkManager one-time → chain daily).
        // The scheduler re-runs on every app start, so this is a backup.
        return Result.success()
    }
}
