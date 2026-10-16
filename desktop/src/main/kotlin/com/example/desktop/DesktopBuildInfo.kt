package com.example.desktop

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
     * Version lue depuis les propriétés système. `jpackage` la renseigne dans le lanceur du paquet
     * natif ; ailleurs elle n'existe pas, et c'est un cas normal, pas une erreur.
     */
    fun versionCourante(): String? = System.getProperty("jpackage.app-version")
}
