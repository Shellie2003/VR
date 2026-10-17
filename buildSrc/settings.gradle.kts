/**
 * `buildSrc` est un build à part entière, avec ses propres dépôts et son propre catalogue : il ne
 * voit RIEN de la configuration du build principal. D'où ce fichier.
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
