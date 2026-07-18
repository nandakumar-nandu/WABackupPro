package com.wabackuppro.data.local.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.wabackuppro.data.local.entities.BackupRecord
import kotlinx.coroutines.flow.Flow

/**
 * BackupRecordDao provides the data access operations for BackupRecord entities.
 */
@Dao
interface BackupRecordDao {

    /**
     * Inserts a new backup record into the database. If a conflict occurs, it replaces the existing record.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: BackupRecord): Long

    /**
     * Retrieves all backup records from the database, ordered by timestamp descending (newest first).
     * Returns a Flow for reactive UI updates.
     */
    @Query("SELECT * FROM backup_records ORDER BY timestamp DESC")
    fun getAll(): Flow<List<BackupRecord>>

    /**
     * Retrieves a specific backup record by its unique ID.
     */
    @Query("SELECT * FROM backup_records WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): BackupRecord?

    /**
     * Deletes backup records that are older than the specified timestamp threshold.
     * This is useful for pruning the history based on user settings (e.g., keep for 30 days).
     */
    @Query("DELETE FROM backup_records WHERE timestamp < :thresholdTimestamp")
    suspend fun deleteOlderThan(thresholdTimestamp: Long): Int
}
