package com.example

import com.example.data.model.Product
import com.example.data.model.Sale
import com.example.data.model.SoldItem
import com.example.util.RapportFinancier
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ce sont les chiffres sur lesquels le gérant décide de ses prix. La formule existait en quatre
 * exemplaires : une copie modifiée et l'autre oubliée lui montreraient deux bénéfices différents
 * pour la même journée selon l'écran ouvert, sans qu'aucune erreur n'apparaisse.
 */
class RapportFinancierTest {

    private fun produit(id: Int, prix: Double, coutAchat: Double = 0.0, gros: Double? = null) =
        Product(
            id = id,
            name = "Produit $id",
            price = prix,
            category = "Alimentation",
            stock = 10.0,
            prixAchatUniteBase = coutAchat,
            wholesalePrice = gros
        )

    private fun vente(vararg articles: SoldItem, horodatage: Long = 1_000L) = Sale(
        id = 1,
        timestamp = horodatage,
        items = articles.toList(),
        totalAmount = articles.sumOf { it.price * it.quantity }
    )

    private fun article(produitId: Int, prix: Double, quantite: Double, taxable: Boolean = false, taux: Double = 0.0) =
        SoldItem(
            productId = produitId,
            name = "Produit $produitId",
            quantity = quantite,
            price = prix,
            taxable = taxable,
            tauxTaxe = taux
        )

    @Test
    fun `le cout retenu est le prix d'achat, jamais le prix de vente en gros`() {
        // `prixAchatUniteBase` est le coût payé au fournisseur ; `wholesalePrice` est un prix de
        // VENTE en gros (section 32). Calculer la marge contre un prix de vente l'écraserait.
        val p = produit(1, prix = 5000.0, coutAchat = 3000.0, gros = 4500.0)
        assertEquals(3000.0, RapportFinancier.coutUnitaire(p), 1e-9)
    }

    @Test
    fun `le prix de gros n'est JAMAIS utilise comme cout d'achat`() {
        // Le repli des écrans était du code mort (`prixAchatUniteBase` n'est pas nullable) — et il
        // valait mieux : le réveiller ferait calculer la marge d'un produit contre son propre prix
        // de vente. Ce test fige le retrait pour que personne ne le « répare ».
        val p = produit(1, prix = 5000.0, coutAchat = 0.0, gros = 4500.0)
        assertEquals(0.0, RapportFinancier.coutUnitaire(p), 1e-9)
    }

    @Test
    fun `un produit sans cout renseigne compte pour zero`() {
        // Comportement conservé à l'identique, et volontairement : l'exclure du calcul masquerait
        // son chiffre d'affaires, ce qui serait pire qu'un bénéfice optimiste.
        assertEquals(0.0, RapportFinancier.coutUnitaire(produit(1, prix = 5000.0)), 1e-9)
    }

    @Test
    fun `un produit introuvable compte pour zero et ne leve pas`() {
        // Cas réel : un produit supprimé après la vente. L'historique doit rester lisible.
        assertEquals(0.0, RapportFinancier.coutUnitaire(null), 1e-9)
        val articles = listOf(article(produitId = 99, prix = 5000.0, quantite = 2.0))
        assertEquals(0.0, RapportFinancier.coutMarchandises(articles, emptyMap()), 1e-9)
    }

    @Test
    fun `le cout des ventes somme chaque article de chaque vente`() {
        val produits = mapOf(
            1 to produit(1, prix = 5000.0, coutAchat = 3000.0),
            2 to produit(2, prix = 2000.0, coutAchat = 1200.0)
        )
        val ventes = listOf(
            vente(article(1, 5000.0, 2.0), article(2, 2000.0, 3.0)),
            vente(article(1, 5000.0, 1.0))
        )
        // (3000x2 + 1200x3) + 3000x1
        assertEquals(6000.0 + 3600.0 + 3000.0, RapportFinancier.coutDesVentes(ventes, produits), 1e-9)
    }

