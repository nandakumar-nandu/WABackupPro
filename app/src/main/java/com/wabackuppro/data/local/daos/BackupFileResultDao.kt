package com.wabackuppro.data.local.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.wabackuppro.data.local.entities.BackupFileResult
import kotlinx.coroutines.flow.Flow

/**
 * BackupFileResultDao handles per-file database query operations for drill-down inspection screens.
 */
@Dao
interface BackupFileResultDao {

    /**
     * Inserts a list of per-file execution results for a backup job into Room DB.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(results: List<BackupFileResult>)

    /**
     * Inserts or replaces a single file execution result.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(result: BackupFileResult): Long

    /**
     * Retrieves all per-file results for a given parent [backupRecordId] as a reactive Flow.
     */
    @Query("SELECT * FROM backup_file_results WHERE backupRecordId = :backupRecordId ORDER BY id ASC")
    fun getByBackupRecordId(backupRecordId: Long): Flow<List<BackupFileResult>>

    /**
     * Direct suspend query returning all per-file results for a given parent [backupRecordId].
     */
    @Query("SELECT * FROM backup_file_results WHERE backupRecordId = :backupRecordId ORDER BY id ASC")
    suspend fun getByBackupRecordIdDirect(backupRecordId: Long): List<BackupFileResult>

    /**
     * Retrieves all failed file results for a specific [backupRecordId] to enable retry operations.
     */
    @Query("SELECT * FROM backup_file_results WHERE backupRecordId = :backupRecordId AND status = 'FAILED'")
    suspend fun getFailedForRecord(backupRecordId: Long): List<BackupFileResult>

    /**
     * Updates the status and optional error message of a specific file result after a single-file retry attempt.
     */
    @Query("UPDATE backup_file_results SET status = :status, errorMessage = :errorMessage WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, errorMessage: String? = null)
}
