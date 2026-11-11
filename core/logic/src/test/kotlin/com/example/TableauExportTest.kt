package com.example

import com.example.data.model.Debt
import com.example.data.model.MouvementCaisse
import com.example.data.model.Product
import com.example.data.model.Restock
import com.example.data.model.Sale
import com.example.data.model.SoldItem
import com.example.util.TableauExport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Le contenu d'un fichier d'export est ce que le gérant envoie à son comptable, et personne ne le
 * relit ligne à ligne. Une colonne décalée ou un accent cassé ne lève aucune erreur : le fichier
 * s'ouvre, il est simplement faux ou illisible, et le gérant conclut que « l'export ne marche
 * pas ». D'où ces cas, qui portent surtout sur ce qu'aucun écran ne montrerait.
 */
class TableauExportTest {

    private fun vente(
        instant: Long,
        total: Double,
        articles: List<SoldItem>
    ) = Sale(id = 1, timestamp = instant, totalAmount = total, items = articles)

    private fun article(nom: String, quantite: Double, prix: Double) =
        SoldItem(productId = 1, name = nom, quantity = quantite, price = prix)

    private fun produit(
        id: Int,
        nom: String,
        stock: Double = 10.0,
        unite: String = "Pièce"
    ) = Product(
        id = id,
        name = nom,
        price = 1000.0,
        stock = stock,
        unit = unite,
        category = "Alimentation"
    )

    private fun dette(
        nom: String,
        note: String = "",
        solde: Double = 1000.0,
        paye: Boolean = false,
        echeance: Long? = null,
        date: Long = 1_000L
    ) = Debt(
        id = 1,
        debtorName = nom,
        amount = 1000.0,
        balance = solde,
        date = date,
        note = note,
        isPaid = paye,
        dueDate = echeance
    )

    // --- Les ventes : une ligne par ARTICLE, pas par vente -------------------------------------

    @Test
    fun `une vente de trois articles donne trois lignes`() {
        // C'est ce qui rend le fichier exploitable : on peut sommer par produit dans un tableur.
        // Une ligne par vente, articles concaténés dans une cellule, l'interdirait.
        val tableau = TableauExport.ventes(
            listOf(
                vente(
                    1_000L, 9_000.0,
                    listOf(
                        article("Riz", 2.0, 3_000.0),
                        article("Savon", 1.0, 1_500.0),
                        article("Menaka", 1.0, 1_500.0)
                    )
                )
            )
        )
        assertEquals(3, tableau.lignes.size)
        // Le total de la VENTE est répété sur chaque ligne, comme sur Android.
        assertTrue(tableau.lignes.all { it.last() == tableau.lignes.first().last() })
    }

    @Test
    fun `les ventes sortent de la plus recente a la plus ancienne`() {
        val tableau = TableauExport.ventes(
            listOf(
                vente(1_000L, 1_000.0, listOf(article("Ancien", 1.0, 1_000.0))),
                vente(9_000L, 2_000.0, listOf(article("Recent", 1.0, 2_000.0)))
            )
        )
        assertEquals("Recent", tableau.lignes.first()[2])
    }

    @Test
    fun `une vente sans article ne produit aucune ligne, et aucune ligne vide`() {
        // Cas réel : une vente dont les articles n'ont pas pu être relus (sauvegarde abîmée).
        // Une ligne aux cellules vides se lirait comme une vente fantôme dans le tableur.
        val tableau = TableauExport.ventes(listOf(vente(1_000L, 0.0, emptyList())))
        assertTrue(tableau.lignes.isEmpty())
    }

    @Test
    fun `chaque ligne a exactement autant de cellules que d en-tetes`() {
        // L'invariant qui décale tout un fichier quand il casse : une cellule de moins sur une
        // ligne et toutes les colonnes suivantes glissent, sans qu'Excel signale rien.
        val tableaux = listOf(
            TableauExport.ventes(listOf(vente(1L, 1.0, listOf(article("Riz", 1.0, 1.0))))),
            TableauExport.stock(listOf(produit(1, "Riz"))),
            TableauExport.dettes(listOf(dette("Rakoto"))),
            TableauExport.mouvementsCaisse(
                listOf(MouvementCaisse(id = 1, type = "ENTREE", montant = 1.0, motif = "Fond"))
            ),
            TableauExport.reappros(
                listOf(
                    Restock(
                        productId = 1, productName = "Riz", cartonsQuantity = 1.0,
                        itemsPerCarton = 10.0, totalUnits = 10.0, totalCostPrice = 1.0,
                        unitSellingPrice = 1.0, supplierId = null, supplierName = null
                    )
                )
            )
        )
        tableaux.forEach { tableau ->
            // Les poids de colonnes servent au rendu PDF d'Android : un poids manquant ferait
            // disparaître une colonne du PDF alors qu'elle est bien dans le CSV.
            assertEquals(tableau.titre, tableau.entetes.size, tableau.poidsColonnes.size)
            tableau.lignes.forEach { ligne ->
                assertEquals(tableau.titre, tableau.entetes.size, ligne.size)
            }
        }
    }

    // --- Le stock ------------------------------------------------------------------------------

    @Test
    fun `le stock est trie par nom sans tenir compte de la casse`() {
        // Un tri binaire placerait « Zebu » avant « ananas », ce qui n'a aucun sens pour
        // quelqu'un qui parcourt la liste des yeux.
        val tableau = TableauExport.stock(
            listOf(produit(1, "Zebu"), produit(2, "ananas"), produit(3, "Menaka"))
        )
        assertEquals(listOf("ananas", "Menaka", "Zebu"), tableau.lignes.map { it[0] })
    }

