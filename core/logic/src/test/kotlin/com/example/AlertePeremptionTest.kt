package com.example

import com.example.util.AlertePeremption
import com.example.util.EtatLot
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * La fenêtre d'alerte était écrite **deux fois** (l'écran Péremption et la notification quotidienne
 * du ViewModel), et le port sur le bureau en aurait fait une troisième. Ces cas fixent la règle
 * commune : la déplacer demande désormais de toucher ce test, donc de constater qu'elle s'applique
 * aux trois endroits à la fois.
 */
class AlertePeremptionTest {

    private val maintenant = 1_700_000_000_000L
    private val jour = 24L * 60 * 60 * 1000

    @Test
    fun `la fenetre d'alerte vaut sept jours`() {
        // La valeur qu'Android appliquait déjà de part et d'autre. L'épingler empêche qu'une
        // « petite » modification d'un côté passe inaperçue.
        assertEquals(7L * 24 * 60 * 60 * 1000, AlertePeremption.FENETRE_ALERTE_MS)
    }

    @Test
    fun `un lot deja depasse est perime`() {
        assertEquals(EtatLot.PERIME, AlertePeremption.etat(maintenant - 1, maintenant))
        assertEquals(EtatLot.PERIME, AlertePeremption.etat(maintenant - 30 * jour, maintenant))
    }

    @Test
    fun `un lot qui perime exactement maintenant n'est pas encore perime`() {
        // La comparaison est stricte (`<`), donc l'instant pile ne bascule pas. C'est le bon sens :
        // un lot dont la date est aujourd'hui est à écouler, pas à retirer.
        assertEquals(EtatLot.BIENTOT, AlertePeremption.etat(maintenant, maintenant))
    }

    @Test
    fun `la borne des sept jours est inclusive et le huitieme jour est bon`() {
        assertEquals(
            EtatLot.BIENTOT,
            AlertePeremption.etat(maintenant + AlertePeremption.FENETRE_ALERTE_MS, maintenant)
        )
        assertEquals(
            EtatLot.BON,
            AlertePeremption.etat(maintenant + AlertePeremption.FENETRE_ALERTE_MS + 1, maintenant)
        )
    }

    @Test
    fun `un lot perime n'est jamais compte deux fois`() {
        // Les deux conditions se recouvrent (un lot dépassé est aussi « dans la fenêtre »), donc
        // l'ordre du `when` décide. Sans cet ordre, le gérant lirait le même lot dans les deux
        // compteurs de l'en-tête et conclurait à deux problèmes là où il n'y en a qu'un.
        val dates = listOf(
            maintenant - jour,
            maintenant - 10 * jour,
            maintenant + 2 * jour,
            maintenant + 100 * jour
        )
        assertEquals(2, AlertePeremption.compterPerimes(dates, maintenant))
        assertEquals(1, AlertePeremption.compterBientot(dates, maintenant))
    }

    @Test
    fun `une liste vide rend zero et non une exception`() {
        assertEquals(0, AlertePeremption.compterPerimes(emptyList(), maintenant))
        assertEquals(0, AlertePeremption.compterBientot(emptyList(), maintenant))
    }
}
