# Walkthrough: WABackupPro

## Why this app exists (The Problem)

WhatsApp Business stores message databases and media locally on devices. While WhatsApp offers personal automated backups to Google Drive, WhatsApp Business backup behaviors can sometimes be restricted by enterprise policies, lack scheduling flexibility, or be difficult to export. Enterprise and power users need an independent, granular, and automated utility to:
- Schedule backups at exact intervals (e.g., hourly or daily).
- Store history records of backups.
- Save backups securely in the App Data space on Google Drive, preventing manual deletions or visibility from other file browsers.

This app bridges this gap by offering a dedicated background scheduler that runs silently in the background, monitors WhatsApp database updates, and sends them to Google Drive.

## Google Drive Setup (Implementation)

The application uses the **Google Drive REST API v3** combined with **Google Play Services Auth**.

1. **Authentication (Least Privilege)**:
   - The app requests the `https://www.googleapis.com/auth/drive.file` scope.
   - **Why?** This scope provides access only to files and folders created or opened by the app itself. It prevents the app from seeing the user's personal documents, adhering to the principle of least privilege.
2. **Folder Creation**:
   - On the first backup or test run, the app checks for/creates a dedicated folder (e.g., `WABackup_Test` or `WABackupPro_Backups`).
3. **Resilient Uploads**:
   - Files are uploaded using `FileContent`, which handles the binary stream efficiently.
   - Every upload is logged in the `Activity Logs` with its unique Drive File ID for auditing.

## Automatic Scheduling (Implementation)

1. **WorkManager Initialization**:
   - Background scheduling is managed by the `BackupScheduler` utility. When the user sets a weekly backup (e.g. every Friday), the scheduler calculates the exact delay until the next upcoming Friday at the chosen time.
2. **Periodic Constraints**:
   - The backup job is queued as a `PeriodicWorkRequest` running every 7 days. It respects OS constraints by running only when the device is connected to Wi-Fi (`NetworkType.UNMETERED`) and the battery is not low.
3. **Foreground Service Promotion**:
   - Because WhatsApp backups can be large and take longer than the 10-minute WorkManager background limit, the `BackupWorker` immediately promotes itself to a Foreground Service (`setForeground()`). This gives the app a higher priority execution state and shows a persistent notification.
4. **Boot Resilience**:
   - A `BootReceiver` is registered to listen for `ACTION_BOOT_COMPLETED`. If the device is restarted, it instantly reschedules the WorkManager task so the user doesn't miss their scheduled Friday backup.

## How the backup works (Planned Mechanism)

1. **Scheduling**:
   - The user opens the app and links their Google Drive account.
   - The app schedules a recurring `PeriodicWorkRequest` using WorkManager.
2. **File Scanning**:
   - The app uses the `MediaStore` API to query for files in the `WhatsApp Business` media folder.
   - It filters for specific types: `.pdf`, `.docx`, `.xlsx`, `.jpg`, `.png`, and `.mp4`.
   - On Android 13+, it requests granular media permissions (`READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`), while on older versions it uses `READ_EXTERNAL_STORAGE`.
3. **Transmission**:
   - The app verifies network constraints (ensuring Wi-Fi is active to save mobile data).
   - Using the authenticated Google Drive client, it uploads the encrypted database file to the user's Google Drive application metadata folder (`appDataFolder`).
4. **Logging & Monitoring**:
   - Each run inserts a status entry (`SUCCESS` or `FAILED`) into the Room database.
   - The main dashboard updates the status card and recyclerView log list in real-time.

## Running a Manual Backup (Implementation)

1. **Triggering the Job**:
   - When the user taps "Start Backup Now", the `MainViewModel` invokes the `RunBackupUseCase`.
2. **Sequential Orchestration**:
   - The use case first scans the local storage to build a list of files.
   - It then establishes a date-stamped folder on Google Drive.
   - Files are uploaded sequentially to prevent network congestion and to allow granular progress tracking.
3. **Progress & Feedback**:
   - Every file upload emits a state change. The UI reacts by updating the `LinearProgressIndicator` and appending a record to the `Activity Logs`.
4. **Error Handling & Retries**:
   - If a file upload fails (e.g., due to a temporary network timeout), the `RunBackupUseCase` automatically retries the operation up to **3 times**.
   - After all retries are exhausted, the file is marked as failed (`❌`), and the process moves to the next file to ensure the entire backup isn't stalled by a single corrupted or missing item.

## User Journey Flowchart

```mermaid
journey
    title User Journey: Installation to Automatic Backups
    section Install & Launch
      Install App from APK: 5: User
      Launch WABackupPro: 5: User
    section Setup & Auth
      Authorize Google Drive: 4: User, App
      Configure Backup Frequency: 4: User
    section Automated Operations
      System runs scheduled job in background: 5: App
      Sync occurs over Wi-Fi: 5: App
      Receive push notification of backup: 5: App
      Check dashboard audit logs: 4: User
```
