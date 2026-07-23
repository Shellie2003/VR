package com.example.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Lightweight Firebase Realtime Database backup client using its public REST API directly over
 * OkHttp (same stack as OpenFoodFactsApi), so no Firebase SDK / Google Play Services / the
 * google-services Gradle plugin is required. This means the app keeps compiling normally even
 * when no Firebase project has been configured yet: the database URL is entered by the user at
 * runtime in Paramètres (no rebuild needed), and calls simply fail with a clear error if it's
 * left empty or misconfigured.
 *
 * Realtime Database is used instead of Cloud Storage because, since October 2024, Firebase
 * Storage requires the paid Blaze plan even for minimal usage, whereas Realtime Database (like
 * Firestore) is still fully usable on the free Spark plan with no billing account required.
 *
 * Requires the target Realtime Database's rules to allow read/write on path "backups", e.g.:
 *   {
 *     "rules": {
 *       "backups": { ".read": true, ".write": true }
 *     }
 *   }
 */
object FirebaseBackupManager {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /** Normalizes the user-entered Database URL and builds the REST endpoint for a backup node. */
    private fun backupUrl(databaseUrl: String, installationId: String): String {
        val base = databaseUrl.trim().trimEnd('/')
        return "$base/backups/$installationId.json"
    }

    suspend fun uploadBackup(databaseUrl: String, installationId: String, json: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            if (databaseUrl.isBlank()) return@withContext Result.failure(IllegalStateException("NOT_CONFIGURED"))
            try {
                val body = json.toRequestBody("application/json".toMediaTypeOrNull())
                val request = Request.Builder().url(backupUrl(databaseUrl, installationId)).put(body).build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        Result.success(Unit)
                    } else {
                        Result.failure(IOException("HTTP ${response.code}"))
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun downloadBackup(databaseUrl: String, installationId: String): Result<String> =
        withContext(Dispatchers.IO) {
            if (databaseUrl.isBlank()) return@withContext Result.failure(IllegalStateException("NOT_CONFIGURED"))
            try {
                val request = Request.Builder().url(backupUrl(databaseUrl, installationId)).get().build()
                client.newCall(request).execute().use { response ->
                    val text = response.body?.string()
                    // The REST API returns HTTP 200 with the literal body "null" when the path doesn't exist yet.
                    if (response.isSuccessful && !text.isNullOrBlank() && text != "null") {
                        Result.success(text)
                    } else {
                        Result.failure(IOException("HTTP ${response.code}"))
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
