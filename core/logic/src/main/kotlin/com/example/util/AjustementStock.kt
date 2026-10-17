package com.example.util

/**
 * Le pas des boutons `+` / `-` d'ajustement de stock.
 *
 * **Pourquoi hors de l'écran.** Cette règle était écrite **deux fois** dans la carte de l'écran
 * Stock d'Android — une fois pour le bouton moins, une fois pour le plus — et la porter sur le
 * bureau en aurait fait une troisième et une quatrième copie. Or les deux copies existantes
 * pouvaient déjà diverger sans que rien ne le signale : un `+` qui ajoute 1 kg quand le `-` en
 * retire 0,25 ne lève aucune erreur, il fausse simplement le stock, et personne ne le remarque
 * avant un inventaire.
 *
 * **Pourquoi 0,25 pour le poids et le volume.** Une épicerie vend du riz et de l'huile au quart de
 * kilo ou de litre ; monter par unités entières y serait inutilisable. Pour tout le reste — pièce,
 * paquet, carton, plaquette, casier — l'unité entière est la seule qui ait un sens : on ne reçoit
 * pas un quart de carton.
 */
object AjustementStock {

    /** Les unités comptées en fractions. Comparaison en minuscules, comme le reste du projet. */
    private val UNITES_FRACTIONNABLES = setOf("litre", "kilogramme")

    /** Le pas d'un appui sur `+` ou `-` pour un produit vendu dans cette unité. */
    fun pas(unite: String): Double =
        if (unite.lowercase() in UNITES_FRACTIONNABLES) 0.25 else 1.0

    /**
     * Le stock après un appui sur `-`. **Jamais négatif** : un stock négatif n'a aucun sens
     * physique et contaminerait tous les calculs qui le lisent ensuite (valeur d'inventaire,
     * alertes, marges).
     */
    fun apresRetrait(stock: Double, unite: String): Double =
        (stock - pas(unite)).coerceAtLeast(0.0)

    /** Le stock après un appui sur `+`. */
    fun apresAjout(stock: Double, unite: String): Double = stock + pas(unite)
}
