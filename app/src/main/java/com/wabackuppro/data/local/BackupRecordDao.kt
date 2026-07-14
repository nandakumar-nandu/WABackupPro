package com.wabackuppro.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.wabackuppro.domain.models.BackupRecord
import kotlinx.coroutines.flow.Flow

/**
 * BackupRecordDao provides operations on the backup_records database table.
 */
@Dao
interface BackupRecordDao {

    @Query("SELECT * FROM backup_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<BackupRecord>>

    @Insert
    suspend fun insertRecord(record: BackupRecord)
}
