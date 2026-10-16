import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/**
 * `:core:logic` — les règles de calcul de l'application, en Kotlin pur.
 *
 * Premier morceau du découpage annoncé en section 76, et volontairement le plus sûr : il ne
 * contient QUE des fichiers qui n'avaient déjà aucune dépendance Android ni Room. Déplacer du code
 * déjà indépendant ne peut rien casser d'autre que la configuration du build — qu'une CI verte
 * suffit à valider. Les utilitaires couplés aux entités Room (`PriceUtil`, `FefoAllocator`,
 * `DebtAllocation`, `EtagereLayout`, `AnomalyDetector`) restent dans `:app` jusqu'à ce que les
 * modèles soient extraits à leur tour, ce qui est un chantier distinct.
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
  testImplementation(libs.junit)
}
