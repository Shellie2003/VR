package com.example.desktop

import com.example.data.model.CaisseSession
import com.example.data.model.Debt
import com.example.data.model.Etagere
import com.example.data.model.EtagereAvecNiveaux
import com.example.data.model.LotLisible
import com.example.data.model.NiveauAvecProduits
import com.example.data.model.NiveauEtagere
import com.example.data.model.ProduitNiveau
import com.example.data.model.MouvementCaisse
import com.example.data.model.Product
import com.example.data.model.Restock
import com.example.data.model.Retour
import com.example.data.model.Sale
import com.example.data.model.SoldItem

/**
 * Un petit catalogue de démonstration, et il est **volontairement dans `:desktop`** et non dans le
 * module d'interface.
 *
 * La version de bureau n'a pas encore de base de données : partager le dépôt et les DAO exigerait
 * Room KMP, c'est-à-dire toucher au fichier de base de chaque boutique en service, ce qui est
 * délibérément remis à plus tard. Il faut donc bien des données pour que l'écran montre quelque
 * chose. Les ranger ici garde `:core:ui` propre — un module d'interface qui embarque de fausses
 * données finit toujours par en livrer en production.
 *
 * Le contenu est choisi pour exercer les règles partagées, pas pour faire joli : un article en
 * stock faible (badge d'alerte), un article avec prix de gros (mode grossiste), deux médicaments
 * avec DCI et dosage (recherche par principe actif, mode pharmacie), un modèle pré-enregistré
 * (`isTemplate`) qui doit rester invisible, et des noms de casses variées pour que le tri se voie.
 */
object DonneesDemo {

    val catalogue: List<Product> = listOf(
        Product(
            id = 1,
            name = "Riz Makalioka 1 kg",
            price = 3500.0,
            category = "Alimentation",
            stock = 42.0,
            unit = "Kilogramme",
            barcode = "6001234500011",
            wholesalePrice = 3100.0,
            prixAchatUniteBase = 2600.0
        ),
        Product(
            id = 2,
            name = "huile de tournesol 1 L",
            price = 9800.0,
            category = "Alimentation",
            stock = 3.0,
            lowStockThreshold = 6.0,
            unit = "Litre",
            barcode = "6001234500028",
            prixAchatUniteBase = 7900.0
        ),
        Product(
            id = 3,
            name = "Eau minérale 1,5 L",
            price = 2000.0,
            category = "Boisson",
            stock = 120.0,
            unit = "Pièce",
            barcode = "6001234500035",
            wholesalePrice = 1700.0,
            prixAchatUniteBase = 1450.0
        ),
        Product(
            id = 4,
            name = "Savon de Marseille",
            price = 1500.0,
            category = "Droguerie",
            stock = 18.0,
            unit = "Pièce",
            barcode = "6001234500042",
            prixAchatUniteBase = 1050.0
        ),
        Product(
            id = 5,
            name = "Doliprane 500 mg",
            price = 4500.0,
            category = "Pharmacie",
            stock = 25.0,
            unit = "Plaquette",
            barcode = "3400930000011",
            dci = "paracétamol",
            dosage = "500 mg",
            formeGalenique = "Comprimé"
        ),
        Product(
            id = 6,
            name = "Efferalgan 1 g",
            price = 7200.0,
            category = "Pharmacie",
            stock = 4.0,
            lowStockThreshold = 10.0,
            unit = "Plaquette",
            barcode = "3400930000028",
            dci = "paracétamol",
            dosage = "1 g",
            formeGalenique = "Comprimé effervescent",
            surOrdonnance = false
        ),
        Product(
            id = 7,
            name = "Zébu séché 500 g",
            price = 18000.0,
            category = "Alimentation",
            stock = 7.0,
            unit = "Paquet",
            barcode = "6001234500059",
            prixAchatUniteBase = 14000.0
        ),
        // Volontairement SANS coût d'achat : c'est le cas documenté en section 94 — sa vente
        // apparaît comme du bénéfice pur, ce qui gonfle la marge affichée. Le voir dans la
        // démonstration vaut mieux que de le découvrir en production.
        Product(
            id = 8,
            name = "ananas frais",
            price = 2500.0,
            category = "Légumes",
            stock = 15.0,
            unit = "Pièce"
        ),
        // Ce modèle pré-enregistré ne doit JAMAIS apparaître dans la liste : c'est exactement ce
        // que `CatalogueFiltre` écarte, et le voir à l'écran signalerait une régression.
        Product(
            id = 9,
            name = "MODÈLE — Boisson gazeuse 33 cl",
            price = 0.0,
            category = "Boisson",
            stock = 0.0,
            isTemplate = true
        )
    )

