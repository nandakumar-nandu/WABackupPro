package com.wabackuppro.data.repository

import com.wabackuppro.data.local.BackupRecordDao
import com.wabackuppro.data.remote.DriveClient
import com.wabackuppro.domain.models.BackupRecord
import kotlinx.coroutines.flow.Flow

/**
 * Legacy Repository pattern abstraction wrapper.
 *
 * @deprecated Legacy architecture wrapper from v0.1.0. Active production flows ([com.wabackuppro.domain.usecases.RunBackupUseCase]
 * and [com.wabackuppro.ui.main.MainViewModel]) interface directly with [com.wabackuppro.data.local.AppDatabase] DAOs. Retained
 * as an inactive architectural migration reference.
 */
@Deprecated(
    message = "Legacy repository abstraction. Active logic uses AppDatabase DAOs and DriveClient directly."
)
class BackupRepository(
    private val recordDao: BackupRecordDao,
    private val driveClient: DriveClient
) {
    val allRecords: Flow<List<BackupRecord>> = recordDao.getAllRecords()

    suspend fun saveRecord(record: BackupRecord) {
        recordDao.insertRecord(record)
    }
}
