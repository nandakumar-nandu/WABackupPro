# Changelog

All notable changes to **WABackupPro** are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.3.0] - 2026-08-14

### Changed
- **Documentation Architecture Overhaul**: Consolidated project overview into `README.md`, developer setup in `DEVELOPMENT.md`, novice instructions in `USER_GUIDE.md`, and technical specifications in `ARCHITECTURE.md`.
- **Legacy Markdown Retention**: Added consolidation header notices to `SCREENTOUR.md` and `WALKTHROUGH.md` while preserving complete original content for link stability.

### Added
- **Per-File Result Tracking**: Added `BackupFileResult` Room entity and `BackupFileResultDao` (Database v3) to persist individual file status (`SUCCESS`, `SKIPPED`, `FAILED`).
- **Backup Detail Screen**: Added `BackupDetailFragment` and `FileResultAdapter` for per-file outcome inspection.
- **Single-File Retry Engine**: Added interactive single-file retry flow in `BackupDetailFragment` for failed uploads.
- **History Search & Filtering**: Added live search bar (`et_search_history`) and Material3 status filter chips (`All`, `Success`, `Partial`, `Failed`) to `BackupHistoryFragment`.

---

## [1.2.0] - 2026-07-28

### Added
- **Selective Backup Categories**: Added `BackupCategory` enum (`DOCUMENTS`, `IMAGES`, `VIDEO`, `AUDIO`, `VOICE_NOTES`).
- **Voice Notes Separation**: Enhanced `FileScanner` to classify `WhatsApp Voice Notes` and `PTT/` folders separately from music tracks.
- **Category Settings UI**: Added Material3 category switches and quick-select buttons in `SettingsFragment`.
- **Empty Category Guard**: Updated `RunBackupUseCase` to short-circuit cleanly if no categories are enabled.

---

## [1.1.0] - 2026-07-28

### Added
- **SHA-256 Delta Engine**: Integrated `DetectChangedFilesUseCase` to calculate cryptographic file hashes and skip unchanged payloads.
- **Delta Persistence**: Added `BackupFileEntry` entity and `BackupFileEntryDao` (Database v2) to store local file hashes and Drive file IDs.
- **Force Full Backup**: Added manual override toggle in `SettingsFragment` to bypass delta detection when needed.

---

## [1.0.0] - 2026-07-18

### Added
- **Full Production Release**: End-to-end background backup orchestration via `RunBackupUseCase`, `BackupWorker`, and `DriveClient`.
- **Edge Case Exception Handling**: Added structured recovery for `DriveStorageFullException`, `AuthExpiredException`, and `NoFilesFoundException`.
- **Material 3 UI Polish**: Integrated progress indicators, audit logs adapter, and app launcher assets.

---

## [0.6.0] - 2026-07-18

### Added
- **Room Database Integration**: Introduced `AppDatabase` (Version 3) and `BackupRecord` schema.
- **History UI**: Built reactive `BackupHistoryFragment` driven by Room database Flows.

---

## [0.5.0] - 2026-07-18

### Added
- **WorkManager Background Engine**: Integrated `BackupScheduler` for periodic Friday backups.
- **Foreground Service Elevation**: Promoted `BackupWorker` to a Foreground Service during execution.
- **Boot Recovery**: Registered `BootReceiver` for `ACTION_BOOT_COMPLETED`.

---

## [0.4.0] - 2026-07-16

### Added
- **Backup Orchestration**: Implemented initial `RunBackupUseCase` with per-file retry policies.

---

## [0.3.0] - 2026-07-16

### Added
- **Google Drive Client**: Integrated Google Drive REST API v3 via Google Play Services Auth (`drive.file` scope).

---

## [0.2.0] - 2026-07-16

### Added
- **File Discovery Engine**: Implemented `FileScanner` using Android `MediaStore` Scoped Storage API.

---

## [0.1.0] - 2026-07-14

### Added
- **Initial Project Scaffold**: Standard Android application with Kotlin, min SDK 26, target SDK 34, and Navigation Component setup.
