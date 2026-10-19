import org.jetbrains.compose.desktop.application.dsl.TargetFormat

/**
 * Version desktop de Varotra — Compose Multiplatform sur JVM.
 *
 * Ce module est la FENÊTRE et rien de plus : il assemble les écrans de `:core:ui`, tient leur état
 * partagé (catalogue, panier, recherche) et porte les comportements propres à un PC — raccourcis
 * clavier posés sur la fenêtre, focus initial pour un lecteur de code-barres USB. Les six sections
 * du menu sont servies (accueil, stock, caisse, dettes, rapports, réglages).
 *
 * Il a d'abord été volontairement MINIMAL, sans aucun écran : son seul rôle était de prouver que la
 * chaîne de build de bureau tenait debout (compilation, tests, paquet natif) AVANT qu'on déplace le
 * moindre morceau de code dans des modules partagés. C'était l'ordre demandé, et c'était le bon :
 * découper le code d'abord, puis découvrir que la chaîne desktop ne construit pas, aurait obligé à
 * défaire le découpage pour diagnostiquer.
 *
 * **Ce qui manque encore, et qui est dit à l'écran plutôt que caché :** il n'y a pas de base de
 * données partagée (Room KMP touche au fichier de base de CHAQUE boutique en service, donc c'est un
 * chantier à mener pour lui-même). Les écrans tournent sur `DonneesDemo`, un bandeau le déclare
 * franchement, et le bouton « Valider » de la caisse est désactivé AVEC sa raison affichée — un
 * logiciel de caisse qui prétendrait encaisser sans rien enregistrer serait le pire des mensonges.
 *
 * `DonneesDemo` vit ici et NON dans `:core:ui` : un module d'interface qui embarque de fausses
 * données finit toujours par en livrer en production.
 *
 * Trois plugins, et aucun n'est de trop :
 *  - `kotlin.jvm` et non `kotlin.android` : ce module n'est PAS un module Android. Il n'utilise ni
 *    AGP, ni le SDK Android — c'est précisément ce qui le rend capable de produire un exécutable
 *    de bureau, et ce qui le rend incapable de compiler du code qui dépend d'Android par
 *    inadvertance. C'est ce garde-fou qui a rendu le découpage vérifiable : si un utilitaire
 *    extrait vers `:core:logic` ramenait une dépendance Android avec lui, ce module le dirait
 *    immédiatement au lieu de laisser le problème se découvrir au moment de partager le code.
 *  - `kotlin.compose` : depuis Kotlin 2.0, le compilateur Compose est livré par Kotlin lui-même et
 *    non plus par Compose Multiplatform.
 *  - `jetbrains.compose` : apporte les artefacts `compose.desktop.*` et la tâche d'empaquetage.
 */
plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.jetbrains.compose)
}

// Le nombre de commits sur la branche sert de versionCode : il n'augmente jamais qu'entre deux
// releases (chaque commit ne peut arriver qu'après le précédent), contrairement à un compteur
// codé en dur qu'on oublie facilement d'incrémenter. Android refuse un versionCode qui n'est pas
// STRICTEMENT supérieur à celui déjà installé.
//
// providers.exec (Provider API) et jamais ProcessBuilder/Runtime.exec : le cache de configuration
// de Gradle interdit tout processus externe lancé à la volée pendant la configuration et fait
// échouer le build entier (voir AGENTS.md section 58bis).
//
// Volontairement DUPLIQUÉ entre :app et :desktop plutôt que partagé. Les classes d'un build inclus
// (build-logic) ne sont pas sur le classpath des scripts du build principal, donc une fonction
// commune ne serait pas appelable ici ; la rendre accessible demanderait de passer par un plugin
// qui écrit une propriété `extra`, soit une indirection moins lisible que ces dix lignes.
fun gitCommitCount(): Int = try {
  providers.exec {
    commandLine("git", "rev-list", "--count", "HEAD")
  }.standardOutput.asText.get().trim().toIntOrNull() ?: 1
} catch (e: Exception) {
  1
}

val gitVersionCode = gitCommitCount()

kotlin {
  // 21 et non 11 comme côté Android : `jpackage`, l'outil du JDK qui fabrique les paquets natifs,
  // n'existe qu'à partir du JDK 14, et la machine de CI tourne déjà sous JDK 21 (contrainte
  // Robolectric, voir verification.yml). Aligner dessus évite qu'un second JDK soit téléchargé.
  jvmToolchain(21)
}

