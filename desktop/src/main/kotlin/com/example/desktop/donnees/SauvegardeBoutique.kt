package com.example.desktop.donnees

import com.example.data.model.Debt
import com.example.data.model.Product
import com.example.data.model.Retour
import com.example.data.model.Sale
import com.example.data.model.SoldItem
import org.json.JSONArray
import org.json.JSONObject

/**
 * Les données d'une boutique, telles que le bureau les lit.
 *
 * Volontairement les quatre collections dont les écrans de bureau ont besoin, et pas une de plus :
 * le catalogue, les ventes, les trosa et les retours. Les mouvements de caisse, sessions, vendeurs,
 * lots et plan d'étagères sont bien dans la sauvegarde mais aucun écran de bureau ne les affiche —
 * les lire pour les ignorer ne ferait qu'ajouter du code à maintenir.
 */
data class DonneesBoutique(
    val produits: List<Product>,
    val ventes: List<Sale>,
    val dettes: List<Debt>,
    val retours: List<Retour>
) {
    val estVide: Boolean
        get() = produits.isEmpty() && ventes.isEmpty() && dettes.isEmpty() && retours.isEmpty()
}

/**
 * Lecture de la sauvegarde consolidée de la boutique, celle que le téléphone envoie sur Firebase.
 *
 * ## Pourquoi ce chemin plutôt que Room KMP
 *
 * Le bureau doit afficher les données **réelles** d'une boutique pour servir à quelque chose : tant
 * qu'il tourne sur un jeu de démonstration, c'est une maquette. Partager la base de données
 * demanderait Room KMP, donc de toucher au fichier de base de **chaque boutique en service** — un
 * chantier qu'on ne mène pas sous la pression du calendrier.
 *
 * Or la même donnée est déjà disponible autrement : le téléphone sérialise toute sa base en JSON à
 * chaque mutation et l'envoie sur Firebase sous le code de récupération du gérant (sections 24
 * et 40). Le bureau n'a donc qu'à la lire. **Aucune base n'est touchée, aucune migration n'est
 * nécessaire, et le format est déjà rétro-compatible par construction** (`optString`/`optDouble`
 * partout côté Android).
 *
 * ## Lecture seule, et c'est une décision
 *
 * Ce lecteur n'écrit rien. Un bureau qui renverrait sa propre version de la sauvegarde écraserait
 * celle du téléphone — la fusion côté Android est faite pour des appareils qui possèdent tous une
 * base Room, ce qui n'est pas le cas ici. Afficher sans écrire est utile tout de suite ; écrire
 * demande la vraie base partagée.
 *
 * ## Tolérance identique à celle d'Android
 *
 * Chaque champ est lu avec son équivalent `opt…` et sa valeur par défaut, exactement comme
 * `syncFullDatabaseSync` : une sauvegarde écrite par une version antérieure de l'application ne
 * doit pas faire échouer la lecture, elle doit simplement rendre moins de champs. Une sauvegarde
 * **illisible**, en revanche, rend `null` plutôt qu'un objet à moitié rempli — afficher un
 * catalogue tronqué sans le dire serait pire que de ne rien afficher.
 */
object SauvegardeBoutique {

    /**
     * Interprète le JSON de sauvegarde. Renvoie `null` si l'enveloppe elle-même est illisible.
     *
     * Les clés de l'enveloppe sont celles de `SyncSerializer.serializeFullSync` ; chacune est
     * facultative, parce qu'une sauvegarde peut légitimement n'avoir ni retour ni dette.
     */
    fun lire(json: String): DonneesBoutique? {
        val racine = try {
            JSONObject(json)
        } catch (e: Exception) {
            return null
        }
        return DonneesBoutique(
            produits = racine.optJSONArray("products").objets().mapNotNull { lireProduit(it) },
            ventes = racine.optJSONArray("sales").objets().mapNotNull { lireVente(it) },
            dettes = racine.optJSONArray("debts").objets().mapNotNull { lireDette(it) },
            retours = racine.optJSONArray("retours").objets().mapNotNull { lireRetour(it) }
        )
    }

