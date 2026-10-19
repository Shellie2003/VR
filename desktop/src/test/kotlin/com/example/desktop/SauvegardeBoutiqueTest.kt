package com.example.desktop

import com.example.desktop.donnees.SauvegardeBoutique
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ces cas protègent la seule chose qui décide de ce que le gérant voit sur son PC.
 *
 * Une erreur ici ne lèverait aucune exception : elle afficherait un catalogue amputé, un prix faux,
 * ou des trosa marqués « EN RETARD » qui ne le sont pas — le genre de défaut qu'on ne distingue pas
 * d'un vrai problème de boutique.
 */
class SauvegardeBoutiqueTest {

    /** Une sauvegarde réaliste, écrite avec les clés EXACTES de `getFullDatabaseJsonSync`. */
    private val sauvegarde = """
        {
          "products": [
            {"id": 1, "name": "Riz", "price": 3000.0, "category": "Alimentation",
             "stock": 12.0, "unit": "Kilogramme", "barcode": "123", "wholesalePrice": 2800.0,
             "prixAchatUniteBase": 2500.0, "lowStockThreshold": 5.0, "taxable": true,
             "tauxTaxe": 20.0, "dci": "", "surOrdonnance": false},
            {"id": 2, "name": "Doliprane", "price": 5000.0, "category": "Sante",
             "stock": 8.0, "unit": "Plaquette", "wholesalePrice": 0.0,
             "dci": "Paracetamol", "dosage": "500 mg", "formeGalenique": "Comprime",
             "surOrdonnance": true}
          ],
          "sales": [
            {"timestamp": 1700000000000, "totalAmount": 6000.0,
             "deviceName": "Caisse 1", "vendeurNom": "Koto",
             "items": [{"productId": 1, "name": "Riz", "quantity": 2.0, "price": 3000.0,
                        "taxable": true, "tauxTaxe": 20.0}]}
          ],
          "debts": [
            {"debtorName": "Rakoto", "amount": 2300.0, "balance": 2300.0,
             "date": 1700000000000, "note": "Riz (1 x 2 300)", "isPaid": false,
             "dueDate": null, "deviceName": "Caisse 1", "vendeurNom": "Koto"},
            {"debtorName": "Rasoa", "amount": 1100.0, "balance": 0.0,
             "date": 1700000500000, "note": "", "isPaid": true, "dueDate": 1800000000000}
          ],
          "retours": [
            {"saleId": 7, "timestamp": 1700000900000, "totalAmount": 3000.0,
             "motif": "Abime", "modePaiementOrigine": "ESPECES",
             "items": [{"productId": 1, "name": "Riz", "quantity": 1.0, "price": 3000.0}]}
          ],
          "mouvementsCaisse": [], "vendeurs": [], "lots": []
        }
    """.trimIndent()

    @Test
    fun `une sauvegarde complete est lue dans ses quatre collections`() {
        val d = SauvegardeBoutique.lire(sauvegarde)
        assertNotNull(d)
        assertEquals(2, d!!.produits.size)
        assertEquals(1, d.ventes.size)
        assertEquals(2, d.dettes.size)
        assertEquals(1, d.retours.size)
        assertTrue(!d.estVide)
    }

    @Test
    fun `un tarif de gros a zero n'est JAMAIS lu comme un tarif de gros`() {
        // Le téléphone écrit 0.0 pour « pas de tarif de gros ». Relu tel quel, le produit serait
        // vendu à ZÉRO en mode Grossiste — exactement le piège corrigé en section 85, et le seul
        // défaut de ce lecteur qui coûterait de l'argent plutôt que de l'affichage.
        val d = SauvegardeBoutique.lire(sauvegarde)!!
        assertEquals(2800.0, d.produits.first { it.name == "Riz" }.wholesalePrice!!, 1e-9)
        assertNull(d.produits.first { it.name == "Doliprane" }.wholesalePrice)
    }

    @Test
    fun `une dette sans echeance n'est pas datee de 1970`() {
        // `optLong` sur un `JSONObject.NULL` rendrait 0, donc une échéance au 1er janvier 1970 :
        // CHAQUE trosa sans échéance de la boutique s'afficherait « EN RETARD ».
        val d = SauvegardeBoutique.lire(sauvegarde)!!
        assertNull(d.dettes.first { it.debtorName == "Rakoto" }.dueDate)
        assertEquals(1800000000000L, d.dettes.first { it.debtorName == "Rasoa" }.dueDate)
    }

