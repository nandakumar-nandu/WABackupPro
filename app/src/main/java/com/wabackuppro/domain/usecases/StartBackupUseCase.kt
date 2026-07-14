package com.wabackuppro.domain.usecases

import com.wabackuppro.data.repository.BackupRepository
import com.wabackuppro.domain.models.BackupRecord

/**
 * StartBackupUseCase handles the logic of packing data and triggering a backup
 * upload to Google Drive, then saving execution records.
 */
class StartBackupUseCase(private val repository: BackupRepository) {

    suspend fun execute(fileName: String, data: ByteArray): Result<BackupRecord> {
        return try {
            val success = repository.uploadToDrive(fileName, data)
            if (success) {
                val record = BackupRecord(
                    timestamp = System.currentTimeMillis(),
                    status = "SUCCESS",
                    details = "Backup '$fileName' uploaded successfully."
                )
                repository.saveRecord(record)
                Result.success(record)
            } else {
                val record = BackupRecord(
                    timestamp = System.currentTimeMillis(),
                    status = "FAILED",
                    details = "Drive upload rejected for file '$fileName'."
                )
                repository.saveRecord(record)
                Result.failure(Exception("Drive upload failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
