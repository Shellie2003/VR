package com.example.util

import com.example.data.model.Debt
import com.example.data.model.MouvementCaisse
import com.example.data.model.Product
import com.example.data.model.Restock
import com.example.data.model.Sale
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Un tableau prêt à exporter : son titre, le nom du fichier, ses en-têtes, ses lignes, et les
 * largeurs relatives de colonnes que le rendu PDF emploie.
 */
data class Tableau(
    val nomFichier: String,
    val titre: String,
    val entetes: List<String>,
    val lignes: List<List<String>>,
    val poidsColonnes: List<Float>
)

/**
 * Les tableaux d'export (ventes, stock, dettes, caisse, réappros) et leur rendu CSV.
 *
 * ## Pourquoi hors de `ExportUtil`
 *
 * `ExportUtil` est un fichier **Android** : il écrit des PDF avec `PdfDocument`, dépose dans
 * `Downloads` par MediaStore et partage via `FileProvider`. Rien de tout cela n'existe sur un PC.
 * Mais le **contenu** des tableaux — les colonnes, leur ordre, leur formatage — n'a aucune raison
 * de différer : un gérant qui exporte ses ventes depuis son téléphone et depuis son PC doit
 * obtenir le même fichier, sinon son comptable reçoit deux formats pour une seule boutique.
 *
 * La construction des lignes vit donc ici, et les deux plateformes s'en servent : Android pour son
 * CSV **et** son PDF, le bureau pour son CSV. Seule l'écriture du fichier reste propre à chacune.
 *
 * ## Le format CSV est celui d'Android, au caractère près
 *
 * Virgule comme séparateur, `\r\n` en fin de ligne, échappement par guillemets doublés quand la
 * valeur contient une virgule, un guillemet ou un retour à la ligne, et surtout le **BOM UTF-8**
 * en tête : sans lui, Excel rend « Péremption » en « PÃ©remption » et le fichier devient illisible
 * pour la seule personne à qui il est destiné.
 */
object TableauExport {

