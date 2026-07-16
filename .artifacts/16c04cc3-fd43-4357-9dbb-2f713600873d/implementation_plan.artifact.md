# Implementation Plan - Commit 4: Full Backup Job with Progress Tracking

Orchestrate the complete backup workflow including folder creation, file scanning, sequential uploads with retries, and real-time UI progress updates.

## User Review Required

> [!IMPORTANT]
> **Retry Logic**: I will implement a per-file exponential backoff retry mechanism (3 attempts) within the `RunBackupUseCase`. If a file fails after all retries, the backup will continue with the next file, but the final status will report errors.

## Proposed Changes

### Domain Layer

#### [NEW] [BackupProgress.kt](file:///D:/projects/WABackupPro/app/src/main/java/com/wabackuppro/domain/models/BackupProgress.kt)
- Data class to track progress.
- Fields: `totalFiles`, `uploadedFiles`, `currentFileName`, `status`, `errors` (List of String).

#### [NEW] [RunBackupUseCase.kt](file:///D:/projects/WABackupPro/app/src/main/java/com/wabackuppro/domain/usecases/RunBackupUseCase.kt)
- Orchestrates the backup:
    1.  Get `GoogleSignInAccount`.
    2.  Scan files using `FileScanner`.
    3.  Create a folder in Drive named `WABackup_YYYY-MM-DD`.
    4.  Iterate through `BackupFile` list:
        - Upload file using `DriveClient`.
        - Implement retry logic (3 attempts) for each upload.
        - Emit `BackupProgress` updates via `Flow`.

### UI & ViewModel

#### [MODIFY] [MainViewModel.kt](file:///D:/projects/WABackupPro/app/src/main/java/com/wabackuppro/ui/main/MainViewModel.kt)
- Add `RunBackupUseCase` to the constructor.
- Add `LiveData<BackupProgress>` to expose progress to the UI.
- Update `startBackup()` to launch the use case and collect the `Flow`.

#### [MODIFY] [fragment_backup.xml](file:///D:/projects/WABackupPro/app/src/main/res/layout/fragment_backup.xml)
- Add a `LinearProgressIndicator` (Material3) below the "Start Backup Now" button.
- Add a `TextView` to show current file progress (e.g., "Uploading file 3 of 10").
- Add a `TextView` for the current file name.

#### [MODIFY] [BackupFragment.kt](file:///D:/projects/WABackupPro/app/src/main/java/com/wabackuppro/ui/main/BackupFragment.kt)
- Observe the new progress LiveData.
- Update the progress bar, status text, and current file name in real-time.
- Log success/failure for each file into the `RecyclerView`.

### Documentation

#### [MODIFY] [CHANGELOG.md](file:///D:/projects/WABackupPro/CHANGELOG.md)
- Add version `0.4.0` entry.

#### [MODIFY] [README.md](file:///D:/projects/WABackupPro/README.md)
- Add a Mermaid sequence diagram for the full backup workflow.

#### [MODIFY] [WALKTHROUGH.md](file:///D:/projects/WABackupPro/WALKTHROUGH.md)
- Fill in the "Running a Manual Backup" section.

#### [MODIFY] [SCREENTOUR.md](file:///D:/projects/WABackupPro/SCREENTOUR.md)
- Add the "Backup Progress Screen" details.

## Verification Plan

### Automated Tests
- Verify compilation of the new `RunBackupUseCase` and its Flow implementation.

### Manual Verification
- **Run Full Backup**: Trigger backup -> Observe progress bar increment -> Verify folder and multiple files appear in Drive.
- **Error Handling**: Simulate a network failure during upload (e.g., toggle Airplane mode) -> Observe retries in logs -> Verify that the job continues or fails gracefully.
- **UI Updates**: Verify that the "X/Y files" text and current file name update correctly.
