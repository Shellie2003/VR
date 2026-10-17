/**
 * `kotlin-dsl` est ce qui permet d'écrire les plugins de convention sous forme de simples fichiers
 * `.gradle.kts` dans `src/main/kotlin` — Gradle les compile en vrais plugins, utilisables ensuite
 * par un `id("…")` ordinaire dans les modules.
 */
plugins {
  `kotlin-dsl`
}

dependencies {
  // Le plugin Kotlin sur le classpath de buildSrc, ce qui autorise les plugins de convention à
  // écrire `id("org.jetbrains.kotlin.jvm")` SANS numéro de version. La version vient du catalogue
  // partagé (voir settings.gradle.kts de ce dossier), donc elle ne peut pas dériver de celle du
  // projet.
  implementation(libs.kotlin.gradle.plugin)
}
