# User Guide: WABackupPro

Welcome to the **WABackupPro** User Guide! This manual explains how to set up, configure, and monitor automated backups of your WhatsApp Business media and databases to Google Drive.

---

## 📱 First Launch & Required Permissions

When you open WABackupPro for the first time:

1. **Storage Permissions**: The app will prompt for storage access.
   - On **Android 13+**: Grant access for Images, Videos, and Audio when prompted.
   - On **Android 12 and older**: Grant Read External Storage permission.
   - **Why?** WABackupPro uses Android's `MediaStore` API to locate files in your `WhatsApp Business` folder.
2. **Notification Permission**: Grant permission to show notifications so you can monitor background backups.

---

## ☁️ Google Drive Setup

To connect your Google Drive account:

1. Navigate to the **Backup Dashboard** (first tab).
2. Tap **Login to Drive**.
3. Select your Google Account from the system account picker.
4. Review the permission request:
   - WABackupPro requests **only** the `drive.file` scope. This allows the app to view and edit **only files that WABackupPro itself creates**. It cannot read your personal Drive documents or photos.

---

## ⚙️ Configuring Your Backups

Navigate to the **Settings** screen (third tab) to customize your backup rules:

### 1. Automated Schedule (Weekly Backup Time)
- Tap the time button (e.g., `02:00 AM`) to open the clock picker.
- Choose your preferred time. WABackupPro automatically schedules a background job using Android **WorkManager** to run every **Friday** at that exact time.

### 2. Wi-Fi Only Switch
- Leave **Backup on Wi-Fi Only** enabled (default) to save mobile data. Backups will pause automatically if you switch to cellular data.

### 3. Backup Categories
- Choose exactly which media types to back up:
  - 📄 **Documents**: PDF, DOCX, XLSX, TXT, ZIP.
  - 🖼️ **Images**: JPG, PNG, WebP, GIF.
  - 🎥 **Video**: MP4, 3GP, MKV.
  - 🎵 **Audio**: MP3, AAC, WAV tracks.
  - 🎙️ **Voice Notes**: PTT and Opus audio messages (separated automatically from music tracks).
- Use **Select All** or **Select None** for quick toggling.

### 4. Force Full Backup (Override)
- Incremental backups are enabled by default, using SHA-256 content hashing to skip files that haven't changed.
- Turn on **Force Full Backup** only if you need to re-upload all files from scratch.

---

## ▶️ Running a Backup

### Manual Backup
- On the **Backup Dashboard**, tap **Start Backup Now**.
- WABackupPro will:
  1. Scan your device for WhatsApp Business files matching your selected categories.
  2. Perform a SHA-256 delta scan to skip unchanged files.
  3. Create a dated folder on Google Drive (e.g., `WABackup_2026-08-14`).
  4. Stream your new and modified files safely to Drive.

### Real-Time Logs & Progress
- Watch the progress bar advance as files upload.
- Review the live activity log:
  - ✅ **Green check**: File uploaded successfully.
  - ℹ️ **Blue info**: File skipped because its content hasn't changed.
  - ❌ **Red cross**: File failed due to network or storage errors.

---

## 📜 History & Detail Drill-Down

### Viewing Past Backups
1. Open the **Backup History** tab (second tab).
2. Use the **Search Bar** to find backups by folder name or date.
3. Tap **Filter Chips** (`All`, `Success`, `Partial`, `Failed`) to narrow down results.

### Inspecting Per-File Outcomes & Retrying
1. Tap any historical backup card to open the **Backup Detail** screen.
2. View overall stats (total files, duration, success/fail breakdown) and a direct **View in Drive** link.
3. Scroll through the file list:
   - ⏭️ **SKIPPED**: File was already backed up and unchanged.
   - ✅ **SUCCESS**: File uploaded cleanly.
   - ❌ **FAILED**: File failed to upload.
4. **Single-File Retry**: Tap any **FAILED** item to view the error message dialog and tap **Retry This File** to re-upload that item immediately.

---

## 🎭 Exploring Demo Sandbox Mode

If you want to test the app without logging into Google Drive:
1. Tap **Explore Demo Sandbox** on the Backup Dashboard.
2. The app seeds realistic historical backup records and simulates file uploads so you can safely test the UI.

---

## 🛠️ Troubleshooting Common Problems

| Problem | Cause | Solution |
| :--- | :--- | :--- |
| **"Authentication Expired"** | Google OAuth token refreshed or revoked | Tap **Login to Drive** in Settings to re-authenticate. |
| **"Drive Storage Full"** | Your Google Drive account is out of space | Clear space on Google Drive or upgrade your storage plan. |

## ❓ Troubleshooting & Frequently Asked Questions

### 1. What happens when files are unchanged?
- WABackupPro computes a streaming SHA-256 hash for each local file. If the hash matches the stored Room entry from a prior backup, the file status is recorded as `SKIPPED`, saving upload bandwidth and Google Drive API quota.

### 2. What happens if an upload fails midway?
- If network connection is lost or a single file fails, WABackupPro logs the failure as `FAILED` for that specific item in the Room database while completing the remaining uploads.
- You can navigate to **History > Detail View**, tap the failed item, and trigger a single-file retry attempt directly.

### 3. Google Drive Login Fails
- Ensure your device has an active internet connection.
- Verify that your OAuth Client ID and SHA-1 fingerprint are correctly registered in the Google Cloud Console.

### 4. Background Backup Did Not Trigger
- Ensure battery optimization is disabled for WABackupPro in Android **Settings > Apps > Special App Access > Battery Optimization**.
- Verify that `RECEIVE_BOOT_COMPLETED` permission was granted to allow automatic schedule restoration on reboot.

---
*WABackupPro User Guide · Version 1.3.0*
