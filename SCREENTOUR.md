> [!NOTE]
> **Documentation Consolidation Notice**: The content of this document has been consolidated into the authoritative [USER_GUIDE.md](USER_GUIDE.md) (for end-user screen tours and usage instructions) and [ARCHITECTURE.md](ARCHITECTURE.md) (for developer screen navigation design). This file is preserved for historical reference.

# Screen Tour: WABackupPro

This document gives an overview of the screens configured in WABackupPro version 1.3.0.

## Complete Screens

### 1. Backup Dashboard (Main Screen)
- **Purpose**: Displays the status of the automated backup service, shows the date of the last run, and provides a manual trigger button.
- **Visual Structure**:
  - **Header Toolbar**: App title "WABackupPro".
  - **Status Card**: Employs a cloud indicator showing states like "No backup run yet", "Analyzing changed files...", "Uploading...", or "All files up to date!". Includes Material3 animations for smooth transitions.
  - **Action Button**: Large "Start Backup Now" button.
  - **Retry Button**: Appears in red if the backup halts due to an error (e.g. Quota Exceeded or Auth Expired).
  - **Progress Indicators**: 
    - **Linear Progress Bar**: Animates as files are uploaded.
    - **Status Labels**: Shows "Uploading X of Y files", skipped files count ("Skipped Z unchanged files"), and the name of the current file (e.g., `msgstore.db.crypt14`).
  - **Auth & Test Row**: 
    - **Login to Drive**: Triggers the Google Sign-In overlay. Changes to "Sign Out (email)" once authenticated.
    - **Test Upload**: Enabled only after login. Uploads a dummy file to verify end-to-end connectivity.
  - **Logs Section**: Displays real-time progress, including success (`✅`), failure (`❌`), or skipped (`ℹ️`) badges for every file.

### 2. Google Sign-In & Permissions Flow
- **Overlay**: The standard system-provided Google Account picker.
- **Scopes Request**: Clearly informs the user that the app wants to "See, edit, create, and delete only the specific Google Drive files you use with this app."
- **Rationale Dialog**: An `AlertDialog` that appears if the user previously denied permissions, explaining why access to media is required.
- **System Prompt**: The standard Android permission request dialog.
- **Denied Message**: A `Snackbar` with a "Settings" button that appears if permissions are permanently denied, allowing users to manually enable them.

### 3. Backup Notification (Foreground Service)
- **Purpose**: Shown automatically when a scheduled WorkManager background backup is executing.
- **Visual Structure**:
  - **Ongoing Badge**: The notification is persistent and cannot be swiped away while the backup is active.
  - **Progress Information**: Displays the current status of the upload (e.g. "Uploading msgstore.db.crypt14 (2 of 5)...").
  - **Service Priority**: Elevates the background worker to avoid being terminated by OS battery optimizations (Doze mode).

### 4. Backup History Screen & Interactive Search Filters
- **Purpose**: Displays a comprehensive, chronological list of all past backups with real-time search and status filtering.
- **Visual Structure**:
  - **Search Input Bar**: An outlined text field (`et_search_history`) allowing live keyword or date filtering.
  - **Status Filter Chips**: A horizontally scrollable `ChipGroup` featuring 4 selectable chips (`All`, `Success`, `Partial Failures`, `Failed`).
  - **RecyclerView**: A scrollable list displaying individual cards for each backup job.
  - **Record Card**: Shows the formatted timestamp (e.g., Jul 18, 2026), file processing metrics (Success: 150, Failed: 0), total upload duration, and per-category breakdown text.
  - **Action Link**: An "Open in Drive" text button that launches a browser or the Drive app to view the backed-up files.

### 5. Backup Detail Screen & Single-File Retry Dialog
- **Purpose**: Provides drill-down inspection of individual file execution outcomes for any selected historical backup.
- **Visual Structure**:
  - **Header Bar**: Back navigation button and screen title.
  - **Summary Card**: Displays parent backup execution date, overall status badge, processing metrics, category count summary, and a direct Google Drive link button.
  - **File Outcomes List**: Displays a list of file result cards showing status icons (`✅ SUCCESS`, `❌ FAILED`, `⏭️ SKIPPED`), filename, category tag, and file size.
  - **Failed Item Dialog**: Tapping a failed item opens an `AlertDialog` showing the exact exception message, equipped with an actionable "Retry This File" button.

### 6. Settings Screen & Selective Category Panel
- **Purpose**: The configuration hub for authentication, constraints, scheduling, category filtering, and delta detection overrides.
- **Visual Structure**:
  - **Schedule Controls**: A labeled row with a button triggering an Android `TimePickerDialog` to set the Friday backup time.
  - **Network Constraints**: A modern `MaterialSwitch` to enforce Wi-Fi-only uploads.
  - **Force Full Backup Switch**: A prominent `MaterialSwitch` control ("Force Full Backup (Bypass Delta Detection)").
  - **Category Selection Panel**:
    - **Header**: "Backup Categories".
    - **Shortcuts Row**: "Select All" and "Select None" text action buttons.
    - **Toggles**: 5 distinct Material3 switches for Documents, Images, Video, Audio, and Voice Notes.
  - **History Management**: An inline `EditText` field allowing the user to specify how many days to retain backup history.
  - **Account Integration**: Displays the currently signed-in Google email address, along with a prominent "Sign Out" button.
  - **Manual Trigger**: A distinct button at the bottom providing a quick shortcut to "Run Backup Now" without leaving the settings screen.

## Screen Navigation Map

```
[ Launcher Icon ]
       │
       ▼
 [ MainActivity ]
   ├──► Backup Dashboard (BackupFragment)
   ├──► Backup History (BackupHistoryFragment) ──► Backup Detail (BackupDetailFragment)
   └──► Settings Configuration (SettingsFragment)
```
