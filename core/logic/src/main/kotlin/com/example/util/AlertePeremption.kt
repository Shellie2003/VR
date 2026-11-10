package com.example.util

/**
 * L'état d'un lot vis-à-vis de sa date de péremption.
 *
 * ## Pourquoi cette classification vit ici
 *
 * La fenêtre d'alerte de 7 jours était écrite **deux fois** : dans `PeremptionScreen` (les
 * compteurs de l'en-tête et la couleur de chaque carte) et dans `InventoryViewModel` (la
 * notification quotidienne). Les deux valaient 7 jours, donc rien ne divergeait — mais c'est un
 * seuil **commercial**, qu'un gérant peut vouloir porter à quinze jours pour un rayon frais. Le
 * changer d'un seul côté donnerait un écran qui signale des lots que la notification ignore, ou
 * l'inverse : l'alerte qu'on ne reçoit pas est précisément celle dont on a besoin.
 *
 * Le port de l'écran sur le bureau en aurait fait une troisième copie.
 *
 * ## Les bornes, et pourquoi elles sont dans cet ordre
 *
 * Un lot déjà dépassé est [PERIME] **même s'il tombe aussi dans la fenêtre d'alerte** : les deux
 * conditions se recouvrent, et l'ordre du `when` décide donc de ce que le gérant lit. « Périmé »
 * l'emporte, parce que c'est l'action la plus urgente — retirer la marchandise, pas la surveiller.
 */
enum class EtatLot { PERIME, BIENTOT, BON }

object AlertePeremption {

    /**
     * La fenêtre d'alerte : un lot qui périme dans moins que cela est signalé.
     *
     * Sept jours, la valeur qu'Android appliquait déjà de part et d'autre. La déplacer ici se fait
     * désormais en un seul endroit — c'est tout l'objet de cette extraction.
     */
    const val FENETRE_ALERTE_MS = 7L * 24 * 60 * 60 * 1000

    /**
     * L'état d'un lot à l'instant [maintenant].
     *
     * `maintenant` est un paramètre et non `System.currentTimeMillis()` : c'est ce qui rend la
     * règle testable sans dépendre du jour d'exécution en CI, et ce qui permet à un écran de figer
     * un instant unique pour toute sa liste — sinon deux cartes rendues à une milliseconde d'écart
     * pourraient se classer différemment.
     */
    fun etat(datePeremption: Long, maintenant: Long): EtatLot = when {
        datePeremption < maintenant -> EtatLot.PERIME
        datePeremption <= maintenant + FENETRE_ALERTE_MS -> EtatLot.BIENTOT
        else -> EtatLot.BON
    }

    /** Le nombre de lots périmés parmi ces dates de péremption. */
    fun compterPerimes(datesPeremption: List<Long>, maintenant: Long): Int =
        datesPeremption.count { etat(it, maintenant) == EtatLot.PERIME }

    /** Le nombre de lots qui périment dans la fenêtre d'alerte, les périmés exclus. */
    fun compterBientot(datesPeremption: List<Long>, maintenant: Long): Int =
        datesPeremption.count { etat(it, maintenant) == EtatLot.BIENTOT }
}
