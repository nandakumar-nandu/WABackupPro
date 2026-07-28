package com.wabackuppro.data.local.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.wabackuppro.data.local.entities.BackupFileEntry

/**
 * BackupFileEntryDao provides data access methods for managing BackupFileEntry records
 * used in delta detection and incremental backup tracking.
 */
@Dao
interface BackupFileEntryDao {

    /**
     * Inserts or updates a file entry in the database. If an entry with the same filePath exists,
     * it is replaced with the newly uploaded file's metadata and hash.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: BackupFileEntry)

    /**
     * Inserts or updates multiple file entries in a single transaction.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entries: List<BackupFileEntry>)

    /**
     * Retrieves the file entry for a given local file path, or null if it has never been backed up.
     */
    @Query("SELECT * FROM backup_file_entries WHERE filePath = :filePath LIMIT 1")
    suspend fun getByPath(filePath: String): BackupFileEntry?

    /**
     * Retrieves all recorded file entries stored in the database for manifest generation
     * and complete incremental state queries.
     */
    @Query("SELECT * FROM backup_file_entries ORDER BY lastBackedUpAt DESC")
    suspend fun getAllForLatestManifest(): List<BackupFileEntry>
}
