import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/**
 * `:core:network` — les clients REST de l'application.
 *
 * Cinquième étape du découpage. **Pourquoi un module distinct et non `:core:logic` :** y faire
 * entrer OkHttp, Retrofit et Moshi transformerait un module de calculs purs en module réseau, et
 * lui ferait perdre la garantie qui en fait tout l'intérêt — à savoir qu'il ne contient que des
 * fonctions sans effet de bord, vérifiables par un test sans serveur ni appareil. La frontière
 * entre « calculer » et « aller chercher dehors » vaut d'être tenue par le build.
 *
 * Contenu : `FirebaseBackupManager` (sauvegarde cloud) et `OpenFoodFactsApi` (recherche produit
 * mondiale). Les deux ont été retenus parce que l'audit a montré qu'ils ne référencent **aucune**
 * classe restée dans `:app` et ne prennent **aucun `Context`** — ce sont des clients HTTP purs,
 * rangés à côté d'Android sans en dépendre.
 *
 * **`LicenceManager` reste dans `:app`, et pour une raison précise :** il utilise
 * `org.json.JSONObject`, fourni par la plateforme Android mais **absent du JDK**. Le chemin
 * identifié pour l'amener ici un jour est `compileOnly("org.json:json")` — l'annotation `compileOnly`
 * étant la clé : elle permet de compiler sans rien empaqueter, donc Android continue d'utiliser sa
 * version de plateforme (aucune classe en double, ce qu'une dépendance ordinaire provoquerait) et
 * seul le bureau aurait à fournir l'implémentation en `runtimeOnly`. Non fait dans ce lot : c'est
 * un changement de configuration invérifiable sans compiler, et le grouper avec la création du
 * module rendrait un échec de CI indiagnosticable.
 *
 * Intérêt concret pour la suite : la sauvegarde cloud est exactement ce dont la version de bureau
 * a besoin pour servir à quelque chose — elle pourra restaurer les données d'une boutique sans
 * qu'une ligne de ce code soit recopiée.
 */
plugins {
  alias(libs.plugins.kotlin.jvm)
  // Explicite plutôt que supposé fourni par le plugin Kotlin : la configuration `api` utilisée
  // plus bas vient de `java-library`.
  `java-library`
}

// Java 11, même contrainte qu'aux sections 78 et 79 : `:app` compile en Java 11 et un module
// Android ne peut pas lire des classes compilées pour une version supérieure.
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
  // `api` pour Retrofit et Moshi, même raisonnement que `room-common` dans `:core:model` : leurs
  // annotations sont posées sur des déclarations PUBLIQUES de ce module (l'interface `Service` et
  // les classes de données de réponse), elles font donc partie de sa signature. Avec
  // `implementation`, un consommateur lisant ces types ne trouverait pas les annotations.
  api(libs.retrofit)
  api(libs.moshi.kotlin)

  // `implementation` pour le reste : le transport ne figure dans aucune signature publique.
  implementation(libs.okhttp)
  implementation(libs.converter.moshi)
  implementation(libs.kotlinx.coroutines.core)

  testImplementation(libs.junit)
}
