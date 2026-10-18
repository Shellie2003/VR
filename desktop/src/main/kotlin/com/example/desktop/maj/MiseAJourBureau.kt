package com.example.desktop.maj

import org.json.JSONObject
import java.io.File

/**
 * Une mise à jour de la version de bureau, telle que décrite par le manifeste
 * `bureau/version.json` du bucket Cloudflare R2.
 *
 * Format identique en esprit à celui d'Android (`latest/version.json`, voir `UpdateManager`), mais
 * sous un **chemin distinct** et avec un champ `archiveUrl` au lieu d'`apkUrl` : les deux
 * plateformes ne publient ni la même chose ni au même rythme, et partager un seul manifeste ferait
 * proposer un APK à un PC — ou l'inverse.
 */
data class InfoMajBureau(
    val versionCode: Int,
    val versionName: String,
    val archiveUrl: String,
    val sha256: String,
    val notes: String
)

/**
 * Les règles de la mise à jour interne du logiciel de bureau, **sans aucune entrée/sortie**.
 *
 * ## Pourquoi tout est ici plutôt que dans le service
 *
 * Le remplacement d'une application par elle-même sous Windows ne peut PAS être exécuté dans cet
 * environnement de développement (ni localement — pas de Windows — ni en CI, qui compile et lance
 * des tests sans ouvrir de fenêtre). Tout ce qui peut être décidé par un calcul est donc sorti du
 * code d'entrée/sortie et posé ici, où un test le vérifie réellement : la comparaison de versions,
 * la lecture du manifeste, la découverte du dossier d'installation et surtout **le texte du script
 * de remplacement**, dont une faute d'ordre ou de guillemet détruirait une installation.
 *
 * C'est le même principe que `UpdateManager.parserManifesteMiseAJour` côté Android, poussé plus
 * loin parce que l'enjeu l'est aussi : là-bas l'installateur du système fait le travail, ici c'est
 * nous.
 */
object MiseAJourBureau {

    /** Même bucket public que la mise à jour Android — seul le chemin diffère. */
    const val BASE_URL = "https://pub-8562b104a83841e09966f730d5763b79.r2.dev"
    const val URL_MANIFESTE = "$BASE_URL/bureau/version.json"

    /** Nom du dossier temporaire où l'archive est décompressée, à côté de l'installation. */
    const val NOM_DOSSIER_NOUVEAU = "Varotra-nouvelle-version"

    /** Suffixe du dossier de l'ancienne version, conservé jusqu'au démarrage réussi de la nouvelle. */
    const val SUFFIXE_ANCIEN = ".ancien"

    /**
     * Le numéro de version comparable, extrait du nom de version.
     *
     * Le paquet porte `1.0.<nombre de commits>` (contrainte MSI : le premier nombre doit être
     * supérieur à zéro, voir `desktop/build.gradle.kts`). C'est donc le **troisième** segment qui
     * croît d'une version à l'autre, et c'est lui qu'on compare.
     *
     * Le troisième segment et non le dernier : un éventuel `1.0.150.2` verrait son dernier segment
     * valoir 2, donc une version **plus récente** passerait pour plus ancienne — exactement le
     * genre d'erreur qui laisse une installation sur place pour toujours sans rien signaler.
     *
     * Renvoie `null` quand la version est absente ou illisible. L'absence est un cas **normal** :
     * elle signifie que le logiciel tourne depuis les sources ou l'IDE, et il ne faut alors surtout
     * pas se mettre à jour par-dessus un arbre de développement.
     */
    fun versionCodeDepuisNom(versionName: String?): Int? {
        val segments = versionName?.trim()?.split('.') ?: return null
        if (segments.size < 3) return null
        return segments[2].toIntOrNull()?.takeIf { it > 0 }
    }

