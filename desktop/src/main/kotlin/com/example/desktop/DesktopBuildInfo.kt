package com.example.desktop

import com.example.desktop.maj.MiseAJourBureau

/**
 * Les quelques informations que la fenêtre de bureau affiche sur elle-même.
 *
 * Volontairement en Kotlin pur, hors de tout composable : c'est ce qui rend ces règles testables
 * dans `:desktop:test` sans lancer d'interface graphique. Une machine de CI n'a pas d'écran, donc
 * tout ce qui doit être vérifié automatiquement a intérêt à vivre en dehors de la fenêtre.
 *
 * Cette classe a aussi un rôle de garde-fou, moins évident : elle donne à `:desktop:test` quelque
 * chose de réel à exécuter. Une tâche de test sans aucun test passe « au vert » sans rien vérifier
 * — on ne verrait donc jamais que le jeu de sources de test est mal configuré.
 */
object DesktopBuildInfo {

    const val NOM_APPLICATION = "Varotra"

    /**
     * Le numéro de version est injecté par Gradle à l'empaquetage ; lancé depuis l'IDE ou par
     * `:desktop:run`, il est simplement absent. Afficher « Varotra » seul est alors préférable à
     * « Varotra null » ou « Varotra  » — ce que produiraient une interpolation naïve ou une
     * concaténation.
     */
    fun titreFenetre(version: String?): String {
        val propre = version?.trim()
        return if (propre.isNullOrEmpty()) NOM_APPLICATION else "$NOM_APPLICATION $propre"
    }

    /**
     * Version lue depuis les propriétés système, `null` hors d'un paquet natif — un cas **normal**
     * (lancement depuis l'IDE ou `:desktop:run`), pas une erreur.
     *
     * Deux sources, dans cet ordre, et l'ordre compte. `varotra.versionName` est l'argument de JVM
     * gravé par Gradle dans le lanceur (voir `desktop/build.gradle.kts`) : il ne dépend d'aucune
     * hypothèse sur l'outil d'empaquetage. `jpackage.app-version` est conservé en second parce que
     * c'était la seule source jusqu'ici — un paquet construit avant cette version continue donc
     * d'afficher son numéro.
     *
     * Cette distinction est devenue importante avec la mise à jour interne : un logiciel qui ne
     * connaît pas sa propre version ne peut pas savoir s'il en existe une plus récente.
     */
    fun versionCourante(): String? =
        System.getProperty("varotra.versionName")?.takeIf { it.isNotBlank() }
            ?: System.getProperty("jpackage.app-version")?.takeIf { it.isNotBlank() }

    /**
     * Le numéro comparable de la version installée, ou `null` hors d'un paquet.
     *
     * Lu directement depuis l'argument de JVM quand il existe, sinon **déduit du nom de version**
     * — ce qui permet à un paquet antérieur à cette fonctionnalité de se mettre à jour quand même,
     * puisqu'il porte bien `1.0.<n>` dans `jpackage.app-version`.
     */
    fun versionCodeCourant(): Int? =
        System.getProperty("varotra.versionCode")?.trim()?.toIntOrNull()?.takeIf { it > 0 }
            ?: MiseAJourBureau.versionCodeDepuisNom(versionCourante())
}
