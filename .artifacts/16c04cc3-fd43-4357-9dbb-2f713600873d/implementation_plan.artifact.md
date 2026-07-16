# Implementation Plan - Commit 2: WhatsApp Business File Scanner

Implement a media scanner for WhatsApp Business files using the MediaStore API, adhering to Scoped Storage guidelines and handling permissions gracefully.

## User Review Required

> [!IMPORTANT]
> **Storage Permissions on Android 13+ (API 33)**:
> Accessing PDF/DOCX files via MediaStore on Android 13+ usually requires `READ_EXTERNAL_STORAGE` (which is restricted) or using the Storage Access Framework (SAF) to pick a folder. However, since the requirement specifies **MediaStore API**, I will use `READ_EXTERNAL_STORAGE` for API < 33 and the granular `READ_MEDIA_IMAGES` / `READ_MEDIA_VIDEO` for API 33+. For Documents on API 33+, I will include logic to handle potential access restrictions if MediaStore doesn't return them without broad permissions.

## Proposed Changes

### Domain Layer

#### [NEW] [BackupFile.kt](file:///D:/projects/WABackupPro/app/src/main/java/com/wabackuppro/domain/models/BackupFile.kt)
- Data class to hold file metadata.
- Fields: `path`, `name`, `size`, `type`.

### Utils Layer

#### [NEW] [FileScanner.kt](file:///D:/projects/WABackupPro/app/src/main/java/com/wabackuppro/utils/FileScanner.kt)
- Use `ContentResolver` to query `MediaStore.Files`.
- Filter by `RELATIVE_PATH` containing "WhatsApp Business".
- Filter by extensions: `.pdf`, `.docx`, `.xlsx`, `.jpg`, `.png`, `.mp4`.
- Detailed KDoc and parameter comments as requested.

### UI & Framework Layer

#### [MODIFY] [AndroidManifest.xml](file:///D:/projects/WABackupPro/app/src/main/AndroidManifest.xml)
- Add `<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32" />`.
- Add `<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />`.
- Add `<uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />`.

#### [MODIFY] [MainViewModel.kt](file:///D:/projects/WABackupPro/app/src/main/java/com/wabackuppro/ui/main/MainViewModel.kt)
- Add `scanFiles()` function that calls `FileScanner`.
- Add a new `LiveData<Int>` or state for `foundFileCount`.

#### [MODIFY] [BackupFragment.kt](file:///D:/projects/WABackupPro/app/src/main/java/com/wabackuppro/ui/main/BackupFragment.kt)
- Implement `ActivityResultLauncher` for permission requests.
- Show rationale `AlertDialog` if permissions are denied.
- Handle "Permanent Denial" with a snackbar/button leading to App Settings.
- Update UI to show the scanned file count when the button is tapped.

### Documentation

#### [MODIFY] [CHANGELOG.md](file:///D:/projects/WABackupPro/CHANGELOG.md)
- Add version `0.2.0` entry.

#### [MODIFY] [README.md](file:///D:/projects/WABackupPro/README.md)
- Add Mermaid diagram for the Permission Flow.

#### [MODIFY] [WALKTHROUGH.md](file:///D:/projects/WABackupPro/WALKTHROUGH.md)
- Update "File Scanning" section with implementation details.

#### [MODIFY] [SCREENTOUR.md](file:///D:/projects/WABackupPro/SCREENTOUR.md)
- Update "Permissions Flow" and "Main Screen with file count" sections.

## Verification Plan

### Automated Tests
- No specific unit tests requested for this commit, but I will verify code compilation.

### Manual Verification
- Deploy to an emulator/device.
- Trigger "Start Backup Now".
- Verify permission request popups appear.
- Verify that denying permissions shows the rationale.
- Verify that granting permissions results in a "Found X files" log/status update.
