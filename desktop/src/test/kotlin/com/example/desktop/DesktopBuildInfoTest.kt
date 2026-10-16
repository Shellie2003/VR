package com.example.desktop

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Premier test de la version de bureau.
 *
 * Son objet n'est pas la richesse de ce qu'il vérifie — le titre d'une fenêtre — mais le fait
 * qu'il s'exécute du tout : il prouve que le jeu de sources de test du module desktop est
 * correctement configuré et réellement lancé par la CI. Une tâche `test` sans aucun test réussit
 * silencieusement, et on découvrirait le problème le jour où un vrai test, lui, ne serait pas
 * exécuté non plus.
 */
class DesktopBuildInfoTest {

    @Test
    fun `le titre porte la version quand elle est connue`() {
        assertEquals("Varotra 1.0.116", DesktopBuildInfo.titreFenetre("1.0.116"))
    }

    @Test
    fun `sans version, le titre reste propre plutot que d'afficher un vide ou un null`() {
        // Lancée depuis les sources (`:desktop:run`), l'application n'a pas de version de paquet.
        // C'est un cas NORMAL, et une interpolation naïve afficherait « Varotra null ».
        assertEquals("Varotra", DesktopBuildInfo.titreFenetre(null))
        assertEquals("Varotra", DesktopBuildInfo.titreFenetre(""))
        assertEquals("Varotra", DesktopBuildInfo.titreFenetre("   "))
    }

    @Test
    fun `les espaces autour de la version sont retires`() {
        assertEquals("Varotra 1.0.116", DesktopBuildInfo.titreFenetre("  1.0.116  "))
    }
}
