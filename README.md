# WABackupPro

Automated background backups of WhatsApp Business databases and files to Google Drive.

## App Concept
**WABackupPro** is designed to provide secure, automated backups of WhatsApp Business database files (e.g., `msgstore.db.crypt14`) and media assets directly to a user's Google Drive account. Utilizing WorkManager for reliable background job execution, Room for storing audit history logs, and the Google Drive REST API, the application runs incrementally in the background without affecting daily productivity.

## Architecture

This application adheres to **MVVM + Clean Architecture** guidelines to decouple dependencies and make the codebase highly testable.

### App Architecture Layers Diagram
```mermaid
graph TD
    UI["UI Layer<br>(MainActivity, Fragment, components)"] -->|Observes state| VM["ViewModel Layer<br>(MainViewModel)"]
    VM -->|Triggers| UC["Use Cases Layer<br>(StartBackupUseCase)"]
    UC -->|Interacts| Repo["Repository Layer<br>(BackupRepository)"]
    Repo -->|Queries / Inserts| Local["Local Database<br>(Room Database / SQLite)"]
    Repo -->|Uploads / Authenticates| Remote["Remote Storage API<br>(Google Drive Client)"]
    Workers["WorkManager Background Job<br>(BackupWorker)"] -->|Triggers| UC
```

### Backup Workflow Flowchart
```mermaid
graph TD
    Start([Scheduled Job Triggered]) --> CheckNetwork{Wi-Fi Available?}
    CheckNetwork -- No --> Reschedule[Reschedule Backup]
    CheckNetwork -- Yes --> FetchData[Fetch WhatsApp Business Database]
    FetchData --> Encrypt[Encrypt Backup File]
    Encrypt --> Authenticate{Authenticate Google Account}
    Authenticate -- Failed --> LogError[Log Error & Notify User]
    Authenticate -- Success --> Upload[Upload Backup to Drive]
    Upload -- Success --> SaveRecord[Save Success Record to Room]
    Upload -- Failed --> SaveFailedRecord[Save Failed Record to Room]
    SaveRecord --> Complete([Backup Complete])
    SaveFailedRecord --> NotifyFail[Notify User]
```

### Permission Request Flow
```mermaid
graph TD
    Start([Click Start Backup]) --> CheckPerm{Permissions Granted?}
    CheckPerm -- Yes --> RunScan[Scan WhatsApp Files]
    CheckPerm -- No --> ShowRationale{Show Rationale?}
    ShowRationale -- Yes --> RationaleDialog[Show Explanation Dialog]
    RationaleDialog --> Request[Request Permissions]
    ShowRationale -- No --> Request
    Request --> UserResponse{User Response}
    UserResponse -- Granted --> RunScan
    UserResponse -- Denied --> DeniedSnack[Show Denied Message]
    UserResponse -- PermDenied --> SettingsSnack[Show Settings Link]
```

### Full Backup Orchestration Sequence
```mermaid
sequenceDiagram
    participant User
    participant VM as MainViewModel
    participant UC as RunBackupUseCase
    participant FS as FileScanner
    participant DC as DriveClient
    participant MS as MediaStore / Drive API

    User->>VM: Click "Start Backup"
    VM->>UC: execute(account)
    UC->>FS: scanWhatsAppBusinessFiles()
    FS->>MS: Query MediaStore
    MS-->>FS: File List
    FS-->>UC: List<BackupFile>
    UC->>DC: createFolder("WABackup_YYYY-MM-DD")
    DC->>MS: Drive API (POST Folder)
    MS-->>DC: folderId
    DC-->>UC: folderId
    loop For each file
        UC->>DC: uploadFile(file, folderId)
        DC->>MS: Drive API (POST File)
        MS-->>DC: fileId (Success/Retry)
        DC-->>UC: progressUpdate()
        UC-->>VM: Emit(BackupProgress)
        VM-->>User: Update UI Progress Bar
    end
    UC-->>VM: emit(Complete)
    VM-->>User: Show Success Notification
```

## Setup Prerequisites

To set up and run this application locally, you will need:

1. **Android Studio**: Android Studio Hedgehog (2023.1.1) or newer.
2. **Android SDK**: API level 26 (Android 8.0) minimum, compiled and targeted to API level 34 (Android 14.0).
3. **Java Development Kit**: JDK 17 (set as Gradle JDK in Android Studio settings).
4. **Google Cloud Console Setup**:
   - **Step 1: Create Project**: Go to [Google Cloud Console](https://console.cloud.google.com/) and create a project named "WABackupPro".
   - **Step 2: Enable APIs**: Search for and enable the **Google Drive API**.
   - **Step 3: Configure Consent Screen**:
     - Set User Type to **External**.
     - Add `.../auth/drive.file` scope (allows app to access only files it creates).
   - **Step 4: Create Credentials**:
     - Create an **OAuth 2.0 Client ID** for **Android**.
     - Package name: `com.wabackuppro`.
     - SHA-1 fingerprint: Obtain via `./gradlew signingReport`.
5. **Local configuration**:
   - Copy `.env.example` to `.env` and fill in client parameters.
   - Configure local.properties with actual `google.client.id` and `google.client.secret`.