dependencies {
  // Les écrans. Ce module expose `:core:logic` en `api`, donc les modèles et les calculs sont
  // disponibles de toute façon ; `:core:logic` reste déclaré explicitement parce que cette fenêtre
  // l'utilise directement (lecture de montant, mode de boutique) et qu'une dépendance utilisée
  // doit être déclarée, pas héritée par chance.
  implementation(project(":core:ui"))
  // Le code de calcul partagé avec Android : les mêmes règles de lecture des montants, de prix et
  // de mise en forme tournent des deux côtés, à partir d'une seule source.
  implementation(project(":core:logic"))
  // Les clients REST, pour lire la sauvegarde cloud de la boutique avec le code de récupération
  // (voir `donnees/`). C'est ce qui remplace les données de démonstration par les vraies sans
  // partager la base de données — donc sans toucher au fichier Room d'aucune boutique en service.
  implementation(project(":core:network"))
  implementation(compose.desktop.currentOs)
  // Material 3 est à demander EXPLICITEMENT : `compose.desktop.currentOs` n'embarque que
  // Material 2. Sans cette ligne, les imports `androidx.compose.material3.*` ne résolvent pas —
  // et l'erreur, un import introuvable, n'évoque en rien une dépendance manquante.
  implementation(compose.material3)

  // Mise à jour interne du logiciel (voir `maj/`). Deux dépendances seulement, et aucune pour le
  // réseau : `java.net.http.HttpClient` est fourni par le JDK depuis la version 11, et ce module
  // tourne sous 21 — OkHttp n'apporterait ici qu'un mégaoctet de plus dans un paquet qui en pèse
  // déjà 93.
  //
  // `org.json` en revanche est une VRAIE dépendance et non `compileOnly` comme dans
  // `:core:network` : là-bas Android fournit la classe depuis sa plateforme, ici personne ne la
  // fournit. Sans cette ligne le logiciel compilerait et tomberait sur `NoClassDefFoundError` à la
  // première vérification de mise à jour — un échec qui ne se verrait qu'à l'exécution.
  implementation(libs.org.json)
  implementation(libs.kotlinx.coroutines.core)

  testImplementation(libs.junit)
}

compose.desktop {
  application {
    mainClass = "com.example.desktop.MainKt"

    // Le numéro de version est GRAVÉ dans le lanceur du paquet, et c'est la mise à jour interne
    // qui l'exige : pour savoir s'il existe une version plus récente, le logiciel doit d'abord
    // connaître la sienne. Il la lisait jusqu'ici dans `jpackage.app-version`, une propriété que
    // `jpackage` ne garantit pas — et une version inconnue ferait soit proposer une mise à jour en
    // boucle sur une installation déjà à jour, soit n'en proposer aucune pour toujours. Un
    // argument de JVM explicite ne dépend d'aucune hypothèse sur l'outil d'empaquetage.
    //
    // Ces arguments sont écrits dans le fichier de configuration du paquet : ils n'existent donc
    // pas quand le logiciel est lancé depuis l'IDE ou `:desktop:run`, ce qui est exactement le
    // comportement voulu — on ne met pas à jour un arbre de développement par-dessus lui-même.
    jvmArgs += listOf(
      "-Dvarotra.versionCode=$gitVersionCode",
      "-Dvarotra.versionName=1.0.$gitVersionCode"
    )

    nativeDistributions {
      // Les formats sont déclarés ensemble, mais `packageDistributionForCurrentOS` ne fabrique
      // que celui du système sur lequel il tourne — d'où la matrice dans le workflow. Aucune
      // machine ne peut produire ceux des autres systèmes.
      //
      // `Exe` est ajouté à côté de `Msi` parce que c'est le fichier que l'utilisateur attend quand
      // il demande « un .exe » : un installateur qu'on double-clique. Les deux sortent du même
      // `jpackage` et dépendent tous deux de WiX, donc l'un ne sauve pas l'autre si l'outil manque
      // sur l'image GitHub du moment — c'est l'archive portable produite par le workflow qui joue
      // ce rôle de secours, puisqu'elle ne dépend de rien d'autre que du JDK.
      targetFormats(TargetFormat.Deb, TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Exe)

      packageName = "Varotra"
      // Contrainte héritée de Windows : MSI impose un numéro de version dont le premier nombre
      // est supérieur à zéro. "0.1.x" ferait échouer l'empaquetage sur Windows uniquement — un
      // échec qui ne se verrait pas en développant sous Linux.
      packageVersion = "1.0.$gitVersionCode"
      description = "Varotra — caisse et inventaire"
      vendor = "Ainadigit"

      macOS {
        // macOS refuse de signer un paquet sans identifiant de lot. Le laisser dériver du nom du
        // paquet fonctionne par chance ; l'écrire évite de dépendre de cette chance.
        bundleID = "com.ainadigit.varotra.desktop"
      }
    }
  }
}
