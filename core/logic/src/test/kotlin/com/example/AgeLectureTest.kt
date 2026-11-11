package com.example

import com.example.util.AgeLecture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Cette règle décide de deux choses qu'un gérant ne peut pas vérifier lui-même : **à quelle
 * fréquence le PC relit la boutique**, et **ce qu'il lit sur l'âge de ce qu'il voit**. Une erreur
 * sur la première ferait payer des données inutiles ou figerait l'écran ; une erreur sur la seconde
 * lui ferait croire ses chiffres frais alors qu'ils datent du matin.
 *
 * Tous les instants sont injectés : le test ne dépend pas de l'heure d'exécution en CI.
 */
class AgeLectureTest {

    private val minute = 60_000L
    private val heure = 60L * minute
    private val jour = 24L * heure

    // --- La décision de relire -----------------------------------------------------------------

    @Test
    fun `rien n a encore ete lu, donc on lit`() {
        assertTrue(AgeLecture.doitRelire(derniereLecture = null, maintenant = 1_000L))
    }

    @Test
    fun `deux lectures coup sur coup sont refusees`() {
        // Le cas réel : une fenêtre peut gagner et perdre le focus plusieurs fois par seconde
        // quand un clic passe par une autre application. Sans ce plancher, chaque va-et-vient
        // paierait une requête réseau.
        val lecture = 1_000_000L
        assertFalse(AgeLecture.doitRelire(lecture, lecture))
        assertFalse(AgeLecture.doitRelire(lecture, lecture + 5_000L))
        assertFalse(AgeLecture.doitRelire(lecture, lecture + AgeLecture.DELAI_MINIMAL_MS - 1L))
    }

    @Test
    fun `au delai minimal exactement, on relit`() {
        val lecture = 1_000_000L
        assertTrue(AgeLecture.doitRelire(lecture, lecture + AgeLecture.DELAI_MINIMAL_MS))
    }

    @Test
    fun `une horloge qui a recule ne declenche pas une relecture en boucle`() {
        // Correction NTP, pile de carte mère vide, utilisateur qui change la date : l'âge devient
        // négatif. Le traiter comme « très vieux » relirait aussi vite que le réseau le permet,
        // sur une connexion que le gérant paie au mégaoctet.
        val lecture = 1_000_000L
        assertFalse(AgeLecture.doitRelire(lecture, maintenant = lecture - heure))
    }

    @Test
    fun `l intervalle automatique reste raisonnable pour une connexion payee au megaoctet`() {
        // Épinglé : descendre à la minute multiplierait par cinq le coût réseau pour un gain que
        // personne ne remarquerait ; monter à l'heure ramènerait le défaut qu'on corrige.
        assertEquals(5L * 60L * 1000L, AgeLecture.INTERVALLE_RELECTURE_MS)
        // Et le plancher doit rester strictement sous l'intervalle, sinon la relecture périodique
        // serait elle-même refusée par le plancher et plus rien ne se relirait jamais.
        assertTrue(AgeLecture.DELAI_MINIMAL_MS < AgeLecture.INTERVALLE_RELECTURE_MS)
    }

    // --- Le texte affiché ----------------------------------------------------------------------

    @Test
    fun `sans lecture, le bandeau n affiche rien plutot qu un zero`() {
        // Le jeu de démonstration : afficher « lu il y a 0 min » sur des données qui ne viennent
        // d'aucune lecture serait un mensonge, et la mention n'a pas lieu d'être.
        assertEquals("", AgeLecture.texte(null, 1_000L, "fr"))
    }

    @Test
    fun `sous la minute, c est a l instant`() {
        val t = 10L * jour
        assertEquals("Lu à l'instant", AgeLecture.texte(t, t, "fr"))
        assertEquals("Lu à l'instant", AgeLecture.texte(t, t + 59_999L, "fr"))
    }

    @Test
    fun `les minutes, puis les heures, puis les jours`() {
        val t = 100L * jour
        assertEquals("Lu il y a 1 min", AgeLecture.texte(t, t + minute, "fr"))
        assertEquals("Lu il y a 59 min", AgeLecture.texte(t, t + 59L * minute, "fr"))
        assertEquals("Lu il y a 1 h", AgeLecture.texte(t, t + heure, "fr"))
        assertEquals("Lu il y a 23 h", AgeLecture.texte(t, t + 23L * heure, "fr"))
        assertEquals("Lu il y a 1 jour", AgeLecture.texte(t, t + jour, "fr"))
        assertEquals("Lu il y a 3 jours", AgeLecture.texte(t, t + 3L * jour, "fr"))
    }

    @Test
    fun `le singulier et le pluriel des jours`() {
        // Un PC resté allumé le week-end : « 2 jour » se remarque, et fait douter du reste.
        val t = 100L * jour
        assertTrue(AgeLecture.texte(t, t + jour, "fr").endsWith("1 jour"))
        assertTrue(AgeLecture.texte(t, t + 2L * jour, "fr").endsWith("2 jours"))
        assertTrue(AgeLecture.texte(t, t + jour, "en").endsWith("1 day ago"))
        assertTrue(AgeLecture.texte(t, t + 2L * jour, "en").endsWith("2 days ago"))
    }

    @Test
    fun `une horloge qui a recule affiche a l instant et non une duree negative`() {
        val t = 100L * jour
        assertEquals("Lu à l'instant", AgeLecture.texte(t, t - heure, "fr"))
    }

    @Test
    fun `les trois langues repondent, et une langue inconnue retombe sur l anglais`() {
        val t = 100L * jour
        listOf(0L, minute, heure, jour).forEach { age ->
            listOf("mg", "fr", "en").forEach { langue ->
                assertTrue(
                    "Texte vide pour age=$age langue=$langue",
                    AgeLecture.texte(t, t + age, langue).isNotBlank()
                )
            }
            assertEquals(
                AgeLecture.texte(t, t + age, "en"),
                AgeLecture.texte(t, t + age, "xx")
            )
        }
    }
}
