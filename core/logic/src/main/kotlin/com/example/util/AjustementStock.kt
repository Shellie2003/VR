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
 *
 * **Une divergence réelle, corrigée ici (section 91).** La ligne de panier de la caisse portait une
 * QUATRIÈME copie de cette règle, et elle n'était pas identique : elle reconnaissait aussi les
 * abréviations (`kg`, `L`) et les libellés contenant « kilo » ou « litre ». Un produit dont l'unité
 * valait littéralement `kg` montait donc par quarts dans le panier et par kilos entiers dans
 * l'écran Stock — sur le même produit, dans la même application. La règle retenue est la **plus
 * large** des deux, celle de la caisse : c'est elle qui correspond à l'intention (un produit au
 * poids se compte en fractions, quel que soit le libellé choisi par le gérant), et c'est aussi
 * celle qui était déjà appliquée là où l'argent est en jeu.
 *
 * **Cinq copies au total, dont trois variantes différentes.** La carte produit de la caisse portait
 * encore sa propre détection (`isLiquid`/`isWeight`) pour choisir entre « 1/4 L » et « 1/4 kg ».
 * Cette distinction-là est utile — elle décide du libellé lu par le vendeur — donc elle est
 * conservée, mais sous la forme de [typeUnite] : une seule classification sert désormais le pas
 * d'ajustement ET les libellés de saisie rapide, et les deux ne peuvent plus se contredire.
 */
object AjustementStock {

    /**
     * La nature de l'unité, qui décide à la fois du pas et des libellés de saisie rapide.
     *
     * La distinction liquide / poids n'est pas décorative : la carte produit de la caisse propose
     * « 1/4 L » ou « 1/4 kg » selon le cas, et se tromper ferait lire au vendeur une unité qui
     * n'est pas celle du produit qu'il encaisse.
     */
    enum class TypeUnite { LIQUIDE, POIDS, AUTRE }

    /**
     * Classe une unité. `l` est reconnu **à l'identique et jamais par inclusion** : une seule
     * lettre, et `contains("l")` rendrait fractionnables « Plaquette », « Bouteille », « Flacon »
     * — soit presque tout le catalogue, avec des stocks montant par quarts de plaquette.
     */
    fun typeUnite(unite: String): TypeUnite {
        val u = unite.lowercase()
        return when {
            u.contains("litre") || u == "l" -> TypeUnite.LIQUIDE
            u.contains("kilo") || u.contains("kg") -> TypeUnite.POIDS
            else -> TypeUnite.AUTRE
        }
    }

    /** `true` quand un produit vendu dans cette unité se compte en fractions. */
    fun estFractionnable(unite: String): Boolean = typeUnite(unite) != TypeUnite.AUTRE

    /** Le pas d'un appui sur `+` ou `-` pour un produit vendu dans cette unité. */
    fun pas(unite: String): Double = if (estFractionnable(unite)) 0.25 else 1.0

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
