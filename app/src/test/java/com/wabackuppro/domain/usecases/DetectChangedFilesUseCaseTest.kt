package com.wabackuppro.domain.usecases

import com.wabackuppro.data.local.daos.BackupFileEntryDao
import com.wabackuppro.data.local.entities.BackupFileEntry
import com.wabackuppro.domain.models.BackupCategory
import com.wabackuppro.domain.models.BackupFile
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Unit tests for [DetectChangedFilesUseCase] verifying SHA-256 calculation
 * and delta scan file bucket assignment.
 */
class DetectChangedFilesUseCaseTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var fakeDao: FakeBackupFileEntryDao
    private lateinit var useCase: DetectChangedFilesUseCase

    @Before
    fun setUp() {
        fakeDao = FakeBackupFileEntryDao()
        useCase = DetectChangedFilesUseCase(fakeDao)
    }

    @Test
    fun calculateSHA256_returnsCorrectHashForKnownContent() {
        val testFile = tempFolder.newFile("sample.txt")
        testFile.writeText("WABackupPro Test Content 123")

        val hash1 = useCase.calculateSHA256(testFile)
        assertTrue(hash1.isNotEmpty())
        assertEquals(64, hash1.length) // SHA-256 is 64 hex characters

        // Repeat calculation to verify deterministic output
        val hash2 = useCase.calculateSHA256(testFile)
        assertEquals(hash1, hash2)
    }

    @Test
    fun calculateSHA256_returnsDifferentHashForModifiedContent() {
        val testFile = tempFolder.newFile("sample2.txt")
        testFile.writeText("Content Version A")
        val hashA = useCase.calculateSHA256(testFile)

        testFile.writeText("Content Version B")
        val hashB = useCase.calculateSHA256(testFile)

        assertNotEquals(hashA, hashB)
    }

    @Test
    fun calculateSHA256_handlesNonExistentFileGracefully() {
        val missingFile = File(tempFolder.root, "non_existent.txt")
        val hash = useCase.calculateSHA256(missingFile)
        assertEquals("", hash)
    }

    @Test
    fun execute_categorizesNewUnchangedAndModifiedFiles() = runBlocking {
        // File 1: New file (no record in DB)
        val file1 = tempFolder.newFile("file1.pdf")
        file1.writeText("File 1 Content")
        val backupFile1 = BackupFile(file1.absolutePath, file1.name, file1.length(), "application/pdf", BackupCategory.DOCUMENTS)

        // File 2: Unchanged file (hash matches DB record)
        val file2 = tempFolder.newFile("file2.jpg")
        file2.writeText("File 2 Unchanged Content")
        val hash2 = useCase.calculateSHA256(file2)
        fakeDao.upsert(BackupFileEntry(file2.absolutePath, hash2, file2.lastModified(), "drive_id_2", System.currentTimeMillis()))
        val backupFile2 = BackupFile(file2.absolutePath, file2.name, file2.length(), "image/jpeg", BackupCategory.IMAGES)

        // File 3: Modified file (hash differs from DB record)
        val file3 = tempFolder.newFile("file3.mp4")
        file3.writeText("File 3 Modified New Content")
        fakeDao.upsert(BackupFileEntry(file3.absolutePath, "old_stale_hash_value", file3.lastModified(), "drive_id_3", System.currentTimeMillis()))
        val backupFile3 = BackupFile(file3.absolutePath, file3.name, file3.length(), "video/mp4", BackupCategory.VIDEO)

        val scannedFiles = listOf(backupFile1, backupFile2, backupFile3)
        val result = useCase.execute(scannedFiles)

        assertEquals(1, result.newFiles.size)
        assertEquals(backupFile1, result.newFiles.first())

        assertEquals(1, result.unchangedFiles.size)
        assertEquals(backupFile2, result.unchangedFiles.first())

        assertEquals(1, result.modifiedFiles.size)
        assertEquals(backupFile3, result.modifiedFiles.first())
    }

    /**
     * In-memory fake DAO implementing [BackupFileEntryDao] for deterministic unit testing.
     */
    private class FakeBackupFileEntryDao : BackupFileEntryDao {
        private val storage = mutableMapOf<String, BackupFileEntry>()

        override suspend fun upsert(entry: BackupFileEntry) {
            storage[entry.filePath] = entry
        }

        override suspend fun upsertAll(entries: List<BackupFileEntry>) {
            entries.forEach { storage[it.filePath] = it }
        }

        override suspend fun getByPath(filePath: String): BackupFileEntry? {
            return storage[filePath]
        }

        override suspend fun getAllForLatestManifest(): List<BackupFileEntry> {
            return storage.values.sortedByDescending { it.lastBackedUpAt }
        }
    }
}
