package com.example.desktop.maj

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.MessageDigest
import java.time.Duration
import java.util.zip.ZipInputStream

/**
 * Pourquoi la mise à jour ne peut pas se faire sur cette installation.
 *
 * Un énuméré et non un message : l'appelant doit traduire dans les trois langues, et un texte
 * fabriqué ici serait écrit une fois en français au milieu du code d'entrée/sortie.
 */
enum class ObstacleMaj {
    /** Lancé depuis les sources ou l'IDE : il n'y a pas de paquet à remplacer. */
    HORS_PAQUET,

    /** Système autre que Windows : seul Windows est empaqueté et automatisé aujourd'hui. */
    SYSTEME_NON_PRIS_EN_CHARGE,

    /**
     * Le dossier d'installation n'est pas modifiable par l'utilisateur courant — typiquement une
     * installation par l'installateur sous `C:\Program Files`. Détecté **avant** le
     * téléchargement : faire télécharger 90 Mo pour échouer ensuite serait inexcusable sur une
     * connexion payée au mégaoctet.
     */
    DOSSIER_PROTEGE
}

/**
 * Mise à jour interne du logiciel de bureau : téléchargement de l'archive publiée sur Cloudflare
 * R2, vérification de son empreinte, puis remplacement de l'installation par elle-même.
 *
 * ## Ce que ce service évite au gérant
 *
 * La version de bureau ne passe par aucun magasin d'applications. Sans ce service, chaque correctif
 * imposerait de retrouver le lien, télécharger une archive de ~90 Mo, désinstaller, réinstaller —
 * autrement dit une manipulation que personne ne fait, donc une boutique qui reste indéfiniment sur
 * sa première version. C'est exactement le défaut corrigé côté Android en section 77.
 *
 * ## Aucune dépendance ajoutée
 *
 * `java.net.http.HttpClient` est fourni par le JDK depuis la version 11, et ce module tourne sous
 * 21 : OkHttp n'apporterait rien ici qu'un mégaoctet de plus dans un paquet qui en pèse déjà 93.
 * La lecture du manifeste passe en revanche par `org.json`, comme côté Android — écrire un
 * analyseur JSON à la main pour cinq champs serait la mauvaise économie.
 *
 * ## La limite, dite franchement
 *
 * L'étape de remplacement ne peut être exécutée ni dans cet environnement de développement ni en
 * CI : les deux sont sous Linux et sans interface. Tout ce qui pouvait être vérifié par un test l'a
 * donc été dans [MiseAJourBureau] (comparaison de versions, lecture du manifeste, découverte du
 * dossier, texte exact du script). Ce qui reste ici est délibérément mince, et chaque échec laisse
 * l'installation en état de marche plutôt que de tenter de se rattraper.
 */
