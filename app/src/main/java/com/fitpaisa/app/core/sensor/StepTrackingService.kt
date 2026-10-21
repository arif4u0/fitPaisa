package com.fitpaisa.app.core.sensor

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.fitpaisa.app.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Keeps the hardware step-counter listener alive while fitPaisa is in the
 * background. Android can stop an ordinary app process, so step tracking is
 * hosted here instead of relying on a screen/ViewModel having been opened.
 *
 * The small ongoing notification is required by Android for a foreground
 * service. It is also useful proof to the user that tracking is active.
 */
@AndroidEntryPoint
class StepTrackingService : Service() {

    @Inject
    lateinit var stepSensorRepository: StepSensorRepository

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForegroundWithNotification()
        stepSensorRepository.startTracking()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        stepSensorRepository.startTracking()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundWithNotification() {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("fitPaisa is counting your steps")
            .setContentText("Step tracking is on")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Step tracking",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows while fitPaisa counts steps in the background"
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "fitpaisa_step_tracking"
        private const val NOTIFICATION_ID = 4201

        fun start(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACTIVITY_RECOGNITION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }

            val intent = Intent(context, StepTrackingService::class.java)
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (_: Exception) {
                // Some phone makers block a background start. The service is
                // started again from MainActivity the next time fitPaisa is
                // opened; the saved hardware-counter baseline still lets the
                // app recover the count in the normal background case.
            }
        }
    }
}
