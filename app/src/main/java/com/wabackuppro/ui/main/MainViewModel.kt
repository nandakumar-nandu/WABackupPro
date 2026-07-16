package com.wabackuppro.ui.main

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.wabackuppro.data.remote.DriveClient
import com.wabackuppro.utils.FileScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * MainViewModel manages the UI state for the backup processes and logs list.
 * It coordinates backup actions and updates live status data.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val fileScanner = FileScanner(application)
    private val driveClient = DriveClient(application)

    // 📊 Holds the current status of the backup operation
    private val _backupStatus = MutableLiveData<String>("No backup run yet")
    val backupStatus: LiveData<String> = _backupStatus

    // 👤 Current authenticated Google account
    private val _googleAccount = MutableLiveData<GoogleSignInAccount?>(
        GoogleSignIn.getLastSignedInAccount(application)
    )
    val googleAccount: LiveData<GoogleSignInAccount?> = _googleAccount

    // 📁 Holds the count of files discovered during scanning
    private val _discoveredFilesCount = MutableLiveData<Int>(0)
    val discoveredFilesCount: LiveData<Int> = _discoveredFilesCount

    // 📋 Holds the list of activity logs to display in the RecyclerView
    private val _activityLogs = MutableLiveData<List<String>>(emptyList())
    val activityLogs: LiveData<List<String>> = _activityLogs

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    /**
     * Updates the authenticated account state.
     */
    fun updateAccount(account: GoogleSignInAccount?) {
        _googleAccount.value = account
        if (account != null) {
            addLog("Signed in as ${account.email}")
        } else {
            addLog("Signed out from Google Drive")
        }
    }

    /**
     * Returns the intent to trigger sign-in.
     */
    fun getSignInIntent() = driveClient.getSignInIntent()

    /**
     * Signs out the user.
     */
    fun signOut() {
        driveClient.signOut {
            updateAccount(null)
        }
    }

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
        addLog("Scanned WhatsApp Business media. Found ${files.size} files.")
    }

    /**
     * Performs a test upload to verify Google Drive integration.
     */
    fun testUpload() {
        val account = _googleAccount.value ?: run {
            addLog("Error: Sign-in required for test upload")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                addLog("Starting test upload...")
                
                // 1. Create a "WABackup_Test" folder
                val folderId = driveClient.createFolder(account, "WABackup_Test") ?: throw Exception("Folder creation failed")
                addLog("Created folder: WABackup_Test (ID: $folderId)")

                // 2. Create a dummy local file for testing
                val testFile = java.io.File(getApplication<Application>().cacheDir, "test_backup.txt")
                testFile.writeText("WABackupPro Test Content - ${LocalDateTime.now()}")

                // 3. Upload the file
                val fileId = driveClient.uploadFile(
                    account,
                    testFile.absolutePath,
                    folderId,
                    "text/plain"
                )
                
                addLog("✅ Test upload success! File ID: $fileId")
                _backupStatus.postValue("Test upload complete")
            } catch (e: Exception) {
                addLog("❌ Test upload failed: ${e.message}")
                _backupStatus.postValue("Test upload failed")
            }
        }
    }

    /**
     * Helper to append a log entry to the list.
     */
    private fun addLog(message: String) {
        val timestamp = LocalDateTime.now().format(dateFormatter)
        val currentLogs = _activityLogs.value.orEmpty().toMutableList()
        currentLogs.add(0, "[$timestamp] $message")
        _activityLogs.postValue(currentLogs)
    }
}
