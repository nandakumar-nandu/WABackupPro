package com.wabackuppro.data.repository

import com.wabackuppro.data.local.BackupRecordDao
import com.wabackuppro.data.remote.GoogleDriveClient
import com.wabackuppro.domain.models.BackupRecord
import kotlinx.coroutines.flow.Flow

/**
 * BackupRepository coordinates database and remote client data access operations.
 */
class BackupRepository(
    private val recordDao: BackupRecordDao,
    private val driveClient: GoogleDriveClient
) {
    val allRecords: Flow<List<BackupRecord>> = recordDao.getAllRecords()

    suspend fun saveRecord(record: BackupRecord) {
        recordDao.insertRecord(record)
    }

    fun uploadToDrive(fileName: String, payload: ByteArray): Boolean {
        return driveClient.uploadBackup(fileName, payload)
    }
}