    // --- Les dettes ---------------------------------------------------------------------------

    @Test
    fun `le statut d une dette est en clair, et le retard depend de l instant fourni`() {
        val dettes = listOf(
            dette("Payee", solde = 0.0, paye = true, date = 3_000L),
            dette("EnRetard", echeance = 5_000L, date = 2_000L),
            dette("EnCours", echeance = 9_000L, date = 1_000L)
        )
        val tableau = TableauExport.dettes(dettes, maintenant = 7_000L)
        assertEquals(listOf("Payée", "En retard", "En cours"), tableau.lignes.map { it[4] })
    }

    @Test
    fun `une dette sans echeance affiche un tiret et non une date de 1970`() {
        // Le piège déjà rencontré à la lecture de la sauvegarde : une échéance absente lue comme
        // 0 donnerait « 01/01/1970 », donc une dette éternellement « en retard ».
        val tableau = TableauExport.dettes(listOf(dette("Rakoto")), maintenant = 7_000L)
        assertEquals("-", tableau.lignes.first()[5])
        assertEquals("En cours", tableau.lignes.first()[4])
    }

    // --- Les mouvements de caisse --------------------------------------------------------------

    @Test
    fun `le type d un mouvement est ecrit en clair`() {
        val tableau = TableauExport.mouvementsCaisse(
            listOf(
                MouvementCaisse(id = 1, type = "ENTREE", montant = 5_000.0, motif = "Fond", date = 2_000L),
                MouvementCaisse(id = 2, type = "SORTIE", montant = 3_000.0, motif = "Achat", date = 1_000L)
            )
        )
        assertEquals(listOf("Entrée", "Sortie"), tableau.lignes.map { it[1] })
    }

    // --- Le format CSV -------------------------------------------------------------------------

    @Test
    fun `le fichier commence par le BOM UTF-8`() {
        // Sans lui, Excel ouvre le fichier dans l'encodage de la machine et le gérant lit
        // « PÃ©remption ». C'est le détail qui fait conclure que l'export ne marche pas.
        val csv = TableauExport.versCsv(TableauExport.stock(listOf(produit(1, "Riz"))))
        assertTrue(csv.startsWith("﻿"))
    }

    @Test
    fun `les lignes se terminent par retour chariot plus saut de ligne`() {
        // Ce qu'écrit Android, et ce qu'attendent les tableurs sous Windows — la seule
        // plateforme de bureau que ce projet construit.
        val csv = TableauExport.versCsv(TableauExport.stock(listOf(produit(1, "Riz"))))
        assertTrue(csv.contains("\r\n"))
        assertFalse(csv.replace("\r\n", "").contains("\n"))
    }

    @Test
    fun `une note de trosa contenant des virgules est mise entre guillemets`() {
        // LE cas qui décale tout un fichier : une note de trosa ressemble à
        // « Riz (2 x 3 500), Savon (1 x 1 500) ». Sans échappement, le tableur y voit deux
        // cellules et toutes les colonnes suivantes glissent d'un cran.
        val note = "Riz (2 x 3 500), Savon (1 x 1 500)"
        val csv = TableauExport.versCsv(
            TableauExport.dettes(listOf(dette("Rakoto", note = note)), maintenant = 1L)
        )
        assertTrue(csv.contains("\"$note\""))
        // Une seule ligne de données : la virgule de la note n'en a pas créé une seconde.
        assertEquals(2, csv.trim().split("\r\n").size)
    }

    @Test
    fun `un guillemet dans une valeur est double et la valeur encadree`() {
        val csv = TableauExport.versCsv(
            TableauExport.dettes(listOf(dette("Rakoto", note = "savon \"doux\"")), maintenant = 1L)
        )
        assertTrue(csv.contains("\"savon \"\"doux\"\"\""))
    }

    @Test
    fun `une valeur ordinaire n est pas encadree inutilement`() {
        // Excel sait lire des guillemets partout, mais un fichier qui en met autour de chaque
        // cellule devient illisible pour qui l'ouvre dans un éditeur de texte pour vérifier.
        val csv = TableauExport.versCsv(TableauExport.stock(listOf(produit(1, "Riz"))))
        assertTrue(csv.contains(",Riz,") || csv.contains("\nRiz,"))
    }

    @Test
    fun `un tableau sans ligne garde quand meme ses en-tetes`() {
        // La décision d'écrire ou non un fichier appartient à l'appelant (`ExportFichier` refuse
        // une liste vide) ; le rendu, lui, doit rester cohérent et ne pas lever.
        val csv = TableauExport.versCsv(TableauExport.stock(emptyList()))
        assertEquals(1, csv.trim().split("\r\n").size)
        assertTrue(csv.contains("Nom"))
    }

    @Test
    fun `les noms de fichier sont ceux d Android, un par tableau`() {
        // Ils sont le seul repère du gérant dans son dossier d'export : les renommer rendrait
        // introuvables les fichiers déjà envoyés au comptable sous l'ancien nom.
        assertEquals("historique_ventes", TableauExport.ventes(emptyList()).nomFichier)
        assertEquals("inventaire_stock", TableauExport.stock(emptyList()).nomFichier)
        assertEquals("dettes", TableauExport.dettes(emptyList()).nomFichier)
        assertEquals("mouvements_caisse", TableauExport.mouvementsCaisse(emptyList()).nomFichier)
        assertEquals("reappro", TableauExport.reappros(emptyList()).nomFichier)
    }
}