    /**
     * Quelques trosa de démonstration, choisis pour exercer les règles plutôt que pour faire joli :
     * un débiteur à **plusieurs** dettes (la carte de groupe et le règlement groupé de la
     * section 51), un débiteur à une seule dette (la carte simple, qui doit rester inchangée), une
     * dette déjà **partiellement** remboursée (la ligne « Initiale : »), une dette **soldée** et
     * une dette **en retard**.
     *
     * Les dates sont relatives à l'instant de lancement : des horodatages figés afficheraient
     * « 1970 » ou des dates anciennes qui n'exerceraient jamais le badge de retard.
     */
    val dettes: List<Debt> = run {
        val jour = 24L * 60 * 60 * 1000
        val maintenant = System.currentTimeMillis()
        listOf(
            // Trois trosa pour la même personne : c'est le cas qui a motivé le regroupement.
            Debt(
                id = 1,
                debtorName = "Rasoa",
                amount = 2300.0,
                balance = 2300.0,
                date = maintenant - 9 * jour,
                note = "Riz Makalioka 1 kg (2 x 3 500), Savon de Marseille (1 x 1 500)"
            ),
            Debt(
                id = 2,
                debtorName = "rasoa",
                amount = 1100.0,
                balance = 1100.0,
                date = maintenant - 4 * jour,
                note = "Eau minérale 1,5 L (2 x 2 000)",
                // Échéance dépassée : exerce le badge « EN RETARD » sur la carte ET sur le groupe.
                dueDate = maintenant - jour
            ),
            Debt(
                id = 3,
                debtorName = "RASOA",
                amount = 5000.0,
                balance = 1200.0,
                date = maintenant - 2 * jour,
                note = "Lait concentré, sucré (1 x 4 000), Pain (2 x 500)"
            ),
            // Un seul trosa : la carte simple, qui doit rester identique à Android.
            Debt(
                id = 4,
                debtorName = "Koto Jean",
                amount = 7500.0,
                balance = 7500.0,
                date = maintenant - jour,
                note = "Huile de tournesol 1 L (1 x 9 800)",
                dueDate = maintenant + 6 * jour
            ),
            Debt(
                id = 5,
                debtorName = "Naivo",
                amount = 3000.0,
                balance = 0.0,
                date = maintenant - 15 * jour,
                note = "Ananas frais (2 x 2 500)",
                isPaid = true
            )
        )
    }

    /**
     * Un historique de ventes de démonstration, pour que l'écran Rapports ait de quoi calculer.
     *
     * **Les horodatages sont relatifs au lancement**, comme les dettes : une période « 7 jours »
     * calculée sur des dates figées ne contiendrait jamais rien, et l'écran paraîtrait cassé alors
     * qu'il appliquerait correctement son filtre.
     *
     * Le contenu exerce les cas que les cartes de synthèse affichent conditionnellement : des
     * articles **taxables** (la carte TVA), une marge réelle (coût d'achat renseigné sur une partie
     * seulement des produits, donc le zéro de coût documenté en section 94 est visible), des jours
     * **sans vente** (les barres atténuées du graphique) et un **retour** (la carte de retours et la
     * déduction du chiffre d'affaires).
     */
    val ventes: List<Sale> = run {
        val jour = 24L * 60 * 60 * 1000
        val maintenant = System.currentTimeMillis()
        fun article(produitId: Int, nom: String, prix: Double, quantite: Double, taxable: Boolean = false) =
            SoldItem(
                productId = produitId,
                name = nom,
                quantity = quantite,
                price = prix,
                taxable = taxable,
                tauxTaxe = if (taxable) 20.0 else 0.0
            )
        fun vente(id: Int, decalageJours: Long, vararg articles: SoldItem) = Sale(
            id = id,
            timestamp = maintenant - decalageJours * jour,
            items = articles.toList(),
            totalAmount = articles.sumOf { it.price * it.quantity }
        )
        listOf(
            vente(1, 0, article(1, "Riz Makalioka 1 kg", 3500.0, 4.0), article(3, "Eau minérale 1,5 L", 2000.0, 6.0)),
            vente(2, 0, article(2, "Huile de tournesol 1 L", 9800.0, 1.0, taxable = true)),
            vente(3, 1, article(1, "Riz Makalioka 1 kg", 3500.0, 10.0)),
            vente(4, 1, article(4, "Savon de Marseille", 1500.0, 3.0, taxable = true)),
            // Rien le jour -2 : exerce la barre atténuée du graphique.
            vente(5, 3, article(7, "Zébu séché 500 g", 18000.0, 2.0)),
            vente(6, 4, article(3, "Eau minérale 1,5 L", 2000.0, 12.0)),
            vente(7, 6, article(8, "ananas frais", 2500.0, 5.0)),
            vente(8, 12, article(1, "Riz Makalioka 1 kg", 3500.0, 20.0))
        )
    }

