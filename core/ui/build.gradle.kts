/**
 * `:core:ui` — les écrans Compose de la version de bureau.
 *
 * **Pourquoi ce module n'est PAS consommé par `:app`, et c'est une décision, pas un oubli.**
 * L'intention naturelle serait un module d'interface partagé entre Android et le bureau. Elle
 * suppose un module KMP (`androidTarget` + `jvm`) sur `org.jetbrains.compose`, dont la variante
 * Android résout vers les artefacts `androidx.compose.*` de la version de Compose Multiplatform
 * utilisée — ici 1.9.x. Or `:app` est figé sur le BOM `2024.09.00`, soit Compose 1.7.x, et Gradle
 * aligne toujours vers la version la PLUS HAUTE : brancher un tel module sur `:app` ferait monter
 * le Compose runtime de l'application de production de deux versions mineures, silencieusement,
 * dans le même commit que l'arrivée d'un écran de bureau. Sur une application installée dans des
 * boutiques, ce n'est pas un risque qu'on prend en passant — et surtout pas un risque qu'on prend
 * groupé avec autre chose, puisqu'un échec ne dirait plus lequel des deux l'a causé.
 *
 * Le module est donc Kotlin/JVM pur et sert le bureau seul. **Ce qui est réellement partagé est
 * dessous :** les modèles (`:core:model`), les calculs (`:core:logic`, dont `PriceUtil`,
 * `FormatUtil`, `CatalogueFiltre`, `LanguageManager`) et la palette de la charte. Les composables
 * sont écrits sans aucune dépendance à une source de données — état et rappels passés en
 * paramètres, jamais de ViewModel — précisément pour qu'ils soient déplaçables tels quels dans un
 * module KMP le jour où la montée de version de Compose côté `:app` sera traitée pour elle-même.
 *
 * `jvmToolchain(21)` et non la convention Java 11 de `varotra.module-jvm` : cette convention
 * existe pour qu'un module Android puisse lire les classes produites, contrainte sans objet ici
 * puisque seul `:desktop` (21) consomme ce module. Le jour où `:app` le consommera, c'est la
 * première ligne à changer.
 */
plugins {
  alias(libs.plugins.kotlin.jvm)
  // Depuis Kotlin 2.0, le compilateur Compose est livré par Kotlin lui-même.
  alias(libs.plugins.kotlin.compose)
  // Apporte les artefacts `compose.*` (runtime, foundation, material3) côté multiplateforme.
  alias(libs.plugins.jetbrains.compose)
}

kotlin {
  jvmToolchain(21)
}

dependencies {
  // `api` : les signatures publiques des écrans exposent `Product` et `ShopMode`, donc `:desktop`
  // doit pouvoir les nommer sans redéclarer la dépendance (même raisonnement qu'entre
  // `:core:logic` et `:core:model`).
  api(project(":core:logic"))

  // Material 3 est à demander EXPLICITEMENT : les artefacts Compose Multiplatform n'embarquent que
  // Material 2 par défaut, et l'erreur — un import `androidx.compose.material3.*` introuvable —
  // n'évoque en rien une dépendance manquante.
  api(compose.runtime)
  api(compose.foundation)
  api(compose.material3)
  // `Modifier`, `Color` et `dp` apparaissent dans les signatures publiques ; déclaré explicitement
  // plutôt que de compter sur la transitivité de `foundation`.
  api(compose.ui)

  testImplementation(libs.junit)
}
