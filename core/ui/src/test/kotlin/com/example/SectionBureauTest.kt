package com.example

import com.example.ui.partage.coquille.SectionBureau
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Le seul test de ce module, et il a une raison précise d'exister : les libellés du menu sont du
 * texte traduit à la main, et une traduction oubliée se verrait comme un trou dans le menu — pas
 * comme une erreur. Aucun test d'interface ne remarquerait un libellé vide ; celui-ci, oui.
 *
 * Il garde aussi `:core:ui:test` d'être une tâche qui réussit sans rien vérifier : enregistrée dans
 * la CI mais vide, elle laisserait croire que le module est couvert.
 */
class SectionBureauTest {

    private val langues = listOf("mg", "fr", "en")

    @Test
    fun `chaque section a un libelle dans les trois langues`() {
        SectionBureau.entries.forEach { section ->
            langues.forEach { langue ->
                val libelle = section.libelle(langue)
                assertTrue(
                    "Libellé vide pour $section en $langue",
                    libelle.isNotBlank()
                )
            }
        }
    }

    @Test
    fun `une langue inconnue retombe sur l'anglais au lieu de rendre du vide`() {
        // Même règle de secours que `LanguageManager`. Une langue non prévue (préférence restaurée
        // depuis une sauvegarde, valeur corrompue) ne doit jamais vider l'interface.
        SectionBureau.entries.forEach { section ->
            assertEquals(section.libelle("en"), section.libelle("xx"))
        }
    }

    @Test
    fun `les libelles d'une meme langue sont tous distincts`() {
        // Deux sections au même nom rendraient le menu inutilisable : on ne saurait plus laquelle
        // on ouvre. C'est l'erreur typique d'un copier-coller en ajoutant une section.
        langues.forEach { langue ->
            val libelles = SectionBureau.entries.map { it.libelle(langue) }
            assertEquals(
                "Libellés dupliqués en $langue : $libelles",
                libelles.size,
                libelles.distinct().size
            )
        }
    }

    @Test
    fun `le catalogue est servi, les sections qui dependent de la base ne le sont pas encore`() {
        // Épingle l'état réel annoncé à l'écran. Le jour où la caisse de bureau existera, ce test
        // échouera — c'est précisément ce qu'on veut : il faudra alors mettre à jour l'annonce
        // faite à l'utilisateur, au lieu de lui laisser lire « à venir » sur une section servie.
        assertTrue(SectionBureau.CATALOGUE.disponible)
        assertTrue(SectionBureau.STOCK.disponible)
        assertTrue(SectionBureau.A_PROPOS.disponible)
        assertTrue(!SectionBureau.CAISSE.disponible)
        assertTrue(!SectionBureau.DETTES.disponible)
        assertTrue(!SectionBureau.RAPPORTS.disponible)
    }
}
