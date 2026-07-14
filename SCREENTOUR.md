# Screen Tour: WABackupPro

This document gives an overview of the screens configured in the initial scaffolding of WABackupPro.

## Planned Screens

### 1. Backup Dashboard (Main Screen)
- **Purpose**: Displays the status of the automated backup service, shows the date of the last run, and provides a manual trigger button.
- **Visual Structure**:
  - **Header Toolbar**: App title "WABackupPro".
  - **Status Card**: Employs a cloud indicator showing states like "No backup run yet", "Backup in progress...", or "Backup complete".
  - **Action Button**: Large "Start Backup Now" button with an icon.
  - **Logs Section**: Live RecyclerView showing recent activity logs (e.g., connection logs, upload confirmations, warnings).

### 2. Backup History Screen
- **Purpose**: Displays a list of all past backups (both successful and failed runs) retrieved from the local Room database.
- **Visual Structure**:
  - Paged list showing timestamp, backup file size, status badge (Success/Failure in color coding), and elapsed upload time.

### 3. Settings Screen
- **Purpose**: Configuration screen for authentication, constraints, and scheduling.
- **Visual Structure**:
  - **Account Integration**: Google Account authorization/sign-out buttons.
  - **Frequency Controls**: Dropdown/options for scheduling frequency (e.g., every 6 hours, daily, weekly).
  - **Network Constraint Switch**: Toggle for "Backup only on Wi-Fi".
  - **Battery Constraint Switch**: Toggle for "Backup only when charging".

## Screen Navigation Map

```mermaid
graph TD
    Launcher([Launcher Icon]) --> MainActivity[MainActivity]
    MainActivity -->|Default Screen| BackupFragment[Backup Dashboard]
    MainActivity -->|Select Tab 2| HistoryFragment[Backup History]
    MainActivity -->|Select Tab 3| SettingsFragment[Settings Configuration]
    
    BackupFragment -->|Click Start Backup| LogView[ViewModel Updates Logs List]
    SettingsFragment -->|Click Login| GoogleConsent[Google OAuth Consent Flow]
```
