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
 * **`LicenceManager` a rejoint le module dans un second temps**, une fois la création de celui-ci
 * validée par la CI. Il utilise `org.json.JSONObject`, fourni par la plateforme Android mais
 * **absent du JDK** — d'où le `compileOnly` déclaré plus bas, qui est la clé de l'affaire : il
 * permet de compiler sans rien empaqueter, donc le téléphone continue d'utiliser la version de la
 * plateforme (aucune classe en double, ce qu'une dépendance ordinaire provoquerait sous R8) et seul
 * un consommateur hors Android aurait à fournir une implémentation à l'exécution.
 *
 * Effet secondaire bienvenu : `DeviceMatchingTest` devient un test **JVM pur** au lieu de dépendre
 * du harnais Android, parce que les tests de ce module disposent d'une vraie implémentation
 * `org.json` en `testImplementation`.
 *
 * Intérêt concret pour la suite : la sauvegarde cloud est exactement ce dont la version de bureau
 * a besoin pour servir à quelque chose — elle pourra restaurer les données d'une boutique sans
 * qu'une ligne de ce code soit recopiée.
 */
plugins {
  // Convention partagée (buildSrc) : Kotlin/JVM, `java-library` et surtout la cible Java 11, sans
  // laquelle `:app` ne pourrait pas lire les classes produites ici. Voir le KDoc du plugin.
  id("varotra.module-jvm")
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

  // `compileOnly` et c'est TOUT L'INTÉRÊT de cette ligne : `LicenceManager` a besoin de
  // `org.json.JSONObject` pour compiler, mais Android fournit déjà cette classe depuis sa
  // plateforme. Une dépendance ordinaire l'empaquetterait en double et ferait échouer R8 sur
  // « Duplicate class org.json.* ». En `compileOnly`, rien n'est livré : le téléphone continue
  // d'utiliser la version de la plateforme — donc le comportement en production ne change pas —
  // et seul un consommateur hors Android (le bureau) aurait à fournir une implémentation en
  // `runtimeOnly`.
  compileOnly(libs.org.json)

  testImplementation(libs.junit)
  // Les tests tournent sur une JVM nue, sans plateforme Android pour fournir org.json : ici il
  // faut donc une vraie implémentation, pas seulement de quoi compiler.
  testImplementation(libs.org.json)
}
