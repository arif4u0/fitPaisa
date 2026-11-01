package com.fitpaisa.app.core.sensor

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Legacy service retained for backward safety with any cached OS intents.
 * Step tracking is handled silently via hardware Sensor.TYPE_STEP_COUNTER and
 * background WorkManager sync, completely removing the persistent notification bar.
 */
@AndroidEntryPoint
class StepTrackingService : Service() {

    @Inject
    lateinit var stepSensorRepository: StepSensorRepository

    override fun onCreate() {
        super.onCreate()
        stop(this)
        stopSelf()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        stop(this)
        stopSelf()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val NOTIFICATION_ID = 4201

        /**
         * Clears any legacy persistent notification and halts the service so
         * no ongoing notification bar is shown to users.
         */
        fun stop(context: Context) {
            try {
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                notificationManager?.cancel(NOTIFICATION_ID)
                context.stopService(Intent(context, StepTrackingService::class.java))
            } catch (_: Exception) {}
        }

        /**
         * Deprecated start method kept for backward safety; directs to silent sync.
         */
        fun start(context: Context) {
            stop(context)
            StepSyncWorker.schedule(context)
        }
    }
}
