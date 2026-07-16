package com.wabackuppro.domain.models

/**
 * BackupFile represents a single file identified by the scanner that is
 * eligible for backup to Google Drive.
 */
data class BackupFile(
    // 📁 The absolute filesystem path or URI string to the file
    val path: String,

    // 📄 The display name of the file (e.g., "IMG_20240101.jpg")
    val name: String,

    // ⚖️ The size of the file in bytes
    val size: Long,

    // 🏷️ The MIME type or category of the file (e.g., "image/jpeg")
    val type: String
)
