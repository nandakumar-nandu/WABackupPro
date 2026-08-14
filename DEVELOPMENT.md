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

## 📦 Target Package Structure (Clean Modular Monolith - Option A)

```
app/src/main/java/com/wabackuppro/
├── core/                           # Core configuration, common utilities & shared visual components
│   ├── config/                     # Shared Preference keys & constants
│   ├── common/                     # Common helpers (NetworkUtils)
│   └── ui/                         # Reusable custom UI components (StatusIndicator)
├── data/                           # Data layer implementations
│   ├── local/                      # Room Database (AppDatabase), entities & DAOs
│   │   ├── daos/                   # Active DAOs (BackupFileEntryDao, BackupFileResultDao, BackupRecordDao)
│   │   └── entities/               # Active Entities (BackupFileEntry, BackupFileResult, BackupRecord)
│   ├── remote/                     # Remote storage API wrappers (DriveClient)
│   └── repository/                 # Data repositories (BackupRepository - Deprecated)
├── domain/                         # Pure business logic and domain models
│   ├── models/                     # Data models (BackupFile, BackupProgress, BackupRecord, BackupCategory)
│   └── usecases/                   # Business use cases (DetectChangedFilesUseCase, RunBackupUseCase)
├── feature/                        # User-facing features and screens
│   ├── main/                       # MainActivity, BackupFragment, MainViewModel, LogsAdapter
│   ├── history/                    # BackupHistoryFragment, BackupDetailFragment, HistoryAdapter, FileResultAdapter
│   ├── settings/                   # SettingsFragment
│   └── about/                      # AboutActivity
└── background/                     # System integration & background execution
    ├── workers/                    # WorkManager workers (BackupWorker)
    ├── receivers/                  # System broadcast receivers (BootReceiver)
    └── utils/                      # File discovery & scheduling helpers (FileScanner, BackupScheduler)

app/src/test/java/com/wabackuppro/
├── domain/
│   ├── models/
│   │   └── BackupCategoryTest.kt             # Unit tests for category enum & display names
│   └── usecases/
│       └── DetectChangedFilesUseCaseTest.kt  # Unit tests for SHA-256 calculation & delta sorting
```

---

## ⚙️ Automated CI Pipeline Integration

GitHub Actions workflow `.github/workflows/ci.yml` validates code quality on every push and pull request:
1. **JDK Setup**: Configures JDK 17 environment.
2. **Unit Testing**: Runs `./gradlew test` (verifying SHA-256 delta detection & category models).
3. **Debug Build**: Compiles `./gradlew assembleDebug` to verify compilation integrity.

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
- [x] Update [ARCHITECTURE.md](ARCHITECTURE.md) if structural boundaries change.
- [x] Update [USER_GUIDE.md](USER_GUIDE.md) if user flows or settings change.
- [x] Update [CHANGELOG.md](CHANGELOG.md) with a clean release entry.
- [x] Run `.\gradlew.bat test` to confirm all unit tests pass before committing.

---
*WABackupPro Developer Documentation · Version 1.3.0*
