package com.wabackuppro.domain.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying [BackupProgress] data class properties and default values.
 */
class BackupProgressTest {

    @Test
    fun backupProgress_defaultValues() {
        val progress = BackupProgress()
        assertEquals(0, progress.totalFiles)
        assertEquals(0, progress.uploadedFiles)
        assertEquals(0, progress.skippedFiles)
        assertEquals("", progress.currentFileName)
        assertEquals("", progress.status)
        assertTrue(progress.errors.isEmpty())
    }

    @Test
    fun backupProgress_customPayload() {
        val progress = BackupProgress(
            totalFiles = 10,
            uploadedFiles = 7,
            skippedFiles = 2,
            currentFileName = "msgstore.db.crypt14",
            status = "Uploading...",
            errors = listOf("File 3 timeout")
        )

        assertEquals(10, progress.totalFiles)
        assertEquals(7, progress.uploadedFiles)
        assertEquals(2, progress.skippedFiles)
        assertEquals("msgstore.db.crypt14", progress.currentFileName)
        assertEquals("Uploading...", progress.status)
        assertEquals(1, progress.errors.size)
        assertEquals("File 3 timeout", progress.errors.first())
    }
}
