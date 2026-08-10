package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/** Une mise à jour disponible, telle que décrite par le manifeste `latest/version.json` du bucket
 * Cloudflare R2. Le format de ce manifeste (publié par la release.yml du dépôt) est :
 * `{"versionCode": 42, "versionName": "1.0.42", "apkUrl": "https://.../varotra.apk",
 *   "sha256": "...", "notesVersion": "..."}`. */
data class InfoMiseAJour(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val sha256: String,
    val notesVersion: String
)

/**
 * Mise à jour interne de l'application, sans passer par un store : l'APK signé est publié sur le
 * bucket R2 public `varotra` par le pipeline `release.yml` (voir la nouvelle étape « Publier sur
 * Cloudflare R2 »), et ce client se contente de comparer son propre `versionCode` à celui du
 * manifeste distant. Toujours **facultatif** : aucun appel automatique ne force l'installation,
 * l'utilisateur choisit quand télécharger et quand installer (voir SettingsScreen).
 *
 * Même stack qu'ailleurs dans le projet (OkHttp brut + `org.json.JSONObject`, voir LicenceManager
 * / FirebaseBackupManager) plutôt que Retrofit/Moshi, pour rester cohérent avec ces appels REST
 * ponctuels et légers.
 */
object UpdateManager {

    /** URL publique du bucket R2 « varotra » (lecture publique, aucune credential nécessaire ici —
     * seule la publication depuis release.yml exige les clés R2, gardées en secret GitHub). */
    const val BASE_URL = "https://pub-8562b104a83841e09966f730d5763b79.r2.dev"
    private const val VERSION_MANIFEST_URL = "$BASE_URL/latest/version.json"

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Fonction pure : interprète le corps JSON du manifeste et décide s'il représente une mise à
     * jour par rapport à [versionCodeActuel]. Séparée de l'appel réseau pour rester testable sans
     * serveur, comme [EtagereColors.normaliser].
     *
     * Renvoie null aussi bien pour « déjà à jour » que pour un manifeste invalide/incomplet : dans
     * les deux cas, il n'y a simplement rien à proposer à l'utilisateur.
     */
    fun parserManifesteMiseAJour(corpsJson: String, versionCodeActuel: Int): InfoMiseAJour? {
        return try {
            val json = JSONObject(corpsJson)
            val versionCodeDistant = json.optInt("versionCode", 0)
            val apkUrl = json.optString("apkUrl", "")
            if (versionCodeDistant <= versionCodeActuel || apkUrl.isBlank()) return null
            InfoMiseAJour(
                versionCode = versionCodeDistant,
                versionName = json.optString("versionName", ""),
                apkUrl = apkUrl,
                sha256 = json.optString("sha256", ""),
                notesVersion = json.optString("notesVersion", "")
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun verifierMiseAJourDisponible(versionCodeActuel: Int): Result<InfoMiseAJour?> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(VERSION_MANIFEST_URL).get().build()
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string()
                    if (!response.isSuccessful || body.isNullOrBlank() || body == "null") {
                        return@withContext Result.failure(IOException("HTTP ${response.code}"))
                    }
                    Result.success(parserManifesteMiseAJour(body, versionCodeActuel))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /** Empreinte SHA-256 d'un fichier local, en hexadécimal minuscule. Fonction pure de bas
     * niveau (aucun accès réseau), testable directement sur un fichier temporaire. */
    fun calculerSha256(fichier: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        fichier.inputStream().use { flux ->
            val buffer = ByteArray(8 * 1024)
            var lus: Int
            while (flux.read(buffer).also { lus = it } != -1) {
                digest.update(buffer, 0, lus)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /** Fichier de destination du téléchargement, dans le stockage interne (couvert par
     * `files-path` dans file_paths.xml, indispensable pour le FileProvider utilisé ensuite). */
    fun fichierApkTelecharge(context: Context): File = File(context.filesDir, "mise_a_jour.apk")

    suspend fun telechargerApk(
        context: Context,
        info: InfoMiseAJour,
        onProgress: (Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        val destination = fichierApkTelecharge(context)
        try {
            val request = Request.Builder().url(info.apkUrl).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("HTTP ${response.code}"))
                }
                val corps = response.body ?: return@withContext Result.failure(IOException("Réponse vide"))
                val totalOctets = corps.contentLength()
                corps.byteStream().use { entree ->
                    destination.outputStream().use { sortie ->
                        val buffer = ByteArray(8 * 1024)
                        var telecharges = 0L
                        var lus: Int
                        while (entree.read(buffer).also { lus = it } != -1) {
                            sortie.write(buffer, 0, lus)
                            telecharges += lus
                            if (totalOctets > 0) onProgress(telecharges.toFloat() / totalOctets)
                        }
                    }
                }
            }

            if (info.sha256.isNotBlank() && !calculerSha256(destination).equals(info.sha256, ignoreCase = true)) {
                destination.delete()
                return@withContext Result.failure(IOException("EMPREINTE_INVALIDE"))
            }

            Result.success(destination)
        } catch (e: Exception) {
            destination.delete()
            Result.failure(e)
        }
    }

    /** Sur Android 8+ (API 26+), installer depuis une app tierce exige une autorisation par-app
     * distincte du simple `<uses-permission android:name="REQUEST_INSTALL_PACKAGES">` déclaré au
     * manifeste — celui-ci ne fait qu'autoriser l'app à LA DEMANDER. */
    fun autorisationInstallationAccordee(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun intentParametresAutorisationInstallation(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))

    /** Lance l'installateur système sur l'APK téléchargé, via FileProvider (Android 7+ interdit de
     * passer un chemin file:// direct à un Intent d'un autre processus). L'appelant doit avoir
     * vérifié au préalable [autorisationInstallationAccordee]. */
    fun lancerInstallation(context: Context, apkFile: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
