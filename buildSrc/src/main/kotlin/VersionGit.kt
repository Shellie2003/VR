import org.gradle.api.Project

/**
 * Nombre de commits de la branche, utilisé comme `versionCode` par `:app` et comme numéro de
 * version du paquet par `:desktop`.
 *
 * **Pourquoi le nombre de commits :** il ne peut que croître d'une release à l'autre, chaque commit
 * n'arrivant qu'après le précédent. Android refuse catégoriquement d'installer par-dessus une
 * application existante un APK dont le `versionCode` n'est pas STRICTEMENT supérieur — un compteur
 * tenu à la main se serait fait oublier, et la mise à jour interne de l'application (voir
 * `UpdateManager`) serait restée muette sans qu'on comprenne pourquoi.
 *
 * **`providers.exec` et jamais `ProcessBuilder` ni `Runtime.exec`.** Le cache de configuration de
 * Gradle (actif par défaut) rejette tout processus externe lancé à la volée pendant la phase de
 * configuration et fait échouer le build entier sur « Starting an external process … is
 * unsupported ». `providers.exec` est l'API qui sait l'enregistrer proprement comme entrée du
 * cache. La leçon a coûté un échec de CI (voir AGENTS.md section 58bis) : elle vaut d'être
 * centralisée ici plutôt que de dépendre du souvenir de celui qui créera le prochain module.
 *
 * **Le repli sur 1 n'est pas une négligence** mais le comportement voulu hors d'un dépôt git
 * (archive source téléchargée, par exemple) : mieux vaut un numéro de version faible qu'un build
 * impossible. En revanche cela impose que la CI fasse un `checkout` avec `fetch-depth: 0` — un
 * clone superficiel ne contient qu'un commit et renverrait toujours 1, donc des releases
 * successives porteraient toutes le même numéro et aucune ne s'installerait sur la précédente.
 */
fun Project.nombreDeCommitsGit(): Int = try {
  providers.exec {
    commandLine("git", "rev-list", "--count", "HEAD")
  }.standardOutput.asText.get().trim().toIntOrNull() ?: 1
} catch (e: Exception) {
  1
}
