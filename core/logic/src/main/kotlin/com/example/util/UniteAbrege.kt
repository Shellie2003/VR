package com.example.util

/**
 * Le libellé du badge de stock affiché sur une carte produit : « Total: 42 kg », « Tahiry: 12 pcs ».
 *
 * **Pourquoi cette fonction existe.** Ce `when` sur l'unité vivait à l'intérieur de la carte
 * produit d'Android. Porter cette carte sur le bureau l'aurait recopié, et deux copies d'une table
 * d'abréviations finissent toujours par diverger — le gérant lirait alors « kap » sur son téléphone
 * et autre chose sur son PC pour le même article. Une seule source, et les deux plateformes ne
 * peuvent plus se contredire.
 *
 * Les abréviations sont reprises **à l'identique** de l'original, y compris leurs choix propres au
 * malgache (« kap » pour kapoaka, « btl » pour bouteille). Les renommer serait un changement de
 * design, pas une amélioration.
 *
 * **Deux formes, et c'est ce qui a permis de supprimer la duplication sans rien changer à l'écran.**
 * Le vocabulaire existait en deux exemplaires qui ne disaient pas la même chose : la carte produit
 * lisait « L » et « kap », la ligne de l'historique des ventes « Litatra » et « Kapoaka ». Forcer
 * l'un des deux partout aurait modifié un libellé que les boutiques ont sous les yeux. Or les deux
 * variantes se justifient par la place disponible : le badge de la carte produit tient dans une
 * pastille de 9–10sp, la ligne de l'historique occupe toute la largeur de l'écran. Les deux formes
 * sont donc assumées et nommées ([Forme.COURTE], [Forme.LONGUE]), au lieu d'être deux tables que
 * rien n'empêchait de dériver l'une de l'autre.
 *
 * Les quatre unités que la table de l'historique ne connaissait pas (carton, sac, boîte, bouteille)
 * y tombaient sur le libellé brut saisi par le gérant — « Carton », « Bouteille ». La forme longue
 * reproduit exactement cela : elle rend le libellé tel quel plutôt que l'abréviation, donc
 * l'historique affiche aujourd'hui ce qu'il affichait hier.
 *
 * **Une bizarrerie reproduite volontairement, et signalée plutôt que corrigée :** quand l'unité est
 * vide, l'original produit le couple `"Tahir" to "y"` puis n'affiche que la première partie — le
 * badge lit donc « Total: Tahir », un mot tronqué qui ressemble fortement à une coquille pour
 * « Tahiry » (« stock » en malgache). Le comportement est conservé tel quel : corriger l'affichage
 * d'un badge de production relève d'une décision du gérant, pas d'un effet de bord d'un portage.
 */
object UniteAbrege {

    /**
     * La place disponible pour écrire l'unité, et donc la forme à employer.
     *
     * [COURTE] pour une pastille (carte produit, badge de stock) ; [LONGUE] pour une ligne de
     * liste, où le mot entier se lit mieux qu'une abréviation de trois lettres.
     */
    enum class Forme { COURTE, LONGUE }

    /**
     * L'unité telle qu'elle doit s'afficher, dans la forme demandée.
     *
     * Une unité que cette table ne connaît pas est rendue **telle que le gérant l'a saisie**, dans
     * les deux formes : c'est ce qui permet aux unités propres à un métier (plaquette, casier,
     * fardeau… — section 73) de s'afficher sans que cette table ait à les connaître.
     */
    fun abreviation(unite: String, forme: Forme = Forme.COURTE): String {
        val longue = forme == Forme.LONGUE
        return when (unite.lowercase()) {
            "kilogramme", "kg" -> "kg"
            "litre", "l" -> if (longue) "Litatra" else "L"
            "pièce", "piece", "pcs" -> "pcs"
            "paquet" -> "paq"
            "tasse", "kapoaka" -> if (longue) "Kapoaka" else "kap"
            // Ces quatre-là n'avaient pas d'abréviation dans la table de l'historique : le libellé
            // brut y était affiché. La forme longue le reproduit, pour ne pas modifier un écran
            // de production au passage.
            "carton" -> if (longue) unite else "ctn"
            "sac" -> if (longue) unite else "sac"
            "boîte", "boite" -> if (longue) unite else "bt"
            "bouteille" -> if (longue) unite else "btl"
            else -> unite
        }
    }

    /** L'abréviation affichée pour une unité, et la valeur de stock formatée qui l'accompagne. */
    fun valeurEtAbreviation(stock: Double, unite: String): Pair<String, String> =
        if (unite.isEmpty()) "Tahir" to "y"
        else stock.toInt().toString() to abreviation(unite, Forme.COURTE)

    /** Le texte complet du badge, préfixe de langue compris. */
    fun libelleBadgeStock(stock: Double, unite: String, langue: String): String {
        val (valeur, abreviation) = valeurEtAbreviation(stock, unite)
        val libelle = if (unite.isEmpty()) valeur else "$valeur $abreviation"
        return when (langue) {
            "mg" -> "Tahiry: $libelle"
            else -> "Total: $libelle"
        }
    }
}
