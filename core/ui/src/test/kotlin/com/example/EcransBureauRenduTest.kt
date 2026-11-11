package com.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.data.model.CaisseSession
import com.example.data.model.CartItem
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
import com.example.ui.partage.accueil.AccueilEcran
import com.example.ui.partage.caisse.CaisseEcran
import com.example.ui.partage.caisse.MouvementsCaisseEcran
import com.example.ui.partage.dettes.DettesEcran
import com.example.ui.partage.etagere.EtagereEcran
import com.example.ui.partage.historique.HistoriqueEcran
import com.example.ui.partage.historique.OngletHistorique
import com.example.ui.partage.marge.CalculateurMargeEcran
import com.example.ui.partage.peremption.PeremptionEcran
import com.example.ui.partage.rapports.RapportsEcran
import com.example.ui.partage.stock.StockEcran
import com.example.ui.partage.theme.VarotraTheme
import com.example.util.CalculMarge
import com.example.util.ShopMode
import org.junit.Rule
import org.junit.Test

/**
 * Le rendu réel des dix écrans de bureau, **avec données et SURTOUT à vide**.
 *
 * **Pourquoi ces tests existent.** Jusqu'ici la CI ne faisait que *compiler* ces écrans. Or les
 * défauts qui restent après une compilation réussie sont précisément ceux qui plantent à
 * l'exécution sur un cas limite : un `maxOf` sur une liste vide, une division par zéro dans le
 * graphique, un `first()` sur un groupe sans élément. Et le cas limite le plus certain de tous,
 * c'est **le premier lancement chez un nouveau client** : aucun produit, aucune vente, aucune
 * dette. Un écran qui plante là est un écran qui plante au tout premier contact.
 *
 * Chaque écran est rendu en clair **et** en sombre : un `Color` mal dérivé ou un token absent ne se
 * voit pas à la compilation. Le test ne juge pas l'apparence — il établit que la composition
 * aboutit sans lever, ce qu'aucune autre vérification de ce dépôt ne fait pour le bureau.
 *
 * **Pas de `createAndroidComposeRule` ici, contrairement aux tests d'écran d'Android** (section 61) :
 * il n'y a pas d'Activity sur un bureau. `createComposeRule` de Compose Desktop monte une scène
 * Skia hors écran, et le `Box` de taille explicite lui donne des bornes concrètes — sans quoi un
 * écran en `fillMaxSize()` se compose dans une fenêtre de dimensions non garanties.
 */
class EcransBureauRenduTest {

    @get:Rule
    val compose = createComposeRule()

    // ---- Jeux de données minimaux, et leur contrepartie vide ----

    private val produits = listOf(
        Product(
            id = 1,
            name = "Riz Makalioka 1 kg",
            price = 3500.0,
            category = "Alimentation",
            stock = 42.0,
            unit = "Kilogramme",
            wholesalePrice = 3100.0,
            prixAchatUniteBase = 2600.0
        ),
        Product(
            id = 2,
            name = "Huile 1 L",
            price = 9800.0,
            category = "Alimentation",
            // Sous le seuil : exerce la carte en couleurs d'alerte de l'écran Stock.
            stock = 2.0,
            lowStockThreshold = 6.0,
            unit = "Litre"
        )
    )

    private val panier = listOf(
        CartItem(
            id = "product_1",
            name = "Riz Makalioka 1 kg",
            price = 3500.0,
            quantity = 2.0,
            unit = "Kilogramme",
            productId = 1,
            maxStock = 42.0
        )
    )

    private val dettes = listOf(
        // Deux dettes du MÊME débiteur : exerce la carte de groupe, pas seulement la carte simple.
        Debt(id = 1, debtorName = "Rasoa", amount = 2300.0, balance = 2300.0, date = 1_000L, note = "Riz (2 x 3 500)"),
        Debt(id = 2, debtorName = "rasoa", amount = 1100.0, balance = 1100.0, date = 2_000L, note = "", dueDate = 1L),
        Debt(id = 3, debtorName = "Koto", amount = 500.0, balance = 0.0, date = 3_000L, note = "", isPaid = true)
    )

    private val ventes = listOf(
        Sale(
            id = 1,
            timestamp = System.currentTimeMillis(),
            items = listOf(
                SoldItem(productId = 1, name = "Riz Makalioka 1 kg", quantity = 2.0, price = 3500.0, taxable = true, tauxTaxe = 20.0)
            ),
            totalAmount = 7000.0
        )
    )

