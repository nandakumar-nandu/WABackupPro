package com.wabackuppro.domain.models

/**
 * BackupCategory defines the media and document types that users can selectively include
 * or exclude from automated WhatsApp Business backups.
 * 
 * Category Mapping Rules:
 * 
 * 1. DOCUMENTS:
 *    - Directory Paths: Contains "WhatsApp Documents"
 *    - File Extensions: .pdf, .docx, .xlsx, .pptx, .txt, .csv, .doc, .xls, .ppt, .zip
 *    - MIME Types: application/pdf, application/msword, application/vnd.openxmlformats-officedocument.*, text/plain
 * 
 * 2. IMAGES:
 *    - Directory Paths: Contains "WhatsApp Images"
 *    - File Extensions: .jpg, .jpeg, .png, .webp, .gif
 *    - MIME Types: image/jpeg, image/png, image/webp, image/gif
 * 
 * 3. VIDEO:
 *    - Directory Paths: Contains "WhatsApp Video"
 *    - File Extensions: .mp4, .3gp, .mkv, .webm, .avi
 *    - MIME Types: video/mp4, video/3gpp, video/x-matroska, video/webm
 * 
 * 4. AUDIO:
 *    - Directory Paths: Contains "WhatsApp Audio" (excluding voice notes/PTT folders)
 *    - File Extensions: .mp3, .aac, .wav, .flac
 *    - MIME Types: audio/mpeg, audio/aac, audio/wav, audio/x-wav, audio/mp3
 * 
 * 5. VOICE_NOTES:
 *    - Directory Paths: Contains "WhatsApp Voice Notes" or "PTT" (Push-To-Talk)
 *    - File Extensions: .opus, .m4a, .ogg
 *    - MIME Types: audio/opus, audio/ogg, audio/mp4, audio/aac
 *    - Note: Voice notes are specifically distinguished from general audio by directory path inspection
 *      ("WhatsApp Voice Notes" or "PTT") because WhatsApp stores voice recordings in separate subfolders.
 */
enum class BackupCategory(val displayName: String) {
    // 📄 Documents: PDFs, Word documents, Spreadsheets, Text files
    DOCUMENTS("Documents"),

    // 🖼️ Images: Photos, received pictures, webp stickers
    IMAGES("Images"),

    // 🎥 Video: Video clips and shared movies
    VIDEO("Video"),

    // 🎵 Audio: Shared music tracks and general audio files
    AUDIO("Audio"),

    // 🎙️ Voice Notes: Recorded voice messages (PTT / Voice Notes subfolders)
    VOICE_NOTES("Voice Notes")
}
