package com.example.util

import com.example.data.model.Product
import com.example.data.model.Sale
import com.example.data.model.SoldItem
import java.util.Calendar

/**
 * Le coût d'achat des marchandises vendues (COGS), le bénéfice et le taux de marge.
 *
 * **Pourquoi hors des écrans.** La formule du coût unitaire était écrite **quatre fois** — deux
 * fois dans `DashboardScreen` (coût des ventes, coût des retours), une fois dans le « Kajy momba ny
 * Vola » de `SalesHistoryScreen`, et le taux de marge deux fois. Ce sont les chiffres sur lesquels
 * le gérant décide de ses prix : une copie modifiée et l'autre oubliée lui montrerait deux
 * bénéfices différents pour la même journée selon l'écran ouvert, sans qu'aucune erreur
 * n'apparaisse nulle part.
 *
 * **Le repli vers `wholesalePrice` des écrans était du CODE MORT, et le retirer ne change rien à
 * l'exécution — c'est le constat le plus utile de cette extraction.** Les deux écrans écrivaient
 * `prod?.prixAchatUniteBase ?: prod?.wholesalePrice ?: 0.0`. Or `Product.prixAchatUniteBase` n'est
 * **pas nullable** (`Double = 0.0`) : dès que `prod` existe, le premier terme vaut toujours quelque
 * chose, donc le `?:` ne tombe JAMAIS sur `wholesalePrice` ; et quand `prod` est nul, le second
 * terme est nul aussi. L'expression se réduisait donc à `prod?.prixAchatUniteBase ?: 0.0`.
 *
 * **Et il valait mieux qu'il soit mort.** `wholesalePrice` est un prix de **VENTE** en gros, pas un
 * coût d'achat — la section 32 a précisément corrigé le réapprovisionnement qui les confondait.
 * Le réveiller ferait calculer la marge d'un produit contre son propre prix de vente, donc
 * l'écraserait. Le repli est retiré au lieu d'être réparé.
 *
 * **Ce qui reste, et qui est volontaire :** un produit dont le coût d'achat n'a jamais été saisi
 * compte pour **zéro de coût**, ce qui fait apparaître sa vente comme du bénéfice pur. C'est
 * optimiste, mais l'alternative — l'exclure du calcul — masquerait son chiffre d'affaires, ce qui
 * est pire. Le vrai remède est de renseigner le prix d'achat ; le taux de marge d'une boutique qui
 * ne le fait pas est de toute façon sans signification.
 */
object RapportFinancier {

    /**
     * Le coût d'achat unitaire d'un produit, ou 0 s'il n'a jamais été renseigné (ou si le produit
     * a été supprimé depuis la vente). Strictement équivalent à ce que les écrans calculaient —
     * voir le KDoc de l'objet sur le repli mort vers `wholesalePrice`.
     */
    fun coutUnitaire(produit: Product?): Double = produit?.prixAchatUniteBase ?: 0.0

    /**
     * Le coût d'achat d'une liste d'articles vendus (ou retournés).
     *
     * `produitsParId` plutôt qu'une `List<Product>` parcourue par `find` : les écrans faisaient un
     * `allProducts.find { it.id == … }` **par article de chaque vente**, donc un parcours complet
     * du catalogue par ligne. Sur 30 jours de ventes d'une boutique de 2000 références, cela se
     * compte en millions de comparaisons à chaque recomposition.
     */
    fun coutMarchandises(articles: List<SoldItem>, produitsParId: Map<Int, Product>): Double =
        articles.sumOf { article ->
            coutUnitaire(produitsParId[article.productId]) * article.quantity
        }

    /** Le coût d'achat de tout ce qui a été vendu sur une période. */
    fun coutDesVentes(ventes: List<Sale>, produitsParId: Map<Int, Product>): Double =
        ventes.sumOf { coutMarchandises(it.items, produitsParId) }

    /** Le chiffre d'affaires brut d'une période (avant déduction des retours). */
    fun chiffreAffaires(ventes: List<Sale>): Double = ventes.sumOf { it.totalAmount }

    /** La TVA comprise dans ce chiffre d'affaires (section 20 : les prix sont toujours TTC). */
    fun taxeCollectee(ventes: List<Sale>): Double =
        ventes.sumOf { vente -> vente.items.sumOf { it.taxAmount } }

    /**
     * Le taux de marge, en pourcentage du **coût** et non du chiffre d'affaires — c'est la
     * convention des deux écrans existants, et la changer ferait croire à une chute de rentabilité
     * du jour au lendemain sans qu'aucun prix n'ait bougé.
     *
     * Rend 0 quand le coût est nul : une division par zéro donnerait `Infinity`, qui s'afficherait
     * tel quel dans le tableau de bord.
     */
    fun tauxMarge(benefice: Double, cout: Double): Double =
        if (cout > 0.0) (benefice / cout) * 100.0 else 0.0

