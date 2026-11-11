package com.example

import com.example.util.CalculMarge
import com.example.util.RapportFinancier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * C'est le calcul sur lequel le gérant **décide de ses prix de vente**. Une erreur ici ne plante
 * rien et ne s'affiche pas comme une anomalie : elle lui fait simplement vendre à perte, ou
 * renoncer à un produit rentable. Les cas les plus utiles sont donc ceux d'un formulaire **en
 * cours de frappe**, forcément incomplet, où une division non gardée produirait `Infinity` ou
 * `NaN` — qui s'afficheraient tels quels dans le tableau.
 */
class CalculMargeTest {

    @Test
    fun `un exemple complet, verifiable a la main`() {
        // 10 cartons de 24 pièces payés 240 000 Ar, revendus 15 000 Ar l'unité.
        val r = CalculMarge.calculer(
            cartons = 10.0,
            unitesParCarton = 24.0,
            coutTotalAchat = 240_000.0,
            prixVenteUnitaire = 15_000.0
        )
        assertEquals(240.0, r.totalUnites, 0.0001)
        assertEquals(24_000.0, r.coutParCarton, 0.0001)
        assertEquals(1_000.0, r.coutParUnite, 0.0001)
        assertEquals(3_600_000.0, r.recetteTotale, 0.0001)
        assertEquals(3_360_000.0, r.beneficeTotal, 0.0001)
        assertEquals(336_000.0, r.beneficeParCarton, 0.0001)
        assertEquals(14_000.0, r.beneficeParUnite, 0.0001)
        assertEquals(1_400.0, r.tauxMarge, 0.0001)
        assertTrue(r.actif)
        assertEquals(CalculMarge.SanteMarge.EXCELLENTE, r.sante)
    }

    @Test
    fun `un formulaire vide ne produit ni Infinity ni NaN`() {
        // LE cas qui compte : le calculateur est vide à l'ouverture, et tout y est à zéro.
        // Une division non gardée afficherait « Ar ∞ » ou « Ar NaN » dès l'arrivée sur l'écran.
        val r = CalculMarge.calculer(0.0, 0.0, 0.0, 0.0)
        listOf(
            r.totalUnites, r.coutParCarton, r.coutParUnite, r.recetteTotale,
            r.beneficeTotal, r.beneficeParCarton, r.beneficeParUnite, r.tauxMarge
        ).forEach { valeur ->
            assertFalse("Valeur non finie : $valeur", valeur.isNaN() || valeur.isInfinite())
            assertEquals(0.0, valeur, 0.0)
        }
        assertFalse(r.actif)
        assertEquals(CalculMarge.SanteMarge.INACTIVE, r.sante)
    }

    @Test
    fun `aucune division par zero pendant la frappe, champ par champ`() {
        // La saisie se remplit dans l'ordre du formulaire : chaque état intermédiaire doit tenir.
        val etapes = listOf(
            CalculMarge.calculer(10.0, 0.0, 0.0, 0.0),
            CalculMarge.calculer(10.0, 24.0, 0.0, 0.0),
            CalculMarge.calculer(10.0, 24.0, 240_000.0, 0.0),
            CalculMarge.calculer(0.0, 24.0, 240_000.0, 15_000.0),
            CalculMarge.calculer(10.0, 0.0, 240_000.0, 15_000.0)
        )
        etapes.forEachIndexed { index, r ->
            listOf(
                r.coutParCarton, r.coutParUnite, r.beneficeParCarton,
                r.beneficeParUnite, r.tauxMarge
            ).forEach { valeur ->
                assertFalse(
                    "Étape $index : valeur non finie $valeur",
                    valeur.isNaN() || valeur.isInfinite()
                )
            }
        }
    }

    @Test
    fun `vendre sous le cout de revient est signale comme une perte`() {
        // Le cas que le badge existe pour attraper : 10 cartons payés 240 000 revendus 900 l'unité
        // rapportent 216 000 — soit 24 000 de moins que l'achat.
        val r = CalculMarge.calculer(10.0, 24.0, 240_000.0, 900.0)
        assertTrue(r.beneficeTotal < 0.0)
        assertEquals(CalculMarge.SanteMarge.PERTE, r.sante)
        // La marge négative est rendue TELLE QUELLE : c'est précisément ce qu'il faut voir.
        assertTrue(r.tauxMarge < 0.0)
    }

    @Test
    fun `revendre exactement au prix d achat est une marge nulle, pas une marge faible`() {
        val r = CalculMarge.calculer(10.0, 24.0, 240_000.0, 1_000.0)
        assertEquals(0.0, r.beneficeTotal, 0.0)
        assertEquals(CalculMarge.SanteMarge.NULLE, r.sante)
    }

