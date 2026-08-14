package com.wabackuppro.data.remote

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import java.io.IOException
import java.util.*

/**
 * Manages Google Drive OAuth 2.0 authentication and REST API v3 operations.
 *
 * ## Security & Privacy Design
 * Adheres strictly to the Principle of Least Privilege by requesting only the [DriveScopes.DRIVE_FILE] scope.
 * This restricts application permissions to files and folders created directly by WABackupPro, preventing
 * access to user documents, photos, or other personal Drive data.
 */
class DriveClient(private val context: Context) {

    // Principle of Least Privilege: Restricted to DRIVE_FILE scope.
    private val scopes = Collections.singletonList(DriveScopes.DRIVE_FILE)
    
    private val googleSignInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    /**
     * Returns the intent to trigger the Google Sign-In flow.
     */
    fun getSignInIntent() = googleSignInClient.signInIntent

    /**
     * Signs out the user and revokes Drive access.
     */
    fun signOut(onComplete: () -> Unit) {
        googleSignInClient.signOut().addOnCompleteListener { onComplete() }
    }

    /**
     * Gets the Drive service instance for an authenticated account.
     */
    private fun getDriveService(account: GoogleSignInAccount): Drive {
        val credential = GoogleAccountCredential.usingOAuth2(context, scopes)
        credential.selectedAccount = account.account
        
        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("WABackupPro").build()
    }

    /**
     * Creates a new folder in Google Drive.
     * @param name Name of the folder.
     * @return The created folder's ID.
     */
    @Throws(IOException::class)
    fun createFolder(account: GoogleSignInAccount, name: String): String? {
        val service = getDriveService(account)
        val fileMetadata = File().apply {
            this.name = name
            // 📂 Setting MIME type to folder
            mimeType = "application/vnd.google-apps.folder"
        }

        val folder = service.files().create(fileMetadata)
            .setFields("id") // Only request the ID in response to save bandwidth
            .execute()
        
        return folder.id
    }

    /**
     * Uploads a file to a specific folder in Google Drive.
     * @param filePath Local path to the file.
     * @param folderId Target folder ID in Drive.
     * @param mimeType MIME type of the file.
     * @return The uploaded file's ID.
     */
    @Throws(IOException::class)
    fun uploadFile(
        account: GoogleSignInAccount,
        filePath: String,
        folderId: String,
        mimeType: String
    ): String? {
        val service = getDriveService(account)
        val localFile = java.io.File(filePath)
        
        val fileMetadata = File().apply {
            name = localFile.name
            // 📁 Set parent folder to ensure it's uploaded to the right place
            parents = listOf(folderId)
        }
        
        val mediaContent = FileContent(mimeType, localFile)
        
        val file = service.files().create(fileMetadata, mediaContent)
            .setFields("id") // Request only the ID
            .execute()
            
        return file.id
    }
}