    /**
     * Un retour, pour que la carte « Retours/remboursements » et la déduction du chiffre d'affaires
     * soient réellement exercées plutôt que toujours masquées.
     */
    val retours: List<Retour> = run {
        val jour = 24L * 60 * 60 * 1000
        listOf(
            Retour(
                id = 1,
                saleId = 3,
                timestamp = System.currentTimeMillis() - jour,
                items = listOf(
                    SoldItem(productId = 1, name = "Riz Makalioka 1 kg", quantity = 2.0, price = 3500.0)
                ),
                totalAmount = 7000.0,
                motif = "Sac percé"
            )
        )
    }

    /**
     * Des réapprovisionnements, choisis pour exercer ce que la carte affiche conditionnellement
     * plutôt que pour faire joli : un fournisseur renseigné et un autre absent (deux libellés
     * différents au dépliage), et surtout **une livraison à zéro unité** — c'est le cas qui faisait
     * afficher « Ar ∞ » sur le téléphone, et qui doit donc se voir dans la démonstration.
     */
    val reappros: List<Restock> = run {
        val jour = 24L * 60 * 60 * 1000
        val maintenant = System.currentTimeMillis()
        listOf(
            Restock(
                id = 1,
                productId = 1,
                productName = "Riz Makalioka 1 kg",
                cartonsQuantity = 4.0,
                itemsPerCarton = 25.0,
                totalUnits = 100.0,
                totalCostPrice = 260000.0,
                unitSellingPrice = 3500.0,
                supplierId = 1L,
                supplierName = "Grossiste Analakely",
                timestamp = maintenant - 2 * 60 * 60 * 1000,
                deviceName = "Caisse 1",
                vendeurNom = "Koto"
            ),
            Restock(
                id = 2,
                productId = 3,
                productName = "Eau minérale 1,5 L",
                cartonsQuantity = 10.0,
                itemsPerCarton = 6.0,
                totalUnits = 60.0,
                totalCostPrice = 84000.0,
                unitSellingPrice = 2000.0,
                supplierId = null,
                supplierName = null,
                timestamp = maintenant - 5 * 60 * 60 * 1000,
                deviceName = "Caisse 1",
                vendeurNom = ""
            ),
            Restock(
                id = 3,
                productId = 8,
                productName = "Ananas frais",
                cartonsQuantity = 1.0,
                itemsPerCarton = 0.0,
                // Zéro unité : la division nue donnait « Ar ∞ », `coutUnitaireReappro` rend « — ».
                totalUnits = 0.0,
                totalCostPrice = 15000.0,
                unitSellingPrice = 2500.0,
                supplierId = null,
                supplierName = "Marché d'Anosibe",
                timestamp = maintenant - jour,
                deviceName = "Téléphone de Rasoa",
                vendeurNom = "Rasoa"
            )
        )
    }

    /** Des entrées et des sorties d'espèces, avec et sans note, de deux postes différents. */
    val mouvementsCaisse: List<MouvementCaisse> = run {
        val heure = 60L * 60 * 1000
        val maintenant = System.currentTimeMillis()
        listOf(
            MouvementCaisse(
                id = 1,
                type = "ENTREE",
                montant = 50000.0,
                motif = "Fond de caisse",
                date = maintenant - 8 * heure,
                deviceName = "Caisse 1",
                vendeurNom = "Koto"
            ),
            MouvementCaisse(
                id = 2,
                type = "SORTIE",
                montant = 12000.0,
                motif = "Achat sacs plastique",
                note = "Payé au marché",
                date = maintenant - 5 * heure,
                deviceName = "Caisse 1",
                vendeurNom = "Koto"
            ),
            MouvementCaisse(
                id = 3,
                type = "SORTIE",
                montant = 30000.0,
                motif = "Retrait du gérant",
                date = maintenant - 2 * heure,
                deviceName = "Téléphone de Rasoa",
                vendeurNom = "Rasoa"
            )
        )
    }

