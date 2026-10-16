import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/**
 * `:core:logic` — les règles de calcul de l'application, en Kotlin pur.
 *
 * Constitué en deux temps (sections 78 et 80). D'abord les six utilitaires qui n'avaient **déjà**
 * aucune dépendance Android ni Room — l'étape la plus sûre possible, puisque déplacer du code déjà
 * indépendant ne peut rien casser d'autre que la configuration du build. Puis, une fois les
 * modèles extraits dans `:core:model`, les cinq calculs qui en dépendaient : `PriceUtil`,
 * `FefoAllocator`, `DebtAllocation`, `EtagereLayout`, `AnomalyDetector`.
 *
 * Reste dans `:app` : `RechercheEtagere`, seul utilitaire encore couplé à une classe imbriquée
 * dans `InventoryViewModel`. Le sortir demande d'abord d'en extraire cette classe — un chantier
 * distinct, qui touche au ViewModel et non aux calculs.
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
  alias(libs.plugins.kotlin.jvm)
  // Explicite plutôt que supposé fourni par le plugin Kotlin : la configuration `api` utilisée
  // plus bas vient de `java-library`.
  `java-library`
}

// Java 11 et non 21, et c'est la contrainte à ne pas rater : `:app` compile en Java 11, et un
// module Android ne peut pas lire des classes compilées pour une version supérieure. Le code est
// donc compilé PAR le JDK de la machine (21 en CI) mais POUR Java 11 — plutôt que via
// `jvmToolchain(11)`, qui ferait télécharger un second JDK à chaque build.
// `:desktop`, en 21, lit sans difficulté des classes Java 11.
kotlin {
  compilerOptions {
    jvmTarget.set(JvmTarget.JVM_11)
  }
}

java {
  sourceCompatibility = JavaVersion.VERSION_11
  targetCompatibility = JavaVersion.VERSION_11
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
