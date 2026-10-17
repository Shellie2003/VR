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
 *  1. à l'installation, l'app est VERROUILLÉE et affiche son "ID d'installation" (6 chiffres)
 *     et son "code appareil" (dérivé d'ANDROID_ID, stable pour le téléphone physique) ;
 *  2. le client paie et communique les deux ;
 *  3. le développeur crée le nœud de licence dans la console Firebase, avec le code appareil
 *     dans la liste `appareils` ;
 *  4. le client appuie sur « Vérifier mon activation » : l'app lit son nœud et se déverrouille.
 *
 * Structure attendue côté Firebase (`/licences/<numéroDeLicence>` — le numéro de licence est
 * l'ID d'installation du premier appareil du client) :
 * ```json
 * {
 *   "actif": true,
 *   "expiration": 1790000000000,        // millis, 0 ou absent = licence sans échéance
 *   "epicerie": "Épicerie Koto",        // libellé affiché à l'activation
 *   "note": "payé le 12/03 - Mvola",
 *   "recoveryCode": "VRT-A3F9-K2M7-QP4X",  // copie du code de récupération des sauvegardes
 *   "maxAppareils": 1,                  // information de gestion (le nombre vendu au client)
 *   "transferts": 0,                    // compteur tenu à la main : un client qui « perd » son
 *                                       // téléphone trois fois dans l'année se voit tout de suite
 *   "appareils": {
 *     "a3f29b10c44de5f6": { "nom": "Téléphone de Koto", "depuis": 1790000000000 }
 *   }
 * }
 * ```
 *
 * La liste `appareils` est la clé du dispositif anti-« fausse perte » : seul un appareil dont le
 * code figure dans la liste est ACTIVE. Elle n'est modifiable QUE par le développeur dans la
 * console (règles en lecture seule) — c'est un choix délibéré : un client qui prétend avoir perdu
 * son téléphone pour équiper un second poste doit passer par le fournisseur, qui retire l'ancien
 * code en ajoutant le nouveau. L'ancien téléphone, retiré de la liste, se verrouille de lui-même
 * à sa prochaine vérification périodique ([ETAT_TRANSFEREE]). Mentir ne rapporte plus d'appareil
 * supplémentaire. Un nœud SANS champ `appareils` reste traité comme valide (licences créées avant
 * cette version).
 *
 * Règles de sécurité à poser sur cette base (les clients LISENT leur licence, ils ne peuvent
 * jamais l'écrire — sinon n'importe qui s'auto-activerait ou s'ajouterait à la liste
 * `appareils`) ; les sauvegardes vivent dans un AUTRE projet Firebase, voir
 * [FirebaseBackupManager] :
 * ```json
 * {
 *   "rules": {
 *     "licences": { "$id": { ".read": true, ".write": false } }
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
    const val CENTRAL_DATABASE_URL = "https://varotra-8c4d8-default-rtdb.asia-southeast1.firebasedatabase.app/"

    /** Durée pendant laquelle une licence déjà validée reste acceptée sans réseau. */
    private const val GRACE_PERIOD_MS = 30L * 24 * 60 * 60 * 1000 // 30 jours

    /** Au-delà de ce délai on retente une vérification en ligne en tâche de fond au démarrage. */
    const val RECHECK_INTERVAL_MS = 3L * 24 * 60 * 60 * 1000 // 3 jours

    const val ETAT_ACTIVE = "ACTIVE"
    const val ETAT_EXPIREE = "EXPIREE"
    const val ETAT_SUSPENDUE = "SUSPENDUE"
    const val ETAT_INCONNUE = "INCONNUE"

    /** La licence existe et est payée, mais CET appareil n'est pas (ou plus) dans sa liste. */
    const val ETAT_TRANSFEREE = "TRANSFEREE"

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
     * Interroge la base centrale pour le numéro de licence donné et vérifie que [deviceId]
     * (le code appareil canonique, voir [DeviceIdentity.rawId]) figure bien dans la liste
     * `appareils` du nœud.
     *
     * - succès : [Result] portant la [Licence] telle que le serveur la décrit ;
     * - nœud absent (jamais payé, ou numéro mal communiqué) : [Licence] avec [ETAT_INCONNUE] ;
     * - nœud présent mais appareil hors liste : [ETAT_TRANSFEREE] ;
     * - panne réseau : [Result.failure], l'appelant décide alors s'il applique la tolérance
     *   hors-ligne (voir [estUtilisableHorsLigne]).
     */
    suspend fun verifierLicence(numeroLicence: String, deviceId: String): Result<Licence> = withContext(Dispatchers.IO) {
        try {
            val url = "${CENTRAL_DATABASE_URL.trimEnd('/')}/licences/${numeroLicence.trim()}.json"
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

                // Liste des appareils autorisés. Absente ou vide = licence d'avant cette version,
                // acceptée telle quelle pour ne pas verrouiller les clients existants ; le
                // fournisseur la remplira à leur prochain contact.
                val appareils = json.optJSONObject("appareils")
                val listeRenseignee = appareils != null && appareils.length() > 0

                // Comparaison TOLÉRANTE, pas une égalité stricte de clé.
                //
                // Le code appareil est affiché au client sous la forme "A3F2-9B10-C44D-E5F6"
                // (majuscules, tirets) — c'est ce qu'il lit et transmet tel quel. Le fournisseur,
                // en le recopiant dans la console Firebase, a un réflexe naturel : coller
                // exactement ce qu'il a reçu, tirets et majuscules compris, plutôt que d'appliquer
                // mentalement la conversion documentée dans le guide. Avec une comparaison stricte
                // (`appareils.has(deviceId)`), cette faute de frappe la plus probable du monde
                // refusait purement et simplement un client qui avait pourtant tout fait "correct"
                // de son point de vue. On normalise donc les deux côtés (minuscules, on ne garde
                // que les caractères hexadécimaux) avant de comparer.
                val deviceIdNormalise = normaliserCodeAppareil(deviceId)
                var appareilAutorise = false
                if (appareils != null) {
                    val cles = appareils.keys()
                    while (cles.hasNext()) {
                        // JSONObject#keys() renvoie Iterator<String> sur Android comme dans
                        // l'implémentation JVM utilisée par les tests Robolectric ; le cast
                        // couvre les deux sans dépendre d'un typage générique précis.
                        val cle = cles.next() as String
                        if (normaliserCodeAppareil(cle) == deviceIdNormalise) {
                            appareilAutorise = true
                            break
                        }
                    }
                }

                val etat = when {
                    !actif -> ETAT_SUSPENDUE
                    expiration > 0L && expiration < System.currentTimeMillis() -> ETAT_EXPIREE
                    listeRenseignee && !appareilAutorise -> ETAT_TRANSFEREE
                    else -> ETAT_ACTIVE
                }
                Result.success(Licence(etat, expiration, epicerie, note))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Ramène un code appareil à sa forme comparable : minuscules, uniquement les caractères
     * hexadécimaux. Que la source soit le code brut ([DeviceIdentity.rawId], déjà propre) ou une
     * clé saisie à la main dans la console Firebase (avec tirets, espaces ou majuscules, comme le
     * client le lit sur son écran), les deux convergent vers la même chaîne — la comparaison ne
     * dépend donc plus d'une transcription manuelle parfaite.
     */
    private fun normaliserCodeAppareil(code: String): String =
        code.lowercase().filter { it in "0123456789abcdef" }

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
