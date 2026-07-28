# Changelog

All notable changes to this project will be documented in this file.

## [1.2.0] - 2026-07-28 12:55

### Added
- **Selective Backup Categories**: Introduced `BackupCategory` enum (`DOCUMENTS`, `IMAGES`, `VIDEO`, `AUDIO`, `VOICE_NOTES`) allowing users to customize media types backed up to Google Drive.
- **Voice Notes Separation Engine**: Enhanced `FileScanner` to inspect directory paths (`WhatsApp Voice Notes` / `PTT`) to differentiate voice notes from general audio tracks.
- **Category Settings UI**: Built a Material3 category selection panel in `SettingsFragment` complete with per-category toggle switches, SharedPreferences persistence (`PREF_CAT_*`), and "Select All" / "Select None" quick shortcut buttons.
- **Empty Selection Short-Circuiting**: Updated `RunBackupUseCase` to short-circuit immediately with a clear user notice when no categories are selected, avoiding zero-item folder creations and API quota consumption.
- **History Per-Category Breakdown**: Updated `HistoryAdapter` and `item_backup_record.xml` to display formatted per-category file breakdown summaries on each history card.
- **Documentation**: Added category-to-file-extension mapping table to `README.md`, updated `WALKTHROUGH.md`, and added "Settings — Category Selection" screen to `SCREENTOUR.md`.

## [1.1.0] - 2026-07-28 12:50

### Added
- **Incremental Delta Detection Engine**: Introduced `DetectChangedFilesUseCase` that computes cryptographic SHA-256 hashes of scanned files to skip unchanged payloads.
- **Delta Database Persistence**: Added `BackupFileEntry` entity and `BackupFileEntryDao` (Room DB schema version 2) to store local file hashes, paths, modification times, and Google Drive file IDs across backup runs.
- **Manifest Tracking**: Extended `BackupRecord` entity with `uploadedFilesManifest` field.
- **Optimized Backup Workflow**: Updated `RunBackupUseCase` and `BackupWorker` to only upload `newFiles` and `modifiedFiles`, dramatically conserving Drive API quota, data transfer, and battery power.
- **Progress Tracking Improvements**: Extended `BackupProgress` model to report skipped file counts (`skippedFiles`) in real-time.
- **Force Full Backup Toggle**: Added a Material3 switch in `SettingsFragment` (`PREF_FORCE_FULL_BACKUP`) serving as an escape hatch to bypass delta detection if needed.
- **Documentation Updates**: Added Mermaid delta detection flowchart to `README.md`, updated `WALKTHROUGH.md`, and added "Settings — Force Full Backup" screen to `SCREENTOUR.md`.

## [0.1.0] - 2026-07-14 15:15

### Added
- **Android Project Scaffold**: Standard Android project setup using Kotlin, min SDK 26, target SDK 34, and Java 17.
- **Gradle Version Catalog**: Integrated `libs.versions.toml` to manage Room, WorkManager, Google Drive API, and Navigation dependencies centrally.
- **MVVM Architecture Layout**: Configured packages for `ui`, `data`, `domain`, `workers`, and `utils`.
- **Navigation Flow UI**: Built `MainActivity` with bottom navigation targeting `BackupFragment`, `BackupHistoryFragment`, and `SettingsFragment`.
- **Material 3 Custom Styling**: Defined responsive layouts (`activity_main.xml`, `fragment_backup.xml`) with a dark-theme/light-theme compatible palette.
- **Data & Work Placeholders**: Created skeleton implementations for Room (`BackupDatabase`, `BackupRecordDao`), Use Cases, and WorkManager (`BackupWorker`).
- **Initial Project Documentation**: Completed `README.md`, `WALKTHROUGH.md`, and `SCREENTOUR.md`.

## [0.2.0] - 2026-07-16 12:30

### Added
- **WhatsApp Business File Scanner**: Implemented `FileScanner` utility using MediaStore API to discover media and document files.
- **BackupFile Model**: Defined a data class for scanned file metadata.
- **Scoped Storage Permissions**: Added robust handling for storage permissions across Android versions, including Android 13+ granular media permissions.
- **Permission UX**: Implemented rationale dialogs and settings deep-links for a graceful user experience.
- **Real-time Feedback**: Updated the dashboard to display the count of discovered files during a scan.

