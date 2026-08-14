# Developer Guide: WABackupPro

Welcome to the developer documentation for **WABackupPro**. This guide provides practical instructions for setting up your environment, building, testing, navigating the codebase, and adding new features.

---

## 🛠️ Development Environment Setup

### Required Tools
- **Operating System**: Windows, macOS, or Linux.
- **Java Development Kit (JDK)**: JDK 17 (Ensure JDK 17 is selected in Android Studio under **Settings > Build, Execution, Deployment > Build Tools > Gradle > Gradle JDK**).
- **Android Studio**: Android Studio Iguana (2023.2.1) or newer.
- **Android SDK Components**:
  - Compile SDK: 34
  - Target SDK: 34
  - Min SDK: 26

### Environment Configuration
1. Clone the repository:
   ```bash
   git clone https://github.com/nandakumar-nandu/WABackupPro.git
   cd WABackupPro
   ```
2. Copy the template properties file:
   ```bash
   cp local.properties.example local.properties
   ```
3. Open `local.properties` and verify your SDK directory path:
   ```properties
   sdk.dir=C\:\\Users\\YOUR_USERNAME\\AppData\\Local\\Android\\Sdk
   google.web.client.id=YOUR_OAUTH_CLIENT_ID
   ```
4. Open the project in Android Studio and perform a **Gradle Sync**.

---

## 📦 Package & Project Structure

```
app/src/main/java/com/wabackuppro/
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt          # Active Room Database (v3, "wabackuppro_database")
│   │   ├── BackupDatabase.kt       # Legacy v1 DB (Deprecated migration reference)
│   │   ├── BackupRecordDao.kt      # Legacy v1 DAO (Deprecated migration reference)
│   │   ├── daos/                   # Active Room DAOs
│   │   │   ├── BackupFileEntryDao.kt
│   │   │   ├── BackupFileResultDao.kt
│   │   │   └── BackupRecordDao.kt
│   │   └── entities/               # Active Room Entities
│   │       ├── BackupFileEntry.kt
│   │       ├── BackupFileResult.kt
│   │       └── BackupRecord.kt
│   ├── remote/
│   │   └── DriveClient.kt          # Google Drive REST API v3 wrapper
│   └── repository/
│       └── BackupRepository.kt     # Legacy repository abstraction (Deprecated)
├── domain/
│   ├── models/                     # Core domain data models
│   │   ├── BackupCategory.kt
│   │   ├── BackupFile.kt
│   │   ├── BackupProgress.kt
│   │   └── BackupRecord.kt
│   └── usecases/                   # Business logic orchestrators
│       ├── DetectChangedFilesUseCase.kt  # SHA-256 delta calculation engine
│       └── RunBackupUseCase.kt           # Main execution workflow engine
├── receivers/
│   └── BootReceiver.kt             # Handles ACTION_BOOT_COMPLETED to restore schedules
├── ui/
│   ├── about/
│   │   └── AboutActivity.kt        # About, legal terms, and payment integration
│   ├── components/
│   │   └── StatusIndicator.kt      # Shared custom status view component
│   ├── history/
│   │   ├── BackupDetailFragment.kt # Drill-down view of per-file execution results
│   │   ├── BackupHistoryFragment.kt# History list view with search & chip filters
│   │   ├── FileResultAdapter.kt    # Adapter for per-file detail cards
│   │   └── HistoryAdapter.kt       # Adapter for history summary cards
│   ├── main/
│   │   ├── BackupFragment.kt       # Active backup dashboard
│   │   ├── LogsAdapter.kt          # Adapter for dashboard real-time activity logs
│   │   ├── MainActivity.kt         # Application shell & bottom navigation host
│   │   └── MainViewModel.kt        # Dashboard state & demo mode manager
│   └── settings/
│       └── SettingsFragment.kt     # Preferences, category toggles & schedule picker
├── utils/
│   ├── BackupScheduler.kt          # WorkManager periodic job scheduler
│   ├── FileScanner.kt              # Scoped Storage MediaStore scanner
│   └── NetworkUtils.kt             # Connectivity check helper
└── workers/
    └── BackupWorker.kt             # CoroutineWorker promoted to Foreground Service
```

---

## 🧪 Build & Test Execution

Use the system-appropriate Gradle wrapper command:

### Windows (Command Prompt / PowerShell)
```powershell
# Build Debug APK
.\gradlew.bat assembleDebug

# Run Unit Tests
.\gradlew.bat test

# Run Lint Checks
.\gradlew.bat lint
```

### macOS / Linux
```bash
# Build Debug APK
./gradlew assembleDebug

# Run Unit Tests
./gradlew test

# Run Lint Checks
./gradlew lint
```

---

## 📝 Coding & Commenting Guidelines

1. **Architecture Rule**: Active features should be written in clean MVVM style. Business logic belongs in `domain/usecases/`.
2. **Commenting Policy**:
   - Write clear KDoc for public classes, methods, and complex UseCases.
   - Explain *why* a business rule, path filter, or hash strategy exists.
   - Do not write trivial comments that simply restate the code.
3. **Legacy Code Rule**:
   - Mark inactive or deprecated classes with `@Deprecated` annotations explaining why the class is retained and what replaces it.

---

## 🔄 Documentation Synchronization Checklist

When adding or modifying a feature:
- [ ] Update [ARCHITECTURE.md](ARCHITECTURE.md) if structural boundaries change.
- [ ] Update [USER_GUIDE.md](USER_GUIDE.md) if user flows or settings change.
- [ ] Update [CHANGELOG.md](CHANGELOG.md) with a clean release entry.
- [ ] Run `.\gradlew.bat test` to confirm all unit tests pass before committing.

---
*WABackupPro Developer Documentation · Version 1.3.0*
