package com.wabackuppro.domain.usecases

import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.wabackuppro.data.remote.DriveClient
import com.wabackuppro.domain.models.BackupProgress
import com.wabackuppro.utils.FileScanner
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * RunBackupUseCase orchestrates the full backup workflow.
 * It scans files, creates a remote folder, and uploads files with retry logic.
 */
class RunBackupUseCase(
    private val fileScanner: FileScanner,
    private val driveClient: DriveClient
) {

    /**
     * Executes the backup process and emits progress updates.
     * 
     * @param account The authenticated Google account.
     * @return A Flow of [BackupProgress] updates.
     */
    fun execute(account: GoogleSignInAccount): Flow<BackupProgress> = flow {
        val errors = mutableListOf<String>()
        emit(BackupProgress(status = "Initializing backup..."))

        // 🔍 Step 1: Scan all WhatsApp Business files
        emit(BackupProgress(status = "Scanning files..."))
        val files = fileScanner.scanWhatsAppBusinessFiles()
        val totalFiles = files.size
        
        if (totalFiles == 0) {
            emit(BackupProgress(status = "No files found to backup.", totalFiles = 0))
            return@flow
        }

        // 📂 Step 2: Create Drive folder with name "WABackup_YYYY-MM-DD"
        val folderName = "WABackup_${LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)}"
        emit(BackupProgress(status = "Creating folder: $folderName...", totalFiles = totalFiles))
        
        val folderId = try {
            driveClient.createFolder(account, folderName)
        } catch (e: Exception) {
            emit(BackupProgress(status = "Failed to create folder: ${e.message}", totalFiles = totalFiles))
            return@flow
        }

        if (folderId == null) {
            emit(BackupProgress(status = "Error: Folder creation returned null ID", totalFiles = totalFiles))
            return@flow
        }

        // 📤 Step 3: Upload each file with retry logic
        var uploadedCount = 0
        for (file in files) {
            uploadedCount++
            emit(BackupProgress(
                totalFiles = totalFiles,
                uploadedFiles = uploadedCount - 1,
                currentFileName = file.name,
                status = "Uploading ${file.name} ($uploadedCount of $totalFiles)...",
                errors = errors
            ))

            val success = uploadWithRetry(account, file.path, folderId, file.type, maxAttempts = 3)
            
            if (!success) {
                errors.add("Failed to upload: ${file.name}")
            }

            emit(BackupProgress(
                totalFiles = totalFiles,
                uploadedFiles = if (success) uploadedCount else uploadedCount - 1,
                currentFileName = file.name,
                status = if (success) "Uploaded ${file.name}" else "Failed to upload ${file.name}",
                errors = errors.toList()
            ))
        }

        emit(BackupProgress(
            totalFiles = totalFiles,
            uploadedFiles = uploadedCount - errors.size,
            status = if (errors.isEmpty()) "Backup complete successfully!" else "Backup complete with errors.",
            errors = errors
        ))
    }

    /**
     * Uploads a single file to Drive with a simple retry algorithm.
     * Uses linear backoff (1s, 2s, 3s).
     */
    private suspend fun uploadWithRetry(
        account: GoogleSignInAccount,
        path: String,
        folderId: String,
        mimeType: String,
        maxAttempts: Int
    ): Boolean {
        var attempts = 0
        while (attempts < maxAttempts) {
            attempts++
            try {
                val fileId = driveClient.uploadFile(account, path, folderId, mimeType)
                if (fileId != null) return true
            } catch (e: Exception) {
                if (attempts >= maxAttempts) break
                // ⏱️ Exponential or linear backoff
                delay(attempts * 1000L) 
            }
        }
        return false
    }
}
