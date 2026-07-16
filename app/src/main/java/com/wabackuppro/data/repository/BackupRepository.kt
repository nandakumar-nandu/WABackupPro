package com.wabackuppro.data.repository

import com.wabackuppro.data.local.BackupRecordDao
import com.wabackuppro.data.remote.DriveClient
import com.wabackuppro.domain.models.BackupRecord
import kotlinx.coroutines.flow.Flow

/**
 * BackupRepository coordinates database and remote client data access operations.
 */
class BackupRepository(
    private val recordDao: BackupRecordDao,
    private val driveClient: DriveClient
) {
    val allRecords: Flow<List<BackupRecord>> = recordDao.getAllRecords()

    suspend fun saveRecord(record: BackupRecord) {
        recordDao.insertRecord(record)
    }
}