class ServiceMajBureau(
    private val urlManifeste: String = MiseAJourBureau.URL_MANIFESTE
) {

    /**
     * Construit **à la première requête** et non à la construction du service, et ce n'est pas une
     * micro-optimisation : c'est ce qui a planté l'écran « À propos » chez le gérant.
     *
     * `jpackage` fabrique un runtime Java réduit par `jlink`, dont `java.net.http` est absent par
     * défaut — le module est désormais déclaré dans `desktop/build.gradle.kts`, mais le défaut
     * valait d'être rendu inoffensif : la carte construit son service **au moment où elle
     * s'affiche** (`remember { ServiceMajBureau() }`), donc un champ initialisé dans le
     * constructeur touchait `java.net.http` dès l'ouverture de l'écran, bien avant tout clic.
     * Résultat : « À propos » ne s'ouvrait plus du tout, pour un composant dont on n'avait pas
     * encore besoin. Différé ici, seul le bouton « Vérifier » peut encore y buter — et il échoue
     * alors avec un message, ce que la gestion de [LinkageError] plus bas garantit.
     */
    private val client: HttpClient by lazy {
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build()
    }

    /**
     * Ce qui empêche la mise à jour sur cette installation, ou `null` si elle est possible.
     *
     * Appelé **avant** toute requête réseau : un logiciel qui annonce une nouvelle version puis
     * refuse de l'installer a fait perdre deux fois le temps de son utilisateur.
     */
    fun obstacle(racine: File?): ObstacleMaj? = when {
        !MiseAJourBureau.estWindows() -> ObstacleMaj.SYSTEME_NON_PRIS_EN_CHARGE
        racine == null -> ObstacleMaj.HORS_PAQUET
        !dossierModifiable(racine) -> ObstacleMaj.DOSSIER_PROTEGE
        else -> null
    }

    /**
     * Le dossier d'installation est-il modifiable ?
     *
     * `File.canWrite()` ne suffit pas sous Windows : il renvoie volontiers `true` sur un dossier
     * que le contrôle d'accès refuse en écriture réelle. On écrit donc un fichier témoin, seul test
     * qui dise la vérité — et on le supprime aussitôt.
     *
     * Ce qui est réellement vérifié est l'écriture dans le dossier **parent**, car c'est là que le
     * renommage de l'installation aura lieu : un dossier d'installation modifiable dont le parent
     * ne l'est pas ferait échouer le script à l'étape 2.
     */
    fun dossierModifiable(racine: File): Boolean {
        val parent = racine.parentFile ?: return false
        return try {
            val temoin = File(parent, ".varotra-maj-test")
            temoin.writeText("")
            temoin.delete()
            true
        } catch (e: Exception) {
            false
        }
    }

    /** Lit le manifeste distant et renvoie la mise à jour disponible, ou `null` si déjà à jour. */
    suspend fun verifier(versionCodeActuel: Int): Result<InfoMajBureau?> =
        withContext(Dispatchers.IO) {
            try {
                val requete = HttpRequest.newBuilder(URI.create(urlManifeste))
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build()
                val reponse = client.send(requete, HttpResponse.BodyHandlers.ofString())
                val corps = reponse.body()
                if (reponse.statusCode() !in 200..299 || corps.isNullOrBlank() || corps == "null") {
                    return@withContext Result.failure(IOException("HTTP ${reponse.statusCode()}"))
                }
                Result.success(MiseAJourBureau.parserManifeste(corps, versionCodeActuel))
            } catch (e: Exception) {
                Result.failure(e)
            } catch (e: LinkageError) {
                // Un `NoClassDefFoundError` n'est PAS une `Exception` : sans cette branche, un
                // module absent du runtime réduit traverse tout `catch (e: Exception)` et ferme
                // le logiciel. C'est exactement ce qui est arrivé à l'écran « À propos ».
                Result.failure(IOException(ERREUR_COMPOSANT_MANQUANT, e))
            }
        }

    /**
     * Télécharge l'archive, vérifie son empreinte, la décompresse à côté de l'installation, et
     * renvoie le dossier prêt à prendre la place de l'ancien.
     *
     * L'archive est **décompressée avant** la fermeture du logiciel, pour que le script n'ait plus
     * qu'un déplacement de dossier à faire : décompresser 90 Mo après la fermeture laisserait le
     * gérant devant un écran vide pendant une minute, sans rien lui dire de ce qui se passe.
     *
     * Une empreinte qui ne correspond pas **détruit le fichier** et échoue, au lieu d'installer une
     * archive tronquée par une coupure de réseau — sur le parc visé, les coupures sont la règle.
     */
    suspend fun telechargerEtPreparer(
        info: InfoMajBureau,
        racine: File,
        onProgression: (Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        val parent = racine.parentFile
            ?: return@withContext Result.failure(IOException("Dossier parent introuvable"))
        val archive = File(parent, "varotra-bureau-maj.zip")
        val nouveau = File(parent, MiseAJourBureau.NOM_DOSSIER_NOUVEAU)
        try {
            val requete = HttpRequest.newBuilder(URI.create(info.archiveUrl))
                .timeout(Duration.ofMinutes(30))
                .GET()
                .build()
            val reponse = client.send(requete, HttpResponse.BodyHandlers.ofInputStream())
            if (reponse.statusCode() !in 200..299) {
                return@withContext Result.failure(IOException("HTTP ${reponse.statusCode()}"))
            }
            val taille = reponse.headers().firstValueAsLong("content-length").orElse(-1L)
            reponse.body().use { entree ->
                archive.outputStream().use { sortie ->
                    copierAvecProgression(entree, sortie, taille, onProgression)
                }
            }

            if (info.sha256.isNotBlank() &&
                !calculerSha256(archive).equals(info.sha256, ignoreCase = true)
            ) {
                archive.delete()
                return@withContext Result.failure(IOException("EMPREINTE_INVALIDE"))
            }

            if (nouveau.exists()) nouveau.deleteRecursively()
            decompresser(archive, nouveau)
            archive.delete()

            Result.success(nouveau)
        } catch (e: Exception) {
            archive.delete()
            nouveau.deleteRecursively()
            Result.failure(e)
        } catch (e: LinkageError) {
            archive.delete()
            nouveau.deleteRecursively()
            Result.failure(IOException(ERREUR_COMPOSANT_MANQUANT, e))
        }
    }

    /**
     * Écrit le script de remplacement, le lance, et rend la main à l'appelant — à qui il reste à
     * **fermer le logiciel**, sans quoi le script attendra en vain puis abandonnera sans rien
     * casser.
     *
     * Le script est lancé avec sa sortie redirigée vers un journal, et c'est volontaire : si la
     * mise à jour échoue chez un gérant, ce fichier est la seule chose qui dira pourquoi. Sans lui,
     * on en serait à deviner à distance — ce que la section 47 a déjà payé une fois.
     */
    fun lancerRemplacement(racine: File, nouveau: File): Result<File> {
        return try {
            val lanceur = trouverLanceur(racine)
                ?: return Result.failure(IOException("Lanceur introuvable dans $racine"))
            val tempo = File(System.getProperty("java.io.tmpdir"))
            val script = File(tempo, "varotra-maj-bureau.cmd")
            val journal = File(tempo, "varotra-maj-bureau.log")
            script.writeText(
                MiseAJourBureau.scriptRemplacement(
                    pid = ProcessHandle.current().pid(),
                    racine = racine,
                    nouveau = nouveau,
                    nomLanceur = lanceur
                )
            )
            ProcessBuilder("cmd", "/c", script.absolutePath)
                .redirectOutput(ProcessBuilder.Redirect.to(journal))
                .redirectErrorStream(true)
                .start()
            Result.success(journal)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Le nom du lanceur, découvert et non écrit en dur : il vient de `packageName` dans
     * `desktop/build.gradle.kts`, et un renommage là-bas casserait silencieusement une valeur figée
     * ici — le script relancerait alors un exécutable inexistant, donc laisserait le gérant sans
     * logiciel ouvert après une mise à jour par ailleurs réussie.
     */
    private fun trouverLanceur(racine: File): String? =
        racine.listFiles()?.firstOrNull { it.isFile && it.name.endsWith(".exe", ignoreCase = true) }
            ?.name

    private fun copierAvecProgression(
        entree: InputStream,
        sortie: java.io.OutputStream,
        taille: Long,
        onProgression: (Float) -> Unit
    ) {
        val tampon = ByteArray(64 * 1024)
        var cumul = 0L
        var lus: Int
        while (entree.read(tampon).also { lus = it } != -1) {
            sortie.write(tampon, 0, lus)
            cumul += lus
            if (taille > 0) onProgression(cumul.toFloat() / taille)
        }
    }

    /** Empreinte SHA-256 en hexadécimal minuscule, comme celle calculée par la CI (`sha256sum`). */
    fun calculerSha256(fichier: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        fichier.inputStream().use { flux ->
            val tampon = ByteArray(64 * 1024)
            var lus: Int
            while (flux.read(tampon).also { lus = it } != -1) digest.update(tampon, 0, lus)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * Décompresse l'archive dans [destination].
     *
     * **Protection contre le Zip Slip**, et ce n'est pas théorique ici : une entrée nommée
     * `..\..\Windows\System32\…` ferait écrire l'archive n'importe où sur le disque. La règle déjà
     * posée en section 46 pour les archives de sauvegarde s'applique d'autant plus à une archive
     * téléchargée depuis le réseau. Contrairement à `ArchiveUtil`, on ne peut pas réduire les
     * entrées à leur dernier segment — l'arborescence `app/` et `runtime/` doit être conservée —
     * donc la vérification porte sur le chemin canonique résolu.
     */
    private fun decompresser(archive: File, destination: File) {
        destination.mkdirs()
        val racineAutorisee = destination.canonicalFile
        ZipInputStream(archive.inputStream().buffered()).use { zip ->
            var entree = zip.nextEntry
            while (entree != null) {
                val cible = File(destination, entree.name).canonicalFile
                if (!cible.path.startsWith(racineAutorisee.path + File.separator) &&
                    cible.path != racineAutorisee.path
                ) {
                    throw IOException("Entrée d'archive hors du dossier autorisé : ${entree.name}")
                }
                if (entree.isDirectory) {
                    cible.mkdirs()
                } else {
                    cible.parentFile?.mkdirs()
                    cible.outputStream().use { sortie -> zip.copyTo(sortie, 64 * 1024) }
                }
                zip.closeEntry()
                entree = zip.nextEntry
            }
        }
    }

    companion object {
        /**
         * Marque un échec dû à un composant **absent du runtime du paquet** (module JPMS non
         * déclaré), et non à une panne de réseau.
         *
         * Une sentinelle plutôt qu'un message, pour la raison de la section 56 : un seul libellé
         * pour deux causes fait attendre un réseau qui marchait. Ici, le gérant n'a rien à
         * attendre — c'est le paquet qu'il faut reconstruire, et le dire est la seule façon que
         * cela remonte jusqu'au fournisseur au lieu d'être pris pour une coupure.
         */
        const val ERREUR_COMPOSANT_MANQUANT = "COMPOSANT_MANQUANT"
    }
}
