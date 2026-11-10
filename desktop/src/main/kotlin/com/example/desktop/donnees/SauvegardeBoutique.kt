package com.example.desktop.donnees

import com.example.data.model.CaisseSession
import com.example.data.model.Debt
import com.example.data.model.Etagere
import com.example.data.model.EtagereAvecNiveaux
import com.example.data.model.LotLisible
import com.example.data.model.MouvementCaisse
import com.example.data.model.NiveauAvecProduits
import com.example.data.model.NiveauEtagere
import com.example.data.model.Product
import com.example.data.model.ProduitNiveau
import com.example.data.model.Restock
import com.example.data.model.Retour
import com.example.data.model.Sale
import com.example.data.model.SoldItem
import org.json.JSONArray
import org.json.JSONObject

/**
 * Les données d'une boutique, telles que le bureau les lit.
 *
 * La sauvegarde porte treize collections ; celles qui sont lues ici sont celles qu'un écran de
 * bureau affiche réellement. Les lire pour les ignorer n'ajouterait que du code à maintenir — et
 * c'est pourquoi `vendeurs` et `deletedRecords` restent dehors : la gestion des comptes est une
 * écriture (impossible en lecture seule), et une pierre tombale ne se regarde pas, elle sert à la
 * fusion côté Android.
 *
 * Les champs ajoutés après coup portent une valeur par défaut, pour la même raison que les clés
 * additionnelles de `SyncSerializer` : un appelant écrit avant leur arrivée continue de compiler.
 */
