package com.wabackuppro.utils

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.wabackuppro.workers.BackupWorker
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.TimeUnit

/**
 * BackupScheduler handles scheduling background backup jobs using WorkManager.
 */
class BackupScheduler(private val context: Context) {

    companion object {
        const val BACKUP_WORK_NAME = "weekly_friday_backup"
    }

    /**
     * Schedules an automatic backup to run every Friday at the specified [LocalTime].
     * 
     * Algorithm for delay calculation:
     * 1. Get the current date and time.
     * 2. Find the next occurrence of Friday using TemporalAdjusters.
     * 3. Apply the chosen [time] to that Friday.
     * 4. If that time has already passed today (and today is Friday), advance to the NEXT Friday.
     * 5. Calculate the delay in milliseconds from now until the target Friday time.
     */
    fun scheduleFridayBackup(time: LocalTime) {
        val now = LocalDateTime.now(ZoneId.systemDefault())
        
        var nextRun = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY))
            .withHour(time.hour)
            .withMinute(time.minute)
            .withSecond(0)
            .withNano(0)

        // If today is Friday but the time has already passed, schedule for next Friday
        if (nextRun.isBefore(now)) {
            nextRun = nextRun.plusWeeks(1)
        }

        val initialDelayMillis = nextRun.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() - 
                               now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // Configure constraints: require Wi-Fi and adequate battery
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .setRequiresBatteryNotLow(true)
            .build()

        // Create a periodic work request that repeats every 7 days
        val backupRequest = PeriodicWorkRequestBuilder<BackupWorker>(7, TimeUnit.DAYS)
            .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .addTag(BACKUP_WORK_NAME)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            BACKUP_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            backupRequest
        )
    }

    /**
     * Cancels the scheduled backup job.
     */
    fun cancelSchedule() {
        WorkManager.getInstance(context).cancelUniqueWork(BACKUP_WORK_NAME)
    }
}
