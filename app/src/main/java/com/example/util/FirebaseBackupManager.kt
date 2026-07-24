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
 * google-services Gradle plugin is required — et donc aucune clé d'API Google embarquée dans
 * l'APK. Les sauvegardes partent par défaut vers [CENTRAL_BACKUP_DATABASE_URL] (la base du
 * développeur) ; un gérant peut toujours renseigner sa propre base dans Paramètres pour héberger
 * ses données lui-même.
 *
 * Realtime Database is used instead of Cloud Storage because, since October 2024, Firebase
 * Storage requires the paid Blaze plan even for minimal usage, whereas Realtime Database (like
 * Firestore) is still fully usable on the free Spark plan with no billing account required.
 *
 * Règles de sécurité à poser sur la base des sauvegardes. Noter le niveau : l'autorisation est
 * donnée SOUS `backups/$token`, jamais sur `backups` lui-même — sinon un seul GET sur
 * `/backups.json` récupérerait les données de tous les clients d'un coup. Le token étant un UUID
 * aléatoire jamais affiché à l'écran (voir AppPreferences.firebaseBackupToken), il faut le
 * connaître exactement pour atteindre une sauvegarde :
 *   {
 *     "rules": {
 *       "backups": {
 *         "$token": { ".read": true, ".write": true }
 *       }
 *     }
 *   }
 */
object FirebaseBackupManager {

    /**
     * Base Realtime Database du DÉVELOPPEUR dédiée aux sauvegardes des clients — volontairement
     * **distincte** de celle des licences ([com.example.util.LicenceManager.CENTRAL_DATABASE_URL]).
     *
     * Pourquoi deux projets Firebase séparés plutôt qu'un seul :
     *  - les sauvegardes sont ce qui remplit le quota (1 Go sur le plan gratuit), les licences ne
     *    pèsent que quelques octets. Si les deux partageaient la même base, une saturation des
     *    sauvegardes empêcherait aussi les nouveaux clients de **s'activer** — une panne
     *    commerciale, pas seulement technique ;
     *  - les règles de sécurité n'ont rien à voir : les licences sont en lecture seule pour les
     *    clients, les sauvegardes doivent être écrivables par eux ;
     *  - on peut migrer, purger ou faire payer les sauvegardes sans jamais toucher aux licences.
     *
     * Laisser cette constante vide désactive simplement la sauvegarde cloud par défaut : le client
     * garde alors la possibilité de renseigner sa propre base dans Paramètres.
     */
    const val CENTRAL_BACKUP_DATABASE_URL = "https://varotra-1528a-default-rtdb.asia-southeast1.firebasedatabase.app/"

    /**
     * URL réellement utilisée pour sauvegarder : celle saisie par le client dans Paramètres si
     * elle existe (cas rare d'un gérant qui veut ses données chez lui), sinon la base du
     * développeur. C'est ce qui permet à la sauvegarde cloud de fonctionner **dès l'installation**,
     * sans que le client ait à créer le moindre projet Firebase.
     */
    fun resolveDatabaseUrl(userDatabaseUrl: String): String =
        userDatabaseUrl.trim().ifBlank { CENTRAL_BACKUP_DATABASE_URL }

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
