package com.wabackuppro.workers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.wabackuppro.R
import com.wabackuppro.data.remote.DriveClient
import com.wabackuppro.domain.usecases.RunBackupUseCase
import com.wabackuppro.utils.FileScanner
import kotlinx.coroutines.flow.collect

/**
 * BackupWorker is a scheduled background task that triggers automated WhatsApp Business backups.
 * 
 * WorkManager Lifecycle & Constraints:
 * - Runs based on PeriodicWorkRequest constraints (e.g. requires Wi-Fi, battery not low).
 * - By default, background workers have 10 minutes maximum execution time.
 * - By calling setForeground(), this worker is elevated to a Foreground Service.
 * - This allows it to run longer than 10 minutes (essential for large backups) and guarantees
 *   it won't be killed by the OS Doze mode easily.
 */
class BackupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override suspend fun doWork(): Result {
        // 🛡️ Elevate to foreground service with a persistent notification
        setForeground(createForegroundInfo("Starting automatic backup..."))

        // 🔄 Instantiate dependencies manually (since no DI framework is used)
        val fileScanner = FileScanner(applicationContext)
        val driveClient = DriveClient(applicationContext)
        val useCase = RunBackupUseCase(fileScanner, driveClient)

        // 🔑 Fetch authenticated account
        val account = GoogleSignIn.getLastSignedInAccount(applicationContext)
            ?: return Result.failure() // Cannot proceed without an account

        var isSuccess = true

        try {
            // 🚀 Execute the backup and collect progress updates
            useCase.execute(account).collect { progress ->
                // Update the notification with the current progress
                setForeground(createForegroundInfo(progress.status))
                
                if (progress.errors.isNotEmpty()) {
                    isSuccess = false
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // If it's a retriable error (e.g. network lost), we could return Result.retry()
            // Here we assume unexpected exceptions should just fail or retry
            return Result.retry()
        }

        return if (isSuccess) Result.success() else Result.failure()
    }

    /**
     * Creates the ForegroundInfo containing the persistent notification for this worker.
     */
    private fun createForegroundInfo(progressStatus: String): ForegroundInfo {
        val channelId = "backup_channel_id"
        val notificationId = 1001

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Backup Notifications",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress of background WhatsApp backups"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("WhatsApp Backup")
            .setContentText(progressStatus)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setOngoing(true)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                notificationId, 
                notification, 
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(notificationId, notification)
        }
    }
}
