package com.wabackuppro.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.wabackuppro.utils.BackupScheduler
import java.time.LocalTime

/**
 * BootReceiver listens for the device boot completion event to reschedule background jobs
 * in case they were cleared by the system.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Restore the scheduled backup time
            // In a real app, this should be fetched from SharedPreferences. 
            // For now, we default to 2:00 AM.
            val scheduler = BackupScheduler(context)
            scheduler.scheduleFridayBackup(LocalTime.of(2, 0))
        }
    }
}