    @Test
    fun `la frontiere de marge faible est stricte, au seuil partage`() {
        // Le seuil vient de `RapportFinancier.SEUIL_MARGE_FAIBLE`, partagé avec les deux écrans de
        // rapport : le déplacer d'un seul côté ferait dire « marge faible » à un écran sur un
        // produit que l'autre colore en vert. Le test fixe aussi le SENS de la comparaison : à
        // 15 % exactement, la marge n'est PLUS faible.
        //
        // La garde qui rend ce test possible : il vérifie d'abord la valeur du seuil, parce que les
        // prix ci-dessous sont des littéraux choisis pour tomber EXACTEMENT dessus. Déplacer le
        // seuil fait donc échouer cette ligne au lieu de rendre le test silencieusement inoffensif.
        assertEquals(15.0, RapportFinancier.SEUIL_MARGE_FAIBLE, 0.0)

        // 10 cartons de 10 pièces payés 100 000 Ar : 100 unités à 1 150 Ar donnent 15 000 Ar de
        // bénéfice sur 100 000 Ar de coût, soit 15,0 % **au bit près**. Calculer ce prix
        // (`100000 * 1.15 / 100`) donnerait 1 149,999…, donc un taux de 14,999… et un
        // classement « marge faible » — le test échouerait sur un arrondi binaire et non sur la
        // règle qu'il prétend vérifier.
        val exact = CalculMarge.calculer(10.0, 10.0, 100_000.0, 1_150.0)
        assertEquals(RapportFinancier.SEUIL_MARGE_FAIBLE, exact.tauxMarge, 0.0)
        assertEquals(CalculMarge.SanteMarge.EXCELLENTE, exact.sante)

        val dessous = CalculMarge.calculer(10.0, 10.0, 100_000.0, 1_140.0)
        assertTrue(dessous.tauxMarge < RapportFinancier.SEUIL_MARGE_FAIBLE)
        assertEquals(CalculMarge.SanteMarge.FAIBLE, dessous.sante)
    }

    @Test
    fun `sans cout d achat renseigne, rien n est annonce`() {
        // Un prix de vente saisi sans coût d'achat donnerait une marge de 0 % par la garde de
        // `tauxMarge`, donc « marge faible » — un conseil donné sur une saisie à moitié remplie.
        // `actif` est faux, donc le badge reste muet.
        val r = CalculMarge.calculer(10.0, 24.0, 0.0, 15_000.0)
        assertFalse(r.actif)
        assertEquals(CalculMarge.SanteMarge.INACTIVE, r.sante)
        assertEquals("", CalculMarge.libelleSante(r.sante, "fr"))
        // Le bénéfice brut, lui, est bien calculé : c'est la recette entière.
        assertEquals(3_600_000.0, r.beneficeTotal, 0.0001)
    }

    @Test
    fun `sans prix de vente, rien n est annonce non plus`() {
        val r = CalculMarge.calculer(10.0, 24.0, 240_000.0, 0.0)
        assertFalse(r.actif)
        assertEquals(CalculMarge.SanteMarge.INACTIVE, r.sante)
    }

    @Test
    fun `un lot sans unite ne rend pas la marge active`() {
        // Coût et prix de vente renseignés, mais zéro carton : il n'y a rien à vendre.
        val r = CalculMarge.calculer(0.0, 24.0, 240_000.0, 15_000.0)
        assertEquals(0.0, r.totalUnites, 0.0)
        assertFalse(r.actif)
    }

    @Test
    fun `les cartons fractionnaires sont acceptes`() {
        // Un demi-carton acheté chez le fournisseur, cas réel chez un grossiste de quartier.
        val r = CalculMarge.calculer(0.5, 24.0, 12_000.0, 1_000.0)
        assertEquals(12.0, r.totalUnites, 0.0001)
        assertEquals(24_000.0, r.coutParCarton, 0.0001)
        assertEquals(1_000.0, r.coutParUnite, 0.0001)
        assertEquals(0.0, r.beneficeTotal, 0.0001)
        assertEquals(CalculMarge.SanteMarge.NULLE, r.sante)
    }

    @Test
    fun `le benefice par unite est bien la recette unitaire moins le cout de revient`() {
        // Deux chemins mènent à ce chiffre (`beneficeTotal / totalUnites` ou
        // `prixVente - coutParUnite`) : ils doivent donner le même, sinon le tableau se
        // contredirait d'une ligne à l'autre.
        val r = CalculMarge.calculer(7.0, 13.0, 50_000.0, 900.0)
        assertEquals(r.beneficeTotal / r.totalUnites, r.beneficeParUnite, 0.0001)
    }

    @Test
    fun `chaque etat de sante a un libelle dans les trois langues, sauf l etat inactif`() {
        val langues = listOf("mg", "fr", "en")
        CalculMarge.SanteMarge.entries.forEach { sante ->
            langues.forEach { langue ->
                val libelle = CalculMarge.libelleSante(sante, langue)
                if (sante == CalculMarge.SanteMarge.INACTIVE) {
                    // Vide à dessein : le badge n'est alors pas dessiné du tout.
                    assertEquals("", libelle)
                } else {
                    assertTrue("Libellé vide pour $sante en $langue", libelle.isNotBlank())
                }
            }
        }
    }

    @Test
    fun `une langue inconnue retombe sur l anglais`() {
        CalculMarge.SanteMarge.entries.forEach { sante ->
            assertEquals(
                CalculMarge.libelleSante(sante, "en"),
                CalculMarge.libelleSante(sante, "xx")
            )
        }
    }
}
