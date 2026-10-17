/**
 * `:core:logic` — les règles de calcul de l'application, en Kotlin pur.
 *
 * Constitué en deux temps (sections 78 et 80). D'abord les six utilitaires qui n'avaient **déjà**
 * aucune dépendance Android ni Room — l'étape la plus sûre possible, puisque déplacer du code déjà
 * indépendant ne peut rien casser d'autre que la configuration du build. Puis, une fois les
 * modèles extraits dans `:core:model`, les cinq calculs qui en dépendaient : `PriceUtil`,
 * `FefoAllocator`, `DebtAllocation`, `EtagereLayout`, `AnomalyDetector`.
 *
 * Puis, l'étape qui a débloqué le dernier verrou : les classes du plan d'étagères sorties
 * d'`InventoryViewModel` vers `:core:model` ont permis à `RechercheEtagere` de suivre, et
 * `LanguageManager` (table de traductions sans aucun import) les a rejoints.
 *
 * **Le module est autonome** : aucune référence à une classe restée dans `:app`, et aucun import
 * `android.*` ni `androidx.*`. Les clients REST vivent dans `:core:network`, séparément, pour que
 * cette garantie tienne.
 *
 * **Le package reste `com.example.util`.** Un module Gradle n'impose aucun changement de package,
 * et le conserver rend l'extraction invisible pour les ~100 sites d'appel : aucun import à
 * retoucher, donc aucune occasion de casser un fichier qu'on ne peut pas compiler localement.
 *
 * `kotlin("jvm")` et non `com.android.library` : c'est ce qui donne au module sa valeur de
 * garde-fou. Le compilateur refusera désormais toute dépendance Android ajoutée ici par
 * inadvertance, au lieu de l'accepter silencieusement et de rendre le code inutilisable côté
 * bureau — ce qui ne se découvrirait qu'au moment de le partager.
 */
plugins {
  // Convention partagée (buildSrc) : Kotlin/JVM, `java-library` et surtout la cible Java 11, sans
  // laquelle `:app` ne pourrait pas lire les classes produites ici. Voir le KDoc du plugin.
  id("varotra.module-jvm")
}

dependencies {
  // `api` et non `implementation` : les signatures publiques d'ici exposent les modèles
  // (`PriceUtil.displayPrice(product: Product, ...)`, `FefoAllocator.repartir(lots: List<LotProduit>, ...)`).
  // Avec `implementation`, un module qui dépend de :core:logic verrait ces fonctions sans pouvoir
  // nommer leurs paramètres — une erreur de compilation déroutante chez l'appelant, pour une
  // dépendance qu'il n'a pas déclarée et dont il ignore l'existence.
  api(project(":core:model"))

  testImplementation(libs.junit)
}
