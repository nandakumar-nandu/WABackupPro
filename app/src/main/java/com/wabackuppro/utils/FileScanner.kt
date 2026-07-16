package com.wabackuppro.utils

import android.content.ContentResolver
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import com.wabackuppro.domain.models.BackupFile

/**
 * FileScanner provides utility methods to scan for WhatsApp Business media files
 * using the Android MediaStore API, ensuring compatibility with Scoped Storage.
 */
class FileScanner(private val context: Context) {

    /**
     * Scans the MediaStore for specific file types within the WhatsApp Business directory.
     * Supported types: PDF, DOCX, XLSX, JPG, PNG, MP4.
     * 
     * @return List of [BackupFile] objects containing file metadata.
     */
    fun scanWhatsAppBusinessFiles(): List<BackupFile> {
        val backupFiles = mutableListOf<BackupFile>()
        val contentResolver: ContentResolver = context.contentResolver

        // 🛠️ Define the columns we want to retrieve from the MediaStore
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.DATA // Required for legacy path access or unique identification
        )

        // 🔍 Filter for WhatsApp Business media directories
        // WhatsApp Business typically stores media in: Android/media/com.whatsapp.w4b/WhatsApp Business/Media/
        val selection = "${MediaStore.Files.FileColumns.RELATIVE_PATH} LIKE ?"
        
        // 📁 The query looks for files where the relative path contains "WhatsApp Business"
        val selectionArgs = arrayOf("%WhatsApp Business%")

        // 📑 Define the URI for external content
        val queryUri = MediaStore.Files.getContentUri("external")

        contentResolver.query(
            queryUri,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.Files.FileColumns.DATE_ADDED} DESC" // Sort by newest first
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)

            while (cursor.moveToNext()) {
                val name = cursor.getString(nameColumn)
                val size = cursor.getLong(sizeColumn)
                val mimeType = cursor.getString(mimeTypeColumn) ?: "application/octet-stream"
                val path = cursor.getString(dataColumn)

                // 🏷️ Filter by desired file extensions/types
                if (isSupportedType(name, mimeType)) {
                    backupFiles.add(
                        BackupFile(
                            path = path,
                            name = name,
                            size = size,
                            type = mimeType
                        )
                    )
                }
            }
        }

        return backupFiles
    }

    /**
     * Checks if the file is of a type we wish to backup.
     */
    private fun isSupportedType(fileName: String, mimeType: String): Boolean {
        val supportedExtensions = listOf(".pdf", ".docx", ".xlsx", ".jpg", ".png", ".mp4")
        val supportedMimeTypes = listOf(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "image/jpeg",
            "image/png",
            "video/mp4"
        )

        return supportedExtensions.any { fileName.lowercase().endsWith(it) } ||
                supportedMimeTypes.contains(mimeType)
    }
}
