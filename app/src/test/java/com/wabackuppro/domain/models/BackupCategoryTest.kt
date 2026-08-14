package com.wabackuppro.domain.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying [BackupCategory] enum values and display text mappings.
 */
class BackupCategoryTest {

    @Test
    fun backupCategory_containsAllRequiredTypes() {
        val categories = BackupCategory.values().toSet()

        assertTrue(categories.contains(BackupCategory.DOCUMENTS))
        assertTrue(categories.contains(BackupCategory.IMAGES))
        assertTrue(categories.contains(BackupCategory.VIDEO))
        assertTrue(categories.contains(BackupCategory.AUDIO))
        assertTrue(categories.contains(BackupCategory.VOICE_NOTES))
        assertEquals(5, categories.size)
    }

    @Test
    fun backupCategory_displayNameReturnsNonEmptyStrings() {
        BackupCategory.values().forEach { category ->
            assertTrue(category.displayName.isNotEmpty())
        }
    }
}
