package com.wabackuppro.domain.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * BackupRecord represents a historical entry of a backup operation execution.
 */
@Entity(tableName = "backup_records")
data class BackupRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val status: String,
    val details: String
)