    @Test
    fun `les quantites decimales d'un produit au poids traversent le calcul`() {
        val produits = mapOf(1 to produit(1, prix = 4000.0, coutAchat = 2800.0))
        val ventes = listOf(vente(article(1, 4000.0, 0.75)))
        assertEquals(2100.0, RapportFinancier.coutDesVentes(ventes, produits), 1e-9)
        assertEquals(3000.0, RapportFinancier.chiffreAffaires(ventes), 1e-9)
    }

    @Test
    fun `le taux de marge est calcule sur le cout et non sur le chiffre d'affaires`() {
        // Convention des deux écrans existants : la changer ferait croire à une chute de
        // rentabilité du jour au lendemain sans qu'aucun prix n'ait bougé.
        assertEquals(50.0, RapportFinancier.tauxMarge(benefice = 1500.0, cout = 3000.0), 1e-9)
    }

    @Test
    fun `un cout nul rend zero et jamais l'infini`() {
        // Une division par zéro donnerait `Infinity`, qui s'afficherait tel quel dans le tableau
        // de bord — « Marge : Infinity % ».
        assertEquals(0.0, RapportFinancier.tauxMarge(benefice = 5000.0, cout = 0.0), 1e-9)
        assertEquals(0.0, RapportFinancier.tauxMarge(benefice = 0.0, cout = 0.0), 1e-9)
    }

    @Test
    fun `une marge negative est rendue telle quelle`() {
        // Vendre en dessous du prix d'achat est précisément ce que le gérant doit voir ; ramener
        // ce taux à zéro cacherait une perte.
        assertEquals(-20.0, RapportFinancier.tauxMarge(benefice = -600.0, cout = 3000.0), 1e-9)
    }

    @Test
    fun `la TVA est isolee du prix TTC et jamais ajoutee`() {
        val ventes = listOf(vente(article(1, 11500.0, 1.0, taxable = true, taux = 15.0)))
        assertEquals(11500.0, RapportFinancier.chiffreAffaires(ventes), 1e-9)
        assertEquals(1500.0, RapportFinancier.taxeCollectee(ventes), 1e-6)
    }

    @Test
    fun `une periode sans vente totalise zero partout`() {
        // Le cas le plus fréquent au premier lancement d'un tableau de bord.
        assertEquals(0.0, RapportFinancier.chiffreAffaires(emptyList()), 1e-9)
        assertEquals(0.0, RapportFinancier.coutDesVentes(emptyList(), emptyMap()), 1e-9)
        assertEquals(0.0, RapportFinancier.taxeCollectee(emptyList()), 1e-9)
    }

    @Test
    fun `les meilleures ventes sont classees par chiffre d'affaires et non par quantite`() {
        // 300 sachets à 100 Ar = 30 000 ; 1 carton à 50 000 Ar = 50 000. C'est le carton qui
        // compte le plus pour la boutique, et c'est la rentabilité que le gérant regarde.
        val ventes = listOf(
            vente(article(1, 100.0, 300.0)),
            vente(article(2, 50000.0, 1.0))
        )
        val top = RapportFinancier.meilleuresVentes(ventes)
        assertEquals(2, top.size)
        assertEquals(50000.0, top.first().chiffreAffaires, 1e-9)
        assertEquals(300.0, top[1].quantite, 1e-9)
    }

    @Test
    fun `le meme article vendu plusieurs fois est agrege en une seule ligne`() {
        val ventes = listOf(
            vente(article(1, 3500.0, 2.0)),
            vente(article(1, 3500.0, 3.0))
        )
        val top = RapportFinancier.meilleuresVentes(ventes)
        assertEquals(1, top.size)
        assertEquals(5.0, top.first().quantite, 1e-9)
        assertEquals(17500.0, top.first().chiffreAffaires, 1e-9)
    }

