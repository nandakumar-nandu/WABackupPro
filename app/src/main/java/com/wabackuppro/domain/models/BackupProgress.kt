package com.wabackuppro.domain.models

/**
 * BackupProgress represents the current state of a backup job, used to update
 * the UI with real-time progress information.
 */
data class BackupProgress(
    // 📁 Total number of files discovered for backup
    val totalFiles: Int = 0,

    // ✅ Number of files successfully uploaded so far
    val uploadedFiles: Int = 0,

    // ⏭️ Number of unchanged files skipped during incremental backup
    val skippedFiles: Int = 0,

    // 📄 Name of the file currently being processed
    val currentFileName: String = "",

    // 🔄 Current status message (e.g., "Starting...", "Uploading...", "Complete")
    val status: String = "",

    // ❌ List of error messages encountered during the backup job
    val errors: List<String> = emptyList()
)
