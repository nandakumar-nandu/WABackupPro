package com.wabackuppro.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * BackupFileEntry stores delta detection metadata for individual files that have been backed up.
 * This entity enables WABackupPro to identify whether a file is new, modified, or unchanged
 * across backup cycles, skipping unchanged files to conserve Drive API quota and battery life.
 */
@Entity(tableName = "backup_file_entries")
data class BackupFileEntry(
    // 📁 The absolute file path on the device system (serves as unique primary key for entry lookup)
    @PrimaryKey
    val filePath: String,

    // 🔒 SHA-256 cryptographic content hash of the file payload.
    // Content hash is required because modification timestamp alone is unreliable due to system clock skews,
    // timezone adjustments, or file copy actions that reset file timestamps without content changes.
    val contentHash: String,

    // ⏱️ Last modified timestamp reported by the filesystem in milliseconds.
    // Serves as a fast pre-check before computing SHA-256 hashes to speed up delta scans.
    val lastModified: Long,

    // ☁️ The unique Google Drive file ID returned upon successful upload.
    // Needed to reference or update the remote file on Drive if modified in future backups.
    val driveFileId: String,

    // 📅 The epoch timestamp in milliseconds when this file was last backed up to Google Drive.
    // Useful for audit records and manifest generation.
    val lastBackedUpAt: Long
)