    /**
     * Une session **ouverte** et une session **fermée en manquant**, pour que les deux branches de
     * la carte de session soient exercées : l'état courant et le récapitulatif d'écart. L'écart est
     * négatif, donc rouge — c'est celui qui compte pour le gérant.
     */
    val sessionsCaisse: List<CaisseSession> = run {
        val jour = 24L * 60 * 60 * 1000
        val maintenant = System.currentTimeMillis()
        listOf(
            CaisseSession(
                id = 1,
                dateOuverture = maintenant - 2 * jour,
                montantOuverture = 50000.0,
                dateFermeture = maintenant - 2 * jour + 10 * 60 * 60 * 1000,
                montantCompteFermeture = 243000.0,
                montantTheoriqueFermeture = 248500.0,
                ecart = -5500.0,
                note = "Manquant constaté au comptage"
            ),
            CaisseSession(
                id = 2,
                dateOuverture = maintenant - 9 * 60 * 60 * 1000,
                montantOuverture = 50000.0
            )
        )
    }

    /**
     * Des lots **dans les trois états** : un périmé, un qui périme dans la fenêtre d'alerte, et un
     * encore bon. Sans les trois, deux des trois couleurs de l'écran ne seraient jamais rendues —
     * et c'est précisément ce que `EcransBureauRenduTest` doit pouvoir traverser.
     */
    val lots: List<LotLisible> = run {
        val jour = 24L * 60 * 60 * 1000
        val maintenant = System.currentTimeMillis()
        listOf(
            LotLisible(
                produitNom = "Doliprane 500 mg",
                produitBarcode = "3400930000001",
                numeroLot = "L-2403",
                quantite = 12.0,
                datePeremption = maintenant - 3 * jour,
                dateReception = maintenant - 90 * jour,
                unite = "Plaquette"
            ),
            LotLisible(
                produitNom = "Efferalgan 1 g",
                produitBarcode = "3400930000002",
                numeroLot = "L-2411",
                quantite = 30.0,
                datePeremption = maintenant + 4 * jour,
                dateReception = maintenant - 30 * jour,
                unite = "Plaquette"
            ),
            LotLisible(
                produitNom = "Zébu séché 500 g",
                numeroLot = null,
                quantite = 8.0,
                datePeremption = maintenant + 60 * jour,
                dateReception = maintenant - 5 * jour,
                unite = "Paquet"
            )
        )
    }

    /**
     * Un plan de deux étagères, dont une **case vide** et une étagère colorée : les deux branches
     * d'affichage de la case (remplie / vide) et les deux de la teinte (couleur choisie / couleur
     * du thème) sont ainsi exercées.
     *
     * Les identifiants sont posés à la main ici, comme le lecteur de sauvegarde les fabrique :
     * un plan n'en porte aucun dans la sauvegarde, ce sont des clés naturelles (section 52).
     */
    val planEtagere: List<EtagereAvecNiveaux> = run {
        val riz = catalogue.first { it.id == 1 }
        val huile = catalogue.first { it.id == 2 }
        val eau = catalogue.first { it.id == 3 }
        val savon = catalogue.first { it.id == 4 }
        listOf(
            EtagereAvecNiveaux(
                etagere = Etagere(id = 1, nom = "Rayon A", position = 0, couleur = "#2E7D32"),
                niveaux = listOf(
                    NiveauAvecProduits(
                        niveau = NiveauEtagere(id = 1, etagereId = 1, position = 0, nom = "Haut"),
                        liens = listOf(ProduitNiveau(id = 1, niveauId = 1, produitId = riz.id)),
                        produits = listOf(riz)
                    ),
                    NiveauAvecProduits(
                        niveau = NiveauEtagere(id = 2, etagereId = 1, position = 1, nom = "Milieu"),
                        liens = listOf(
                            ProduitNiveau(id = 2, niveauId = 2, produitId = huile.id),
                            ProduitNiveau(id = 3, niveauId = 2, produitId = eau.id)
                        ),
                        produits = listOf(huile, eau)
                    ),
                    NiveauAvecProduits(
                        niveau = NiveauEtagere(id = 3, etagereId = 1, position = 2, nom = "Bas"),
                        liens = emptyList(),
                        produits = emptyList()
                    )
                )
            ),
            EtagereAvecNiveaux(
                etagere = Etagere(id = 2, nom = "Droguerie", position = 1),
                niveaux = listOf(
                    NiveauAvecProduits(
                        niveau = NiveauEtagere(id = 4, etagereId = 2, position = 0),
                        liens = listOf(ProduitNiveau(id = 4, niveauId = 4, produitId = savon.id)),
                        produits = listOf(savon)
                    )
                )
            )
        )
    }
}
