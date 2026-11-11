package com.example.util

/**
 * L'arithmétique de mise en page d'un tableau imprimé : largeurs de colonnes et découpage en pages.
 *
 * ## Pourquoi c'est à part, et testé
 *
 * Le dessin lui-même ne peut pas être partagé : Android trace sur un `Canvas`, un PC sur un
 * `Graphics2D`, et mesurer un texte demande une police concrète. Mais **le calcul des positions
 * ne dépend d'aucune des deux**, et c'est précisément lui qui casse en silence : une largeur de
 * colonne mal répartie ne lève aucune erreur — elle sort la dernière colonne de la feuille, et le
 * gérant découvre que son inventaire imprimé n'a pas de prix seulement après avoir imprimé
 * quarante pages.
 *
 * ## Les gardes ne sont pas du zèle
 *
 * Une feuille a des marges, une police a une hauteur, et rien ne garantit qu'il reste de la place :
 * un format exotique ou une marge trop grande peut donner une hauteur utile nulle ou négative.
 * Sans garde, le nombre de lignes par page vaudrait zéro et la boucle d'impression tournerait
 * indéfiniment sans jamais avancer.
 */
object MiseEnPageTableau {

    /**
     * La largeur de chaque colonne, répartie selon [poids] sur [largeurUtile].
     *
     * **Les poids sont relatifs**, pas des pourcentages : ils viennent de `Tableau.poidsColonnes`,
     * où ils expriment seulement qu'une colonne « Produit » mérite deux fois la place d'une
     * colonne « Date ». Leur somme est donc quelconque, et la normalisation se fait ici.
     *
     * **Dès qu'UN poids est nul ou négatif, toute la répartition devient égale.** Ne mettre à zéro
     * que la colonne fautive donnerait une colonne de largeur nulle, donc **invisible à
     * l'impression** — un prix ou un solde qui disparaît sans que rien ne le signale. Un tableau
     * aux colonnes régulières se remarque et reste complet ; c'est le bon échec pour une faute de
     * frappe dans une constante.
     */
    fun largeursColonnes(largeurUtile: Float, poids: List<Float>): List<Float> {
        if (poids.isEmpty()) return emptyList()
        val largeur = if (largeurUtile > 0f) largeurUtile else 0f
        val somme = poids.sum()
        if (poids.any { it <= 0f } || somme <= 0f) {
            val egale = largeur / poids.size
            return List(poids.size) { egale }
        }
        return poids.map { largeur * it / somme }
    }

    /** La position horizontale du bord gauche de chaque colonne, à partir de [margeGauche]. */
    fun positionsColonnes(margeGauche: Float, largeurs: List<Float>): List<Float> {
        var x = margeGauche
        return largeurs.map { l ->
            val depart = x
            x += l
            depart
        }
    }

    /**
     * Le nombre de lignes de données qui tiennent sur une page.
     *
     * **Au moins une**, toujours : si la hauteur utile ne suffit pas même pour une ligne, rendre
     * zéro ferait tourner la boucle d'impression sans jamais avancer. Imprimer une ligne qui
     * déborde un peu vaut mieux qu'une impression qui ne s'arrête jamais.
     */
    fun lignesParPage(hauteurUtile: Float, hauteurEntete: Float, hauteurLigne: Float): Int {
        if (hauteurLigne <= 0f) return 1
        val restant = hauteurUtile - hauteurEntete
        if (restant < hauteurLigne) return 1
        return (restant / hauteurLigne).toInt().coerceAtLeast(1)
    }

    /**
     * Le nombre de pages nécessaires pour [nombreDeLignes].
     *
     * **Un tableau vide fait une page, pas zéro** : elle porte le titre et les en-têtes, ce qui est
     * l'information « il n'y avait rien dans cette liste ». Rendre zéro ferait croire à une panne
     * d'impression. (Côté fichier, `ExportFichier` refuse en revanche d'écrire une liste vide :
     * là, l'absence de fichier est elle-même le message.)
     */
    fun nombreDePages(nombreDeLignes: Int, lignesParPage: Int): Int {
        if (lignesParPage <= 0) return 1
        if (nombreDeLignes <= 0) return 1
        return (nombreDeLignes + lignesParPage - 1) / lignesParPage
    }

    /**
     * L'intervalle de lignes à dessiner sur la page [page] (comptée à partir de 0), ou `null` si
     * cette page est au-delà du tableau.
     *
     * `null` est ce que l'impression Java attend pour dire « il n'y a plus rien » : le système
     * demande les pages une par une et ne connaît pas d'avance la fin.
     */
    fun lignesDeLaPage(page: Int, nombreDeLignes: Int, lignesParPage: Int): IntRange? {
        if (page < 0 || lignesParPage <= 0) return null
        val debut = page * lignesParPage
        // Une page d'en-têtes seuls reste légitime pour un tableau vide, mais une DEUXIÈME page
        // vide ne l'est pas.
        if (debut >= nombreDeLignes) return if (page == 0 && nombreDeLignes == 0) IntRange.EMPTY else null
        val fin = (debut + lignesParPage - 1).coerceAtMost(nombreDeLignes - 1)
        return debut..fin
    }
}
