package com.wabackuppro.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * BackupFileResult represents the execution outcome for an individual file within a backup job run.
 * Stored in Room DB to allow users to inspect drill-down file-by-file results in the Backup Detail screen.
 */
@Entity(tableName = "backup_file_results")
data class BackupFileResult(
    // 🔑 Unique auto-generated primary key ID for Room persistence
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // 🔗 Foreign key link targeting the parent BackupRecord entry ID
    val backupRecordId: Long,

    // 📄 The display filename (e.g., "DOC-20260718-WA0001.pdf")
    val fileName: String,

    // 📁 Absolute filesystem path to the file on device storage
    val filePath: String,

    // 🗂️ The classified category string (DOCUMENTS, IMAGES, VIDEO, AUDIO, VOICE_NOTES)
    val category: String,

    // 🚥 Outcome status code: "SUCCESS", "FAILED", or "SKIPPED"
    val status: String,

    // ⚠️ Detailed error exception message if status is "FAILED", or null if successful
    val errorMessage: String? = null,

    // ⚖️ File payload size in bytes
    val sizeBytes: Long
)