    private val retours = listOf(
        Retour(
            id = 1,
            saleId = 1,
            timestamp = System.currentTimeMillis(),
            items = listOf(SoldItem(productId = 1, name = "Riz Makalioka 1 kg", quantity = 1.0, price = 3500.0)),
            totalAmount = 3500.0
        )
    )

    /** Monte un écran dans une scène de dimensions concrètes, puis attend la fin de la composition. */
    private fun rendre(sombre: Boolean, contenu: @androidx.compose.runtime.Composable () -> Unit) {
        compose.setContent {
            VarotraTheme(sombre = sombre) {
                Box(modifier = Modifier.size(1280.dp, 860.dp)) { contenu() }
            }
        }
        compose.waitForIdle()
    }

    // ---- Accueil ----

    @Test
    fun `l'accueil se rend en clair et en sombre`() {
        listOf(false, true).forEach { sombre ->
            rendre(sombre) {
                AccueilEcran(
                    produits = produits,
                    nomEpicerie = "Varotra",
                    recherche = "",
                    onRechercheChange = {},
                    quantitesPanier = mapOf(1 to 2.0),
                    onAjouter = {},
                    onRetirer = {},
                    modeBoutique = ShopMode.DETAIL,
                    langue = "fr",
                    couleurTheme = androidx.compose.ui.graphics.Color(0xFF012D1D)
                )
            }
        }
    }

    @Test
    fun `l'accueil se rend sans aucun produit`() {
        // Le tout premier lancement chez un nouveau client.
        rendre(sombre = false) {
            AccueilEcran(
                produits = emptyList(),
                nomEpicerie = "Varotra",
                recherche = "",
                onRechercheChange = {},
                quantitesPanier = emptyMap(),
                onAjouter = {},
                onRetirer = {},
                modeBoutique = ShopMode.DETAIL,
                langue = "mg",
                couleurTheme = androidx.compose.ui.graphics.Color(0xFF012D1D)
            )
        }
    }

    // ---- Stock ----

    @Test
    fun `le stock se rend en clair et en sombre`() {
        listOf(false, true).forEach { sombre ->
            rendre(sombre) {
                StockEcran(
                    produits = produits,
                    recherche = "",
                    onRechercheChange = {},
                    categorieChoisie = "All",
                    onCategorieChange = {},
                    stockFaibleSeulement = false,
                    onStockFaibleChange = {},
                    modeBoutique = ShopMode.GROSSISTE,
                    langue = "fr",
                    onAjusterStock = { _, _ -> },
                    // Avec un `onExporter` non nul, le bouton d'export est réellement composé —
                    // sinon ce chemin ne serait jamais rendu par aucun test.
                    onExporter = {}
                )
            }
        }
    }

    @Test
    fun `le stock se rend sans aucun produit et avec un filtre sans resultat`() {
        rendre(sombre = true) {
            StockEcran(
                produits = emptyList(),
                recherche = "introuvable",
                onRechercheChange = {},
                categorieChoisie = "All",
                onCategorieChange = {},
                stockFaibleSeulement = true,
                onStockFaibleChange = {},
                modeBoutique = ShopMode.DETAIL,
                langue = "en",
                onAjusterStock = { _, _ -> }
            )
        }
    }

    // ---- Caisse ----

    @Test
    fun `la caisse se rend en clair et en sombre, panier rempli`() {
        listOf(false, true).forEach { sombre ->
            rendre(sombre) {
                CaisseEcran(
                    produits = produits,
                    panier = panier,
                    recherche = "",
                    onRechercheChange = {},
                    onAjouterProduit = { _, _ -> },
                    onAjouterDivers = {},
                    onQuantiteDelta = { _, _ -> },
                    onFixerQuantite = { _, _ -> },
                    onViderPanier = {},
                    modeBoutique = ShopMode.DETAIL,
                    langue = "fr",
                    couleurTheme = androidx.compose.ui.graphics.Color(0xFF012D1D),
                    messageValidationImpossible = "Enregistrement indisponible"
                )
            }
        }
    }

