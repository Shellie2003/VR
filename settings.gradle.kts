pluginManagement {
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
include(":core:logic")

include(":desktop")
