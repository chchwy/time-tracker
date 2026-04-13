package com.mattchang.timetracker.data.repository

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.InputStreamContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.mattchang.timetracker.domain.repository.DriveBackupRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Google Drive AppData backup implementation.
 *
 * SETUP REQUIRED before this will work:
 * 1. Create a project in Google Cloud Console (console.cloud.google.com)
 * 2. Enable the Google Drive API
 * 3. Create an OAuth 2.0 Android client ID:
 *    - Package name: com.mattchang.timetracker
 *    - SHA-1 fingerprint of your signing key (debug: `keytool -list -v -keystore ~/.android/debug.keystore`)
 * 4. No google-services.json needed — Google Sign-In works without Firebase.
 */
@Singleton
class DriveBackupRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : DriveBackupRepository {

    companion object {
        private const val BACKUP_FILE_NAME = "time_tracker_backup.csv"
        private const val PREFS_NAME = "drive_backup_prefs"
        private const val KEY_LAST_BACKUP = "last_backup_time"
        private val TIMESTAMP_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    }

    private val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
        .build()

    private fun signInClient() = GoogleSignIn.getClient(context, signInOptions)

    override fun isSignedIn(): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return false
        return GoogleSignIn.hasPermissions(account, Scope(DriveScopes.DRIVE_APPDATA))
    }

    override fun getSignedInEmail(): String? =
        GoogleSignIn.getLastSignedInAccount(context)?.email

    override fun getSignInIntent(): Intent = signInClient().signInIntent

    override suspend fun handleSignInResult(data: Intent?): Boolean {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            task.getResult(ApiException::class.java)
            true
        } catch (_: ApiException) {
            false
        }
    }

    override suspend fun signOut() = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { cont ->
            signInClient().signOut().addOnCompleteListener { cont.resume(Unit) }
        }
    }

    private fun getDriveService(): Drive? {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return null
        val credential = GoogleAccountCredential.usingOAuth2(
            context, listOf(DriveScopes.DRIVE_APPDATA)
        )
        credential.selectedAccount = account.account
        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("TimeTracker").build()
    }

    override suspend fun uploadBackup(csvContent: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val drive = getDriveService() ?: error("Not signed in")
            val bytes = csvContent.toByteArray(Charsets.UTF_8)
            val mediaContent = InputStreamContent("text/csv", ByteArrayInputStream(bytes))

            val existingId = findBackupFileId(drive)
            if (existingId != null) {
                drive.files().update(existingId, null, mediaContent).execute()
            } else {
                val meta = File().apply {
                    name = BACKUP_FILE_NAME
                    parents = listOf("appDataFolder")
                }
                drive.files().create(meta, mediaContent).execute()
            }

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putLong(KEY_LAST_BACKUP, System.currentTimeMillis()).apply()
        }
    }

    override suspend fun downloadBackup(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val drive = getDriveService() ?: error("Not signed in")
            val fileId = findBackupFileId(drive) ?: error("No backup found on Drive")
            val out = ByteArrayOutputStream()
            drive.files().get(fileId).executeMediaAndDownloadTo(out)
            out.toString("UTF-8")
        }
    }

    private fun findBackupFileId(drive: Drive): String? =
        drive.files().list()
            .setSpaces("appDataFolder")
            .setQ("name='$BACKUP_FILE_NAME'")
            .setFields("files(id)")
            .execute()
            .files
            .firstOrNull()?.id

    override fun getLastBackupTimestamp(): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val millis = prefs.getLong(KEY_LAST_BACKUP, 0L)
        if (millis == 0L) return null
        return Instant.ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
            .format(TIMESTAMP_FMT)
    }
}
