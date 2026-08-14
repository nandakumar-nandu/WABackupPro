package com.wabackuppro.domain.usecases

import com.wabackuppro.data.local.daos.BackupFileEntryDao
import com.wabackuppro.domain.models.BackupFile
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

/**
 * Encapsulates the three file lists produced by delta detection analysis.
 *
 * @property newFiles Discovered local files that have never been uploaded to Google Drive.
 * @property modifiedFiles Local files previously backed up whose SHA-256 content hash has changed.
 * @property unchangedFiles Local files previously backed up whose SHA-256 content hash remains identical.
 */
data class DeltaScanResult(
    val newFiles: List<BackupFile>,
    val modifiedFiles: List<BackupFile>,
    val unchangedFiles: List<BackupFile>
)

/**
 * Evaluates discovered WhatsApp Business files against historical Room database records
 * to isolate changed content from unchanged files.
 *
 * ## Business Rule & Cryptographic Strategy
 * Filesystem modification timestamps (`lastModified`) and file sizes are intentionally NOT used as primary
 * indicators of file changes because device clock drift, timezone shifts (e.g. DST), or manual date changes
 * can alter timestamps without changing content. Calculating a cryptographic SHA-256 hash guarantees
 * absolute content integrity verification.
 *
 * @param backupFileEntryDao Data Access Object for local [com.wabackuppro.data.local.entities.BackupFileEntry] records.
 */
class DetectChangedFilesUseCase(
    private val backupFileEntryDao: BackupFileEntryDao
) {

    /**
     * Categorizes a list of scanned WhatsApp Business files into new, modified, and unchanged buckets.
     *
     * @param scannedFiles The list of discovered local [BackupFile] items.
     * @return [DeltaScanResult] containing the three classified file lists.
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
     * Computes the hex-encoded SHA-256 hash of a file using chunked streaming (8192-byte buffer).
     *
     * Reads input streams in fixed 8KB chunks to prevent memory overhead when hashing large files (e.g., videos or database archives).
     * Returns an empty string if an [Exception] occurs, which safely triggers a hash mismatch and forces a re-upload fallback.
     *
     * @param file The physical file payload to hash.
     * @return The 64-character lowercase hexadecimal SHA-256 string, or empty string on failure.
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