    @Test
    fun `la limite tronque le classement sans le desordonner`() {
        val ventes = (1..12).map { n -> vente(article(n, n * 1000.0, 1.0)) }
        val top = RapportFinancier.meilleuresVentes(ventes, limite = 3)
        assertEquals(3, top.size)
        assertEquals(listOf(12000.0, 11000.0, 10000.0), top.map { it.chiffreAffaires })
    }

    @Test
    fun `une periode sans vente ne rend aucune meilleure vente`() {
        assertEquals(emptyList<RapportFinancier.VenteAgregee>(), RapportFinancier.meilleuresVentes(emptyList()))
    }

    @Test
    fun `le chiffre d'affaires par jour rend une valeur par jour, zero comprise`() {
        // Un jour vide vaut 0 et non une barre absente : un trou dans le graphique ferait croire à
        // une donnée manquante là où il n'y a simplement pas eu de vente.
        val debut = debutDeJournee()
        val unJour = 24L * 60 * 60 * 1000
        val ventes = listOf(
            vente(article(1, 1000.0, 1.0), horodatage = debut + 3600_000),
            vente(article(1, 2000.0, 1.0), horodatage = debut + 2 * unJour + 3600_000)
        )
        val parJour = RapportFinancier.chiffreAffairesParJour(ventes, debut, 3)
        assertEquals(3, parJour.size)
        assertEquals(1000.0, parJour[0], 1e-9)
        assertEquals(0.0, parJour[1], 1e-9)
        assertEquals(2000.0, parJour[2], 1e-9)
    }

    @Test
    fun `une vente anterieure a la periode n'est comptee dans aucun jour`() {
        val debut = debutDeJournee()
        val ventes = listOf(vente(article(1, 9999.0, 1.0), horodatage = debut - 1))
        assertEquals(listOf(0.0, 0.0), RapportFinancier.chiffreAffairesParJour(ventes, debut, 2))
    }

    @Test
    fun `une vente a la milliseconde de bascule n'est comptee qu'une fois`() {
        // Avec deux bornes inclusives, elle apparaîtrait dans les deux journées — donc le total
        // du graphique dépasserait le chiffre d'affaires réel.
        val debut = debutDeJournee()
        val calendrier = Calendar.getInstance().apply { timeInMillis = debut }
        calendrier.add(Calendar.DAY_OF_YEAR, 1)
        val ventes = listOf(vente(article(1, 5000.0, 1.0), horodatage = calendrier.timeInMillis))
        val parJour = RapportFinancier.chiffreAffairesParJour(ventes, debut, 2)
        assertEquals(0.0, parJour[0], 1e-9)
        assertEquals(5000.0, parJour[1], 1e-9)
        assertEquals(5000.0, parJour.sum(), 1e-9)
    }

    @Test
    fun `un nombre de jours nul ou negatif rend une liste vide sans lever`() {
        val debut = debutDeJournee()
        assertEquals(emptyList<Double>(), RapportFinancier.chiffreAffairesParJour(emptyList(), debut, 0))
        assertEquals(emptyList<Double>(), RapportFinancier.chiffreAffairesParJour(emptyList(), debut, -3))
    }

