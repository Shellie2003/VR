package com.example.ui.partage.coquille

/**
 * Les sections de la version de bureau, dans l'ordre où elles apparaissent dans le menu latéral.
 *
 * Volontairement une énumération et non une liste de chaînes : le compilateur vérifie alors
 * l'exhaustivité de tout `when` qui réagit à la section active, donc ajouter une section oblige à
 * traiter son contenu — là où une chaîne laisserait un écran vide sans le moindre avertissement.
 * C'est la même raison qui avait fait remplacer le mode de boutique (`"retail"`/`"wholesale"`)
 * par `ShopMode`.
 *
 * Les libellés sont portés par l'énumération elle-même, hors de tout composable : c'est ce qui
 * permet à `SectionBureauTest` de vérifier qu'aucune traduction ne manque. Une étiquette oubliée
 * afficherait un menu avec un trou, ce qu'aucun test d'interface ne remarquerait.
 *
 * **Les six sections sont désormais servies.** Le mécanisme « à venir » qui grisait une entrée de
 * menu et expliquait ce qu'elle attendait a donc été retiré : un drapeau toujours vrai est une
 * documentation fausse. Il est dans l'historique git si une future section doit être annoncée avant
 * d'être écrite.
 *
 * Ce qui manque encore est dit **dans** les écrans, là où c'est utile : le bandeau de démonstration
 * sur chacun, et la raison affichée sous le bouton « Valider » désactivé de la caisse — tout son
 * calcul est réel et partagé avec Android, seule l'écriture en base manque.
 */
enum class SectionBureau {
    CAISSE,
    CATALOGUE,
    STOCK,
    DETTES,
    RAPPORTS,
    A_PROPOS;

    /**
     * Le libellé affiché, dans la langue active. Toute autre valeur que `mg` ou `fr` retombe sur
     * l'anglais — même règle que `LanguageManager`, où l'anglais est la langue de secours.
     */
    fun libelle(langue: String): String = when (this) {
        CAISSE -> when (langue) {
            "mg" -> "Varotra"
            "fr" -> "Caisse"
            else -> "Checkout"
        }
        CATALOGUE -> when (langue) {
            "mg" -> "Entana"
            "fr" -> "Catalogue"
            else -> "Catalogue"
        }
        STOCK -> when (langue) {
            "mg" -> "Tahiry"
            "fr" -> "Stock"
            else -> "Stock"
        }
        DETTES -> when (langue) {
            "mg" -> "Trosa"
            "fr" -> "Dettes"
            else -> "Debts"
        }
        RAPPORTS -> when (langue) {
            "mg" -> "Tatitra"
            "fr" -> "Rapports"
            else -> "Reports"
        }
        A_PROPOS -> when (langue) {
            "mg" -> "Momba"
            "fr" -> "À propos"
            else -> "About"
        }
    }
}
