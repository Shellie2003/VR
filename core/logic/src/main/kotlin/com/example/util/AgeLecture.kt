package com.example.util

/**
 * L'âge d'une lecture de données, en texte lisible, et la décision de relire.
 *
 * ## Pourquoi cette règle existe
 *
 * La version de bureau lit la sauvegarde de la boutique **une seule fois au lancement**. Un PC
 * laissé ouvert toute la journée affichait donc la boutique telle qu'elle était au matin — et rien
 * à l'écran ne le disait. C'est la pire forme de donnée périmée : elle a l'air juste. Un gérant qui
 * consulte son stock à 16 h sur un chiffre figé à 8 h commande ce qu'il a déjà, ou croit avoir ce
 * qu'il a vendu.
 *
 * Deux moitiés, donc : **relire régulièrement**, et **dire quand on a lu**. La seconde est au moins
 * aussi importante que la première, parce qu'elle reste vraie même quand la relecture échoue
 * (réseau coupé, téléphone qui n'a rien envoyé depuis des heures).
 *
 * ## « Lu il y a… » et non « à jour il y a… »
 *
 * Le PC ne sait pas quand la boutique a réellement changé : il sait seulement quand **il** a lu la
 * sauvegarde. Et cette sauvegarde a elle-même l'âge du dernier envoi du téléphone (section 102).
 * Annoncer « à jour » promettrait donc quelque chose que personne ici ne peut garantir ; « lu il y
 * a 3 min » est exactement ce qui est su.
 *
 * ## Pas de dépendance de plateforme
 *
 * L'instant courant est un **paramètre** et non `System.currentTimeMillis()` : c'est ce qui rend la
 * règle vérifiable sans dépendre de l'heure d'exécution en CI, comme pour `RapportFinancier.periode`
 * et `TableauExport.dettes`.
 */
object AgeLecture {

    /**
     * L'intervalle entre deux relectures automatiques.
     *
     * **Cinq minutes, et le choix est dicté par ce que la lecture coûte au gérant.** Depuis la
     * section 40, une sauvegarde ne porte plus les photos et pèse quelques dizaines de kilooctets ;
     * douze lectures par heure restent donc négligeables même sur une connexion facturée au
     * mégaoctet. Descendre à la minute multiplierait ce coût par cinq pour un gain que personne ne
     * remarquerait — on ne consulte pas un stock à la seconde. Monter à l'heure ramènerait le
     * défaut qu'on corrige.
     */
    const val INTERVALLE_RELECTURE_MS = 5L * 60L * 1000L

    /**
     * Le délai minimal entre deux relectures, quelle qu'en soit la cause.
     *
     * La relecture est aussi déclenchée quand la fenêtre reprend le focus — revenir à son PC est
     * précisément le moment où l'on veut des chiffres frais. Mais une fenêtre peut gagner et perdre
     * le focus plusieurs fois par seconde (un clic qui passe par une autre application), et sans
     * ce plancher chaque va-et-vient paierait une requête.
     */
    const val DELAI_MINIMAL_MS = 30L * 1000L

    /**
     * Faut-il relire maintenant ? `derniereLecture` est `null` quand rien n'a encore été lu.
     *
     * **Une lecture dans le futur est traitée comme une lecture à l'instant** : l'horloge d'un PC
     * peut reculer (correction NTP, pile de carte mère vide, utilisateur qui change la date), et un
     * âge négatif ferait relire en boucle aussi vite que le réseau le permet.
     */
    fun doitRelire(derniereLecture: Long?, maintenant: Long, delaiMinimal: Long = DELAI_MINIMAL_MS): Boolean {
        if (derniereLecture == null) return true
        val age = maintenant - derniereLecture
        if (age < 0L) return false
        return age >= delaiMinimal
    }

    /**
     * L'âge de la dernière lecture, en texte court, dans la langue active.
     *
     * Court à dessein : ce texte vit dans le bandeau, à côté du code de la boutique et du bouton
     * « Actualiser ». Une phrase y pousserait le bouton hors de vue sur une fenêtre réduite.
     */
    fun texte(derniereLecture: Long?, maintenant: Long, langue: String): String {
        if (derniereLecture == null) return ""
        val age = maintenant - derniereLecture
        // Horloge qui a reculé : on ne va pas annoncer « lu dans 2 h ».
        if (age < 0L) return aLInstant(langue)
        val minutes = age / 60_000L
        return when {
            minutes < 1L -> aLInstant(langue)
            minutes < 60L -> ilYA("$minutes min", langue)
            minutes < 24L * 60L -> ilYA("${minutes / 60L} h", langue)
            else -> {
                val jours = minutes / (24L * 60L)
                ilYA(jours.toString() + " " + uniteJour(jours, langue), langue)
            }
        }
    }

    private fun aLInstant(langue: String): String = when (langue) {
        "mg" -> "Vao nalaina"
        "fr" -> "Lu à l'instant"
        else -> "Read just now"
    }

    private fun ilYA(duree: String, langue: String): String = when (langue) {
        "mg" -> "Nalaina $duree lasa"
        "fr" -> "Lu il y a $duree"
        else -> "Read $duree ago"
    }

    private fun uniteJour(jours: Long, langue: String): String = when (langue) {
        "mg" -> "andro"
        "fr" -> if (jours > 1L) "jours" else "jour"
        else -> if (jours > 1L) "days" else "day"
    }
}
