package com.wabackuppro.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * MainViewModel manages the UI state for the backup processes and logs list.
 * It coordinates backup actions and updates live status data.
 */
class MainViewModel : ViewModel() {

    // 📊 Holds the current status of the backup operation
    private val _backupStatus = MutableLiveData<String>("No backup run yet")
    val backupStatus: LiveData<String> = _backupStatus

    // 📋 Holds the list of activity logs to display in the RecyclerView
    private val _activityLogs = MutableLiveData<List<String>>(emptyList())
    val activityLogs: LiveData<List<String>> = _activityLogs

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    /**
     * Triggers the backup process (currently placeholder functionality).
     */
    fun startBackup() {
        // 🚀 Starting backup process logic placeholder
        _backupStatus.value = "Backup in progress..."
        
        // 📝 Append a log entry
        val timestamp = LocalDateTime.now().format(dateFormatter)
        val currentLogs = _activityLogs.value.orEmpty().toMutableList()
        currentLogs.add(0, "[$timestamp] Backup process started (Placeholder)")
        _activityLogs.value = currentLogs
    }
}
