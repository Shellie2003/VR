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
    fun `l'ordre du menu place la caisse en tete et les reglages a part`() {
        // L'ordre de l'énumération EST l'ordre du menu latéral. L'épingler évite qu'un ajout de
        // section se glisse au milieu et déplace sous le curseur des entrées que l'utilisateur
        // a appris à viser sans lire.
        assertEquals(
            listOf("CAISSE", "CATALOGUE", "STOCK", "DETTES", "RAPPORTS", "A_PROPOS"),
            SectionBureau.entries.map { it.name }
        )
    }
}
