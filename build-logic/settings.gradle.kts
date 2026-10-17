/**
 * Ce build INCLUS est indépendant, avec ses propres dépôts et son propre catalogue : il ne voit
 * RIEN de la configuration du build principal. D'où ce fichier.
 *
 * **Build inclus et non `buildSrc`, et ce n'est pas un détail de rangement.** La première
 * tentative utilisait `buildSrc` : ses dépendances fuient sur le classpath du build principal,
 * si bien que la racine ne pouvait plus déclarer ses plugins Kotlin avec une version — Gradle
 * refusait avec « the plugin is already on the classpath with an unknown version », et c'est le
 * build ENTIER qui ne se configurait plus. Un build inclus garde son classpath pour lui.
 *
 * Le catalogue est rattaché explicitement au même `libs.versions.toml` que le reste du projet.
 * C'est le point qui donne tout son sens au plugin de convention : sans cela, `buildSrc` devrait
 * répéter en dur la version de Kotlin, et on remplacerait une duplication (trois blocs de
 * configuration identiques) par une autre, plus sournoise — un numéro de version qui dériverait en
 * silence de celui du projet.
 */
dependencyResolutionManagement {
  repositories {
    mavenCentral()
    gradlePluginPortal()
  }
  versionCatalogs {
    create("libs") {
      from(files("../gradle/libs.versions.toml"))
    }
  }
}
