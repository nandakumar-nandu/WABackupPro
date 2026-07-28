package com.wabackuppro.domain.usecases

import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.wabackuppro.data.local.daos.BackupFileEntryDao
import com.wabackuppro.data.local.daos.BackupFileResultDao
import com.wabackuppro.data.local.daos.BackupRecordDao
import com.wabackuppro.data.local.entities.BackupFileEntry
import com.wabackuppro.data.local.entities.BackupFileResult
import com.wabackuppro.data.local.entities.BackupRecord
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
 * RunBackupUseCase orchestrates the incremental backup workflow with delta detection, category filtering,
 * and per-file result persistence in Room DB.
 */
class RunBackupUseCase(
    private val fileScanner: FileScanner,
    private val driveClient: DriveClient,
    private val backupFileEntryDao: BackupFileEntryDao? = null,
    private val detectChangedFilesUseCase: DetectChangedFilesUseCase? = null,
    private val backupRecordDao: BackupRecordDao? = null,
    private val backupFileResultDao: BackupFileResultDao? = null
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
        val startTime = System.currentTimeMillis()
        val errors = mutableListOf<String>()
        val fileResults = mutableListOf<BackupFileResult>()

        emit(BackupProgress(status = "Initializing backup..."))

        // 🚫 Empty Selection Short-Circuit
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
        val unchangedFiles: List<BackupFile>
        val skippedCount: Int

        if (!forceFullBackup && detectChangedFilesUseCase != null && backupFileEntryDao != null) {
            emit(BackupProgress(status = "Analyzing changed files (Delta Detection)...", totalFiles = totalFiles))
            val deltaResult = detectChangedFilesUseCase.execute(scannedFiles)
            filesToUpload = deltaResult.newFiles + deltaResult.modifiedFiles
            unchangedFiles = deltaResult.unchangedFiles
            skippedCount = unchangedFiles.size
        } else {
            filesToUpload = scannedFiles
            unchangedFiles = emptyList()
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

        // Create initial parent BackupRecord in Room DB to obtain recordId for per-file FK linkage
        val initialRecord = BackupRecord(
            timestamp = startTime,
            folderName = folderName,
            totalFiles = totalFiles,
            successCount = 0,
            failCount = 0,
            driveFolderLink = "https://drive.google.com/drive/folders/$folderId",
            durationSeconds = 0,
            uploadedFilesManifest = buildCategorySummary(scannedFiles)
        )
        
        val recordId = backupRecordDao?.insert(initialRecord) ?: 0L

        // Record SKIPPED results for unchanged files
        for (file in unchangedFiles) {
            fileResults.add(
                BackupFileResult(
                    backupRecordId = recordId,
                    fileName = file.name,
                    filePath = file.path,
                    category = file.category.name,
                    status = "SKIPPED",
                    errorMessage = null,
                    sizeBytes = file.size
                )
            )
        }

        // 📤 Step 4: Upload new and modified files
        var uploadedCount = 0
        val totalToUpload = filesToUpload.size

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

                    fileResults.add(
                        BackupFileResult(
                            backupRecordId = recordId,
                            fileName = file.name,
                            filePath = file.path,
                            category = file.category.name,
                            status = "SUCCESS",
                            errorMessage = null,
                            sizeBytes = file.size
                        )
                    )

                    emit(BackupProgress(
                        totalFiles = totalFiles,
                        uploadedFiles = uploadedCount,
                        skippedFiles = skippedCount,
                        currentFileName = file.name,
                        status = "Uploaded ${file.name}",
                        errors = errors.toList()
                    ))
                } else {
                    val errMsg = "Failed to upload: ${file.name}"
                    errors.add(errMsg)

                    fileResults.add(
                        BackupFileResult(
                            backupRecordId = recordId,
                            fileName = file.name,
                            filePath = file.path,
                            category = file.category.name,
                            status = "FAILED",
                            errorMessage = errMsg,
                            sizeBytes = file.size
                        )
                    )
                }
            } catch (e: DriveStorageFullException) {
                errors.add("Storage Full: ${e.message}")

                fileResults.add(
                    BackupFileResult(
                        backupRecordId = recordId,
                        fileName = file.name,
                        filePath = file.path,
                        category = file.category.name,
                        status = "FAILED",
                        errorMessage = "Drive Storage Full",
                        sizeBytes = file.size
                    )
                )

                // Persist collected file results before terminating
                if (backupFileResultDao != null && recordId > 0) {
                    backupFileResultDao.insertAll(fileResults)
                }

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

        // Persist all per-file results into Room DB
        if (backupFileResultDao != null && recordId > 0) {
            backupFileResultDao.insertAll(fileResults)
        }

        // Finalize parent BackupRecord in Room DB
        val endTime = System.currentTimeMillis()
        val durationSeconds = ((endTime - startTime) / 1000L).coerceAtLeast(1L)
        val successCount = fileResults.count { it.status == "SUCCESS" || it.status == "SKIPPED" }
        val failCount = fileResults.count { it.status == "FAILED" }

        if (backupRecordDao != null && recordId > 0) {
            val updatedRecord = initialRecord.copy(
                id = recordId,
                successCount = successCount,
                failCount = failCount,
                durationSeconds = durationSeconds,
                uploadedFilesManifest = buildCategorySummary(scannedFiles)
            )
            backupRecordDao.insert(updatedRecord)
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
     * Builds a human-readable per-category count summary (e.g. "12 docs · 340 photos · 8 videos").
     */
    private fun buildCategorySummary(files: List<BackupFile>): String {
        val docs = files.count { it.category == BackupCategory.DOCUMENTS }
        val photos = files.count { it.category == BackupCategory.IMAGES }
        val videos = files.count { it.category == BackupCategory.VIDEO }
        val audio = files.count { it.category == BackupCategory.AUDIO }
        val voice = files.count { it.category == BackupCategory.VOICE_NOTES }

        val parts = mutableListOf<String>()
        if (docs > 0) parts.add("$docs docs")
        if (photos > 0) parts.add("$photos photos")
        if (videos > 0) parts.add("$videos videos")
        if (audio > 0) parts.add("$audio audio")
        if (voice > 0) parts.add("$voice voice notes")

        return if (parts.isEmpty()) "${files.size} files" else parts.joinToString(" · ")
    }

    /**
     * Uploads a single file to Drive with retry logic.
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