    @Test
    fun `les champs du mode pharmacie survivent a la lecture`() {
        val medicament = SauvegardeBoutique.lire(sauvegarde)!!
            .produits.first { it.name == "Doliprane" }
        assertEquals("Paracetamol", medicament.dci)
        assertEquals("500 mg", medicament.dosage)
        assertEquals("Comprime", medicament.formeGalenique)
        assertTrue(medicament.surOrdonnance)
        // Un champ vide doit rendre `null` et non une chaîne vide, sinon la fiche afficherait un
        // séparateur orphelin devant rien.
        assertNull(SauvegardeBoutique.lire(sauvegarde)!!.produits.first { it.name == "Riz" }.dci)
    }

    @Test
    fun `la ventilation TVA des articles vendus est conservee`() {
        // Sans `taxable`/`tauxTaxe` sur la LIGNE, la carte « TVA collectée » du rapport afficherait
        // zéro sur une boutique qui en collecte.
        val article = SauvegardeBoutique.lire(sauvegarde)!!.ventes.first().items.first()
        assertTrue(article.taxable)
        assertEquals(20.0, article.tauxTaxe, 1e-9)
        assertTrue("La part de TVA doit être calculable", article.taxAmount > 0.0)
    }

    @Test
    fun `l'auteur de chaque transaction est conserve`() {
        val d = SauvegardeBoutique.lire(sauvegarde)!!
        assertEquals("Caisse 1", d.ventes.first().deviceName)
        assertEquals("Koto", d.ventes.first().vendeurNom)
    }

    @Test
    fun `une sauvegarde sans retours ni dettes se lit sans exception`() {
        // Le cas le plus courant d'une petite boutique, et pas une anomalie : les clés absentes
        // doivent rendre des listes vides.
        val d = SauvegardeBoutique.lire("""{"products": [], "sales": []}""")
        assertNotNull(d)
        assertTrue(d!!.estVide)
        assertEquals(emptyList<Any>(), d.retours)
        assertEquals(emptyList<Any>(), d.dettes)
    }

    @Test
    fun `une sauvegarde ancienne privee des champs recents reste lisible`() {
        // Une boutique qui n'a pas encore mis son téléphone à jour : la sauvegarde ne porte ni
        // champs pharmacie, ni auteur, ni TVA. Elle doit se lire, avec les valeurs par défaut.
        val d = SauvegardeBoutique.lire(
            """{"products": [{"id": 3, "name": "Savon", "price": 500.0,
                              "category": "Droguerie", "stock": 4.0}]}"""
        )
        val p = d!!.produits.single()
        assertEquals("Savon", p.name)
        assertEquals("Pièce", p.unit)
        assertEquals(5.0, p.lowStockThreshold, 1e-9)
        assertNull(p.dci)
        assertTrue(!p.surOrdonnance)
        assertNull(p.wholesalePrice)
    }

    @Test
    fun `un JSON illisible rend null plutot qu'un catalogue vide`() {
        // La distinction compte : une liste vide se lit « boutique sans produit », donc le gérant
        // croirait avoir perdu ses données. `null` permet à l'écran de dire que la sauvegarde n'a
        // pas pu être lue.
        assertNull(SauvegardeBoutique.lire("pas du json"))
        assertNull(SauvegardeBoutique.lire(""))
        assertNull(SauvegardeBoutique.lire("<html>403 Forbidden</html>"))
    }

    @Test
    fun `une ligne inexploitable est ignoree et n'empeche pas les autres`() {
        // Un produit sans nom serait une ligne anonyme dans le catalogue, une vente sans horodatage
        // tomberait au 1er janvier 1970 et fausserait tous les rapports. Les deux sont écartées,
        // mais une entrée abîmée ne doit pas emporter la sauvegarde entière.
        val d = SauvegardeBoutique.lire(
            """{"products": [{"id": 1, "name": "", "price": 1.0},
                             {"id": 2, "name": "Sucre", "price": 2000.0,
                              "category": "", "stock": 3.0}],
                "sales": [{"timestamp": 0, "totalAmount": 999.0},
                          {"timestamp": 1700000000000, "totalAmount": 500.0}]}"""
        )!!
        assertEquals(listOf("Sucre"), d.produits.map { it.name })
        assertEquals(1, d.ventes.size)
        assertEquals(500.0, d.ventes.single().totalAmount, 1e-9)
    }

    @Test
    fun `un tableau contenant autre chose que des objets ne fait pas tomber la lecture`() {
        val d = SauvegardeBoutique.lire("""{"products": [null, 42, "texte"]}""")
        assertNotNull(d)
        assertTrue(d!!.produits.isEmpty())
    }
}
