package com.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.data.model.CartItem
import com.example.data.model.Debt
import com.example.data.model.Product
import com.example.data.model.Retour
import com.example.data.model.Sale
import com.example.data.model.SoldItem
import com.example.ui.partage.accueil.AccueilEcran
import com.example.ui.partage.caisse.CaisseEcran
import com.example.ui.partage.dettes.DettesEcran
import com.example.ui.partage.rapports.RapportsEcran
import com.example.ui.partage.stock.StockEcran
import com.example.ui.partage.theme.VarotraTheme
import com.example.util.ShopMode
import org.junit.Rule
import org.junit.Test

/**
 * Le rendu réel des cinq écrans de bureau, **avec données et SURTOUT à vide**.
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
                    onAjusterStock = { _, _ -> }
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
}
