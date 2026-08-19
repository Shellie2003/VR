package com.example.util

import com.example.ui.viewmodel.InventoryViewModel.EtagereAvecNiveaux

/**
 * Un produit retrouvé dans le plan d'étagères, avec tout ce qu'il faut pour y conduire le gérant.
 */
data class EmplacementProduit(
    /**
     * Rang de l'étagère DANS LE PLAN (0 = la première). C'est cet index que l'écran convertit en
     * position de défilement — attention, la `LazyRow` ajoute un bouton « ajouter à gauche » avant
     * la première étagère, donc l'index de défilement vaut celui-ci **+ 1**. Ce décalage est
     * volontairement laissé à l'écran : cette classe décrit le plan, pas la mise en page.
     */
    val indexEtagere: Int,
    val etagereId: Long,
    val etagereNom: String,
    val niveauId: Long,
    val niveauNom: String,
    val produitId: Int,
    val produitNom: String
)

/**
 * « Où est rangé ce produit ? » — la recherche inverse de celle qui existait déjà dans l'écran
 * Étagère.
 *
 * **À ne pas confondre avec la recherche du dialogue de rangement.** Celle-ci répond à « quel
 * produit vais-je poser dans cette case ? » et parcourt donc TOUT le catalogue. Celle-là ne
 * parcourt que ce qui est effectivement RANGÉ, et répond à la question inverse : le client demande
 * un article, le vendeur doit savoir vers quel rayon marcher.
 *
 * Fonction pure, testable sans Compose ni base de données — même principe que
 * [FefoAllocator.repartir] et [EtagereColors.normaliser].
 */
object RechercheEtagere {

    /** En dessous de ce nombre de caractères, on ne cherche pas : tout le plan remonterait. */
    const val LONGUEUR_MINIMALE = 2

    /**
     * @param plan le plan tel que l'écran l'affiche (étagères dans l'ordre, niveaux et produits).
     * @param requete ce que le vendeur a tapé.
     * @return les emplacements correspondants, dans l'ordre du plan (gauche à droite, puis niveau).
     *
     * Un même produit peut apparaître PLUSIEURS fois : la table de rangement est volontairement
     * many-to-many, et un article courant peut être stocké à deux endroits (réserve et rayon). On
     * renvoie donc tous les emplacements plutôt que le premier trouvé — c'est au vendeur de choisir
     * le plus proche.
     */
    fun chercher(plan: List<EtagereAvecNiveaux>, requete: String): List<EmplacementProduit> {
        val terme = requete.trim()
        if (terme.length < LONGUEUR_MINIMALE) return emptyList()

        val resultats = mutableListOf<EmplacementProduit>()
        plan.forEachIndexed { indexEtagere, etagereAvecNiveaux ->
            for (niveau in etagereAvecNiveaux.niveaux) {
                for (produit in niveau.produits) {
                    if (!correspond(produit.name, produit.barcode, terme)) continue
                    resultats += EmplacementProduit(
                        indexEtagere = indexEtagere,
                        etagereId = etagereAvecNiveaux.etagere.id,
                        etagereNom = etagereAvecNiveaux.etagere.nom,
                        niveauId = niveau.niveau.id,
                        niveauNom = niveau.niveau.nom,
                        produitId = produit.id,
                        produitNom = produit.name
                    )
                }
            }
        }
        return resultats
    }

    /**
     * Correspondance sur le nom OU le code-barres. Le code-barres est inclus parce que le geste le
     * plus rapide au comptoir est de scanner l'article que le client tend, puis de demander à
     * l'app où en trouver un autre.
     */
    private fun correspond(nom: String, codeBarre: String, terme: String): Boolean =
        nom.contains(terme, ignoreCase = true) ||
            (codeBarre.isNotBlank() && codeBarre.contains(terme, ignoreCase = true))
}
