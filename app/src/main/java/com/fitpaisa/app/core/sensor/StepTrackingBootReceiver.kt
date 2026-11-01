package com.fitpaisa.app.core.sensor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Restarts background silent step sync after reboot or app update with NO ongoing notification. */
class StepTrackingBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                StepTrackingService.stop(context)
                StepSyncWorker.schedule(context)
            }
        }
    }
}
