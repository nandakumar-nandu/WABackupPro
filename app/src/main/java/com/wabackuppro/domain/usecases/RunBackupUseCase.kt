package com.wabackuppro.domain.usecases

import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.wabackuppro.data.local.daos.BackupFileEntryDao
import com.wabackuppro.data.local.entities.BackupFileEntry
import com.wabackuppro.data.remote.DriveClient
import com.wabackuppro.domain.models.BackupCategory
import com.wabackuppro.domain.models.BackupFile
import com.wabackuppro.domain.models.BackupProgress
import com.wabackuppro.utils.FileScanner
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// Custom Edge Case Exceptions
class NoFilesFoundException(message: String) : Exception(message)
class DriveStorageFullException(message: String) : Exception(message)
class AuthExpiredException(message: String) : Exception(message)
class NoCategoriesSelectedException(message: String) : Exception(message)

/**
 * RunBackupUseCase orchestrates the incremental backup workflow with delta detection and category filtering.
 * 
 * Why Incremental Delta Detection & Category Filtering is Critical:
 * - Skipping unchanged files and unselected categories dramatically reduces Google Drive API request quota consumption.
 * - Saves cellular/Wi-Fi data transfer volume and shortens execution duration.
 * - Significantly lowers battery power consumption during automated background scheduled runs.
 */
