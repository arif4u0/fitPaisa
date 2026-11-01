package com.fitpaisa.app.core.sensor

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit

@EntryPoint
@InstallIn(SingletonComponent::class)
interface StepSyncWorkerEntryPoint {
    fun stepSensorRepository(): StepSensorRepository
}

/**
 * Periodically syncs steps in the background without needing a foreground service
 * or any persistent notification bar in the user's notification shade.
 */
class StepSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                StepSyncWorkerEntryPoint::class.java
            )
            entryPoint.stepSensorRepository().refresh()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "fitpaisa_silent_step_sync"

        fun schedule(context: Context) {
            try {
                val request = PeriodicWorkRequestBuilder<StepSyncWorker>(
                    15, TimeUnit.MINUTES
                ).setConstraints(
                    Constraints.Builder().build()
                ).build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
            } catch (_: Exception) {}
        }
    }
}
