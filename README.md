# WABackupPro

![Kotlin](https://img.shields.io/badge/kotlin-%237F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white) ![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white) ![Google Drive API](https://img.shields.io/badge/Google%20Drive-4285F4?style=for-the-badge&logo=googledrive&logoColor=white) ![WorkManager](https://img.shields.io/badge/WorkManager-v2.9.0-green?style=for-the-badge) ![Room](https://img.shields.io/badge/Room-v2.6.1-blue?style=for-the-badge)

Automated background backups of WhatsApp Business databases and files to Google Drive.

## App Concept
**WABackupPro** is designed to provide secure, automated backups of WhatsApp Business database files (e.g., `msgstore.db.crypt14`) and media assets directly to a user's Google Drive account. Utilizing WorkManager for reliable background job execution, Room for storing audit history logs and SHA-256 delta manifests, and the Google Drive REST API, the application runs incrementally in the background without affecting daily productivity.

## Backup Categories Mapping Table

| Category | Extensions | MIME Types | WhatsApp Directory Path |
| :--- | :--- | :--- | :--- |
| **DOCUMENTS** | `.pdf`, `.docx`, `.xlsx`, `.pptx`, `.txt`, `.csv`, `.zip` | `application/pdf`, `application/msword`, `text/plain` | `WhatsApp Business/Media/WhatsApp Documents/` |
| **IMAGES** | `.jpg`, `.jpeg`, `.png`, `.webp`, `.gif` | `image/jpeg`, `image/png`, `image/webp` | `WhatsApp Business/Media/WhatsApp Images/` |
| **VIDEO** | `.mp4`, `.3gp`, `.mkv`, `.webm`, `.avi` | `video/mp4`, `video/3gpp`, `video/x-matroska` | `WhatsApp Business/Media/WhatsApp Video/` |
| **AUDIO** | `.mp3`, `.aac`, `.wav`, `.flac` | `audio/mpeg`, `audio/aac`, `audio/wav` | `WhatsApp Business/Media/WhatsApp Audio/` |
| **VOICE_NOTES** | `.opus`, `.m4a`, `.ogg` | `audio/opus`, `audio/ogg`, `audio/aac` | `WhatsApp Business/Media/WhatsApp Voice Notes/`, `PTT/` |

## Architecture

This application adheres to **MVVM + Clean Architecture** guidelines to decouple dependencies and make the codebase highly testable.

### App Architecture Layers Diagram
```mermaid
graph TD
    UI["UI Layer<br>(MainActivity, Fragment, components)"] -->|Observes state| VM["ViewModel Layer<br>(MainViewModel)"]
    VM -->|Triggers| UC["Use Cases Layer<br>(StartBackupUseCase / RunBackupUseCase)"]
    UC -->|Checks delta| DeltaUC["Delta Detection UseCase<br>(DetectChangedFilesUseCase)"]
    UC -->|Interacts| Repo["Repository Layer<br>(BackupRepository)"]
    Repo -->|Queries / Inserts| Local["Local Database<br>(Room Database / SQLite)"]
    Repo -->|Uploads / Authenticates| Remote["Remote Storage API<br>(Google Drive Client)"]
    Workers["WorkManager Background Job<br>(BackupWorker)"] -->|Triggers| UC
```

### Delta Detection & Incremental Backup Decision Tree
```mermaid
graph TD
    Start([Discovered Local File]) --> ForceCheck{Force Full Backup Enabled?}
    ForceCheck -- Yes --> Upload[Upload File to Google Drive]
    ForceCheck -- No --> DBCheck{Exists in BackupFileEntry DB?}
    DBCheck -- No (New File) --> CalcHash[Calculate SHA-256 Hash]
    DBCheck -- Yes --> CalcHash
    CalcHash --> HashCompare{SHA-256 Hash Matches DB?}
    HashCompare -- Yes (Unchanged) --> Skip[Skip Upload & Increment Skipped Count]
    HashCompare -- No (Modified) --> Upload
    Upload --> UpdateDB[Upsert BackupFileEntry Record to Room DB]
    UpdateDB --> NextFile([Process Next File])
    Skip --> NextFile
```

### Automatic WorkManager Scheduling Flow
```mermaid
graph TD
    Boot[Device Boot or App Launch] --> Schedule{BackupScheduler}
    Schedule -->|Calculate delay to Friday| Enqueue[PeriodicWorkRequest]
    Enqueue --> WM((WorkManager Engine))
    WM --> Constraint{Check Constraints<br/>(Wi-Fi, Battery)}
    Constraint -- Not Met --> Waiting[Wait for conditions]
    Constraint -- Met --> Worker[BackupWorker runs]
    Worker --> Foreground[Promote to Foreground Service]
    Worker --> DeltaCheck[Perform Delta Scan]
    DeltaCheck --> UseCase[RunBackupUseCase executes]
```

### Backup Workflow Flowchart
```mermaid
graph TD
    Start([Scheduled Job Triggered]) --> CheckNetwork{Wi-Fi Available?}
    CheckNetwork -- No --> Reschedule[Reschedule Backup]
    CheckNetwork -- Yes --> FetchData[Fetch WhatsApp Business Files]
    FetchData --> DeltaScan[Detect Changed Files via SHA-256]
    DeltaScan --> Authenticate{Authenticate Google Account}
    Authenticate -- Failed --> LogError[Log Error & Notify User]
    Authenticate -- Success --> Upload[Upload Changed Files to Drive]
    Upload -- Success --> SaveRecord[Save Record & Hashes to Room]
    Upload -- Failed --> SaveFailedRecord[Save Failed Record to Room]
    SaveRecord --> Complete([Backup Complete])
    SaveFailedRecord --> NotifyFail[Notify User]
```

### Room Database Entity-Relationship (ER) Diagram
```mermaid
erDiagram
    BACKUP_RECORDS {
        Long id PK "Auto-generated primary key"
        Long timestamp "Unix epoch in milliseconds"
        String folderName "Drive folder name"
        Int totalFiles "Total queued files"
        Int successCount "Successfully uploaded files"
        Int failCount "Failed files"
        String driveFolderLink "Web link to Drive folder"
        Long durationSeconds "Total backup time"
        String uploadedFilesManifest "JSON manifest reference"
    }

    BACKUP_FILE_ENTRIES {
        String filePath PK "Absolute filesystem path"
        String contentHash "SHA-256 hash string"
        Long lastModified "File modification epoch"
        String driveFileId "Google Drive file ID"
        Long lastBackedUpAt "Last backup epoch"
    }
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
    participant DCUC as DetectChangedFilesUseCase
    participant FS as FileScanner
    participant DC as DriveClient
    participant MS as MediaStore / Drive API

    User->>VM: Click "Start Backup"
    VM->>UC: execute(account, categories, forceFullBackup)
    UC->>FS: scanWhatsAppBusinessFiles(categories)
    FS->>MS: Query MediaStore
    MS-->>FS: File List
    FS-->>UC: List<BackupFile>
    UC->>DCUC: execute(scannedFiles)
    DCUC-->>UC: DeltaScanResult (New, Modified, Unchanged)
    UC->>DC: createFolder("WABackup_YYYY-MM-DD")
    DC->>MS: Drive API (POST Folder)
    MS-->>DC: folderId
    DC-->>UC: folderId
    loop For each new or modified file
        UC->>DC: uploadFile(file, folderId)
        DC->>MS: Drive API (POST File)
        MS-->>DC: fileId (Success/Retry)
        DC-->>UC: progressUpdate()
        UC-->>VM: Emit(BackupProgress with skippedFiles)
        VM-->>User: Update UI Progress Bar & Skipped Counter
    end
    UC-->>VM: emit(Complete)
    VM-->>User: Show Success Notification
```

## Setup Prerequisites

To set up and run this application locally, you will need:

1. **Android Studio**: Android Studio Iguana (2023.2.1) or newer.
2. **Android SDK**: API level 26 (Android 8.0) minimum, compiled and targeted to API level 34 (Android 14.0).
3. **Java Development Kit**: JDK 17 (set as Gradle JDK in Android Studio settings).

### 1. Google Cloud Console Setup
To enable Google Sign-In and Google Drive API integration:
1. Go to the [Google Cloud Console](https://console.cloud.google.com/).
2. Create a new Project (e.g., "WABackupPro").
3. Navigate to **APIs & Services > Library**.
4. Search for **Google Drive API** and click **Enable**.
5. Navigate to **APIs & Services > OAuth consent screen**.
   - Set User Type to **External**.
   - Add `.../auth/drive.file` scope (allows app to access only files it creates).
6. Navigate to **APIs & Services > Credentials**.
7. Click **Create Credentials** > **OAuth client ID**.
8. Select **Android** as the application type.
9. Enter your package name (`com.wabackuppro`) and your debug/release **SHA-1 certificate fingerprint**.
10. *(Optional for Web/Backend)* Create another OAuth client ID of type **Web application** and copy the Client ID. Place this in your `local.properties` file as `google.web.client.id`.

### 2. Local Environment Setup
1. Clone the repository:
   ```bash
   git clone https://github.com/nandakumar-nandu/WABackupPro.git
   ```
2. Create a `local.properties` file in the root project directory (use `local.properties.example` as a reference):
   ```properties
   google.web.client.id=YOUR_WEB_CLIENT_ID_HERE
   ```
3. Copy `.env.example` to `.env` if you need to configure additional runtime variables.
4. Sync Gradle and build the project in Android Studio.

## Contribution

This project is fully built and maintained. 🚀 
To report issues or suggest features, feel free to open a GitHub Issue!