    @Test
    fun `la caisse se rend panier VIDE et catalogue vide`() {
        // Le total vaut 0, la monnaie rendue n'a rien à calculer, et le bouton est désactivé :
        // trois chemins que le panier rempli ne traverse pas.
        rendre(sombre = false) {
            CaisseEcran(
                produits = emptyList(),
                panier = emptyList(),
                recherche = "",
                onRechercheChange = {},
                onAjouterProduit = { _, _ -> },
                onAjouterDivers = {},
                onQuantiteDelta = { _, _ -> },
                onFixerQuantite = { _, _ -> },
                onViderPanier = {},
                modeBoutique = ShopMode.DETAIL,
                langue = "mg",
                couleurTheme = androidx.compose.ui.graphics.Color(0xFF012D1D)
            )
        }
    }

    // ---- Dettes ----

    @Test
    fun `les dettes se rendent en clair et en sombre, groupe compris`() {
        listOf(false, true).forEach { sombre ->
            rendre(sombre) {
                DettesEcran(
                    dettes = dettes,
                    recherche = "",
                    onRechercheChange = {},
                    dateLisible = { "01/01/2026" },
                    echeanceLisible = { dette -> dette.dueDate?.let { "02/01/2026" } },
                    onRembourser = {},
                    onReglerGroupe = { _, _ -> },
                    onExporter = {},
                    langue = "fr"
                )
            }
        }
    }

    @Test
    fun `les dettes se rendent sans aucune dette`() {
        rendre(sombre = true) {
            DettesEcran(
                dettes = emptyList(),
                recherche = "",
                onRechercheChange = {},
                dateLisible = { "" },
                echeanceLisible = { null },
                onRembourser = {},
                onReglerGroupe = { _, _ -> },
                langue = "en"
            )
        }
    }

    // ---- Rapports ----

    @Test
    fun `les rapports se rendent en clair et en sombre, retours et TVA compris`() {
        listOf(false, true).forEach { sombre ->
            rendre(sombre) {
                RapportsEcran(
                    ventes = ventes,
                    retours = retours,
                    produits = produits,
                    langue = "fr",
                    couleurTheme = androidx.compose.ui.graphics.Color(0xFF012D1D)
                )
            }
        }
    }

    @Test
    fun `les rapports se rendent sans aucune vente`() {
        // LE cas qui justifie ce fichier : `maxOf` sur un classement vide et la division par le
        // maximum des barres sont tous deux des levées potentielles, et c'est l'état du tableau de
        // bord au premier lancement de chaque boutique.
        rendre(sombre = false) {
            RapportsEcran(
                ventes = emptyList(),
                retours = emptyList(),
                produits = emptyList(),
                langue = "mg",
                couleurTheme = androidx.compose.ui.graphics.Color(0xFF012D1D)
            )
        }
    }

    // ---- Historique ----

    private val reappros = listOf(
        Restock(
            id = 1,
            productId = 1,
            productName = "Riz Makalioka 1 kg",
            cartonsQuantity = 2.0,
            itemsPerCarton = 25.0,
            totalUnits = 50.0,
            totalCostPrice = 125000.0,
            unitSellingPrice = 3500.0,
            supplierId = 1L,
            supplierName = "Grossiste Analakely",
            timestamp = System.currentTimeMillis()
        ),
        Restock(
            id = 2,
            productId = 2,
            productName = "Ananas",
            cartonsQuantity = 1.0,
            itemsPerCarton = 0.0,
            // ZÉRO unité : le cas qui faisait afficher « Ar ∞ » sur le téléphone. Il doit être
            // traversé par le rendu, pas seulement par le test de la fonction pure.
            totalUnits = 0.0,
            totalCostPrice = 15000.0,
            unitSellingPrice = 2500.0,
            supplierId = null,
            supplierName = null,
            timestamp = System.currentTimeMillis()
        )
    )

    @Test
    fun `l'historique se rend en clair et en sombre, ventes et reappros`() {
        // Deux rendus et non quatre : un thème par onglet suffit à traverser les deux chemins,
        // et on reste sur le nombre d'appels à `setContent` que les tests voisins pratiquent déjà.
        listOf(false to OngletHistorique.VENTES, true to OngletHistorique.REAPPROS)
            .forEach { (sombre, onglet) ->
                rendre(sombre) {
                    HistoriqueEcran(
                        ventes = ventes,
                        reappros = reappros,
                        retours = retours,
                        produits = produits,
                        onglet = onglet,
                        onOngletChange = {},
                        jourAffiche = System.currentTimeMillis(),
                        onJourChange = {},
                        recherche = "",
                        onRechercheChange = {},
                        dateLisible = { "15/01/2026" },
                        heureLisible = { "14:30" },
                        estAujourdhui = true,
                        onExporter = {},
                        langue = "fr",
                        themeColor = androidx.compose.ui.graphics.Color(0xFF012D1D)
                    )
                }
            }
    }

