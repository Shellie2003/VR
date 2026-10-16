import org.jetbrains.kotlin.gradle.dsl.JvmTarget

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
  alias(libs.plugins.kotlin.jvm)
  // Explicite, et non supposé fourni par le plugin Kotlin : la configuration `api` utilisée plus
  // bas vient de `java-library`. Compter sur le plugin Kotlin pour l'apporter serait un pari
  // qu'on ne peut pas vérifier sans compiler, ce qui est impossible dans cet environnement.
  `java-library`
}

// Java 11, même contrainte qu'en section 78 : `:app` compile en Java 11 et un module Android ne
// peut pas lire des classes compilées pour une version supérieure.
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
  // `api` et non `implementation` : les annotations font partie de la signature publique des
  // entités, et le processeur Room de `:app` doit pouvoir les lire. Avec `implementation` elles
  // resteraient internes à ce module et Room ne verrait plus que des classes de données ordinaires.
  api(libs.androidx.room.common)
}
