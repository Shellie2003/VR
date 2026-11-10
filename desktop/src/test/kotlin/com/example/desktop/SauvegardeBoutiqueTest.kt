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
          "restocks": [
            {"id": 3, "productId": 1, "productName": "Riz", "cartonsQuantity": 2.0,
             "itemsPerCarton": 25.0, "totalUnits": 50.0, "totalCostPrice": 125000.0,
             "unitSellingPrice": 3000.0, "supplierId": 4, "supplierName": "Grossiste",
             "timestamp": 1700001000000, "deviceName": "Caisse 1", "vendeurNom": "Koto"},
            {"id": 4, "productId": 2, "productName": "Ananas", "cartonsQuantity": 1.0,
             "itemsPerCarton": 0.0, "totalUnits": 0.0, "totalCostPrice": 15000.0,
             "unitSellingPrice": 2500.0, "supplierId": -1, "supplierName": "",
             "timestamp": 1700001100000}
          ],
          "mouvementsCaisse": [
            {"type": "ENTREE", "montant": 50000.0, "motif": "Fond de caisse", "note": "",
             "date": 1700002000000, "deviceName": "Caisse 1", "vendeurNom": "Koto"},
            {"type": "SORTIE", "montant": 12000.0, "motif": "Sacs", "note": "au marche",
             "date": 1700002100000},
            {"type": "BIZARRE", "montant": 99999.0, "motif": "Inconnu", "date": 1700002200000}
          ],
          "caisseSessions": [
            {"dateOuverture": 1700000000000, "montantOuverture": 50000.0,
             "dateFermeture": 1700030000000, "montantCompteFermeture": 243000.0,
             "montantTheoriqueFermeture": 248500.0, "ecart": -5500.0, "note": "Manquant"},
            {"dateOuverture": 1700040000000, "montantOuverture": 30000.0,
             "dateFermeture": null, "montantCompteFermeture": null,
             "montantTheoriqueFermeture": null, "ecart": null, "note": ""}
          ],
          "lots": [
            {"productBarcode": "123", "productName": "Riz", "numeroLot": "L-1",
             "quantite": 6.0, "datePeremption": 1800000000000, "dateReception": 1700000000000},
            {"productBarcode": "", "productName": "Inconnu au catalogue", "numeroLot": "",
             "quantite": 2.0, "datePeremption": 1800500000000, "dateReception": 0}
          ],
          "etageres": [
            {"nom": "Rayon A", "position": 1, "couleur": "#2E7D32"},
            {"nom": "Rayon B", "position": 0, "couleur": ""}
          ],
          "niveauxEtagere": [
            {"etagereNom": "Rayon A", "etagerePosition": 1, "position": 0, "nom": "Haut",
             "couleur": ""},
            {"etagereNom": "Rayon A", "etagerePosition": 1, "position": 1, "nom": "Bas",
             "couleur": "#F57C00"},
            {"etagereNom": "Fantome", "etagerePosition": 9, "position": 0, "nom": "Orphelin",
             "couleur": ""}
          ],
          "produitsNiveau": [
            {"etagereNom": "Rayon A", "etagerePosition": 1, "niveauPosition": 0,
             "produitBarcode": "123", "produitSku": "", "produitNom": "Riz", "ordre": 0},
            {"etagereNom": "Rayon A", "etagerePosition": 1, "niveauPosition": 1,
             "produitBarcode": "", "produitSku": "", "produitNom": "Doliprane", "ordre": 0},
            {"etagereNom": "Rayon A", "etagerePosition": 1, "niveauPosition": 0,
             "produitBarcode": "999", "produitSku": "", "produitNom": "Supprime", "ordre": 1}
          ],
          "vendeurs": []
        }
    """.trimIndent()

    @Test
    fun `une sauvegarde complete est lue dans toutes ses collections`() {
        val d = SauvegardeBoutique.lire(sauvegarde)
        assertNotNull(d)
        assertEquals(2, d!!.produits.size)
        assertEquals(1, d.ventes.size)
        assertEquals(2, d.dettes.size)
        assertEquals(1, d.retours.size)
        assertEquals(2, d.reappros.size)
        assertEquals(2, d.sessionsCaisse.size)
        assertEquals(2, d.lots.size)
        assertEquals(2, d.planEtagere.size)
        assertTrue(!d.estVide)
    }

    @Test
    fun `un mouvement de caisse au type inconnu est ignore et non compte au hasard`() {
        // Le type EST le signe du mouvement. Une valeur inconnue traitée comme une entrée
        // gonflerait le solde affiché, traitée comme une sortie le creuserait — dans les deux cas
        // sans rien dire. La ligne « BIZARRE » de la sauvegarde de référence est donc écartée.
        val d = SauvegardeBoutique.lire(sauvegarde)!!
        assertEquals(2, d.mouvementsCaisse.size)
        assertEquals(50000.0, d.mouvementsCaisse.first { it.type == "ENTREE" }.montant, 0.001)
        assertEquals(12000.0, d.mouvementsCaisse.first { it.type == "SORTIE" }.montant, 0.001)
    }

    @Test
    fun `une session ouverte le reste au lieu d'etre lue comme fermee a zero`() {
        // Le téléphone écrit `JSONObject.NULL` sur les quatre champs de fermeture d'une session
        // ouverte, et `optDouble` rendrait 0.0 : l'écran annoncerait une session fermée avec un
        // écart nul, donc un comptage qui tombe juste alors que rien n'a été compté.
        val d = SauvegardeBoutique.lire(sauvegarde)!!
        val ouverte = d.sessionsCaisse.first { it.isOpen }
        assertNull(ouverte.dateFermeture)
        assertNull(ouverte.ecart)
        assertNull(ouverte.montantCompteFermeture)

        // Zéro reste en revanche une valeur LÉGITIME sur une session fermée : un écart nul veut
        // dire que le comptage tombe juste, et le confondre avec « non renseigné » effacerait le
        // seul résultat qui rassure le gérant.
        val fermee = d.sessionsCaisse.first { !it.isOpen }
        assertEquals(-5500.0, fermee.ecart!!, 0.001)
    }

    @Test
    fun `un reappro garde ses unites a zero, l'ecran s'en charge`() {
        // Le lecteur ne corrige pas la donnée : c'est `FiltreHistorique.coutUnitaireReappro` qui
        // décide d'afficher un tiret. Inventer une unité ici fabriquerait un coût unitaire faux.
        val d = SauvegardeBoutique.lire(sauvegarde)!!
        val sansUnite = d.reappros.first { it.productName == "Ananas" }
        assertEquals(0.0, sansUnite.totalUnits, 0.001)
        assertNull("supplierName vide doit devenir null", sansUnite.supplierName)
        assertNull("supplierId -1 doit devenir null", sansUnite.supplierId)
        assertEquals("Grossiste", d.reappros.first { it.productName == "Riz" }.supplierName)
    }

    @Test
    fun `un lot reprend l'unite de son produit, et survit a un produit introuvable`() {
        val d = SauvegardeBoutique.lire(sauvegarde)!!
        val duRiz = d.lots.first { it.produitNom == "Riz" }
        assertEquals("Kilogramme", duRiz.unite)
        assertEquals("L-1", duRiz.numeroLot)

        // Un produit supprimé depuis ne fait pas disparaître le lot : son nom reste celui que la
        // sauvegarde a conservé, et c'est ce nom que le gérant lira pour aller en rayon.
        val orphelin = d.lots.first { it.produitNom == "Inconnu au catalogue" }
        assertEquals("", orphelin.unite)
        assertNull("Un numéro de lot vide doit devenir null", orphelin.numeroLot)
    }

    @Test
    fun `le plan d'etageres est trie par position et resolu par cle naturelle`() {
        val d = SauvegardeBoutique.lire(sauvegarde)!!
        // Trié par position, pas par ordre d'apparition dans le JSON : « Rayon B » est en 0.
        assertEquals(listOf("Rayon B", "Rayon A"), d.planEtagere.map { it.etagere.nom })

        val rayonA = d.planEtagere.first { it.etagere.nom == "Rayon A" }
        // Le niveau dont l'étagère parente n'existe pas est écarté, et non rattaché au hasard :
        // un niveau orphelin accroché à la mauvaise étagère enverrait le vendeur au mauvais rayon.
        assertEquals(2, rayonA.niveaux.size)
        assertEquals(listOf("Haut", "Bas"), rayonA.niveaux.map { it.niveau.nom })

        // Résolution code-barres puis SKU puis nom, l'ordre d'Android. Le produit « Supprime »,
        // absent du catalogue, n'est pas rangé : un lien vers un produit inconnu n'affiche rien.
        assertEquals(listOf("Riz"), rayonA.niveaux[0].produits.map { it.name })
        assertEquals(listOf("Doliprane"), rayonA.niveaux[1].produits.map { it.name })
        assertEquals("#F57C00", rayonA.niveaux[1].niveau.couleur)
        assertNull("Une couleur vide doit devenir null", rayonA.niveaux[0].niveau.couleur)
    }

    @Test
    fun `une sauvegarde sans les nouvelles cles reste lisible`() {
        // Une sauvegarde écrite par une version antérieure n'a ni `restocks`, ni `caisseSessions`,
        // ni plan d'étagères. Elle doit rendre des listes vides, pas échouer : c'est la garantie
        // qui permet de relier un PC à une boutique dont le téléphone n'est pas à jour.
        val ancienne = """
            {"products": [{"id": 1, "name": "Riz", "price": 3000.0}],
             "sales": [], "debts": []}
        """.trimIndent()
        val d = SauvegardeBoutique.lire(ancienne)
        assertNotNull(d)
        assertEquals(1, d!!.produits.size)
        assertTrue(d.reappros.isEmpty())
        assertTrue(d.mouvementsCaisse.isEmpty())
        assertTrue(d.sessionsCaisse.isEmpty())
        assertTrue(d.lots.isEmpty())
        assertTrue(d.planEtagere.isEmpty())
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
