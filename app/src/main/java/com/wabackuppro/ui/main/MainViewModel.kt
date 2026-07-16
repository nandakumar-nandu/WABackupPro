package com.wabackuppro.ui.main

import androidx.lifecycle.AndroidViewModel
import android.app.Application
import com.wabackuppro.utils.FileScanner
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * MainViewModel manages the UI state for the backup processes and logs list.
 * It coordinates backup actions and updates live status data.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val fileScanner = FileScanner(application)

    // 📊 Holds the current status of the backup operation
    private val _backupStatus = MutableLiveData<String>("No backup run yet")
    val backupStatus: LiveData<String> = _backupStatus

    // 📁 Holds the count of files discovered during scanning
    private val _discoveredFilesCount = MutableLiveData<Int>(0)
    val discoveredFilesCount: LiveData<Int> = _discoveredFilesCount

    // 📋 Holds the list of activity logs to display in the RecyclerView
    private val _activityLogs = MutableLiveData<List<String>>(emptyList())
    val activityLogs: LiveData<List<String>> = _activityLogs

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    /**
     * Triggers the backup process, including file scanning.
     */
    fun startBackup() {
        // 🚀 Starting backup process logic
        _backupStatus.value = "Scanning for files..."

        // 🔍 Perform file scan
        val files = fileScanner.scanWhatsAppBusinessFiles()
        _discoveredFilesCount.value = files.size

        // 📝 Update status and logs
        _backupStatus.value = "Scan complete: Found ${files.size} files"
        
        val timestamp = LocalDateTime.now().format(dateFormatter)
        val currentLogs = _activityLogs.value.orEmpty().toMutableList()
        currentLogs.add(0, "[$timestamp] Scanned WhatsApp Business media. Found ${files.size} files.")
        _activityLogs.value = currentLogs
    }
}
