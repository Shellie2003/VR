package com.example.util

/**
 * Métier de la boutique. Détermine ce que l'app met en avant : le prix appliqué à la caisse
 * aujourd'hui, et — au fur et à mesure — les écrans et contrôles propres à chaque métier.
 *
 * **Pourquoi une énumération alors qu'une chaîne suffisait.** Le mode vivait jusqu'ici sous forme de
 * `String` comparée à des littéraux (`"retail"`, `"wholesale"`) recopiés dans quatre fichiers
 * différents. Une faute de frappe dans l'une de ces comparaisons ne se voit ni à la compilation ni
 * au test : elle se traduit en production par un prix de gros silencieusement ignoré. Passer par une
 * énumération rend l'ensemble des modes exhaustif, vérifié par le compilateur, et supprime tout
 * littéral disséminé.
 *
 * **[cle] est la valeur PERSISTÉE, et elle ne doit jamais changer.** `"retail"` et `"wholesale"`
 * sont exactement les chaînes déjà écrites dans les préférences des appareils en service : les
 * conserver telles quelles fait que la mise à jour ne modifie le réglage de personne. C'est ce qui
 * permet d'ajouter deux métiers sans perturber les boutiques existantes.
 */
enum class ShopMode(val cle: String) {
    /** Épicerie au détail : prix unitaire client. Mode par défaut, historique. */
    DETAIL("retail"),

    /** Épicerie en gros (ambongadiny) : applique `wholesalePrice` quand il est renseigné. */
    GROSSISTE("wholesale"),

    /** Pharmacie : suivi des lots et des dates de péremption au premier plan. */
    PHARMACIE("pharmacie"),

    /** Bar / débit de boissons. */
    BAR("bar");

    companion object {
        val PAR_DEFAUT = DETAIL

        /**
         * Relit une valeur persistée. Toute valeur inconnue — préférence corrompue, sauvegarde
         * restaurée depuis une version plus récente de l'app, appareil rétrogradé — retombe sur le
         * mode par défaut au lieu de faire échouer le démarrage. Un réglage illisible ne doit jamais
         * empêcher d'ouvrir la caisse.
         */
        fun depuisCle(cle: String?): ShopMode =
            entries.firstOrNull { it.cle == cle } ?: PAR_DEFAUT
    }
}