    /** Un article agrégé sur la période, pour le classement des meilleures ventes. */
    data class VenteAgregee(
        val nom: String,
        val quantite: Double,
        val chiffreAffaires: Double
    )

    /**
     * Les articles les plus vendus de la période, **classés par chiffre d'affaires** et non par
     * quantité : vendre trois cents sachets à 100 Ar compte moins pour la boutique qu'un carton à
     * 50 000 Ar, et c'est la rentabilité que le gérant regarde.
     *
     * **Le regroupement se fait par NOM d'article et non par `productId`**, à l'identique de
     * l'écran Android. Deux produits portant exactement le même nom se retrouvent donc fondus. Le
     * comportement est conservé parce que c'est le nom que le gérant lit dans la liste, et qu'un
     * classement par id afficherait deux lignes identiques qu'il prendrait pour un bug — mais c'est
     * une limite réelle, pas une subtilité.
     */
    fun meilleuresVentes(ventes: List<Sale>, limite: Int = 8): List<VenteAgregee> {
        val parNom = LinkedHashMap<String, VenteAgregee>()
        ventes.forEach { vente ->
            vente.items.forEach { article ->
                val montant = article.price * article.quantity
                val existant = parNom[article.name]
                parNom[article.name] = if (existant == null) {
                    VenteAgregee(article.name, article.quantity, montant)
                } else {
                    existant.copy(
                        quantite = existant.quantite + article.quantity,
                        chiffreAffaires = existant.chiffreAffaires + montant
                    )
                }
            }
        }
        return parNom.values.sortedByDescending { it.chiffreAffaires }.take(limite)
    }

    /**
     * Le chiffre d'affaires jour par jour, pour le graphique en barres : `nombreDeJours` valeurs à
     * partir de `debut`, dans l'ordre chronologique.
     *
     * **Les bornes sont calculées par `Calendar` et non par un multiple de 86 400 000 ms**, et ce
     * n'est pas du zèle : une journée ne fait pas toujours 24 heures (changement d'heure), et une
     * arithmétique en millisecondes décalerait progressivement les barres d'un jour sur l'autre.
     * Un jour vide vaut **0** et non une barre absente : un trou dans le graphique ferait croire à
     * une donnée manquante là où il n'y a simplement pas eu de vente.
     */
    fun chiffreAffairesParJour(
        ventes: List<Sale>,
        debut: Long,
        nombreDeJours: Int
    ): List<Double> {
        if (nombreDeJours <= 0) return emptyList()
        val calendrier = Calendar.getInstance().apply { timeInMillis = debut }
        return (0 until nombreDeJours).map { index ->
            val jour = (calendrier.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, index) }
            val finDeJour = (jour.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
            val borneBasse = jour.timeInMillis
            val borneHaute = finDeJour.timeInMillis
            // Borne haute EXCLUSIVE : avec deux bornes inclusives, une vente tombant exactement à
            // la milliseconde de bascule serait comptée dans les deux journées.
            ventes.filter { it.timestamp >= borneBasse && it.timestamp < borneHaute }
                .sumOf { it.totalAmount }
        }
    }

    /** La fenêtre d'analyse : son premier instant et son nombre de jours. */
    data class Periode(val debut: Long, val nombreDeJours: Int)

    /** Valeur de [periode] désignant « ce mois-ci » plutôt qu'un nombre de jours glissant. */
    const val CE_MOIS = -1

    /**
     * La fenêtre d'analyse du tableau de bord : `7`, `30`, ou [CE_MOIS].
     *
     * **Le début est ramené à MINUIT**, et c'est ce qui rend « 7 jours » honnête : sans cela la
     * fenêtre démarrerait à l'heure courante il y a six jours, donc la première barre du graphique
     * ne couvrirait qu'une fraction de journée et paraîtrait anormalement basse.
     *
     * « 7 jours » recule de **six** jours et non de sept : aujourd'hui compte comme le septième.
     * Reculer de sept donnerait huit barres pour un libellé qui en annonce sept.
     */
    fun periode(choix: Int, maintenant: Long = System.currentTimeMillis()): Periode {
        val calendrier = Calendar.getInstance().apply {
            timeInMillis = maintenant
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val jourDuMois = calendrier.get(Calendar.DAY_OF_MONTH)
        return when (choix) {
            7 -> {
                calendrier.add(Calendar.DAY_OF_YEAR, -6)
                Periode(calendrier.timeInMillis, 7)
            }
            30 -> {
                calendrier.add(Calendar.DAY_OF_YEAR, -29)
                Periode(calendrier.timeInMillis, 30)
            }
            // Ce mois-ci : du 1er à AUJOURD'HUI, pas jusqu'à la fin du mois. Des barres vides pour
            // des jours à venir donneraient l'impression d'un effondrement des ventes.
            else -> {
                calendrier.set(Calendar.DAY_OF_MONTH, 1)
                Periode(calendrier.timeInMillis, jourDuMois)
            }
        }
    }
}
