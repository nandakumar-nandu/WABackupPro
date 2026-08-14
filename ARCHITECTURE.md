# Architectural Documentation: WABackupPro

## Executive Summary & Engineering Audit

**WABackupPro** is an automated background backup utility for WhatsApp Business media and database files on Android. It leverages modern Android architecture patterns (MVVM + Domain Use Cases), Android Jetpack libraries (**Room Database**, **WorkManager**), and the **Google Drive REST API v3**.

This document outlines the technical design, architectural layers, data flows, background lifecycle management, and legacy component statuses of the application.

---

## 1. High-Level Architecture Overview

The application follows an **MVVM + Clean Architecture Use Case** model:

- **Presentation Layer (`com.wabackuppro.ui.*`)**:
  - `MainActivity`: Single activity hosting the primary bottom navigation and fragment view container.
  - `BackupFragment`: Active backup dashboard displaying progress indicators, state cards, and real-time logs.
  - `BackupHistoryFragment`: Interactive historical list supporting search queries and status filtering (`All`, `Success`, `Partial`, `Failed`).
  - `BackupDetailFragment`: Per-file drill-down view showing individual execution outcomes (`SUCCESS`, `SKIPPED`, `FAILED`) and single-file retry triggers.
  - `SettingsFragment`: User configuration panel for backup schedules, Wi-Fi constraints, delta overrides, selective categories, and account management.
  - `AboutActivity`: Standalone support and legal activity supporting Razorpay and UPI developer donations.
- **Domain Layer (`com.wabackuppro.domain.*`)**:
  - Models: `BackupFile`, `BackupProgress`, `BackupRecord`, `BackupCategory`.
  - Use Cases:
    - `RunBackupUseCase`: Core orchestrator for file scanning, folder creation, upload loops, retry policies, and Room DB logging.
    - `DetectChangedFilesUseCase`: SHA-256 cryptographic delta calculation comparing local payloads against stored Room entries.
- **Data & Remote Layer (`com.wabackuppro.data.*`)**:
  - Local Database: Room DB (`AppDatabase`, Version 3, database name `"wabackuppro_database"`).
  - DAOs: `BackupRecordDao`, `BackupFileResultDao`, `BackupFileEntryDao`.
  - Remote API: `DriveClient` (Google Drive REST API v3 via Play Services OAuth with `drive.file` scope).
- **Background Engine (`com.wabackuppro.workers.*` & `receivers.*`)**:
  - `BackupWorker`: `CoroutineWorker` promoted to a Foreground Service (`ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC`) during execution.
  - `BackupScheduler`: Utility configuring WorkManager periodic jobs (`weekly_friday_backup`) with hardware constraints.
  - `BootReceiver`: `BroadcastReceiver` listening for `ACTION_BOOT_COMPLETED` to reschedule WorkManager jobs after device reboots.

---

## 2. Component Call Graph & Data Flow

```
[ UI Layer ]
 BackupFragment / SettingsFragment
       │
       ▼
[ ViewModel Layer ]
 MainViewModel ───► (Observes LiveData / State)
       │
       ▼
[ Domain Layer ]
 RunBackupUseCase ──► DetectChangedFilesUseCase (SHA-256 Delta Engine)
   │        │
   │        ▼
   │   FileScanner (MediaStore Queries)
   │
   ├───► DriveClient (Google Drive REST API v3)
   │
   └───► AppDatabase DAOs (Room DB v3: BackupRecord, BackupFileResult, BackupFileEntry)
```

---

## 3. Storage & Scoped Storage Integration

`FileScanner` queries Android's `ContentResolver` against `MediaStore.Files.getContentUri("external")`.
- **Path Filter**: Uses SQL selection `RELATIVE_PATH LIKE ?` with `%WhatsApp Business%`.
- **Scoped Storage Compliance**: Accesses public shared directories without requiring legacy root filesystem access.
- **Category Isolation**:
  - `VOICE_NOTES`: Path contains `Voice Notes` or `PTT/`, or extension ends with `.opus`.
  - `IMAGES`: Extensions `.jpg`, `.png`, `.webp`, `.gif` or `image/*` MIME type.
  - `VIDEO`: Extensions `.mp4`, `.3gp`, `.mkv` or `video/*` MIME type.
  - `AUDIO`: Extensions `.mp3`, `.aac`, `.wav`, `.m4a` or `audio/*` MIME type outside voice notes.
  - `DOCUMENTS`: Extensions `.pdf`, `.docx`, `.xlsx`, `.pptx`, `.txt`, `.zip` or document MIME types.

---

## 4. Delta Detection & SHA-256 Algorithm

To conserve network bandwidth and Drive quota, `DetectChangedFilesUseCase` calculates a SHA-256 hash for every scanned file payload:
1. Streams `FileInputStream` in 8192-byte chunks into `MessageDigest.getInstance("SHA-256")`.
2. Queries `BackupFileEntryDao.getByPath(filePath)`.
3. Bucket Assignment:
   - **No DB Entry**: Assigned to `newFiles` bucket (requires upload).
   - **Hash Match**: Assigned to `unchangedFiles` bucket (skipped from upload, logged as `SKIPPED`).
   - **Hash Mismatch**: Assigned to `modifiedFiles` bucket (requires re-upload).
4. Upon successful Drive upload, `BackupFileEntryDao.upsert()` commits the updated SHA-256 string, timestamp, and Drive File ID to Room DB.

---

## 5. Background Execution & WorkManager Resilience

- **Periodic Scheduling**: `BackupScheduler` calculates the epoch millisecond delay to the next target Friday and enqueues a `PeriodicWorkRequest` repeating every 7 days.
- **Constraints**: Requires `NetworkType.UNMETERED` (Wi-Fi) and `RequiresBatteryNotLow`.
- **Foreground Service Elevation**: `BackupWorker` immediately calls `setForeground()` displaying an ongoing status notification. This prevents the OS from terminating long-running uploads (bypassing the 10-minute background job limit).
- **Boot Recovery**: `BootReceiver` intercepts `ACTION_BOOT_COMPLETED` and calls `BackupScheduler` to re-hydrate the periodic job.

---

## 6. Legacy & Transitional Code Analysis

- **`AppDatabase.kt` (Active)**: Database name `"wabackuppro_database"`, Version 3. Manages `BackupRecord`, `BackupFileEntry`, and `BackupFileResult`.
- **`BackupDatabase.kt` (Inactive/Legacy)**: Database name `"wa_backup_database"`, Version 1. Retained as an inactive migration artifact. Current source and inspected history indicate `AppDatabase` is the active database; it has not been conclusively proven that `BackupDatabase` was never included in an early distributed build.
- **`BackupRepository.kt` (Inactive/Legacy)**: Wraps legacy `data.local.BackupRecordDao`. Active production flows (`RunBackupUseCase` and `MainViewModel`) interact directly with `AppDatabase` DAOs. Retained as an inactive migration reference.

---

## 7. Known Architectural Limitations

1. **Direct DAO Access**: UI Fragments (`BackupHistoryFragment`, `BackupDetailFragment`) currently instantiate `AppDatabase` directly to observe room flows, bypassing the ViewModel layer.
2. **ViewModel Manual Construction**: `MainViewModel` manually instantiates `AppDatabase`, `FileScanner`, `DriveClient`, and `RunBackupUseCase` without a dependency injection framework (e.g., Hilt/Dagger).
3. **BootReceiver Schedule Reset**: `BootReceiver` reschedules jobs to a default time rather than reading custom user preferences from `SharedPreferences`.

---
*Document Version: 1.3.0 · Last Updated: August 2026*
