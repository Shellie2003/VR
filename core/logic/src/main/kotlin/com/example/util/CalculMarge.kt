package com.example.util

/**
 * Le calcul d'approvisionnement et de marge du calculateur (`CommissionScreen`).
 *
 * ## Pourquoi hors de l'écran
 *
 * Les dix valeurs dérivées étaient calculées **en ligne, au milieu du composable**, entre deux
 * champs de saisie. Porter le calculateur sur le bureau en aurait fait une seconde copie — et
 * c'est le calcul sur lequel le gérant **décide de ses prix de vente**. Deux copies qui
 * divergeraient lui donneraient deux marges pour le même carton selon l'appareil consulté, sans
 * qu'aucune erreur n'apparaisse nulle part : le chiffre serait simplement faux.
 *
 * ## La classification à quatre états est celle du calculateur, et seulement la sienne
 *
 * La section 97 a délibérément refusé d'unifier cette classification avec celle des deux écrans de
 * rapport : le calculateur distingue en plus la **perte** et la **marge nulle**, les rapports non.
 * Ce qui est extrait ici est donc la classification **du calculateur**, pour que son portage sur le
 * bureau soit identique — pas une règle commune à toute l'application. Seul le seuil
 * (`RapportFinancier.SEUIL_MARGE_FAIBLE`) est partagé, parce que c'est une décision commerciale
 * que le gérant peut vouloir déplacer, et qu'un seuil déplacé d'un seul côté ferait dire « marge
 * faible » à un écran sur un produit que l'autre colore en vert.
 */
object CalculMarge {

    /** L'état de santé de la marge, tel que le badge du calculateur l'annonce. */
    enum class SanteMarge {
        /** La saisie est incomplète : le calculateur n'affiche aucun badge. */
        INACTIVE,

        /** Le prix de vente est sous le coût de revient : vendre ferait perdre de l'argent. */
        PERTE,

        /** Prix de vente exactement égal au coût de revient. */
        NULLE,

        /** Marge positive mais sous le seuil. */
        FAIBLE,

        /** Marge au-dessus du seuil. */
        EXCELLENTE
    }

    /**
     * Le résultat complet du calcul. Toutes les valeurs valent `0.0` tant que la saisie est
     * incomplète — ce qui est ce qu'affichait déjà l'écran Android, et vaut mieux qu'un champ vide
     * ou un tiret au milieu d'un tableau de chiffres.
     */
    data class Resultat(
        /** Nombre total d'unités reçues : cartons × unités par carton. */
        val totalUnites: Double,
        /** Coût d'achat d'un carton. */
        val coutParCarton: Double,
        /** Coût de revient d'une unité — le chiffre sous lequel il ne faut jamais vendre. */
        val coutParUnite: Double,
        /** Recette si tout le lot est vendu au prix de vente saisi. */
        val recetteTotale: Double,
        /** Bénéfice sur le lot entier. */
        val beneficeTotal: Double,
        val beneficeParCarton: Double,
        val beneficeParUnite: Double,
        /** Taux de marge en pourcentage du coût (jamais `Infinity`, voir `RapportFinancier`). */
        val tauxMarge: Double,
        /** Faux tant que la saisie est incomplète : rien ne doit alors être annoncé au gérant. */
        val actif: Boolean,
        val sante: SanteMarge
    )

    /**
     * Calcule tout à partir des quatre saisies du formulaire.
     *
     * **Chaque division est gardée.** Un calculateur se remplit champ par champ, donc il est
     * normal qu'il soit incomplet pendant la frappe : diviser par un nombre de cartons encore vide
     * donnerait `Infinity` ou `NaN`, qui s'afficheraient tels quels dans le tableau — c'est
     * précisément le défaut qu'avait la carte de réappro de l'historique (« Ar ∞ », section 104).
     *
     * **La comparaison de bénéfice nul reste une égalité exacte**, comme en production. La rendre
     * tolérante ferait annoncer « marge nulle » sur un bénéfice de quelques ariary, donc changerait
     * ce que des gérants lisent déjà aujourd'hui pour un gain théorique.
     */
    fun calculer(
        cartons: Double,
        unitesParCarton: Double,
        coutTotalAchat: Double,
        prixVenteUnitaire: Double
    ): Resultat {
        val totalUnites = cartons * unitesParCarton
        val coutParCarton = if (cartons > 0.0) coutTotalAchat / cartons else 0.0
        val coutParUnite = if (totalUnites > 0.0) coutTotalAchat / totalUnites else 0.0
        val recetteTotale = totalUnites * prixVenteUnitaire
        val beneficeTotal = recetteTotale - coutTotalAchat
        val beneficeParCarton = if (cartons > 0.0) beneficeTotal / cartons else 0.0
        val beneficeParUnite = if (totalUnites > 0.0) prixVenteUnitaire - coutParUnite else 0.0
        val tauxMarge = RapportFinancier.tauxMarge(beneficeTotal, coutTotalAchat)

        // Les trois conditions de l'écran Android, mot pour mot : sans coût d'achat il n'y a pas de
        // marge à juger, sans prix de vente il n'y a rien à comparer, et sans unités le lot est
        // vide. Annoncer « marge excellente » sur une saisie à moitié remplie serait un conseil
        // donné sur rien.
        val actif = coutTotalAchat > 0.0 && prixVenteUnitaire > 0.0 && totalUnites > 0.0

        val sante = when {
            !actif -> SanteMarge.INACTIVE
            beneficeTotal < 0.0 -> SanteMarge.PERTE
            beneficeTotal == 0.0 -> SanteMarge.NULLE
            tauxMarge < RapportFinancier.SEUIL_MARGE_FAIBLE -> SanteMarge.FAIBLE
            else -> SanteMarge.EXCELLENTE
        }

        return Resultat(
            totalUnites = totalUnites,
            coutParCarton = coutParCarton,
            coutParUnite = coutParUnite,
            recetteTotale = recetteTotale,
            beneficeTotal = beneficeTotal,
            beneficeParCarton = beneficeParCarton,
            beneficeParUnite = beneficeParUnite,
            tauxMarge = tauxMarge,
            actif = actif,
            sante = sante
        )
    }

    /**
     * Le libellé du badge de santé de la marge, dans la langue active.
     *
     * Les émojis font partie du libellé de production : ils portent l'alerte à eux seuls pour un
     * gérant qui parcourt l'écran des yeux sans lire la phrase.
     */
    fun libelleSante(sante: SanteMarge, langue: String): String = when (sante) {
        SanteMarge.INACTIVE -> ""
        SanteMarge.PERTE -> when (langue) {
            "mg" -> "Fatiantoka ⚠️ (Mila ampiakarina ny vidiny)"
            "fr" -> "Perte ⚠️ (Augmentez le prix)"
            else -> "Loss ⚠️ (Increase price)"
        }
        SanteMarge.NULLE -> when (langue) {
            "mg" -> "Tsy misy tombony"
            "fr" -> "Marge nulle"
            else -> "Zero margin"
        }
        SanteMarge.FAIBLE -> when (langue) {
            "mg" -> "Tombony kely (Marge faible) ⚠️"
            "fr" -> "Marge faible ⚠️"
            else -> "Low margin ⚠️"
        }
        SanteMarge.EXCELLENTE -> when (langue) {
            "mg" -> "Tombony tsara (Marge excellente) ✨"
            "fr" -> "Marge excellente ✨"
            else -> "Excellent margin ✨"
        }
    }
}
