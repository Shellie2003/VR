import org.jetbrains.compose.desktop.application.dsl.TargetFormat

/**
 * Version desktop de Varotra — Compose Multiplatform sur JVM.
 *
 * Ce module est volontairement MINIMAL à ce stade. Il ne contient encore aucun écran métier : son
 * seul rôle est de prouver que la chaîne de build de bureau tient debout (compilation, tests,
 * fabrication d'un paquet natif) AVANT qu'on déplace le moindre morceau de code dans des modules
 * partagés. C'est l'ordre demandé, et c'est le bon : découper le code d'abord, puis découvrir que
 * la chaîne desktop ne construit pas, obligerait à défaire le découpage pour diagnostiquer.
 *
 * Trois plugins, et aucun n'est de trop :
 *  - `kotlin.jvm` et non `kotlin.android` : ce module n'est PAS un module Android. Il n'utilise ni
 *    AGP, ni le SDK Android — c'est précisément ce qui le rend capable de produire un exécutable
 *    de bureau, et ce qui le rendra incapable de compiler du code qui dépend d'Android par
 *    inadvertance. Ce garde-fou est l'intérêt principal du module aujourd'hui.
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
  implementation(compose.desktop.currentOs)
  // Material 3 est à demander EXPLICITEMENT : `compose.desktop.currentOs` n'embarque que
  // Material 2. Sans cette ligne, les imports `androidx.compose.material3.*` ne résolvent pas —
  // et l'erreur, un import introuvable, n'évoque en rien une dépendance manquante.
  implementation(compose.material3)
  testImplementation(libs.junit)
}

compose.desktop {
  application {
    mainClass = "com.example.desktop.MainKt"

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
