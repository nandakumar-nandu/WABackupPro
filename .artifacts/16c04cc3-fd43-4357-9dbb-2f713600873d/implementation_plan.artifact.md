# Implementation Plan - Commit 3: Google Drive API Integration

Implement Google Sign-In and core Drive API operations (folder creation and file upload) with a focus on minimal permissions and robust error handling.

## User Review Required

> [!IMPORTANT]
> **OAuth Scopes**: I will use `DriveScopes.DRIVE_FILE`. This scope allows the app to access only the files it has created, which is the "principle of least privilege" requested. If we need access to the `appDataFolder` specifically, I would use `DRIVE_APPDATA`, but `DRIVE_FILE` is more flexible for user-visible backups if we want them to see the folder. I'll stick with `DRIVE_FILE` for now.

> [!NOTE]
> **Google Cloud Console**: The user needs to ensure their `SHA-1` fingerprint is registered in the Google Cloud Console for the OAuth 2.0 Client ID to work during testing.

## Proposed Changes

### Data Layer (Remote)

#### [NEW] [DriveClient.kt](file:///D:/projects/WABackupPro/app/src/main/java/com/wabackuppro/data/remote/DriveClient.kt) (Renaming/Replacing `GoogleDriveClient.kt`)
- Use `GoogleSignIn` for authentication.
- Initialize `com.google.api.services.drive.Drive` using `GoogleAccountCredential`.
- Implement:
    - `signInIntent()`: Returns the Intent for the sign-in activity.
    - `handleSignInResult(data: Intent)`: Processes the sign-in result.
    - `signOut()`: Revokes access.
    - `createFolder(name: String)`: Creates a folder in Drive.
    - `uploadFile(filePath: String, folderId: String, mimeType: String)`: Uploads a file.
- Add detailed comments for all Drive API parameters.

### UI & ViewModel

#### [MODIFY] [MainViewModel.kt](file:///D:/projects/WABackupPro/app/src/main/java/com/wabackuppro/ui/main/MainViewModel.kt)
- Add `DriveClient` instance.
- Add `testUpload()` function.
- Add state for "Is Logged In".

#### [MODIFY] [BackupFragment.kt](file:///D:/projects/WABackupPro/app/src/main/java/com/wabackuppro/ui/main/BackupFragment.kt)
- Add a "Login to Drive" button or update the "Start Backup" flow to check for login.
- Add a dedicated "Test Upload" button (hardcoded to upload a small dummy file or the first found file).
- Implement `ActivityResultLauncher` for Google Sign-In.

### Documentation

#### [MODIFY] [CHANGELOG.md](file:///D:/projects/WABackupPro/CHANGELOG.md)
- Add version `0.3.0` entry.

#### [MODIFY] [README.md](file:///D:/projects/WABackupPro/README.md)
- Add step-by-step instructions for Google Cloud Console setup.

#### [MODIFY] [WALKTHROUGH.md](file:///D:/projects/WABackupPro/WALKTHROUGH.md)
- Fill in the "Google Drive Setup" section.

#### [MODIFY] [SCREENTOUR.md](file:///D:/projects/WABackupPro/SCREENTOUR.md)
- Update with details about the Google Sign-In screen/flow.

## Verification Plan

### Automated Tests
- Verify compilation after adding new Drive API calls.

### Manual Verification
- **Sign-In**: Click "Login" -> Choose Google Account -> Verify success log.
- **Sign-Out**: Verify that access is revoked.
- **Test Upload**: Click "Test Upload" -> Verify folder creation in Drive -> Verify file appears in Drive -> Check logs for File ID.