data class DonneesBoutique(
    val produits: List<Product>,
    val ventes: List<Sale>,
    val dettes: List<Debt>,
    val retours: List<Retour>,
    val reappros: List<Restock> = emptyList(),
    val mouvementsCaisse: List<MouvementCaisse> = emptyList(),
    val sessionsCaisse: List<CaisseSession> = emptyList(),
    val lots: List<LotLisible> = emptyList(),
    val planEtagere: List<EtagereAvecNiveaux> = emptyList()
) {
    /**
     * Rien du tout n'a été lu.
     *
     * Toutes les collections comptent, et pas seulement le catalogue : une boutique qui n'aurait
     * que des mouvements de caisse n'est pas vide, et l'annoncer comme telle ferait croire au
     * gérant que ses données ont disparu.
     */
    val estVide: Boolean
        get() = produits.isEmpty() && ventes.isEmpty() && dettes.isEmpty() && retours.isEmpty() &&
            reappros.isEmpty() && mouvementsCaisse.isEmpty() && sessionsCaisse.isEmpty() &&
            lots.isEmpty() && planEtagere.isEmpty()
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
        val produits = racine.optJSONArray("products").objets().mapNotNull { lireProduit(it) }
        return DonneesBoutique(
            produits = produits,
            ventes = racine.optJSONArray("sales").objets().mapNotNull { lireVente(it) },
            dettes = racine.optJSONArray("debts").objets().mapNotNull { lireDette(it) },
            retours = racine.optJSONArray("retours").objets().mapNotNull { lireRetour(it) },
            reappros = racine.optJSONArray("restocks").objets().mapNotNull { lireReappro(it) },
            mouvementsCaisse = racine.optJSONArray("mouvementsCaisse").objets()
                .mapNotNull { lireMouvementCaisse(it) },
            sessionsCaisse = racine.optJSONArray("caisseSessions").objets()
                .mapNotNull { lireSessionCaisse(it) },
            lots = racine.optJSONArray("lots").objets().mapNotNull { lireLot(it, produits) },
            planEtagere = lirePlanEtagere(
                etageres = racine.optJSONArray("etageres").objets(),
                niveaux = racine.optJSONArray("niveauxEtagere").objets(),
                liens = racine.optJSONArray("produitsNiveau").objets(),
                produits = produits
            )
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

    /**
     * Un réapprovisionnement sans produit nommé est ignoré : la ligne n'aurait rien à annoncer.
     *
     * `totalUnits` est conservé tel quel, y compris à zéro. L'écran qui affiche un coût unitaire
     * doit donc s'en défier : la carte Android divise `totalCostPrice / totalUnits` sans garde,
     * ce qui donne « Ar ∞ » sur une saisie à zéro unité (défaut constaté à la lecture, laissé en
     * place côté Android faute de pouvoir le vérifier à l'écran ; le port bureau s'en protège).
     */
    private fun lireReappro(o: JSONObject): Restock? {
        val nom = o.optString("productName", "").trim()
        if (nom.isEmpty()) return null
        return Restock(
            id = o.optInt("id", 0),
            productId = o.optInt("productId", 0),
            productName = nom,
            cartonsQuantity = o.optDouble("cartonsQuantity", 0.0),
            itemsPerCarton = o.optDouble("itemsPerCarton", 0.0),
            totalUnits = o.optDouble("totalUnits", 0.0),
            totalCostPrice = o.optDouble("totalCostPrice", 0.0),
            unitSellingPrice = o.optDouble("unitSellingPrice", 0.0),
            supplierId = o.optLong("supplierId", -1L).takeIf { it > 0L },
            supplierName = o.optString("supplierName", "").ifBlank { null },
            timestamp = o.optLong("timestamp", 0L),
            deviceName = o.optString("deviceName", ""),
            vendeurNom = o.optString("vendeurNom", "")
        )
    }

    /**
     * Un mouvement de caisse dont le type n'est ni `ENTREE` ni `SORTIE` est ignoré.
     *
     * Ce n'est pas du zèle : c'est le signe du mouvement. Une valeur inconnue traitée comme une
     * entrée **gonflerait** le solde affiché, et traitée comme une sortie le creuserait — dans les
     * deux cas sans rien dire. Mieux vaut une ligne en moins qu'un total faux.
     */
    private fun lireMouvementCaisse(o: JSONObject): MouvementCaisse? {
        val type = o.optString("type", "").trim().uppercase()
        if (type != "ENTREE" && type != "SORTIE") return null
        val date = o.optLong("date", 0L)
        if (date <= 0L) return null
        return MouvementCaisse(
            type = type,
            montant = o.optDouble("montant", 0.0),
            motif = o.optString("motif", ""),
            note = o.optString("note", ""),
            date = date,
            deviceName = o.optString("deviceName", ""),
            vendeurNom = o.optString("vendeurNom", "")
        )
    }

    /**
     * Une session de caisse sans date d'ouverture est ignorée : c'est elle qui la situe.
     *
     * Les quatre champs de fermeture passent par `isNull`, comme l'échéance d'une dette : le
     * téléphone écrit `JSONObject.NULL` sur une session encore **ouverte**, et `optDouble` rendrait
     * 0.0 — donc un écart de caisse nul et une session annoncée fermée alors qu'elle ne l'est pas.
     */
    private fun lireSessionCaisse(o: JSONObject): CaisseSession? {
        val ouverture = o.optLong("dateOuverture", 0L)
        if (ouverture <= 0L) return null
        return CaisseSession(
            dateOuverture = ouverture,
            montantOuverture = o.optDouble("montantOuverture", 0.0),
            dateFermeture = o.optionnelLong("dateFermeture"),
            montantCompteFermeture = o.optionnelDouble("montantCompteFermeture"),
            montantTheoriqueFermeture = o.optionnelDouble("montantTheoriqueFermeture"),
            ecart = o.optionnelDouble("ecart"),
            note = o.optString("note", "")
        )
    }

    /**
     * Un lot sans date de péremption est ignoré : c'est la seule donnée qui le rend utile.
     *
     * Le produit est retrouvé **par code-barres puis par nom**, l'ordre employé par la sauvegarde
     * elle-même (section 29), et uniquement pour en reprendre l'unité. Un produit supprimé depuis
     * ne fait pas disparaître le lot : son nom reste celui que la sauvegarde a conservé, et c'est
     * ce nom que le gérant lira pour aller chercher la marchandise en rayon.
     */
    private fun lireLot(o: JSONObject, produits: List<Product>): LotLisible? {
        val peremption = o.optLong("datePeremption", 0L)
        if (peremption <= 0L) return null
        val nom = o.optString("productName", "").trim()
        val barcode = o.optString("productBarcode", "").trim()
        if (nom.isEmpty() && barcode.isEmpty()) return null
        val produit = produits.firstOrNull { barcode.isNotEmpty() && it.barcode == barcode }
            ?: produits.firstOrNull { nom.isNotEmpty() && it.name.equals(nom, ignoreCase = true) }
        return LotLisible(
            produitNom = nom.ifEmpty { produit?.name ?: barcode },
            produitBarcode = barcode,
            numeroLot = o.optString("numeroLot", "").ifBlank { null },
            quantite = o.optDouble("quantite", 0.0),
            datePeremption = peremption,
            dateReception = o.optLong("dateReception", 0L),
            unite = produit?.unit ?: ""
        )
    }

    /**
     * Reconstruit le plan d'étagères depuis ses trois tableaux à clés naturelles.
     *
     * **Les identifiants sont fabriqués ici, et c'est la seule façon de faire.** La sauvegarde n'en
     * porte aucun, par conception (sections 29 et 52) : un id local désignerait une autre étagère
     * sur un autre poste. Les entités du plan en exigent pourtant, pour se référencer entre elles.
     * On en attribue donc des locaux, valables le temps de cette lecture — exactement ce que fait
     * Android à la restauration, sauf qu'il les tient de Room.
     *
     * Le produit est résolu dans l'ordre code-barres → SKU → nom, **le même** qu'à la restauration
     * côté Android : deux ordres différents rangeraient le même article sur deux étagères selon
     * l'appareil qui l'affiche.
     */
    private fun lirePlanEtagere(
        etageres: List<JSONObject>,
        niveaux: List<JSONObject>,
        liens: List<JSONObject>,
        produits: List<Product>
    ): List<EtagereAvecNiveaux> {
        if (etageres.isEmpty()) return emptyList()

        val etageresLues = etageres.mapIndexedNotNull { index, o ->
            val nom = o.optString("nom", "").trim()
            val position = o.optInt("position", 0)
            if (nom.isEmpty()) return@mapIndexedNotNull null
            Etagere(
                id = (index + 1).toLong(),
                nom = nom,
                position = position,
                couleur = o.optString("couleur", "").ifBlank { null }
            )
        }
        val etagereParCle = etageresLues.associateBy { "${it.nom}|${it.position}" }

        val niveauxLus = niveaux.mapIndexedNotNull { index, o ->
            val cle = "${o.optString("etagereNom", "")}|${o.optInt("etagerePosition", 0)}"
            val parente = etagereParCle[cle] ?: return@mapIndexedNotNull null
            NiveauEtagere(
                id = (index + 1).toLong(),
                etagereId = parente.id,
                position = o.optInt("position", 0),
                nom = o.optString("nom", ""),
                couleur = o.optString("couleur", "").ifBlank { null }
            )
        }
        val etagereParId = etageresLues.associateBy { it.id }
        val niveauParCle = niveauxLus.mapNotNull { niveau ->
            val parente = etagereParId[niveau.etagereId] ?: return@mapNotNull null
            "${parente.nom}|${parente.position}|${niveau.position}" to niveau
        }.toMap()

        val liensLus = liens.mapIndexedNotNull { index, o ->
            val cle = "${o.optString("etagereNom", "")}|${o.optInt("etagerePosition", 0)}" +
                "|${o.optInt("niveauPosition", 0)}"
            val niveau = niveauParCle[cle] ?: return@mapIndexedNotNull null
            val barcode = o.optString("produitBarcode", "").trim()
            val sku = o.optString("produitSku", "").trim()
            val nom = o.optString("produitNom", "").trim()
            val produit = produits.firstOrNull { barcode.isNotEmpty() && it.barcode == barcode }
                ?: produits.firstOrNull { sku.isNotEmpty() && it.sku.equals(sku, ignoreCase = true) }
                ?: produits.firstOrNull { nom.isNotEmpty() && it.name.equals(nom, ignoreCase = true) }
                ?: return@mapIndexedNotNull null
            ProduitNiveau(
                id = (index + 1).toLong(),
                niveauId = niveau.id,
                produitId = produit.id,
                ordre = o.optInt("ordre", 0)
            )
        }

        val produitsParId = produits.associateBy { it.id }
        val liensParNiveau = liensLus.groupBy { it.niveauId }
        return etageresLues.sortedBy { it.position }.map { etagere ->
            EtagereAvecNiveaux(
                etagere = etagere,
                niveaux = niveauxLus.filter { it.etagereId == etagere.id }
                    .sortedBy { it.position }
                    .map { niveau ->
                        val liensDuNiveau = (liensParNiveau[niveau.id] ?: emptyList())
                            .sortedBy { it.ordre }
                        NiveauAvecProduits(
                            niveau = niveau,
                            liens = liensDuNiveau,
                            produits = liensDuNiveau.mapNotNull { produitsParId[it.produitId] }
                        )
                    }
            )
        }
    }

    /** `null` quand la clé porte `JSONObject.NULL`, une valeur absente ou une valeur non positive. */
    private fun JSONObject.optionnelLong(cle: String): Long? =
        if (isNull(cle)) null else optLong(cle, 0L).takeIf { it > 0L }

    /**
     * `null` quand la clé porte `JSONObject.NULL` ou est absente — mais **0.0 est conservé**.
     *
     * Contrairement au `Long`, zéro est ici une valeur légitime et importante : un écart de caisse
     * nul veut dire que le comptage tombe juste, et un fonds de fermeture à zéro que le tiroir a
     * été vidé. Les confondre avec « non renseigné » effacerait le seul résultat qui rassure.
     */
    private fun JSONObject.optionnelDouble(cle: String): Double? =
        if (isNull(cle)) null else optDouble(cle, 0.0)

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
