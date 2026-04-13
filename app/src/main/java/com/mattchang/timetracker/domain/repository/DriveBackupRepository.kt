package com.mattchang.timetracker.domain.repository

import android.content.Intent

interface DriveBackupRepository {
    fun isSignedIn(): Boolean
    fun getSignedInEmail(): String?
    fun getSignInIntent(): Intent
    suspend fun handleSignInResult(data: Intent?): Boolean
    suspend fun signOut()
    suspend fun uploadBackup(csvContent: String): Result<Unit>
    suspend fun downloadBackup(): Result<String>
    fun getLastBackupTimestamp(): String?
}