    @Test
    fun `l'historique se rend sans aucune transaction et avec une recherche sans resultat`() {
        // Le premier lancement, et le jour où le gérant remonte à une date sans vente : la carte
        // de variation ne doit pas annoncer une progression depuis zéro.
        rendre(sombre = true) {
            HistoriqueEcran(
                ventes = emptyList(),
                reappros = emptyList(),
                retours = emptyList(),
                produits = emptyList(),
                onglet = OngletHistorique.VENTES,
                onOngletChange = {},
                jourAffiche = System.currentTimeMillis(),
                onJourChange = {},
                recherche = "introuvable",
                onRechercheChange = {},
                dateLisible = { "15/01/2026" },
                heureLisible = { "14:30" },
                estAujourdhui = false,
                langue = "mg",
                themeColor = androidx.compose.ui.graphics.Color(0xFF012D1D)
            )
        }
    }

    // ---- Mouvements de caisse ----

    private val mouvements = listOf(
        MouvementCaisse(id = 1, type = "ENTREE", montant = 50000.0, motif = "Fond de caisse"),
        MouvementCaisse(
            id = 2,
            type = "SORTIE",
            montant = 12000.0,
            motif = "Sacs",
            note = "Payé au marché",
            deviceName = "Caisse 1",
            vendeurNom = "Koto"
        )
    )

    private val sessions = listOf(
        // Une fermée AVEC écart et une ouverte : les deux branches de la carte de session.
        CaisseSession(
            id = 1,
            dateOuverture = 1_000L,
            montantOuverture = 50000.0,
            dateFermeture = 2_000L,
            montantCompteFermeture = 243000.0,
            montantTheoriqueFermeture = 248500.0,
            ecart = -5500.0
        ),
        CaisseSession(id = 2, dateOuverture = 3_000L, montantOuverture = 30000.0)
    )

    @Test
    fun `les mouvements de caisse se rendent en clair et en sombre`() {
        listOf(false, true).forEach { sombre ->
            rendre(sombre) {
                MouvementsCaisseEcran(
                    mouvements = mouvements,
                    sessions = sessions,
                    dateHeureLisible = { "15/01/2026 14:30" },
                    onExporter = {},
                    langue = "fr",
                    themeColor = androidx.compose.ui.graphics.Color(0xFF012D1D)
                )
            }
        }
    }

    @Test
    fun `les mouvements de caisse se rendent sans mouvement ni session`() {
        rendre(sombre = false) {
            MouvementsCaisseEcran(
                mouvements = emptyList(),
                sessions = emptyList(),
                dateHeureLisible = { "15/01/2026 14:30" },
                langue = "en",
                themeColor = androidx.compose.ui.graphics.Color(0xFF012D1D)
            )
        }
    }

    // ---- Péremption ----

    private val lots = run {
        val jour = 24L * 60 * 60 * 1000
        val maintenant = System.currentTimeMillis()
        listOf(
            // Les TROIS états : sans eux, deux des trois couleurs ne seraient jamais rendues.
            LotLisible(
                produitNom = "Doliprane",
                numeroLot = "L-1",
                quantite = 12.0,
                datePeremption = maintenant - 3 * jour,
                unite = "Plaquette"
            ),
            LotLisible(
                produitNom = "Efferalgan",
                numeroLot = null,
                quantite = 30.0,
                datePeremption = maintenant + 2 * jour
            ),
            LotLisible(
                produitNom = "Zébu séché",
                numeroLot = "L-9",
                quantite = 8.0,
                datePeremption = maintenant + 90 * jour,
                unite = "Paquet"
            )
        )
    }

    @Test
    fun `la peremption se rend en clair et en sombre, les trois etats presents`() {
        listOf(false, true).forEach { sombre ->
            rendre(sombre) {
                PeremptionEcran(
                    lots = lots,
                    dateLisible = { "15/01/2026" },
                    langue = "fr",
                    themeColor = androidx.compose.ui.graphics.Color(0xFF012D1D)
                )
            }
        }
    }

