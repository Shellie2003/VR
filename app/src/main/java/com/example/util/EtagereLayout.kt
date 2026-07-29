package com.example.util

/**
 * Calcule où insérer un nouvel élément dans une rangée (étagères, gauche/droite) ou une colonne
 * (niveaux d'une étagère, haut/bas) extensible.
 *
 * Fonction pure, sans dépendance Android : la position n'est volontairement PAS contiguë (on
 * prend `min - 1` ou `max + 1`, jamais un décalage des positions existantes). Décaler les
 * éléments déjà en place à chaque ajout ferait glisser silencieusement une étagère ou un niveau
 * déjà rangé — avec ses produits — vers une autre colonne/ligne à chaque fois qu'on en ajoute un
 * nouveau. Avec des positions non contiguës, un élément existant ne change jamais de place.
 */
object EtagereLayout {

    enum class Cote { DEBUT, FIN }

    /**
     * Position du nouvel élément. Sans élément existant, la première position est toujours 0,
     * quel que soit le côté choisi — il n'y a rien avant ou après quoi se placer.
     */
    fun nouvellePosition(positionsExistantes: List<Int>, cote: Cote): Int {
        if (positionsExistantes.isEmpty()) return 0
        return when (cote) {
            Cote.DEBUT -> positionsExistantes.min() - 1
            Cote.FIN -> positionsExistantes.max() + 1
        }
    }
}
