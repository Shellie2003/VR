package com.example.util

/**
 * Unités de vente proposées dans le formulaire d'ajout d'article, adaptées au métier de la boutique.
 *
 * **Pourquoi dépendre du [ShopMode] plutôt que d'allonger une liste unique.** Une pharmacie a besoin
 * de « plaquette » et « flacon », un bar de « casier » et « canette » — mais noyer ces termes dans
 * une seule liste obligerait chaque épicier à faire défiler des unités qui ne le concerneront
 * jamais, sur un menu déjà long. Chaque métier voit donc le tronc commun, puis SES unités.
 *
 * **Le tronc commun reste en tête et dans son ordre d'origine.** Ce n'est pas cosmétique : `Pièce`
 * est l'unité par défaut de tout produit existant, et des milliers d'articles déjà enregistrés
 * portent ces libellés exacts. Les réordonner ou les renommer ferait que le menu n'afficherait plus
 * l'unité réellement enregistrée sur un produit qu'on rouvre pour le modifier.
 */
object UnitesMesure {

    /** Unités présentes quel que soit le métier — celles de toutes les versions précédentes. */
    private val COMMUNES = listOf(
        "Pièce", "Litre", "Kilogramme", "Paquet", "Carton", "Sac", "Boîte", "Bouteille", "Tasse/Kapoaka"
    )

    /** Conditionnements de pharmacie : ce qui se délivre réellement au comptoir. */
    private val PHARMACIE = listOf(
        "Plaquette", "Comprimé", "Gélule", "Flacon", "Ampoule", "Sachet", "Tube", "Pommade", "Seringue"
    )

    /** Conditionnements de bar : ce qui se commande au fournisseur et ce qui se sert au client. */
    private val BAR = listOf(
        "Casier", "Caseau", "Canette", "Verre", "Demi", "Fût", "Pack", "Dose"
    )

    /** Unités de gros : on achète et revend par contenant entier. */
    private val GROSSISTE = listOf(
        "Palette", "Colis", "Fardeau", "Balle", "Bidon"
    )

    /**
     * Unités proposées pour ce métier : le tronc commun d'abord, puis les unités spécifiques.
     *
     * Une unité déjà enregistrée sur un produit mais absente de la liste du métier courant est
     * ajoutée à la fin par [pourFormulaire] — sans quoi rouvrir un médicament depuis une boutique
     * repassée en mode épicerie afficherait une unité vide, et l'enregistrement l'écraserait
     * silencieusement.
     */
    fun pourMode(mode: ShopMode): List<String> = COMMUNES + when (mode) {
        ShopMode.PHARMACIE -> PHARMACIE
        ShopMode.BAR -> BAR
        ShopMode.GROSSISTE -> GROSSISTE
        ShopMode.DETAIL -> emptyList()
    }

    /**
     * Liste à afficher dans le formulaire, en garantissant que [uniteActuelle] y figure.
     *
     * C'est la protection contre la perte de données décrite plus haut : le mode de la boutique peut
     * changer, ou un produit peut venir d'une synchronisation multi-terminal avec une unité d'un
     * autre métier. Dans tous les cas, l'unité réellement enregistrée doit rester sélectionnable.
     */
    fun pourFormulaire(mode: ShopMode, uniteActuelle: String?): List<String> {
        val base = pourMode(mode)
        val actuelle = uniteActuelle?.trim().orEmpty()
        return if (actuelle.isNotEmpty() && base.none { it.equals(actuelle, ignoreCase = true) }) {
            base + actuelle
        } else {
            base
        }
    }
}
