package com.example.util

import com.example.data.model.ProduitNiveau

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

    /** Ce qu'il faut changer en base pour appliquer le choix du sélecteur de rayon. */
    data class DiffEmplacement(
        /** Ids des liens `ProduitNiveau` à supprimer (tout emplacement autre que celui choisi). */
        val liensASupprimer: List<Long>,
        /** Niveau à lier au produit, ou null si aucune nouvelle écriture n'est nécessaire
         * (aucun choix, ou le produit y était déjà). */
        val niveauAAjouter: Long?
    )

    /**
     * Le sélecteur de rayon de la fiche produit gère UN SEUL emplacement par produit — un modèle
     * plus simple que le many-to-many complet du plan d'étagères (qui, lui, autorise un produit
     * dans plusieurs niveaux à la fois, ex. tête de gondole + emplacement habituel, ajouté depuis
     * l'écran Étagère). Choisir un nouveau niveau ici retire donc tout autre emplacement existant
     * de ce produit ; choisir « Aucun » les retire tous sans en ajouter.
     *
     * Fonction pure : ne lit ni n'écrit rien, ne fait que calculer le diff à appliquer.
     */
    fun diffEmplacementUnique(liensExistants: List<ProduitNiveau>, niveauChoisiId: Long?): DiffEmplacement {
        val aSupprimer = liensExistants.filter { it.niveauId != niveauChoisiId }.map { it.id }
        val dejaPresent = liensExistants.any { it.niveauId == niveauChoisiId }
        val aAjouter = if (niveauChoisiId != null && !dejaPresent) niveauChoisiId else null
        return DiffEmplacement(aSupprimer, aAjouter)
    }
}