    @Test
    fun `la peremption se rend sans aucun lot`() {
        // La carte de synthèse passe alors du rouge à la couleur du thème : les deux branches du
        // fond sont donc traversées par ce test et le précédent.
        rendre(sombre = true) {
            PeremptionEcran(
                lots = emptyList(),
                dateLisible = { "15/01/2026" },
                langue = "mg",
                themeColor = androidx.compose.ui.graphics.Color(0xFF012D1D)
            )
        }
    }

    // ---- Plan d'étagères ----

    private val plan = listOf(
        EtagereAvecNiveaux(
            etagere = Etagere(id = 1, nom = "Rayon A", position = 0, couleur = "#2E7D32"),
            niveaux = listOf(
                NiveauAvecProduits(
                    niveau = NiveauEtagere(id = 1, etagereId = 1, position = 0, nom = "Haut"),
                    liens = listOf(ProduitNiveau(id = 1, niveauId = 1, produitId = 1)),
                    produits = listOf(produits[0])
                ),
                // Une case VIDE et un niveau SANS NOM : deux branches d'affichage distinctes.
                NiveauAvecProduits(
                    niveau = NiveauEtagere(id = 2, etagereId = 1, position = 1),
                    liens = emptyList(),
                    produits = emptyList()
                )
            )
        ),
        // Une étagère SANS couleur : exerce le repli sur la couleur du thème.
        EtagereAvecNiveaux(
            etagere = Etagere(id = 2, nom = "Rayon B", position = 1),
            niveaux = emptyList()
        )
    )

    @Test
    fun `le plan d'etageres se rend en clair et en sombre`() {
        listOf(false, true).forEach { sombre ->
            rendre(sombre) {
                EtagereEcran(plan = plan, langue = "fr")
            }
        }
    }

    @Test
    fun `le plan d'etageres se rend sans aucune etagere`() {
        rendre(sombre = false) {
            EtagereEcran(plan = emptyList(), langue = "en")
        }
    }

    // ---- Calculateur de marge ----

    @Test
    fun `le calculateur de marge se rend en clair et en sombre`() {
        val resultat = CalculMarge.calculer(10.0, 24.0, 240_000.0, 15_000.0)
        listOf(false, true).forEach { sombre ->
            rendre(sombre) {
                CalculateurMargeEcran(
                    cartons = "10",
                    onCartonsChange = {},
                    unitesParCarton = "24",
                    onUnitesParCartonChange = {},
                    coutTotalAchat = "240000",
                    onCoutTotalAchatChange = {},
                    prixVenteUnitaire = "15000",
                    onPrixVenteUnitaireChange = {},
                    resultat = resultat,
                    langue = "fr",
                    themeColor = androidx.compose.ui.graphics.Color(0xFF012D1D),
                    unite = "Sac"
                )
            }
        }
    }

    @Test
    fun `le calculateur de marge se rend a vide et sur une perte`() {
        // Le formulaire vide est l'état d'ouverture : tout y est à zéro et le badge est muet.
        rendre(sombre = true) {
            CalculateurMargeEcran(
                cartons = "",
                onCartonsChange = {},
                unitesParCarton = "",
                onUnitesParCartonChange = {},
                coutTotalAchat = "",
                onCoutTotalAchatChange = {},
                prixVenteUnitaire = "",
                onPrixVenteUnitaireChange = {},
                resultat = CalculMarge.calculer(0.0, 0.0, 0.0, 0.0),
                langue = "mg",
                themeColor = androidx.compose.ui.graphics.Color(0xFF012D1D)
            )
        }
        // La perte emprunte l'autre branche de couleur de CHAQUE ligne de bénéfice : sans ce
        // rendu, le chemin rouge du panneau ne serait jamais composé par aucun test.
        rendre(sombre = false) {
            CalculateurMargeEcran(
                cartons = "10",
                onCartonsChange = {},
                unitesParCarton = "24",
                onUnitesParCartonChange = {},
                coutTotalAchat = "240000",
                onCoutTotalAchatChange = {},
                prixVenteUnitaire = "900",
                onPrixVenteUnitaireChange = {},
                resultat = CalculMarge.calculer(10.0, 24.0, 240_000.0, 900.0),
                langue = "en",
                themeColor = androidx.compose.ui.graphics.Color(0xFF012D1D)
            )
        }
    }
}
