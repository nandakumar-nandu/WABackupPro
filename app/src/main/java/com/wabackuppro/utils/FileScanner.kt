package com.wabackuppro.utils

import android.content.ContentResolver
import android.content.Context
import android.provider.MediaStore
import com.wabackuppro.domain.models.BackupCategory
import com.wabackuppro.domain.models.BackupFile

/**
 * FileScanner provides utility methods to scan for WhatsApp Business media files
 * using the Android MediaStore API, ensuring compatibility with Scoped Storage.
 */
class FileScanner(private val context: Context) {

    /**
     * Scans the MediaStore for WhatsApp Business media files matching the selected [categories].
     * 
     * How MediaStore Query Parameters Filter Results:
     * - `queryUri`: MediaStore.Files.getContentUri("external") queries the external storage database table.
     * - `projection`: Specifies exact database columns (_ID, DISPLAY_NAME, SIZE, MIME_TYPE, DATA, RELATIVE_PATH)
     *   to retrieve only necessary metadata, minimizing memory usage.
     * - `selection`: "${MediaStore.Files.FileColumns.RELATIVE_PATH} LIKE ?" filters rows to only include
     *   files residing within WhatsApp Business directory trees.
     * - `selectionArgs`: ArrayOf("%WhatsApp Business%") ensures Scoped Storage compatibility across Android 10+.
     * - `sortOrder`: "${MediaStore.Files.FileColumns.DATE_ADDED} DESC" sorts results newest first.
     * 
     * Voice Notes vs. Audio Differentiation:
     * - WhatsApp voice messages are stored in "WhatsApp Voice Notes" or "PTT" (Push-To-Talk) subfolders.
     * - General audio files (music tracks, audio clips) are saved in "WhatsApp Audio" subfolders.
     * - `determineCategory()` inspects both filesystem paths and file extensions/MIME types to accurately
     *   separate Voice Notes from general Audio.
     * 
     * @param categories Set of selected [BackupCategory] values to filter. Defaults to all categories.
     * @return List of [BackupFile] objects matching the selected categories.
     */
    fun scanWhatsAppBusinessFiles(
        categories: Set<BackupCategory> = BackupCategory.values().toSet()
    ): List<BackupFile> {
        val backupFiles = mutableListOf<BackupFile>()
        val contentResolver: ContentResolver = context.contentResolver

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.DATA
        )

        val selection = "${MediaStore.Files.FileColumns.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("%WhatsApp Business%")
        val queryUri = MediaStore.Files.getContentUri("external")

        contentResolver.query(
            queryUri,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
        )?.use { cursor ->
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)

            while (cursor.moveToNext()) {
                val name = cursor.getString(nameColumn) ?: continue
                val size = cursor.getLong(sizeColumn)
                val mimeType = cursor.getString(mimeTypeColumn) ?: "application/octet-stream"
                val path = cursor.getString(dataColumn) ?: ""

                val detectedCategory = determineCategory(path, name, mimeType)

                // 🗂️ Include file only if its detected category is enabled in user settings
                if (detectedCategory != null && categories.contains(detectedCategory)) {
                    backupFiles.add(
                        BackupFile(
                            path = path,
                            name = name,
                            size = size,
                            type = mimeType,
                            category = detectedCategory
                        )
                    )
                }
            }
        }

        return backupFiles
    }

    /**
     * Determines the [BackupCategory] for a given file path, filename, and MIME type.
     * 
     * Differentiates Voice Notes from Audio via directory path inspection ("Voice Notes" / "PTT").
     */
    private fun determineCategory(path: String, fileName: String, mimeType: String): BackupCategory? {
        val lowerPath = path.lowercase()
        val lowerName = fileName.lowercase()
        val lowerMime = mimeType.lowercase()

        // 🎙️ Voice Notes: Located in "WhatsApp Voice Notes" or "PTT" subfolders or ending in .opus
        if (lowerPath.contains("voice notes") || lowerPath.contains("ptt") || lowerName.endsWith(".opus")) {
            return BackupCategory.VOICE_NOTES
        }

        // 🖼️ Images
        if (lowerPath.contains("whatsapp images") ||
            listOf(".jpg", ".jpeg", ".png", ".webp", ".gif").any { lowerName.endsWith(it) } ||
            lowerMime.startsWith("image/")
        ) {
            return BackupCategory.IMAGES
        }

        // 🎥 Video
        if (lowerPath.contains("whatsapp video") ||
            listOf(".mp4", ".3gp", ".mkv", ".webm", ".avi").any { lowerName.endsWith(it) } ||
            lowerMime.startsWith("video/")
        ) {
            return BackupCategory.VIDEO
        }

        // 🎵 Audio (General audio tracks outside PTT)
        if (lowerPath.contains("whatsapp audio") ||
            listOf(".mp3", ".aac", ".wav", ".flac", ".m4a", ".ogg").any { lowerName.endsWith(it) } ||
            lowerMime.startsWith("audio/")
        ) {
            return BackupCategory.AUDIO
        }

        // 📄 Documents
        if (lowerPath.contains("whatsapp documents") ||
            listOf(".pdf", ".docx", ".xlsx", ".pptx", ".txt", ".csv", ".doc", ".xls", ".ppt", ".zip").any { lowerName.endsWith(it) } ||
            lowerMime.startsWith("application/") || lowerMime.startsWith("text/")
        ) {
            return BackupCategory.DOCUMENTS
        }

        return null
    }
}