class RunBackupUseCase(
    private val fileScanner: FileScanner,
    private val driveClient: DriveClient,
    private val backupFileEntryDao: BackupFileEntryDao? = null,
    private val detectChangedFilesUseCase: DetectChangedFilesUseCase? = null
) {

    /**
     * Executes the backup process and emits progress updates.
     * 
     * @param account The authenticated Google account.
     * @param categories Set of [BackupCategory] selected for backup by user preferences.
     * @param forceFullBackup Manual override flag. If true, bypasses delta detection and uploads all files in selected categories.
     * @return A Flow of [BackupProgress] updates.
     */
    fun execute(
        account: GoogleSignInAccount,
        categories: Set<BackupCategory> = BackupCategory.values().toSet(),
        forceFullBackup: Boolean = false
    ): Flow<BackupProgress> = flow {
        val errors = mutableListOf<String>()
        emit(BackupProgress(status = "Initializing backup..."))

        // 🚫 Empty Selection Short-Circuit:
        // Short-circuiting when no categories are selected avoids creating empty folders in Google Drive,
        // eliminates unneeded MediaStore queries, and saves cellular/Wi-Fi data transfer volume.
        if (categories.isEmpty()) {
            emit(BackupProgress(
                status = "Nothing selected. Please enable at least one backup category in Settings.",
                totalFiles = 0,
                errors = listOf("NoCategoriesSelectedException")
            ))
            return@flow
        }

        // 🔍 Step 1: Discover WhatsApp Business files matching selected categories
        emit(BackupProgress(status = "Scanning files for selected categories..."))
        val scannedFiles = fileScanner.scanWhatsAppBusinessFiles(categories)
        val totalFiles = scannedFiles.size

        if (totalFiles == 0) {
            emit(BackupProgress(
                status = "No WhatsApp Business files found matching selected categories.",
                totalFiles = 0,
                errors = listOf("NoFilesFoundException")
            ))
            return@flow
        }

        // 📊 Step 2: Delta Detection (Categorize into new, modified, and unchanged)
        val filesToUpload: List<BackupFile>
        val skippedCount: Int

        if (!forceFullBackup && detectChangedFilesUseCase != null && backupFileEntryDao != null) {
            emit(BackupProgress(status = "Analyzing changed files (Delta Detection)...", totalFiles = totalFiles))
            val deltaResult = detectChangedFilesUseCase.execute(scannedFiles)
            filesToUpload = deltaResult.newFiles + deltaResult.modifiedFiles
            skippedCount = deltaResult.unchangedFiles.size
        } else {
            // Force Full Backup override active -> upload all files
            filesToUpload = scannedFiles
            skippedCount = 0
        }

        // 📂 Step 3: Create Drive folder with name "WABackup_YYYY-MM-DD"
        val folderName = "WABackup_${LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)}"
        emit(BackupProgress(
            status = "Creating folder: $folderName...",
            totalFiles = totalFiles,
            skippedFiles = skippedCount
        ))

        val folderId = try {
            driveClient.createFolder(account, folderName)
        } catch (e: Exception) {
            if (e.message?.contains("401") == true || e.message?.contains("Unauthorized") == true) {
                emit(BackupProgress(
                    status = "Authentication expired. Please sign in again.",
                    totalFiles = totalFiles,
                    skippedFiles = skippedCount,
                    errors = listOf("AuthExpiredException")
                ))
            } else {
                emit(BackupProgress(
                    status = "Failed to create folder: ${e.message}",
                    totalFiles = totalFiles,
                    skippedFiles = skippedCount,
                    errors = listOf(e.message ?: "Unknown Error")
                ))
            }
            return@flow
        }

        if (folderId == null) {
            emit(BackupProgress(
                status = "Error: Folder creation returned null ID",
                totalFiles = totalFiles,
                skippedFiles = skippedCount,
                errors = listOf("Folder ID null")
            ))
            return@flow
        }

        // 📤 Step 4: Upload new and modified files with retry logic
        var uploadedCount = 0
        val totalToUpload = filesToUpload.size

        if (totalToUpload == 0) {
            emit(BackupProgress(
                totalFiles = totalFiles,
                uploadedFiles = 0,
                skippedFiles = skippedCount,
                status = "All files are up to date! (Skipped $skippedCount unchanged files)",
                errors = errors
            ))
            return@flow
        }

        for (file in filesToUpload) {
            uploadedCount++
            emit(BackupProgress(
                totalFiles = totalFiles,
                uploadedFiles = uploadedCount - 1,
                skippedFiles = skippedCount,
                currentFileName = file.name,
                status = "Uploading ${file.name} ($uploadedCount of $totalToUpload)...",
                errors = errors
            ))

            try {
                val driveFileId = uploadWithRetry(account, file.path, folderId, file.type, maxAttempts = 3)

                if (driveFileId != null) {
                    // Update/upsert local BackupFileEntry record in Room database for future delta scans
                    if (backupFileEntryDao != null && detectChangedFilesUseCase != null) {
                        val localFile = File(file.path)
                        val hash = detectChangedFilesUseCase.calculateSHA256(localFile)
                        backupFileEntryDao.upsert(
                            BackupFileEntry(
                                filePath = file.path,
                                contentHash = hash,
                                lastModified = localFile.lastModified(),
                                driveFileId = driveFileId,
                                lastBackedUpAt = System.currentTimeMillis()
                            )
                        )
                    }

                    emit(BackupProgress(
                        totalFiles = totalFiles,
                        uploadedFiles = uploadedCount,
                        skippedFiles = skippedCount,
                        currentFileName = file.name,
                        status = "Uploaded ${file.name}",
                        errors = errors.toList()
                    ))
                } else {
                    errors.add("Failed to upload: ${file.name}")
                }
            } catch (e: DriveStorageFullException) {
                errors.add("Storage Full: ${e.message}")
                emit(BackupProgress(
                    totalFiles = totalFiles,
                    uploadedFiles = uploadedCount - 1,
                    skippedFiles = skippedCount,
                    currentFileName = file.name,
                    status = "Backup halted: Google Drive storage is full.",
                    errors = errors.toList()
                ))
                return@flow
            } catch (e: IOException) {
                throw e
            }
        }

        val finalStatus = when {
            errors.isEmpty() && skippedCount > 0 -> "Backup complete! ($uploadedCount uploaded, $skippedCount skipped)"
            errors.isEmpty() -> "Backup complete successfully!"
            else -> "Backup complete with errors."
        }

        emit(BackupProgress(
            totalFiles = totalFiles,
            uploadedFiles = uploadedCount - errors.size,
            skippedFiles = skippedCount,
            status = finalStatus,
            errors = errors
        ))
    }

    /**
     * Uploads a single file to Drive with a simple retry algorithm.
     * Returns the uploaded Google Drive fileId, or null on failure.
     */
    private suspend fun uploadWithRetry(
        account: GoogleSignInAccount,
        path: String,
        folderId: String,
        mimeType: String,
        maxAttempts: Int
    ): String? {
        var attempts = 0
        while (attempts < maxAttempts) {
            attempts++
            try {
                val fileId = driveClient.uploadFile(account, path, folderId, mimeType)
                if (fileId != null) return fileId
            } catch (e: Exception) {
                val msg = e.message ?: ""
                if (msg.contains("quotaExceeded") || msg.contains("403")) {
                    throw DriveStorageFullException("Google Drive quota exceeded.")
                }
                if (e is IOException) {
                    if (attempts >= maxAttempts) throw e
                }

                if (attempts >= maxAttempts) break
                delay(attempts * 1000L)
            }
        }
        return null
    }
}
