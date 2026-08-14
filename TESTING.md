# WABackupPro Validation & Testing Guide

This document outlines the testing strategy, automated test suite details, and step-by-step physical device verification procedures for **WABackupPro**.

---

## 1. Automated Testing Suite

WABackupPro utilizes JVM-based unit tests for fast, deterministic validation of domain logic, category parsing, hash calculations, and data structures.

### Test Classes & Scope

| Test Class | Package | Tested Functionality |
| :--- | :--- | :--- |
| `DetectChangedFilesUseCaseTest` | `com.wabackuppro.domain.usecases` | Delta detection, SHA-256 calculation, file categorization (new vs modified vs unchanged). |
| `BackupCategoryTest` | `com.wabackuppro.domain.models` | MIME type resolution, extension mapping, WhatsApp Business directory routing. |
| `BackupProgressTest` | `com.wabackuppro.domain.models` | Backup state model initialization and payload handling. |

### Running Unit Tests

```bash
# Windows
.\gradlew.bat test

# Linux / macOS
./gradlew test
```

---

## 2. Physical Device Testing Procedures

The following manual test protocols verify Android system integration, permissions, network changes, and background service execution.

### Test Matrix

- **Android 10 (API 29)**: Standard MediaStore API & legacy path access.
- **Android 11–13 (API 30–33)**: Scoped Storage & granular media permissions (`READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`, `READ_MEDIA_AUDIO`).
- **Android 14+ (API 34+)**: Foreground Service Type constraints (`FOREGROUND_SERVICE_DATA_SYNC`).

---

### Protocol 1: Initial Launch & Permissions Flow

1. Install `app-debug.apk` onto a physical device or emulator.
2. Launch WABackupPro.
3. **Expected Behavior**:
   - The app displays an initial status of "Idle / Ready".
   - Tapping "Backup Now" triggers standard system permission prompts for storage/media access.
   - Granting permissions enables directory scanning.

---

### Protocol 2: Google Drive OAuth Sign-In & Disconnect

1. Tap **Sign In with Google**.
2. Select an active Google account on the device.
3. **Expected Behavior**:
   - Google Auth sheet prompts for permission limited strictly to **Google Drive (View & Manage files created by WABackupPro)**.
   - Upon completion, the screen displays "Connected as user@gmail.com".
   - Tapping **Sign Out** clears tokens and returns UI to disconnected state.

---

### Protocol 3: Manual Incremental Backup Execution

1. Populate `/Android/media/com.whatsapp.w4b/WhatsApp Business/Media/` with test files (Images, Videos, Documents).
2. Ensure device has an active Wi-Fi or Cellular network connection.
3. Tap **Backup Now**.
4. **Expected Behavior**:
   - Status updates dynamically: `Scanning` -> `Uploading (X/Y)` -> `Backup Complete`.
   - A persistent foreground notification appears: **"WABackupPro - Backing up files..."**.
   - Audit record appears in **Backup History** with total files, uploaded files, and skipped count.
   - Re-running **Backup Now** immediately skips unchanged files (0 uploaded, N skipped).

---

### Protocol 4: Demo Mode Verification (No Network / No Google Account)

1. Open **Settings**.
2. Enable **Demo Mode**.
3. Return to Main Dashboard and tap **Backup Now**.
4. **Expected Behavior**:
   - Simulates a full 10-file backup execution with synthetic progress events.
   - Inserts a synthetic history record into Room DB marked status `SUCCESS`.
   - Does not require network access or active Google Drive credentials.

---

### Protocol 5: Background WorkManager & Boot Execution

1. Schedule a periodic backup in **Settings** (e.g. Daily).
2. Trigger WorkManager job execution manually via ADB:
   ```bash
   adb shell cmd jobscheduler run -f com.wabackuppro 1
   ```
3. Reboot the target device (`adb reboot`).
4. **Expected Behavior**:
   - `BootReceiver` receives `ACTION_BOOT_COMPLETED` and schedules `BackupWorker`.
   - Notification appears when background execution starts.
