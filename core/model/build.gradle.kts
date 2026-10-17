/**
 * `:core:model` — les entités de données de l'application.
 *
 * Deuxième étape du découpage (section 78), et l'obstacle qu'on y annonçait : ces classes sont des
 * `@Entity` Room, donc réputées inséparables d'Android. **Elles ne le sont pas.** L'inspection de
 * leurs imports montre que les 12 fichiers n'utilisent au total que quatre annotations —
 * `Entity`, `PrimaryKey`, `Index`, `ForeignKey` — et **aucun** type du framework Android : pas de
 * `Uri`, pas de `Bitmap`, pas de `Parcelable`, et aucun `@TypeConverters` (les convertisseurs sont
 * déclarés sur la base dans `:app`, pas sur les modèles). C'est ce constat, et non un pari, qui
 * rend ce module possible.
 *
 * Ces quatre annotations vivent dans `room-common`, un **JAR ordinaire** et non un AAR : un module
 * `kotlin("jvm")` peut donc en dépendre sans ramener quoi que ce soit d'Android. Le processeur Room
 * de `:app` compile sans difficulté des entités venues d'un module dépendant, du moment qu'elles
 * sont sur son classpath — ce que `implementation(project(":core:model"))` garantit.
 *
 * Le package reste `com.example.data.model`, pour la même raison qu'en section 78 : aucun des
 * nombreux sites d'appel n'a d'import à retoucher, donc aucune occasion de casser un fichier qu'on
 * ne peut pas compiler localement.
 */
plugins {
  // Convention partagée (buildSrc) : Kotlin/JVM, `java-library` et surtout la cible Java 11, sans
  // laquelle `:app` ne pourrait pas lire les classes produites ici. Voir le KDoc du plugin.
  id("varotra.module-jvm")
}

dependencies {
  // `api` et non `implementation` : les annotations font partie de la signature publique des
  // entités, et le processeur Room de `:app` doit pouvoir les lire. Avec `implementation` elles
  // resteraient internes à ce module et Room ne verrait plus que des classes de données ordinaires.
  api(libs.androidx.room.common)
}
