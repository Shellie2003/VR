package com.example.util

/**
 * Le découpage de la note d'un trosa en lignes d'articles.
 *
 * Les articles d'une vente à crédit sont enregistrés dans `Debt.note` sous la forme
 * `"Article (2 x 1 500), Autre (1 x 3 000)"` au moment de l'encaissement. La carte de dette doit
 * rester compacte et tronque donc cette note ; le détail l'affiche en entier, un article par ligne.
 *
 * **Le découpage se fait sur les virgules qui suivent une parenthèse fermante, et c'est tout
 * l'intérêt de cette fonction** : couper sur chaque virgule casserait en deux les noms d'articles
 * qui en contiennent une — « Lait concentré, sucré » deviendrait deux articles fantômes. Un gérant
 * verrait alors dans le détail d'un trosa des articles que le client n'a jamais achetés.
 *
 * Une note saisie **à la main** ne suit pas ce format : elle ressort alors en une seule ligne,
 * telle quelle, plutôt que découpée au hasard.
 */
object NoteTrosa {

    private val SEPARATEUR = Regex("(?<=\\)),\\s*")

    /** Les lignes d'articles de cette note ; liste vide si la note est vide ou blanche. */
    fun articles(note: String): List<String> =
        if (note.isBlank()) {
            emptyList()
        } else {
            note.split(SEPARATEUR).map { it.trim() }.filter { it.isNotEmpty() }
        }
}
