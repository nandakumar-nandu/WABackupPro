# Changelog

All notable changes to this project will be documented in this file.

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
