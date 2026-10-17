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
}
