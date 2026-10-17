pluginManagement {
  // Build INCLUS et non `buildSrc`, et la distinction est tout sauf cosmétique : les dépendances
  // de `buildSrc` fuient sur le classpath du build principal, ce qui rend impossible de déclarer
  // ensuite un plugin Kotlin AVEC sa version — Gradle refuse alors avec « the plugin is already on
  // the classpath with an unknown version ». Un build inclus garde son classpath pour lui, donc la
  // racine et les modules continuent de déclarer leurs plugins normalement.
  includeBuild("build-logic")

  repositories {
    google {
      content {
        includeGroupByRegex("com\\.android.*")
        includeGroupByRegex("com\\.google.*")
        includeGroupByRegex("androidx.*")
      }
    }
    mavenCentral()
    gradlePluginPortal()
  }
}

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
  }
}

rootProject.name = "My Application"

include(":app")
include(":sync")

// Version desktop (Compose Multiplatform sur JVM). Volontairement un module À PART et non une
// cible ajoutée à :app — :app est un module Android (AGP), qui ne sait pas produire de paquet
// de bureau. Le code réellement commun sera extrait en modules indépendants dans un second
// temps ; pour l'instant ce module ne sert qu'à prouver que la chaîne de build desktop tient.
// Règles de calcul en Kotlin pur, partagées entre Android et le bureau. Premier morceau du
// découpage : ne contient que du code qui n'avait déjà aucune dépendance Android, donc son
// extraction ne peut rien casser d'autre que la configuration du build.
// Entités de données. Extraites de :app parce que leur inspection a montré qu'elles n'utilisent
// que quatre annotations Room et AUCUN type du framework Android — elles n'étaient donc pas
// réellement liées à Android, seulement rangées à côté de lui.
include(":core:model")

include(":core:logic")

// Clients REST. Séparés de :core:logic à dessein : y faire entrer OkHttp et Retrofit ferait perdre
// à ce dernier la garantie qui en fait l'intérêt — ne contenir que des fonctions sans effet de
// bord, vérifiables sans serveur ni appareil.
include(":core:network")

// Écrans Compose de la version de bureau. Kotlin/JVM pur et consommé par :desktop SEUL : un module
// d'interface partagé avec :app exigerait une cible Android sur Compose Multiplatform, dont la
// résolution ferait monter le Compose runtime de l'application de production de deux versions
// mineures — un risque à traiter pour lui-même, pas en passant. Voir le KDoc du module.
include(":core:ui")

include(":desktop")