    /** Minuit du jour courant : les bornes de journée se calculent sur un vrai calendrier. */
    private fun debutDeJournee(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    @Test
    fun `sept jours recule de SIX jours, aujourd'hui comptant comme le septieme`() {
        // Reculer de sept donnerait huit barres pour un libellé qui en annonce sept.
        val periode = RapportFinancier.periode(7, maintenant = instant(2026, Calendar.MARCH, 15, heure = 14))
        assertEquals(7, periode.nombreDeJours)
        assertEquals(instant(2026, Calendar.MARCH, 9), periode.debut)
    }

    @Test
    fun `le debut est ramene a minuit et non a l'heure courante`() {
        // Sans cela la première barre ne couvrirait qu'une fraction de journée et paraîtrait
        // anormalement basse — un faux signal de chute des ventes.
        val periode = RapportFinancier.periode(30, maintenant = instant(2026, Calendar.MARCH, 15, heure = 23))
        assertEquals(30, periode.nombreDeJours)
        assertEquals(instant(2026, Calendar.FEBRUARY, 14), periode.debut)
    }

    @Test
    fun `ce mois-ci va du premier a AUJOURD'HUI et non a la fin du mois`() {
        // Des barres vides pour des jours à venir donneraient l'impression d'un effondrement.
        val periode = RapportFinancier.periode(
            RapportFinancier.CE_MOIS,
            maintenant = instant(2026, Calendar.MARCH, 15, heure = 9)
        )
        assertEquals(15, periode.nombreDeJours)
        assertEquals(instant(2026, Calendar.MARCH, 1), periode.debut)
    }

    @Test
    fun `ce mois-ci le premier du mois rend un seul jour`() {
        // Zéro jour rendrait un graphique vide le 1er de chaque mois.
        val periode = RapportFinancier.periode(
            RapportFinancier.CE_MOIS,
            maintenant = instant(2026, Calendar.MARCH, 1, heure = 8)
        )
        assertEquals(1, periode.nombreDeJours)
        assertEquals(instant(2026, Calendar.MARCH, 1), periode.debut)
    }

    @Test
    fun `un choix inconnu est traite comme ce mois-ci plutot que de lever`() {
        // Préférence restaurée depuis une sauvegarde, valeur corrompue : un tableau de bord qui
        // plante au lieu d'afficher une période par défaut serait pire.
        val periode = RapportFinancier.periode(999, maintenant = instant(2026, Calendar.MARCH, 15))
        assertEquals(instant(2026, Calendar.MARCH, 1), periode.debut)
        assertEquals(15, periode.nombreDeJours)
    }

    @Test
    fun `la periode se branche sur le decoupage par jour sans trou ni doublon`() {
        // Le lien entre les deux fonctions : autant de barres que de jours annoncés.
        val periode = RapportFinancier.periode(7, maintenant = instant(2026, Calendar.MARCH, 15))
        val parJour = RapportFinancier.chiffreAffairesParJour(emptyList(), periode.debut, periode.nombreDeJours)
        assertEquals(7, parJour.size)
    }

    @Test
    fun `le seuil de marge faible est unique et vaut 15 pourcent`() {
        // Ce seuil était en dur à quatre endroits (calculateur de marges x2, Tableau de bord,
        // Rapports de bureau). L'épingler ici rend un changement VISIBLE : le déplacer demande de
        // toucher ce test, donc de constater qu'il s'applique aux trois écrans à la fois.
        assertEquals(15.0, RapportFinancier.SEUIL_MARGE_FAIBLE, 1e-9)
    }

    @Test
    fun `le seuil encadre bien les trois zones de tarification`() {
        // La frontière est STRICTE : à 15 % exactement, la marge n'est plus « faible ». Les trois
        // écrans écrivent `< SEUIL`, donc ce test fixe le sens de cette comparaison.
        val sousLeSeuil = RapportFinancier.tauxMarge(benefice = 1400.0, cout = 10000.0)
        val auSeuil = RapportFinancier.tauxMarge(benefice = 1500.0, cout = 10000.0)
        assertEquals(14.0, sousLeSeuil, 1e-9)
        assertEquals(15.0, auSeuil, 1e-9)
        assertTrue(sousLeSeuil < RapportFinancier.SEUIL_MARGE_FAIBLE)
        assertTrue(auSeuil >= RapportFinancier.SEUIL_MARGE_FAIBLE)
    }

    /** Un instant précis et reproductible, pour que ces tests ne dépendent pas du jour d'exécution. */
    private fun instant(annee: Int, mois: Int, jour: Int, heure: Int = 0): Long =
        Calendar.getInstance().apply {
            set(annee, mois, jour, heure, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
}
