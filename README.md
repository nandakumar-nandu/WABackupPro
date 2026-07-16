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

## Setup Prerequisites

To set up and run this application locally, you will need:

1. **Android Studio**: Android Studio Hedgehog (2023.1.1) or newer.
2. **Android SDK**: API level 26 (Android 8.0) minimum, compiled and targeted to API level 34 (Android 14.0).
3. **Java Development Kit**: JDK 17 (set as Gradle JDK in Android Studio settings).
4. **Google Cloud Console Setup**:
   - Go to [Google Cloud Console](https://console.cloud.google.com/).
   - Create a new project.
   - Enable the **Google Drive API**.
   - Configure the **OAuth Consent Screen** (specify User Type as External/Internal, add scope `.../auth/drive.appdata` or `.../auth/drive.file`).
   - Create **OAuth 2.0 Client IDs** for Android (require SHA-1 signing fingerprint and package name `com.wabackuppro`).
5. **Local configuration**:
   - Copy `.env.example` to `.env` and fill in client parameters.
   - Configure local.properties with actual `google.client.id` and `google.client.secret`.
