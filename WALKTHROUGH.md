# Walkthrough: WABackupPro

## Why this app exists (The Problem)

WhatsApp Business stores message databases and media locally on devices. While WhatsApp offers personal automated backups to Google Drive, WhatsApp Business backup behaviors can sometimes be restricted by enterprise policies, lack scheduling flexibility, or be difficult to export. Enterprise and power users need an independent, granular, and automated utility to:
- Schedule backups at exact intervals (e.g., hourly or daily).
- Store history records of backups.
- Save backups securely in the App Data space on Google Drive, preventing manual deletions or visibility from other file browsers.

This app bridges this gap by offering a dedicated background scheduler that runs silently in the background, monitors WhatsApp database updates, and sends them to Google Drive.

## How the backup works (Planned Mechanism)

1. **Scheduling**:
   - The user opens the app and links their Google Drive account.
   - The app schedules a recurring `PeriodicWorkRequest` using WorkManager.
2. **Accessing Database Files**:
   - The worker locates the WhatsApp Business directory (typically under `Android/media/com.whatsapp.w4b/WhatsApp Business/Databases/`).
   - It identifies the most recent database backup file (e.g., `msgstore.db.crypt14`).
3. **Transmission**:
   - The app verifies network constraints (ensuring Wi-Fi is active to save mobile data).
   - Using the authenticated Google Drive client, it uploads the encrypted database file to the user's Google Drive application metadata folder (`appDataFolder`).
4. **Logging & Monitoring**:
   - Each run inserts a status entry (`SUCCESS` or `FAILED`) into the Room database.
   - The main dashboard updates the status card and recyclerView log list in real-time.

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
