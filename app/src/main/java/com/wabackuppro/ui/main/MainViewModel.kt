package com.wabackuppro.ui.main

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.wabackuppro.data.local.AppDatabase
import com.wabackuppro.data.remote.DriveClient
import com.wabackuppro.domain.models.BackupProgress
import com.wabackuppro.domain.usecases.DetectChangedFilesUseCase
import com.wabackuppro.domain.usecases.RunBackupUseCase
import com.wabackuppro.ui.settings.SettingsFragment
import com.wabackuppro.utils.FileScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * MainViewModel manages the UI state for the backup processes and logs list.
 * It coordinates backup actions and updates live status data.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val backupRecordDao = db.backupRecordDao()
    private val backupFileEntryDao = db.backupFileEntryDao()
    private val backupFileResultDao = db.backupFileResultDao()
    private val fileScanner = FileScanner(application)
    private val driveClient = DriveClient(application)
    private val detectChangedFilesUseCase = DetectChangedFilesUseCase(backupFileEntryDao)
    
    private val runBackupUseCase = RunBackupUseCase(
        fileScanner,
        driveClient,
        backupFileEntryDao,
        detectChangedFilesUseCase,
        backupRecordDao,
        backupFileResultDao
    )

    // 📊 Holds the current status of the backup operation
    private val _backupStatus = MutableLiveData<String>("No backup run yet")
    val backupStatus: LiveData<String> = _backupStatus

    // 📈 Holds the real-time progress of the backup job
    private val _backupProgress = MutableLiveData<BackupProgress>()
    val backupProgress: LiveData<BackupProgress> = _backupProgress

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
     * Triggers the backup process with category preferences and force override settings.
     */
    fun startBackup() {
        val account = _googleAccount.value ?: run {
            addLog("❌ Error: Sign-in required to start backup")
            return
        }

        val prefs = getApplication<Application>().getSharedPreferences(SettingsFragment.PREFS_NAME, Context.MODE_PRIVATE)
        val forceFullBackup = prefs.getBoolean(SettingsFragment.PREF_FORCE_FULL_BACKUP, false)
        val selectedCategories = SettingsFragment.getSelectedCategories(getApplication())

        if (selectedCategories.isEmpty()) {
            addLog("⚠️ No categories selected for backup! Enable at least one category in Settings.")
        } else {
            val catNames = selectedCategories.joinToString(", ") { it.displayName }
            addLog("🗂️ Categories selected: $catNames")
        }

        if (forceFullBackup) {
            addLog("⚠️ Force Full Backup override active: skipping delta detection.")
        } else {
            addLog("⚡ Starting Incremental Backup (Delta Detection active)...")
        }

        viewModelScope.launch(Dispatchers.IO) {
            runBackupUseCase.execute(account, selectedCategories, forceFullBackup).collect { progress ->
                _backupProgress.postValue(progress)
                _backupStatus.postValue(progress.status)
                
                // 📝 Log status changes or errors
                if (progress.status.startsWith("Uploaded") || progress.status.startsWith("Failed")) {
                    val icon = if (progress.status.startsWith("Uploaded")) "✅" else "❌"
                    addLog("$icon ${progress.status}")
                } else if (progress.status.contains("Complete") || progress.status.contains("Scanning") || progress.status.contains("up to date") || progress.status.contains("Nothing selected")) {
                    addLog("ℹ️ ${progress.status}")
                }
            }
        }
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
                
                val folderId = driveClient.createFolder(account, "WABackup_Test") ?: throw Exception("Folder creation failed")
                addLog("Created folder: WABackup_Test (ID: $folderId)")

                val testFile = java.io.File(getApplication<Application>().cacheDir, "test_backup.txt")
                testFile.writeText("WABackupPro Test Content - ${LocalDateTime.now()}")

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
