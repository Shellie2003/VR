package com.example

import com.example.util.AjustementStock
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Cette règle était dupliquée entre les boutons `+` et `-` de la carte Stock. Une divergence entre
 * les deux ne lèverait aucune erreur : elle fausserait le stock, et ne se verrait qu'à l'inventaire
 * suivant — sans qu'on puisse dire quel article a dérivé ni quand.
 */
class AjustementStockTest {

    @Test
    fun `le poids et le volume se comptent au quart`() {
        // Une épicerie vend le riz et l'huile au quart de kilo ou de litre.
        assertEquals(0.25, AjustementStock.pas("Kilogramme"), 1e-9)
        assertEquals(0.25, AjustementStock.pas("Litre"), 1e-9)
    }

    @Test
    fun `tout le reste se compte a l'unite`() {
        // On ne reçoit pas un quart de carton.
        assertEquals(1.0, AjustementStock.pas("Pièce"), 1e-9)
        assertEquals(1.0, AjustementStock.pas("Carton"), 1e-9)
        assertEquals(1.0, AjustementStock.pas("Paquet"), 1e-9)
        // Unités des nouveaux métiers (section 73) : elles doivent tomber sur le pas entier sans
        // que cette table ait à les connaître.
        assertEquals(1.0, AjustementStock.pas("Plaquette"), 1e-9)
        assertEquals(1.0, AjustementStock.pas("Casier"), 1e-9)
        assertEquals(1.0, AjustementStock.pas(""), 1e-9)
    }

    @Test
    fun `la casse de l'unite est ignoree`() {
        assertEquals(0.25, AjustementStock.pas("KILOGRAMME"), 1e-9)
        assertEquals(0.25, AjustementStock.pas("litre"), 1e-9)
    }

    @Test
    fun `le plus et le moins utilisent le MEME pas`() {
        // Le coeur du problème que cette extraction supprime : deux copies pouvaient diverger.
        val stock = 10.0
        assertEquals(9.75, AjustementStock.apresRetrait(stock, "Kilogramme"), 1e-9)
        assertEquals(10.25, AjustementStock.apresAjout(stock, "Kilogramme"), 1e-9)
        assertEquals(9.0, AjustementStock.apresRetrait(stock, "Pièce"), 1e-9)
        assertEquals(11.0, AjustementStock.apresAjout(stock, "Pièce"), 1e-9)
    }

    @Test
    fun `le stock ne devient jamais negatif`() {
        // Un stock négatif contaminerait la valeur d'inventaire, les alertes et les marges.
        assertEquals(0.0, AjustementStock.apresRetrait(0.0, "Pièce"), 1e-9)
        assertEquals(0.0, AjustementStock.apresRetrait(0.5, "Pièce"), 1e-9)
        assertEquals(0.0, AjustementStock.apresRetrait(0.1, "Kilogramme"), 1e-9)
    }

    @Test
    fun `les abreviations kg et L se comptent au quart comme les libelles complets`() {
        // La divergence réelle corrigée en section 91 : la ligne de panier de la caisse acceptait
        // déjà ces abréviations, l'écran Stock non — donc le même produit montait par quarts dans
        // le panier et par kilos entiers dans le stock.
        assertEquals(0.25, AjustementStock.pas("kg"), 1e-9)
        assertEquals(0.25, AjustementStock.pas("KG"), 1e-9)
        assertEquals(0.25, AjustementStock.pas("l"), 1e-9)
        assertEquals(0.25, AjustementStock.pas("L"), 1e-9)
    }

    @Test
    fun `un libelle qui contient kilo ou litre est fractionnable`() {
        // Un gérant écrit « Kilo », « kilos », « Demi-litre » : l'intention est la même, et c'est
        // la variante que la caisse appliquait déjà.
        assertEquals(0.25, AjustementStock.pas("Kilo"), 1e-9)
        assertEquals(0.25, AjustementStock.pas("kilos"), 1e-9)
        assertEquals(0.25, AjustementStock.pas("Demi-litre"), 1e-9)
    }

    @Test
    fun `une seule lettre l n'est reconnue qu'exactement et jamais par inclusion`() {
        // Le piège de la règle élargie : `contains("l")` rendrait fractionnables « Plaquette »,
        // « Bouteille », « Litre » compris — soit presque tout le catalogue, et des stocks montant
        // par quarts de plaquette.
        assertEquals(1.0, AjustementStock.pas("Plaquette"), 1e-9)
        assertEquals(1.0, AjustementStock.pas("Bouteille"), 1e-9)
        assertEquals(1.0, AjustementStock.pas("Sac"), 1e-9)
        assertEquals(1.0, AjustementStock.pas("Flacon"), 1e-9)
    }

    @Test
    fun `le type d'unite choisit le bon vocabulaire de saisie rapide`() {
        // C'est ce qui décide si le vendeur lit « 1/4 L » ou « 1/4 kg ». Se tromper lui ferait lire
        // une unité qui n'est pas celle du produit qu'il encaisse.
        assertEquals(AjustementStock.TypeUnite.LIQUIDE, AjustementStock.typeUnite("Litre"))
        assertEquals(AjustementStock.TypeUnite.LIQUIDE, AjustementStock.typeUnite("L"))
        assertEquals(AjustementStock.TypeUnite.POIDS, AjustementStock.typeUnite("Kilogramme"))
        assertEquals(AjustementStock.TypeUnite.POIDS, AjustementStock.typeUnite("kg"))
        assertEquals(AjustementStock.TypeUnite.AUTRE, AjustementStock.typeUnite("Pièce"))
        assertEquals(AjustementStock.TypeUnite.AUTRE, AjustementStock.typeUnite("Casier"))
    }

    @Test
    fun `le pas et le type d'unite ne peuvent plus se contredire`() {
        // L'invariant qui justifie d'avoir une seule classification : toute unité fractionnable
        // porte un pas de 0,25, et aucune autre. Les cinq copies d'avant ne le garantissaient pas.
        listOf("Litre", "L", "Kilogramme", "kg", "Kilo", "Demi-litre", "Pièce", "Carton",
            "Plaquette", "Casier", "Bouteille", "Flacon", "Sac", "").forEach { unite ->
            val attendu = if (AjustementStock.estFractionnable(unite)) 0.25 else 1.0
            assertEquals("Unité « $unite »", attendu, AjustementStock.pas(unite), 1e-9)
        }
    }
}
