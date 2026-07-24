package com.example.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Vérification de la licence d'utilisation auprès de la base Firebase Realtime Database du
 * DÉVELOPPEUR (une seule base, la nôtre — le client n'a strictement rien à configurer).
 *
 * Fonctionnement voulu :
 *  1. à l'installation, l'app est VERROUILLÉE et affiche son "ID d'installation" (6 chiffres) ;
 *  2. le client paie et communique cet ID ;
 *  3. le développeur crée le nœud correspondant dans la console Firebase ;
 *  4. le client appuie sur « Vérifier mon activation » : l'app lit son nœud et se déverrouille.
 *
 * Structure attendue côté Firebase (`/licences/<installationId>`) :
 * ```json
 * {
 *   "actif": true,
 *   "expiration": 1790000000000,        // millis, 0 ou absent = licence sans échéance
 *   "epicerie": "Épicerie Koto",        // libellé affiché à l'activation
 *   "note": "payé le 12/03 - Mvola"
 * }
 * ```
 *
 * Règles de sécurité à poser sur cette base (les clients LISENT leur licence, ils ne peuvent
 * jamais l'écrire — sinon n'importe qui s'auto-activerait) :
 * ```json
 * {
 *   "rules": {
 *     "licences":  { "$id":    { ".read": true, ".write": false } },
 *     "backups":   { "$token": { ".read": true, ".write": true  } }
 *   }
 * }
 * ```
 *
 * Tolérance hors-ligne : une épicerie n'a pas forcément du réseau tous les jours. Une licence
 * déjà validée reste donc valable [GRACE_PERIOD_MS] après la dernière vérification réussie ;
 * au-delà, l'app redemande une vérification en ligne.
 */
object LicenceManager {

    /**
     * Base Firebase du développeur. C'est LA seule base à renseigner avant publication (elle est
     * volontairement en dur : le client ne doit jamais avoir à créer ni saisir quoi que ce soit).
     * Format attendu : "https://<projet>-default-rtdb.firebaseio.com".
     */
    const val CENTRAL_DATABASE_URL = "https://varotra-licences-default-rtdb.firebaseio.com"

    /** Durée pendant laquelle une licence déjà validée reste acceptée sans réseau. */
    private const val GRACE_PERIOD_MS = 30L * 24 * 60 * 60 * 1000 // 30 jours

    /** Au-delà de ce délai on retente une vérification en ligne en tâche de fond au démarrage. */
    const val RECHECK_INTERVAL_MS = 3L * 24 * 60 * 60 * 1000 // 3 jours

    const val ETAT_ACTIVE = "ACTIVE"
    const val ETAT_EXPIREE = "EXPIREE"
    const val ETAT_SUSPENDUE = "SUSPENDUE"
    const val ETAT_INCONNUE = "INCONNUE"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    data class Licence(
        val etat: String,
        val expiration: Long = 0L,
        val epicerie: String = "",
        val note: String = ""
    ) {
        val estActive: Boolean get() = etat == ETAT_ACTIVE
    }

    /**
     * Interroge la base centrale pour l'ID d'installation donné.
     *
     * - succès : [Result] portant la [Licence] telle que le serveur la décrit ;
     * - nœud absent (jamais payé, ou ID mal communiqué) : [Licence] avec [ETAT_INCONNUE] ;
     * - panne réseau : [Result.failure], l'appelant décide alors s'il applique la tolérance
     *   hors-ligne (voir [estUtilisableHorsLigne]).
     */
    suspend fun verifierLicence(installationId: String): Result<Licence> = withContext(Dispatchers.IO) {
        try {
            val url = "${CENTRAL_DATABASE_URL.trimEnd('/')}/licences/$installationId.json"
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(java.io.IOException("HTTP ${response.code}"))
                }
                // La REST API de Realtime Database répond 200 avec le corps littéral "null"
                // quand le chemin n'existe pas encore : c'est une licence non créée, pas une panne.
                if (body.isNullOrBlank() || body == "null") {
                    return@withContext Result.success(Licence(etat = ETAT_INCONNUE))
                }
                val json = JSONObject(body)
                val actif = json.optBoolean("actif", false)
                val expiration = json.optLong("expiration", 0L)
                val epicerie = json.optString("epicerie", "")
                val note = json.optString("note", "")
                val etat = when {
                    !actif -> ETAT_SUSPENDUE
                    expiration > 0L && expiration < System.currentTimeMillis() -> ETAT_EXPIREE
                    else -> ETAT_ACTIVE
                }
                Result.success(Licence(etat, expiration, epicerie, note))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Décide, sans réseau, si l'app doit rester déverrouillée : la dernière vérification en ligne
     * doit avoir conclu à une licence active, dater de moins de [GRACE_PERIOD_MS], et l'échéance
     * éventuelle ne doit pas être dépassée.
     */
    fun estUtilisableHorsLigne(etat: String, derniereVerification: Long, expiration: Long): Boolean {
        if (etat != ETAT_ACTIVE) return false
        val now = System.currentTimeMillis()
        if (expiration > 0L && expiration < now) return false
        if (derniereVerification <= 0L) return false
        return now - derniereVerification <= GRACE_PERIOD_MS
    }
}