    /**
     * Un produit sans nom est ignoré plutôt que rendu sous un libellé vide : une ligne anonyme dans
     * le catalogue ne sert à rien et se confond avec un défaut d'affichage.
     */
    private fun lireProduit(o: JSONObject): Product? {
        val nom = o.optString("name", "").trim()
        if (nom.isEmpty()) return null
        return Product(
            id = o.optInt("id", 0),
            name = nom,
            price = o.optDouble("price", 0.0),
            category = o.optString("category", ""),
            stock = o.optDouble("stock", 0.0),
            imageUrl = o.optString("imageUrl", ""),
            lowStockThreshold = o.optDouble("lowStockThreshold", 5.0),
            unit = o.optString("unit", "Pièce"),
            barcode = o.optString("barcode", ""),
            // `> 0.0` et non la valeur brute : le téléphone écrit 0.0 pour « pas de tarif de gros »,
            // et un 0.0 relu tel quel ferait vendre à zéro en mode Grossiste. Même règle que
            // `PriceUtil.isWholesaleActive`, et exactement le piège corrigé en section 85.
            wholesalePrice = o.optDouble("wholesalePrice", 0.0).takeIf { it > 0.0 },
            sku = o.optString("sku", ""),
            stock_quantity = o.optInt("stock_quantity", 0),
            nomCourt = o.optString("nomCourt", "").ifBlank { null },
            sousCategorie = o.optString("sousCategorie", "").ifBlank { null },
            marque = o.optString("marque", "").ifBlank { null },
            description = o.optString("description", "").ifBlank { null },
            stockMax = o.optDouble("stockMax", 0.0).takeIf { it > 0.0 },
            emplacement = o.optString("emplacement", "").ifBlank { null },
            fournisseurId = o.optLong("fournisseurId", -1L).takeIf { it > 0L },
            gerePeremption = o.optBoolean("gerePeremption", false),
            taxable = o.optBoolean("taxable", false),
            tauxTaxe = o.optDouble("tauxTaxe", 0.0),
            prixAchatUniteBase = o.optDouble("prixAchatUniteBase", 0.0),
            isTemplate = o.optBoolean("isTemplate", false),
            dci = o.optString("dci", "").ifBlank { null },
            dosage = o.optString("dosage", "").ifBlank { null },
            formeGalenique = o.optString("formeGalenique", "").ifBlank { null },
            surOrdonnance = o.optBoolean("surOrdonnance", false)
        )
    }

    /**
     * Une vente sans horodatage est ignorée : c'est l'horodatage qui la place dans une période, donc
     * une vente à zéro apparaîtrait au 1er janvier 1970 et fausserait tout rapport.
     */
    private fun lireVente(o: JSONObject): Sale? {
        val horodatage = o.optLong("timestamp", 0L)
        if (horodatage <= 0L) return null
        return Sale(
            timestamp = horodatage,
            totalAmount = o.optDouble("totalAmount", 0.0),
            items = o.optJSONArray("items").objets().map { lireArticle(it) },
            deviceName = o.optString("deviceName", ""),
            vendeurNom = o.optString("vendeurNom", "")
        )
    }

    private fun lireDette(o: JSONObject): Debt? {
        val nom = o.optString("debtorName", "").trim()
        if (nom.isEmpty()) return null
        return Debt(
            debtorName = nom,
            amount = o.optDouble("amount", 0.0),
            balance = o.optDouble("balance", 0.0),
            date = o.optLong("date", 0L),
            note = o.optString("note", ""),
            isPaid = o.optBoolean("isPaid", false),
            // `isNull` et non `optLong` : le téléphone écrit `JSONObject.NULL` quand il n'y a pas
            // d'échéance, et `optLong` rendrait 0 — donc une dette éternellement « EN RETARD »,
            // datée de 1970, sur chaque trosa sans échéance de la boutique.
            dueDate = if (o.isNull("dueDate")) null else o.optLong("dueDate", 0L).takeIf { it > 0L },
            deviceName = o.optString("deviceName", ""),
            vendeurNom = o.optString("vendeurNom", "")
        )
    }

    private fun lireRetour(o: JSONObject): Retour? {
        val horodatage = o.optLong("timestamp", 0L)
        if (horodatage <= 0L) return null
        return Retour(
            saleId = o.optInt("saleId", 0),
            timestamp = horodatage,
            items = o.optJSONArray("items").objets().map { lireArticle(it) },
            totalAmount = o.optDouble("totalAmount", 0.0),
            motif = o.optString("motif", ""),
            modePaiementOrigine = o.optString("modePaiementOrigine", "ESPECES"),
            deviceName = o.optString("deviceName", ""),
            vendeurNom = o.optString("vendeurNom", "")
        )
    }

    private fun lireArticle(o: JSONObject): SoldItem = SoldItem(
        productId = o.optInt("productId", 0),
        name = o.optString("name", ""),
        quantity = o.optDouble("quantity", 0.0),
        price = o.optDouble("price", 0.0),
        taxable = o.optBoolean("taxable", false),
        tauxTaxe = o.optDouble("tauxTaxe", 0.0)
    )

    /**
     * Les objets d'un tableau JSON, les entrées non-objet ignorées.
     *
     * Un tableau absent rend une liste vide, pas une exception : une boutique sans aucun retour est
     * le cas le plus courant, pas une anomalie.
     */
    private fun JSONArray?.objets(): List<JSONObject> {
        if (this == null) return emptyList()
        return (0 until length()).mapNotNull { optJSONObject(it) }
    }
}
