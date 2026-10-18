package com.example

import com.example.data.model.Debt
import com.example.util.RegroupementDettes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Cette règle décide de ce que le gérant lit comme total dû par une personne. Un regroupement trop
 * large réclamerait de l'argent au mauvais client ; un regroupement trop strict afficherait deux
 * cartes pour la même personne et masquerait le vrai total.
 */
class RegroupementDettesTest {

    private fun dette(
        id: Int,
        nom: String,
        montant: Double = 1000.0,
        solde: Double = montant,
        date: Long = 1_000L,
        paye: Boolean = false,
        echeance: Long? = null
    ) = Debt(
        id = id,
        debtorName = nom,
        amount = montant,
        balance = solde,
        date = date,
        note = "",
        isPaid = paye,
        dueDate = echeance
    )

    @Test
    fun `la casse et les espaces de bord ne separent pas le meme client`() {
        // C'est le cas courant : le même nom saisi trois fois à la main, trois graphies.
        val groupes = RegroupementDettes.grouper(
            listOf(
                dette(1, "Rakoto", date = 10),
                dette(2, "  rakoto ", date = 20),
                dette(3, "RAKOTO", date = 30)
            )
        )
        assertEquals(1, groupes.size)
        assertEquals(3, groupes.first().debts.size)
        assertEquals(3000.0, groupes.first().totalBalance, 1e-9)
    }

    @Test
    fun `un prefixe commun ne fusionne JAMAIS deux personnes`() {
        // La protection délibérée de la section 31 : fondre « Rakoto » et « Rakotoarisoa » ferait
        // réclamer de l'argent au mauvais client.
        val groupes = RegroupementDettes.grouper(
            listOf(dette(1, "Rakoto"), dette(2, "Rakotoarisoa"))
        )
        assertEquals(2, groupes.size)
    }

    @Test
    fun `le nom affiche est celui de la saisie la plus recente`() {
        // Pas une version mise en minuscules, qui ne ressemblerait à aucune saisie du gérant.
        val groupes = RegroupementDettes.grouper(
            listOf(dette(1, "rakoto", date = 10), dette(2, "Rakoto Jean", date = 20))
        )
        // Deux noms complets différents : deux groupes. Celui de « rakoto » garde sa graphie.
        assertEquals(2, groupes.size)

        val memeNom = RegroupementDettes.grouper(
            listOf(dette(1, "rakoto", date = 10), dette(2, "RAKOTO", date = 99))
        )
        assertEquals(1, memeNom.size)
        assertEquals("RAKOTO", memeNom.first().displayName)
    }

    @Test
    fun `les dettes d'un groupe vont de la plus recente a la plus ancienne`() {
        val groupe = RegroupementDettes.grouper(
            listOf(dette(1, "Koto", date = 10), dette(2, "Koto", date = 50), dette(3, "Koto", date = 30))
        ).first()
        assertEquals(listOf(2, 3, 1), groupe.debts.map { it.id })
    }

    @Test
    fun `les groupes sont ordonnes par dette la plus recente`() {
        // Le gérant retrouve en tête ce qu'il vient d'enregistrer.
        val groupes = RegroupementDettes.grouper(
            listOf(dette(1, "Ancien", date = 10), dette(2, "Recent", date = 900))
        )
        assertEquals(listOf("Recent", "Ancien"), groupes.map { it.displayName })
    }

    @Test
    fun `un groupe n'est solde que si TOUTES ses dettes le sont`() {
        // Afficher « payé » sur un groupe qui garde une dette ouverte ferait perdre de l'argent.
        val partiel = RegroupementDettes.grouper(
            listOf(
                dette(1, "Koto", solde = 0.0, paye = true),
                dette(2, "Koto", solde = 500.0)
            )
        ).first()
        assertFalse(partiel.isFullyPaid)
        assertEquals(500.0, partiel.totalBalance, 1e-9)

        val solde = RegroupementDettes.grouper(
            listOf(
                dette(1, "Koto", solde = 0.0, paye = true),
                dette(2, "Koto", solde = 0.0, paye = true)
            )
        ).first()
        assertTrue(solde.isFullyPaid)
    }

    @Test
    fun `un seul retard suffit a marquer le groupe en retard`() {
        val groupe = RegroupementDettes.grouper(
            listOf(
                dette(1, "Koto", echeance = null),
                dette(2, "Koto", echeance = 1L) // échéance au 1er janvier 1970 : largement dépassée
            )
        ).first()
        assertTrue(groupe.hasOverdue)
    }

    @Test
    fun `le total initial reste distinct du solde restant`() {
        // C'est ce qui permet d'afficher « Initiale : X » sous un solde déjà entamé.
        val groupe = RegroupementDettes.grouper(
            listOf(dette(1, "Koto", montant = 2300.0, solde = 300.0))
        ).first()
        assertEquals(2300.0, groupe.totalAmount, 1e-9)
        assertEquals(300.0, groupe.totalBalance, 1e-9)
    }

    @Test
    fun `une liste vide rend une liste vide et non une erreur`() {
        assertTrue(RegroupementDettes.grouper(emptyList()).isEmpty())
    }
}