## [0.3.0] - 2026-07-16 12:45

### Added
- **Google Drive Integration**: Implemented `DriveClient` using the Google Drive REST API.
- **OAuth 2.0 Authentication**: Integrated Google Sign-In with minimal `DRIVE_FILE` scope.
- **Folder Management**: Logic to create and identify backup folders in Drive.
- **File Upload Engine**: Robust upload mechanism with parents-folder targeting.
- **Test Dashboard Features**: Added Login and "Test Upload" buttons to the main dashboard for end-to-end verification.
- **Infrastructure Docs**: Added detailed Google Cloud Console setup guide.

## [0.4.0] - 2026-07-16 13:15

### Added
- **Full Backup Orchestration**: Implemented `RunBackupUseCase` to coordinate the entire backup flow (Scan -> Folder Creation -> Upload).
- **Progress Tracking Engine**: Integrated real-time progress updates using Kotlin `Flow`.
- **Fault-Tolerant Uploads**: Added per-file retry logic (3 attempts) with linear backoff.
- **Enhanced Dashboard UI**: Added a Material3 `LinearProgressIndicator`, progress counters, and current-file labels.
- **Detailed Audit Logging**: Success (`✅`) and Failure (`❌`) indicators for every file in the activity logs.
- **Architecture Documentation**: Added a Mermaid sequence diagram for the backup orchestration layer.

## [0.5.0] - 2026-07-18 12:45

### Added
- **Automatic Background Scheduling**: Implemented `BackupScheduler` to schedule recurring backups every Friday.
- **WorkManager Integration**: Upgraded `BackupWorker` to a `CoroutineWorker` that runs `RunBackupUseCase` seamlessly in the background.
- **Foreground Service Promotion**: The worker now promotes itself to a Foreground Service with a persistent notification to avoid Doze mode restrictions.
- **Resilience on Reboot**: Added `BootReceiver` to automatically reschedule WorkManager tasks when the device restarts.
- **Documentation**: Updated `README.md` with WorkManager flowchart, tech stack badges, and added relevant sections to walkthroughs and tours.

## [0.6.0] - 2026-07-18 13:45

### Added
- **Local Database Engine**: Integrated Android Room to persist backup metadata (timestamp, files count, success/fail counts, duration) locally across sessions.
- **Backup History Dashboard**: Converted the history screen into a dynamic `RecyclerView` listing past backups. Features color-coded status badges and direct "Open in Drive" deep links.
- **Interactive Settings Hub**: Built a comprehensive settings UI with a TimePicker for backup scheduling, Wi-Fi constraints, history retention period, and Google Account management.
- **Data Schemas**: Implemented `BackupRecord` entity and `BackupRecordDao` to structure and query the historical data efficiently.

## [1.0.0] - 2026-07-18 15:30

### Added
- **Edge Case Resilience**: 
  - Catches quota exceeded exceptions (`DriveStorageFullException`) and pauses cleanly with an actionable warning.
  - Detects network drops mid-backup (`IOException`) and intelligently tells WorkManager to pause and resume once network is restored.
  - Automatically identifies expired OAuth tokens and flags the UI for re-authentication (`AuthExpiredException`).
  - Gracefully handles empty WhatsApp folders (`NoFilesFoundException`) with a friendly notification rather than an obscure error.
- **UI Polish**:
  - Introduced Material3 `TransitionManager` animations, ensuring smooth expansion and collapse when status cards update.
  - Added a responsive "Retry Backup" button for immediate recovery when operations halt.
  - Shipped a high-resolution Vector Drawable app icon for launcher screens.
- **Environment Templates**: Added fully documented `.env.example` and `local.properties.example` detailing exact Google Cloud console setup procedures.
- **Documentation Complete**: Finalized `README.md`, `WALKTHROUGH.md`, and `SCREENTOUR.md`. Eliminated all placeholder construction markers. The app is 1.0.0 ready.