    /**
     * Interprète le manifeste et décide s'il représente une mise à jour par rapport à
     * [versionCodeActuel].
     *
     * Renvoie `null` aussi bien pour « déjà à jour » que pour un manifeste invalide ou incomplet :
     * dans les deux cas il n'y a rien à proposer. Un manifeste sans `archiveUrl` est traité comme
     * invalide plutôt que comme une mise à jour sans fichier — proposer un téléchargement qui ne
     * peut pas aboutir serait pire que de ne rien proposer.
     */
    fun parserManifeste(corpsJson: String, versionCodeActuel: Int): InfoMajBureau? {
        return try {
            val json = JSONObject(corpsJson)
            val distant = json.optInt("versionCode", 0)
            val url = json.optString("archiveUrl", "")
            if (distant <= versionCodeActuel || url.isBlank()) return null
            InfoMajBureau(
                versionCode = distant,
                versionName = json.optString("versionName", ""),
                archiveUrl = url,
                sha256 = json.optString("sha256", ""),
                notes = json.optString("notes", "")
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * La racine de l'installation, déduite de l'emplacement de notre propre code.
     *
     * `jpackage` (donc `createDistributable`, donc l'archive portable comme l'installateur) range
     * l'application ainsi :
     *
     * ```
     * Varotra/                 <- la racine cherchée
     *   Varotra.exe            <- le lanceur
     *   app/                   <- nos jars, dont celui qui contient CETTE classe
     *   runtime/               <- le JRE embarqué
     * ```
     *
     * On remonte donc de deux niveaux depuis le jar. **La vérification que le dossier parent
     * s'appelle bien `app` n'est pas une précaution de style** : lancé depuis Gradle ou l'IDE, notre
     * code vit dans `build/classes/kotlin/main`, et remonter de deux niveaux y désignerait
     * `build/classes` — un dossier qu'on s'apprêterait alors à remplacer par une application
     * entière. Renvoyer `null` est la seule réponse correcte hors d'un paquet.
     *
     * Aucun accès disque : la fonction ne raisonne que sur des noms de chemin, ce qui la rend
     * testable sur une machine qui n'a ni Windows ni installation.
     */
    fun racineInstallation(emplacementCode: File?): File? {
        val dossierApp = emplacementCode?.parentFile ?: return null
        if (!dossierApp.name.equals("app", ignoreCase = true)) return null
        return dossierApp.parentFile
    }

    /** Le système courant sait-il appliquer la mise à jour tout seul ? */
    fun estWindows(nomSysteme: String? = System.getProperty("os.name")): Boolean =
        nomSysteme?.lowercase()?.contains("win") == true

    /**
     * Le script qui remplace l'installation **après** la fermeture du logiciel, puis le relance.
     *
     * ## Pourquoi un script, et pas du code Kotlin
     *
     * Windows interdit de renommer ou de supprimer un dossier contenant un exécutable ou une
     * bibliothèque en cours d'utilisation. Le logiciel ne peut donc pas se remplacer lui-même
     * pendant qu'il tourne : il faut un tiers qui attende sa mort. Ce tiers doit vivre **hors** du
     * dossier remplacé, d'où son écriture dans le dossier temporaire du système.
     *
     * ## L'ordre des opérations est la seule chose qui protège l'installation
     *
     * 1. attendre la disparition du processus [pid] ;
     * 2. renommer l'installation en `…ancien` — **déplacement et non copie**, donc instantané et
     *    atomique : à aucun moment il n'existe une installation à moitié écrasée ;
     * 3. mettre la nouvelle version à la place ;
     * 4. relancer, et seulement **ensuite** supprimer l'ancienne.
     *
     * Chaque échec possible a sa sortie, et **aucune ne laisse l'utilisateur sans logiciel** : si
     * l'étape 2 échoue, l'installation est encore intacte et on la relance ; si l'étape 3 échoue,
     * on remet l'ancienne en place avant de relancer. C'est aussi pour cela que la suppression de
     * l'ancienne version vient en dernier et pas avant le relancement.
     *
     * ## Deux détails qui paraissent anodins
     *
     * - `ping` et non `timeout` pour attendre : `timeout` exige une console interactive et échoue
     *   (« input redirection is not supported ») dans un processus lancé sans fenêtre, ce qui ferait
     *   boucler l'attente à pleine vitesse.
     * - une **seconde boucle de tentatives** autour du renommage : la disparition du processus ne
     *   garantit pas que Windows a relâché tous les descripteurs de fichiers à la même milliseconde.
     *
     * Tous les chemins sont entre guillemets : une installation se trouve très normalement sous
     * `C:\Program Files\Varotra`, et un chemin non protégé y couperait la commande au premier espace.
     */
    fun scriptRemplacement(
        pid: Long,
        racine: File,
        nouveau: File,
        nomLanceur: String
    ): String {
        val cible = racine.path
        val ancien = cible + SUFFIXE_ANCIEN
        // Fins de ligne CRLF ÉCRITES EXPLICITEMENT, et non `appendLine`.
        //
        // Sur la JVM, `appendLine` suit le séparateur de ligne du système : le script serait donc
        // en `\n` quand le test l'examine sous Linux et en `\r\n` chez le gérant sous Windows. Un
        // test qui n'inspecte pas le texte réellement produit en production ne vérifie rien — et
        // c'est précisément ce texte-là qui manipule le dossier d'installation. CRLF est par
        // ailleurs ce qu'attend `cmd`.
        //
        // De même `racine.path` et non `racine.absolutePath` : « absolu » se résout contre le
        // répertoire courant, donc un chemin Windows passé à un test tournant sous Linux s'y
        // verrait préfixer le dossier du projet. L'appelant fournit déjà des chemins absolus (ils
        // viennent de l'emplacement de notre propre code), et la fonction reste ainsi
        // indépendante du système qui l'exécute.
        val lignes = listOf(
            "@echo off",
            "rem Mise a jour de Varotra bureau - genere automatiquement, ne pas editer.",
            "setlocal",
            "set \"CIBLE=$cible\"",
            "set \"NOUVEAU=${nouveau.path}\"",
            "set \"ANCIEN=$ancien\"",
            "set \"LANCEUR=$nomLanceur\"",
            "",
            "rem 1. Attendre la fermeture complete du logiciel (PID $pid).",
            "set /a ATTENTE=0",
            ":attendre",
            "tasklist /FI \"PID eq $pid\" 2>nul | find \"$pid\" >nul",
            "if errorlevel 1 goto pret",
            "set /a ATTENTE+=1",
            "if %ATTENTE% GEQ 60 goto abandon",
            "ping -n 2 127.0.0.1 >nul",
            "goto attendre",
            "",
            ":pret",
            "if exist \"%ANCIEN%\" rmdir /s /q \"%ANCIEN%\"",
            "",
            "rem 2. Mettre l'ancienne version de cote. Plusieurs tentatives : la fin du processus",
            "rem    ne garantit pas que Windows a relache ses fichiers a la meme milliseconde.",
            "set /a ESSAI=0",
            ":renommer",
            "move \"%CIBLE%\" \"%ANCIEN%\" >nul 2>&1",
            "if not errorlevel 1 goto installer",
            "set /a ESSAI+=1",
            "if %ESSAI% GEQ 10 goto echec_intact",
            "ping -n 2 127.0.0.1 >nul",
            "goto renommer",
            "",
            ":installer",
            "rem 3. Installer la nouvelle version a la place.",
            "move \"%NOUVEAU%\" \"%CIBLE%\" >nul 2>&1",
            "if errorlevel 1 goto restaurer",
            "",
            "rem 4. Relancer AVANT de supprimer l'ancienne version.",
            "start \"\" \"%CIBLE%\\%LANCEUR%\"",
            "rmdir /s /q \"%ANCIEN%\"",
            "exit /b 0",
            "",
            ":restaurer",
            "rem La nouvelle version n'a pas pu etre posee : on remet l'ancienne en place.",
            "move \"%ANCIEN%\" \"%CIBLE%\" >nul 2>&1",
            "start \"\" \"%CIBLE%\\%LANCEUR%\"",
            "exit /b 1",
            "",
            ":echec_intact",
            "rem Rien n'a bouge : l'installation est intacte, on la relance simplement.",
            "start \"\" \"%CIBLE%\\%LANCEUR%\"",
            "exit /b 1",
            "",
            ":abandon",
            "rem Le logiciel ne s'est pas ferme : on ne touche a rien.",
            "exit /b 2"
        )
        return lignes.joinToString("\r\n", postfix = "\r\n")
    }

}
