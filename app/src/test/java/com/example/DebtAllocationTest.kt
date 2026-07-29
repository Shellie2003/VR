package com.example

import com.example.data.model.Debt
import com.example.util.DebtAllocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DebtAllocationTest {

    private fun dette(id: Int, balance: Double, date: Long = id.toLong()): Debt =
        Debt(id = id, debtorName = "Client A", amount = balance, balance = balance, date = date, note = "")

    @Test
    fun `exemple du gerant - 2300 et 1100, paiement de 2400`() {
        // La plus grosse dette (2300) est soldée en premier, le reliquat (100) s'applique à
        // l'autre (1100 -> 1000). C'est exactement le calcul demandé.
        val dettes = listOf(dette(1, 2300.0), dette(2, 1100.0))
        val plan = DebtAllocation.calculer(dettes, 2400.0)

        assertEquals(2, plan.lignes.size)
        val ligneA = plan.lignes.first { it.debt.id == 1 }
        assertEquals(2300.0, ligneA.montantApplique, 0.001)
        assertEquals(0.0, ligneA.soldeApres, 0.001)
        assertTrue(ligneA.estSoldee)

        val ligneB = plan.lignes.first { it.debt.id == 2 }
        assertEquals(100.0, ligneB.montantApplique, 0.001)
        assertEquals(1000.0, ligneB.soldeApres, 0.001)
        assertTrue(!ligneB.estSoldee)

        assertEquals(0.0, plan.montantNonAffecte, 0.001)
        assertEquals(2400.0, plan.montantTotalAffecte, 0.001)
    }

    @Test
    fun `paiement inferieur a la plus grosse dette ne touche que celle-ci`() {
        val dettes = listOf(dette(1, 2300.0), dette(2, 1100.0))
        val plan = DebtAllocation.calculer(dettes, 500.0)

        assertEquals(1, plan.lignes.size)
        assertEquals(1, plan.lignes[0].debt.id)
        assertEquals(1800.0, plan.lignes[0].soldeApres, 0.001)
        assertEquals(0.0, plan.montantNonAffecte, 0.001)
    }

    @Test
    fun `paiement couvrant tout laisse un reste non affecte`() {
        val dettes = listOf(dette(1, 2300.0), dette(2, 1100.0))
        val plan = DebtAllocation.calculer(dettes, 5000.0)

        assertEquals(2, plan.lignes.size)
        assertTrue(plan.lignes.all { it.estSoldee })
        assertEquals(1600.0, plan.montantNonAffecte, 0.001)
        assertEquals(3400.0, plan.montantTotalAffecte, 0.001)
    }

    @Test
    fun `paiement exact au centime pres solde tout sans reste`() {
        val dettes = listOf(dette(1, 2300.0), dette(2, 1100.0))
        val plan = DebtAllocation.calculer(dettes, 3400.0)

        assertTrue(plan.lignes.all { it.estSoldee })
        assertEquals(0.0, plan.montantNonAffecte, 0.001)
    }

    @Test
    fun `dettes deja soldees ignorees`() {
        val dettes = listOf(
            dette(1, 0.0),
            dette(2, 1100.0)
        )
        val plan = DebtAllocation.calculer(dettes, 500.0)

        assertEquals(1, plan.lignes.size)
        assertEquals(2, plan.lignes[0].debt.id)
    }

    @Test
    fun `solde egal - la plus ancienne est reglee en premier`() {
        val dettes = listOf(
            dette(1, 1000.0, date = 200L), // plus récente
            dette(2, 1000.0, date = 100L)  // plus ancienne
        )
        val plan = DebtAllocation.calculer(dettes, 600.0)

        assertEquals(1, plan.lignes.size)
        assertEquals(2, plan.lignes[0].debt.id) // la plus ancienne (date=100) réglée d'abord
    }

    @Test
    fun `montant nul ou negatif ne produit aucune ligne`() {
        val dettes = listOf(dette(1, 1000.0))
        assertEquals(0, DebtAllocation.calculer(dettes, 0.0).lignes.size)
        assertEquals(0, DebtAllocation.calculer(dettes, -50.0).lignes.size)
    }
}
