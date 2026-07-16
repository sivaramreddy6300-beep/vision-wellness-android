package com.example.visionwellness.optimization

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Manages background task scheduling for battery profiling and data cleanup
 * Uses WorkManager for reliable background execution
 */
class BackgroundTaskScheduler(private val context: Context) {

    private val workManager = WorkManager.getInstance(context)

    companion object {
        private const val BATTERY_PROFILER_WORK_ID = "battery_profiler_work"
        private const val BATTERY_PROFILE_INTERVAL_MINUTES = 30L
    }

    /**
     * Schedule periodic battery profiling
     */
    fun scheduleBatteryProfiling() {
        try {
            val batteryProfilerWork = PeriodicWorkRequestBuilder<BatteryProfilerWorker>(
                BATTERY_PROFILE_INTERVAL_MINUTES,
                TimeUnit.MINUTES
            ).build()

            workManager.enqueueUniquePeriodicWork(
                BATTERY_PROFILER_WORK_ID,
                ExistingPeriodicWorkPolicy.KEEP,
                batteryProfilerWork
            )

            Timber.d("Battery profiling scheduled every $BATTERY_PROFILE_INTERVAL_MINUTES minutes")
        } catch (e: Exception) {
            Timber.e(e, "Error scheduling battery profiling")
        }
    }

    /**
     * Cancel battery profiling
     */
    fun cancelBatteryProfiling() {
        try {
            workManager.cancelUniqueWork(BATTERY_PROFILER_WORK_ID)
            Timber.d("Battery profiling cancelled")
        } catch (e: Exception) {
            Timber.e(e, "Error cancelling battery profiling")
        }
    }
}
