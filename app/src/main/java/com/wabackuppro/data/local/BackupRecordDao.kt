package com.wabackuppro.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.wabackuppro.domain.models.BackupRecord
import kotlinx.coroutines.flow.Flow

/**
 * Legacy Data Access Object for [BackupRecord] entities in [BackupDatabase].
 *
 * @deprecated Legacy DAO interface from v0.1.0 skeleton. Active production code uses
 * [com.wabackuppro.data.local.daos.BackupRecordDao] within [AppDatabase]. Retained as a migration reference.
 */
@Deprecated(
    message = "Legacy v1 DAO. Active DAO is com.wabackuppro.data.local.daos.BackupRecordDao."
)
@Dao
interface BackupRecordDao {

    @Query("SELECT * FROM backup_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<BackupRecord>>

    @Insert
    suspend fun insertRecord(record: BackupRecord)
}
