package com.example.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Lightweight Firebase Cloud Storage backup client using the public Storage REST API
 * directly over OkHttp (same stack as OpenFoodFactsApi), so no Firebase SDK / Google
 * Play Services / google-services Gradle plugin is required. This means the app keeps
 * compiling normally even when no Firebase project has been configured yet: the bucket
 * name is entered by the user at runtime in Paramètres (no rebuild needed), and calls
 * simply fail with a clear error if it's left empty or misconfigured.
 *
 * Requires the target Storage bucket's rules to allow read/write on path
 * "backups/{installationId}/database_backup.json", e.g.:
 *   match /backups/{installationId}/{file} {
 *     allow read, write: if true;
 *   }
 */
object FirebaseBackupManager {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun objectPath(installationId: String) = "backups/$installationId/database_backup.json"

    private fun encodedObjectPath(installationId: String): String =
        URLEncoder.encode(objectPath(installationId), "UTF-8")

    suspend fun uploadBackup(bucket: String, installationId: String, json: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            if (bucket.isBlank()) return@withContext Result.failure(IllegalStateException("NOT_CONFIGURED"))
            try {
                val url = "https://firebasestorage.googleapis.com/v0/b/$bucket/o" +
                    "?uploadType=media&name=${encodedObjectPath(installationId)}"
                val body = json.toRequestBody("application/json".toMediaTypeOrNull())
                val request = Request.Builder().url(url).post(body).build()
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

    suspend fun downloadBackup(bucket: String, installationId: String): Result<String> =
        withContext(Dispatchers.IO) {
            if (bucket.isBlank()) return@withContext Result.failure(IllegalStateException("NOT_CONFIGURED"))
            try {
                val url = "https://firebasestorage.googleapis.com/v0/b/$bucket/o/" +
                    "${encodedObjectPath(installationId)}?alt=media"
                val request = Request.Builder().url(url).get().build()
                client.newCall(request).execute().use { response ->
                    val text = response.body?.string()
                    if (response.isSuccessful && !text.isNullOrBlank()) {
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
