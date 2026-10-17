import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/**
 * Convention des modules Kotlin/JVM partagés (`:core:model`, `:core:logic`, `:core:network`).
 *
 * Les trois portaient **le même bloc de configuration recopié à l'identique** : plugins, cible
 * Java 11, compatibilité source/cible. Ce n'est pas qu'une question d'élégance — c'était trois
 * endroits où la contrainte la plus facile à rater du projet pouvait être oubliée (voir plus bas),
 * et un quatrième module l'aurait oubliée par simple inattention au copier-coller.
 *
 * **La contrainte en question, et c'est la seule raison d'être de ce fichier :** `:app` compile en
 * Java 11, et un module Android **ne peut pas lire** des classes compilées pour une version
 * supérieure. Un nouveau module JVM créé sans ce réglage compilerait en Java 21 (la version du JDK
 * de la machine) et casserait la compilation de `:app` — avec un message portant sur la version du
 * bytecode, qui n'évoque en rien un réglage manquant dans un build file.
 *
 * Le code est donc compilé PAR le JDK de la machine (21 en CI) mais POUR Java 11, via
 * `compilerOptions.jvmTarget` — et non `jvmToolchain(11)`, qui ferait télécharger un second JDK à
 * chaque build. `:desktop`, en 21, lit sans difficulté des classes Java 11.
 *
 * **`java-library` est appliqué ici** parce que les trois modules utilisent la configuration `api`
 * (annotations exposées dans leurs signatures publiques : `room-common` pour les entités, Retrofit
 * et Moshi pour les clients REST). Le plugin Kotlin ne l'apporte pas de façon garantie.
 *
 * Ce qui n'est PAS factorisé, volontairement : les dépendances propres à chaque module, y compris
 * `testImplementation(libs.junit)`. Le catalogue (`libs`) n'est pas accessible depuis un plugin de
 * convention précompilé ; le contourner demanderait de passer par `VersionCatalogsExtension`, soit
 * plus de machinerie que la ligne qu'on éviterait — et les dépendances de test diffèrent de toute
 * façon d'un module à l'autre (`:core:network` a besoin d'`org.json`).
 */
plugins {
  id("org.jetbrains.kotlin.jvm")
  `java-library`
}

kotlin {
  compilerOptions {
    jvmTarget.set(JvmTarget.JVM_11)
  }
}

java {
  sourceCompatibility = JavaVersion.VERSION_11
  targetCompatibility = JavaVersion.VERSION_11
}
