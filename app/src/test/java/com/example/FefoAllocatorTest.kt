package com.example

import com.example.data.model.LotProduit
import com.example.util.FefoAllocator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Cœur du lien ventes/lots : la répartition d'une quantité vendue sur les lots, du plus proche de la
 * péremption au plus lointain. Testée ici SANS base de données ni coroutine — même principe que
 * `EtagereColorsTest` et `UpdateManagerTest` : la logique délicate est isolée en fonction pure,
 * seule la plomberie Room reste non couverte.
 */
class FefoAllocatorTest {

    private fun lot(id: Long, quantite: Double, peremption: Long) = LotProduit(
        id = id,
        produitId = 1L,
        quantite = quantite,
        datePeremption = peremption
    )

    @Test
    fun `sans aucun lot, aucune allocation - le cas de l'immense majorite des epiceries`() {
        val allocations = FefoAllocator.repartir(emptyList(), quantiteDemandee = 5.0)
        assertTrue("Aucun lot saisi ne doit produire aucune allocation", allocations.isEmpty())
    }

    @Test
    fun `le lot qui perime le plus tot est servi en premier, quel que soit l'ordre d'entree`() {
        val lots = listOf(
            lot(id = 1, quantite = 10.0, peremption = 3_000L),
            lot(id = 2, quantite = 10.0, peremption = 1_000L), // périme le plus tôt
            lot(id = 3, quantite = 10.0, peremption = 2_000L)
        )

        val allocations = FefoAllocator.repartir(lots, quantiteDemandee = 4.0)

        assertEquals(1, allocations.size)
        assertEquals(2L, allocations[0].lotId)
        assertEquals(4.0, allocations[0].quantite, 1e-9)
    }

    @Test
    fun `une vente qui depasse un lot deborde sur le suivant - le cas qu'un lotId unique ne saurait pas representer`() {
        val lots = listOf(
            lot(id = 1, quantite = 6.0, peremption = 1_000L),
            lot(id = 2, quantite = 10.0, peremption = 2_000L)
        )

        val allocations = FefoAllocator.repartir(lots, quantiteDemandee = 10.0)

        assertEquals(2, allocations.size)
        assertEquals(1L, allocations[0].lotId)
        assertEquals(6.0, allocations[0].quantite, 1e-9)
        assertEquals(2L, allocations[1].lotId)
        assertEquals(4.0, allocations[1].quantite, 1e-9)
        assertEquals(10.0, allocations.sumOf { it.quantite }, 1e-9)
    }

    @Test
    fun `des lots insuffisants n'empechent pas la vente, ils allouent ce qui existe`() {
        val lots = listOf(lot(id = 1, quantite = 2.0, peremption = 1_000L))

        val allocations = FefoAllocator.repartir(lots, quantiteDemandee = 5.0)

        // La caisse doit passer : la traçabilité est un plus, pas un verrou.
        assertEquals(1, allocations.size)
        assertEquals(2.0, allocations[0].quantite, 1e-9)
        assertEquals(3.0, FefoAllocator.quantiteNonCouverte(lots, 5.0), 1e-9)
    }

    @Test
    fun `les lots vides sont ignores plutot que de produire une allocation de zero`() {
        val lots = listOf(
            lot(id = 1, quantite = 0.0, peremption = 1_000L), // épuisé, mais encore en base
            lot(id = 2, quantite = 5.0, peremption = 2_000L)
        )

        val allocations = FefoAllocator.repartir(lots, quantiteDemandee = 3.0)

        assertEquals(1, allocations.size)
        assertEquals(2L, allocations[0].lotId)
    }

    @Test
    fun `les quantites decimales sont gerees sans reliquat fantome`() {
        // Vendre au kilo ou au litre produit des quantités fractionnaires : 0,25 + 0,5 + 0,25.
        val lots = listOf(
            lot(id = 1, quantite = 0.25, peremption = 1_000L),
            lot(id = 2, quantite = 0.5, peremption = 2_000L),
            lot(id = 3, quantite = 0.25, peremption = 3_000L)
        )

        val allocations = FefoAllocator.repartir(lots, quantiteDemandee = 1.0)

        assertEquals(3, allocations.size)
        assertEquals(1.0, allocations.sumOf { it.quantite }, 1e-9)
        assertEquals(0.0, FefoAllocator.quantiteNonCouverte(lots, 1.0), 1e-9)
    }

    @Test
    fun `une quantite nulle ou negative n'alloue rien`() {
        val lots = listOf(lot(id = 1, quantite = 10.0, peremption = 1_000L))

        assertTrue(FefoAllocator.repartir(lots, quantiteDemandee = 0.0).isEmpty())
        assertTrue(FefoAllocator.repartir(lots, quantiteDemandee = -3.0).isEmpty())
    }

    @Test
    fun `a peremption egale, l'ordre d'entree est conserve pour rester deterministe`() {
        val lots = listOf(
            lot(id = 7, quantite = 2.0, peremption = 1_000L),
            lot(id = 9, quantite = 2.0, peremption = 1_000L)
        )

        val allocations = FefoAllocator.repartir(lots, quantiteDemandee = 3.0)

        assertEquals(listOf(7L, 9L), allocations.map { it.lotId })
    }
}
