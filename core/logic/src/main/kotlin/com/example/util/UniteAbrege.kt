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
 * **Une bizarrerie reproduite volontairement, et signalée plutôt que corrigée :** quand l'unité est
 * vide, l'original produit le couple `"Tahir" to "y"` puis n'affiche que la première partie — le
 * badge lit donc « Total: Tahir », un mot tronqué qui ressemble fortement à une coquille pour
 * « Tahiry » (« stock » en malgache). Le comportement est conservé tel quel : corriger l'affichage
 * d'un badge de production relève d'une décision du gérant, pas d'un effet de bord d'un portage.
 */
object UniteAbrege {

    /** L'abréviation affichée pour une unité, et la valeur de stock formatée qui l'accompagne. */
    fun valeurEtAbreviation(stock: Double, unite: String): Pair<String, String> =
        when (unite.lowercase()) {
            "kilogramme", "kg" -> stock.toInt().toString() to "kg"
            "litre", "l" -> stock.toInt().toString() to "L"
            "pièce", "piece", "pcs" -> stock.toInt().toString() to "pcs"
            "paquet" -> stock.toInt().toString() to "paq"
            "carton" -> stock.toInt().toString() to "ctn"
            "sac" -> stock.toInt().toString() to "sac"
            "boîte", "boite" -> stock.toInt().toString() to "bt"
            "bouteille" -> stock.toInt().toString() to "btl"
            "tasse", "kapoaka" -> stock.toInt().toString() to "kap"
            // Unité inconnue : on affiche le libellé tel que le gérant l'a saisi. C'est ce qui
            // permet aux unités propres à un métier (plaquette, casier, fardeau…) de s'afficher
            // sans que cette table ait à les connaître.
            else -> if (unite.isEmpty()) "Tahir" to "y" else stock.toInt().toString() to unite
        }

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
