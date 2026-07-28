package com.wabackuppro.domain.usecases

import com.wabackuppro.data.local.daos.BackupFileEntryDao
import com.wabackuppro.domain.models.BackupFile
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

/**
 * Result data class containing the three buckets produced by delta detection scanning.
 */
data class DeltaScanResult(
    // 🆕 Files that have never been backed up before
    val newFiles: List<BackupFile>,

    // ✏️ Files previously backed up whose content hash has changed
    val modifiedFiles: List<BackupFile>,

    // ⏭️ Files previously backed up whose content hash remains identical
    val unchangedFiles: List<BackupFile>
)

/**
 * DetectChangedFilesUseCase compares discovered WhatsApp Business files against Room BackupFileEntry database records.
 * Categorizes files into new, modified, and unchanged buckets.
 */
class DetectChangedFilesUseCase(
    private val backupFileEntryDao: BackupFileEntryDao
) {

    /**
     * Executes the delta scan algorithm.
     * 
     * Algorithm Details:
     * 1. Query Room DB for existing [BackupFileEntry] records.
     * 2. For each scanned file, calculate its SHA-256 cryptographic hash.
     * 3. Compare with DB record:
     *    - If no DB record exists -> [newFiles]
     *    - If DB record exists & contentHash matches -> [unchangedFiles]
     *    - If DB record exists & contentHash differs -> [modifiedFiles]
     * 
     * Rationale for SHA-256 Hash Comparison:
     * We do NOT rely solely on filesystem modification time (`lastModified`) or file size because:
     * - System clock skews or manual date changes can alter timestamps without changing content.
     * - Timezone changes (DST or travelling across time zones) can offset file timestamps by hours.
     * - File copies or database operations can change modification timestamps while content is identical, or vice versa.
     * - SHA-256 hashing guarantees cryptographic content integrity verification.
     */
    suspend fun execute(scannedFiles: List<BackupFile>): DeltaScanResult {
        val newFiles = mutableListOf<BackupFile>()
        val modifiedFiles = mutableListOf<BackupFile>()
        val unchangedFiles = mutableListOf<BackupFile>()

        for (file in scannedFiles) {
            val localFile = File(file.path)
            if (!localFile.exists()) continue

            val existingEntry = backupFileEntryDao.getByPath(file.path)

            if (existingEntry == null) {
                newFiles.add(file)
            } else {
                // Compute SHA-256 hash of the local file
                val currentHash = calculateSHA256(localFile)
                
                if (currentHash == existingEntry.contentHash) {
                    unchangedFiles.add(file)
                } else {
                    modifiedFiles.add(file)
                }
            }
        }

        return DeltaScanResult(newFiles, modifiedFiles, unchangedFiles)
    }

    /**
     * Calculates the SHA-256 hash of a file payload.
     */
    fun calculateSHA256(file: File): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(8192)
            FileInputStream(file).use { inputStream ->
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            val hashBytes = digest.digest()
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }
}
