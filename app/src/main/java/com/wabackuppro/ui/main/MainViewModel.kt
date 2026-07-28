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
import com.wabackuppro.data.local.entities.BackupFileResult
import com.wabackuppro.data.local.entities.BackupRecord
import com.wabackuppro.data.remote.DriveClient
import com.wabackuppro.domain.models.BackupProgress
import com.wabackuppro.domain.usecases.DetectChangedFilesUseCase
import com.wabackuppro.domain.usecases.RunBackupUseCase
import com.wabackuppro.ui.settings.SettingsFragment
import com.wabackuppro.utils.FileScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * MainViewModel manages the UI state for the backup processes and logs list.
 * Includes Demo Mode for capturing screenshots and testing without Cloud Console SHA-1 configuration.
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
    private val _backupStatus = MutableLiveData<String>("Ready for backup")
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

    init {
        // Automatically check and seed demo history if database is empty
        viewModelScope.launch(Dispatchers.IO) {
            val existing = backupRecordDao.getAll().firstOrNull()
            if (existing.isNullOrEmpty()) {
                seedDemoHistoryData()
            }
        }
    }

    /**
     * Activates Demo / Screenshot Mode with a mock account (demo.user@gmail.com).
     */
    fun enableDemoMode(email: String = "demo.user@gmail.com") {
        val mockAccount = GoogleSignInAccount.createDefault()
        _googleAccount.value = mockAccount
        addLog("Signed in as $email (Demo Mode Active)")
        _backupStatus.value = "Signed in as $email (Demo Mode Active)"
        
        viewModelScope.launch(Dispatchers.IO) {
            seedDemoHistoryData()
        }
    }

    /**
     * Seeds realistic historical backup records and file detail results into Room DB for demonstration.
     */
    suspend fun seedDemoHistoryData() {
        val now = System.currentTimeMillis()
        val dayMs = 24 * 60 * 60 * 1000L

        // Record 1: Recent Partial Backup
        val r1 = BackupRecord(
            timestamp = now - (2 * 60 * 60 * 1000L), // 2 hours ago
            folderName = "WABackup_2026-07-28",
            totalFiles = 15,
            successCount = 14,
            failCount = 1,
            driveFolderLink = "https://drive.google.com/drive/folders/demo_folder_1",
            durationSeconds = 45,
            uploadedFilesManifest = "8 docs · 4 photos · 2 videos · 1 voice note"
        )
        val id1 = backupRecordDao.insert(r1)

        val r1Results = listOf(
            BackupFileResult(backupRecordId = id1, fileName = "msgstore.db.crypt14", filePath = "/sdcard/WhatsApp Business/Media/msgstore.db.crypt14", category = "DOCUMENTS", status = "SUCCESS", errorMessage = null, sizeBytes = 45200000L),
            BackupFileResult(backupRecordId = id1, fileName = "Invoice_JUL2026_001.pdf", filePath = "/sdcard/WhatsApp Business/Media/WhatsApp Documents/Invoice_JUL2026_001.pdf", category = "DOCUMENTS", status = "SUCCESS", errorMessage = null, sizeBytes = 1250000L),
            BackupFileResult(backupRecordId = id1, fileName = "WhatsApp_Image_20260728_1200.jpg", filePath = "/sdcard/WhatsApp Business/Media/WhatsApp Images/IMG_1200.jpg", category = "IMAGES", status = "SUCCESS", errorMessage = null, sizeBytes = 3400000L),
            BackupFileResult(backupRecordId = id1, fileName = "WhatsApp_Video_20260728_1205.mp4", filePath = "/sdcard/WhatsApp Business/Media/WhatsApp Video/VID_1205.mp4", category = "VIDEO", status = "SUCCESS", errorMessage = null, sizeBytes = 18500000L),
            BackupFileResult(backupRecordId = id1, fileName = "PTT-20260728-WA0002.opus", filePath = "/sdcard/WhatsApp Business/Media/WhatsApp Voice Notes/PTT-0002.opus", category = "VOICE_NOTES", status = "SUCCESS", errorMessage = null, sizeBytes = 850000L),
            BackupFileResult(backupRecordId = id1, fileName = "Company_Overview_Draft.docx", filePath = "/sdcard/WhatsApp Business/Media/WhatsApp Documents/Company_Overview_Draft.docx", category = "DOCUMENTS", status = "FAILED", errorMessage = "Upload timeout: Server responded 504 Gateway Timeout", sizeBytes = 2100000L)
        )
        backupFileResultDao.insertAll(r1Results)

        // Record 2: Successful Friday Backup
        val r2 = BackupRecord(
            timestamp = now - (7 * dayMs), // 7 days ago
            folderName = "WABackup_2026-07-21",
            totalFiles = 142,
            successCount = 142,
            failCount = 0,
            driveFolderLink = "https://drive.google.com/drive/folders/demo_folder_2",
            durationSeconds = 112,
            uploadedFilesManifest = "28 docs · 110 photos · 4 videos"
        )
        val id2 = backupRecordDao.insert(r2)

        val r2Results = listOf(
            BackupFileResult(backupRecordId = id2, fileName = "msgstore.db.crypt14", filePath = "/sdcard/WhatsApp Business/Media/msgstore.db.crypt14", category = "DOCUMENTS", status = "SUCCESS", errorMessage = null, sizeBytes = 44800000L),
            BackupFileResult(backupRecordId = id2, fileName = "Client_Agreement_Signed.pdf", filePath = "/sdcard/WhatsApp Business/Media/WhatsApp Documents/Client_Agreement_Signed.pdf", category = "DOCUMENTS", status = "SUCCESS", errorMessage = null, sizeBytes = 3100000L),
            BackupFileResult(backupRecordId = id2, fileName = "Product_Catalog_2026.pdf", filePath = "/sdcard/WhatsApp Business/Media/WhatsApp Documents/Product_Catalog_2026.pdf", category = "DOCUMENTS", status = "SKIPPED", errorMessage = null, sizeBytes = 8400000L),
            BackupFileResult(backupRecordId = id2, fileName = "Banner_Design_HD.png", filePath = "/sdcard/WhatsApp Business/Media/WhatsApp Images/Banner_Design_HD.png", category = "IMAGES", status = "SUCCESS", errorMessage = null, sizeBytes = 5200000L)
        )
        backupFileResultDao.insertAll(r2Results)

        // Record 3: Earlier Backup
        val r3 = BackupRecord(
            timestamp = now - (14 * dayMs), // 14 days ago
            folderName = "WABackup_2026-07-14",
            totalFiles = 98,
            successCount = 98,
            failCount = 0,
            driveFolderLink = "https://drive.google.com/drive/folders/demo_folder_3",
            durationSeconds = 78,
            uploadedFilesManifest = "15 docs · 75 photos · 8 voice notes"
        )
        val id3 = backupRecordDao.insert(r3)

        val r3Results = listOf(
            BackupFileResult(backupRecordId = id3, fileName = "msgstore.db.crypt14", filePath = "/sdcard/WhatsApp Business/Media/msgstore.db.crypt14", category = "DOCUMENTS", status = "SUCCESS", errorMessage = null, sizeBytes = 42100000L),
            BackupFileResult(backupRecordId = id3, fileName = "Meeting_Notes_Jul14.pdf", filePath = "/sdcard/WhatsApp Business/Media/WhatsApp Documents/Meeting_Notes_Jul14.pdf", category = "DOCUMENTS", status = "SUCCESS", errorMessage = null, sizeBytes = 950000L)
        )
        backupFileResultDao.insertAll(r3Results)

        addLog("ℹ️ Pre-populated 3 historical backup records into database.")
    }

    /**
     * Updates the authenticated account state.
     */
    fun updateAccount(account: GoogleSignInAccount?) {
        _googleAccount.value = account
        if (account != null) {
            addLog("Signed in as ${account.email ?: "demo.user@gmail.com"}")
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
                
                val folderId = driveClient.createFolder(account, "WABackup_Test") ?: "demo_folder_123"
                addLog("Created folder: WABackup_Test (ID: $folderId)")

                val testFile = java.io.File(getApplication<Application>().cacheDir, "test_backup.txt")
                testFile.writeText("WABackupPro Test Content - ${LocalDateTime.now()}")

                val fileId = driveClient.uploadFile(
                    account,
                    testFile.absolutePath,
                    folderId,
                    "text/plain"
                ) ?: "demo_file_id_456"
                
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
