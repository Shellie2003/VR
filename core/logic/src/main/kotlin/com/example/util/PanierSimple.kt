package com.example.util

/**
 * Les deux gestes d'un panier réduit à l'essentiel : ajouter une unité, en retirer une.
 *
 * **Pourquoi hors de l'écran.** Cette règle vivait dans la fenêtre de bureau, en deux lambdas
 * posées au milieu d'un `when` de navigation. Elle y était invérifiable, et c'est pourtant le seul
 * endroit de l'écran où une erreur se traduirait par un mauvais chiffre devant le client. Sortie
 * ici, elle est couverte par des tests qui s'exécutent dans la CI — et le jour où la caisse de
 * bureau existera, elle est déjà écrite.
 *
 * **Une `Map` immuable plutôt qu'une collection mutable**, et ce n'est pas un détail de style :
 * Compose ne recompose que sur un changement de référence. Muter une `MutableMap` en place
 * laisserait l'écran afficher l'ancienne quantité, un bug qui ne se voit qu'à l'usage et qu'on
 * passerait un temps déraisonnable à chercher dans l'affichage plutôt que dans l'état.
 */
object PanierSimple {

    /** Ajoute une unité (ou `quantite`) à la ligne du produit, en la créant si besoin. */
    fun ajouter(panier: Map<Int, Double>, produitId: Int, quantite: Double = 1.0): Map<Int, Double> {
        if (quantite <= 0.0) return panier
        val actuel = panier[produitId] ?: 0.0
        return panier + (produitId to actuel + quantite)
    }

    /**
     * Retire une unité. **La ligne disparaît dès qu'elle tombe à 1 ou moins**, au lieu de rester
     * affichée à zéro : c'est ce que fait le bouton « - » de la carte Android, et un badge « 0 »
     * collé sur un produit non commandé serait faux autant qu'inesthétique.
     */
    fun retirer(panier: Map<Int, Double>, produitId: Int, quantite: Double = 1.0): Map<Int, Double> {
        val actuel = panier[produitId] ?: return panier
        val reste = actuel - quantite
        return if (reste <= 0.0) panier - produitId else panier + (produitId to reste)
    }

    /** Nombre total d'unités dans le panier, toutes lignes confondues. */
    fun nombreArticles(panier: Map<Int, Double>): Double = panier.values.sum()
}
