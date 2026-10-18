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
 * `RAPPORTS` est annoncée mais pas encore servie : le bureau n'a pas de base de données (voir le
 * KDoc du module), et un tableau de bord sans historique réel n'aurait rien à dire. La déclarer
 * tout de suite plutôt que l'ajouter plus tard donne la structure réelle du logiciel à l'écran, et
 * montre honnêtement ce qui reste à faire.
 *
 * `CAISSE` est servie, mais ne peut pas encore ENREGISTRER une vente, pour la même raison : tout le
 * calcul y est réel et partagé avec Android, seul l'écriture manque. Le bouton « Valider » est
 * désactivé avec sa raison affichée, plutôt que de laisser croire à un encaissement qui n'aurait
 * lieu nulle part.
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

    /** `true` quand la section a un véritable contenu ; sinon le menu l'annonce comme à venir. */
    val disponible: Boolean
        get() = this != RAPPORTS
}