    private val formatDate = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
    private val formatHeure = SimpleDateFormat("HH:mm", Locale.FRANCE)
    private val formatDateHeure = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE)

    /**
     * Les ventes, **une ligne par article vendu** et non par vente.
     *
     * C'est ce qui rend le fichier exploitable dans un tableur : on peut y faire la somme par
     * produit, ce qu'une ligne par vente avec ses articles concaténés dans une cellule
     * interdirait. Le total de la vente est répété sur chaque ligne, comme sur Android.
     */
    fun ventes(ventes: List<Sale>): Tableau {
        val lignes = mutableListOf<List<String>>()
        ventes.sortedByDescending { it.timestamp }.forEach { vente ->
            val date = Date(vente.timestamp)
            vente.items.forEach { article ->
                lignes.add(
                    listOf(
                        formatDate.format(date),
                        formatHeure.format(date),
                        article.name,
                        FormatUtil.formatQty(article.quantity, "").trim(),
                        FormatUtil.formatPrice(article.price),
                        FormatUtil.formatPrice(article.price * article.quantity),
                        FormatUtil.formatPrice(vente.totalAmount)
                    )
                )
            }
        }
        return Tableau(
            nomFichier = "historique_ventes",
            titre = "Historique des ventes",
            entetes = listOf(
                "Date", "Heure", "Produit", "Quantité",
                "Prix unitaire (Ar)", "Total ligne (Ar)", "Total vente (Ar)"
            ),
            lignes = lignes,
            poidsColonnes = listOf(1.2f, 0.8f, 2.2f, 1f, 1.3f, 1.3f, 1.3f)
        )
    }

    /** Le stock, trié par nom insensible à la casse — l'ordre dans lequel on parcourt une liste. */
    fun stock(produits: List<Product>): Tableau = Tableau(
        nomFichier = "inventaire_stock",
        titre = "Inventaire / État des stocks",
        entetes = listOf(
            "Nom", "Catégorie", "SKU", "Code-barres", "Unité",
            "Stock actuel", "Seuil alerte", "Prix vente (Ar)", "Prix achat (Ar)"
        ),
        lignes = produits.sortedBy { it.name.lowercase() }.map { produit ->
            listOf(
                produit.name,
                produit.category,
                produit.sku,
                produit.barcode,
                produit.unit,
                FormatUtil.formatQty(produit.stock, "").trim(),
                FormatUtil.formatQty(produit.lowStockThreshold, "").trim(),
                FormatUtil.formatPrice(produit.price),
                FormatUtil.formatPrice(produit.prixAchatUniteBase)
            )
        },
        poidsColonnes = listOf(2f, 1.3f, 1.2f, 1.4f, 1f, 1f, 1f, 1.2f, 1.2f)
    )

    /**
     * Les dettes, transaction par transaction et non regroupées par débiteur.
     *
     * Volontairement à plat, comme sur Android : le regroupement par personne (section 31) est un
     * confort d'affichage, alors qu'un export sert à retrouver une ligne précise — « quand a-t-il
     * pris ça ? ». Le tableur sait regrouper tout seul.
     *
     * `maintenant` est un paramètre : le statut « En retard » dépend de l'instant, et le figer
     * rend la fonction testable sans dépendre du jour d'exécution.
     */
    fun dettes(dettes: List<Debt>, maintenant: Long = System.currentTimeMillis()): Tableau = Tableau(
        nomFichier = "dettes",
        titre = "Dettes clients",
        entetes = listOf(
            "Débiteur", "Date", "Montant initial (Ar)", "Solde restant (Ar)",
            "Statut", "Échéance", "Note"
        ),
        lignes = dettes.sortedByDescending { it.date }.map { dette ->
            listOf(
                dette.debtorName,
                formatDate.format(Date(dette.date)),
                FormatUtil.formatPrice(dette.amount),
                FormatUtil.formatPrice(dette.balance),
                when {
                    dette.isPaid -> "Payée"
                    dette.isOverdue(maintenant) -> "En retard"
                    else -> "En cours"
                },
                dette.dueDate?.let { formatDate.format(Date(it)) } ?: "-",
                dette.note
            )
        },
        poidsColonnes = listOf(1.6f, 1f, 1.3f, 1.3f, 1f, 1f, 2f)
    )

    /** Les mouvements d'espèces. Le type est écrit en clair : « Entrée » / « Sortie ». */
    fun mouvementsCaisse(mouvements: List<MouvementCaisse>): Tableau = Tableau(
        nomFichier = "mouvements_caisse",
        titre = "Mouvements de caisse",
        entetes = listOf("Date", "Type", "Motif", "Montant (Ar)", "Note"),
        lignes = mouvements.sortedByDescending { it.date }.map { mouvement ->
            listOf(
                formatDateHeure.format(Date(mouvement.date)),
                if (mouvement.type == "ENTREE") "Entrée" else "Sortie",
                mouvement.motif,
                FormatUtil.formatPrice(mouvement.montant),
                mouvement.note
            )
        },
        poidsColonnes = listOf(1.4f, 1f, 1.6f, 1.2f, 2f)
    )

    /** Les réapprovisionnements. */
    fun reappros(reappros: List<Restock>): Tableau = Tableau(
        nomFichier = "reappro",
        titre = "Historique des réapprovisionnements",
        entetes = listOf(
            "Date", "Produit", "Cartons", "Unités/carton", "Total unités",
            "Coût total (Ar)", "Prix vente unité (Ar)", "Fournisseur"
        ),
        lignes = reappros.sortedByDescending { it.timestamp }.map { reappro ->
            listOf(
                formatDateHeure.format(Date(reappro.timestamp)),
                reappro.productName,
                FormatUtil.formatQty(reappro.cartonsQuantity, "").trim(),
                FormatUtil.formatQty(reappro.itemsPerCarton, "").trim(),
                FormatUtil.formatQty(reappro.totalUnits, "").trim(),
                FormatUtil.formatPrice(reappro.totalCostPrice),
                FormatUtil.formatPrice(reappro.unitSellingPrice),
                reappro.supplierName ?: ""
            )
        },
        poidsColonnes = listOf(1.3f, 1.8f, 1f, 1.2f, 1.1f, 1.3f, 1.3f, 1.4f)
    )

    /**
     * Le contenu CSV complet d'un tableau, BOM compris.
     *
     * Le **BOM UTF-8** n'est pas une précaution théorique : sans lui, Excel ouvre le fichier dans
     * l'encodage de la machine et le gérant lit « PÃ©remption ». C'est le genre de détail qui fait
     * conclure que l'export « ne marche pas ».
     *
     * `\r\n` et non `\n` : c'est ce qu'écrit Android, et ce qu'attendent les tableurs sous Windows,
     * la seule plateforme de bureau que ce projet construit.
     */
    fun versCsv(tableau: Tableau): String {
        val sortie = StringBuilder()
        sortie.append(BOM)
        sortie.append(tableau.entetes.joinToString(",") { echapper(it) }).append("\r\n")
        tableau.lignes.forEach { ligne ->
            sortie.append(ligne.joinToString(",") { echapper(it) }).append("\r\n")
        }
        return sortie.toString()
    }

    /**
     * Échappement CSV, identique à celui d'Android : guillemets doublés, et la valeur entière
     * entre guillemets dès qu'elle contient une virgule, un guillemet ou un retour à la ligne.
     *
     * Une note de trosa ressemble à `"Riz (2 x 3 500), Savon (1 x 1 500)"` — elle contient donc
     * des virgules, et sans cet échappement elle décalerait toutes les colonnes suivantes.
     */
    private fun echapper(valeur: String): String =
        if (valeur.contains(',') || valeur.contains('"') || valeur.contains('\n')) {
            "\"" + valeur.replace("\"", "\"\"") + "\""
        } else {
            valeur
        }

    /** Le BOM UTF-8, écrit en toute première position du fichier. */
    const val BOM = "﻿"
}
