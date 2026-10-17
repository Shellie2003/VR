package com.example.ui.partage.catalogue

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Product
import com.example.util.CatalogueFiltre
import com.example.util.FormatUtil
import com.example.util.PriceUtil
import com.example.util.ShopMode

/**
 * Le catalogue de la boutique, version de bureau.
 *
 * **Ce que cet écran prouve concrètement.** Chaque ligne affichée passe par les mêmes règles que
 * le téléphone : le prix par [PriceUtil.displayPrice] (donc le mode « gros » est honoré à
 * l'identique, y compris le badge), la mise en forme des montants et des quantités par
 * [FormatUtil], le filtrage par [CatalogueFiltre] — qui épingle champ par champ les critères de la
 * requête SQL d'Android. Aucune de ces règles n'est recopiée ici ; c'est tout l'objet du découpage
 * en modules des étapes précédentes.
 *
 * **État entièrement remonté.** Recherche, catégorie et mode de boutique arrivent en paramètres
 * avec leurs rappels. Ce composable ne connaît ni base de données, ni ViewModel, ni plateforme :
 * c'est la condition pour qu'il devienne un jour l'écran des DEUX plateformes sans réécriture.
 *
 * Le filtrage est mémorisé sur ses entrées : il traverse tout le catalogue, et le recalculer à
 * chaque frappe du clavier **puis** à chaque recomposition sans rapport serait gratuit. Sur un
 * catalogue de quelques milliers de références, c'est la différence entre une liste qui suit la
 * frappe et une liste qui traîne.
 */
@Composable
fun CatalogueEcran(
    produits: List<Product>,
    recherche: String,
    onRechercheChange: (String) -> Unit,
    categorieChoisie: String,
    onCategorieChange: (String) -> Unit,
    stockFaibleSeulement: Boolean,
    onStockFaibleChange: (Boolean) -> Unit,
    modeBoutique: ShopMode,
    langue: String,
    modifier: Modifier = Modifier
) {
    val categories = remember(produits) { CatalogueFiltre.categories(produits) }
    val visibles = remember(produits, recherche, categorieChoisie, stockFaibleSeulement) {
        CatalogueFiltre.filtrer(
            produits = produits,
            recherche = recherche,
            categorie = categorieChoisie,
            stockFaibleSeulement = stockFaibleSeulement
        )
    }

    Column(modifier = modifier.fillMaxSize().padding(24.dp)) {
        Text(
            text = when (langue) {
                "mg" -> "Entana"
                "fr" -> "Catalogue des produits"
                else -> "Product catalogue"
            },
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = recherche,
            onValueChange = onRechercheChange,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text(
                    when (langue) {
                        "mg" -> "Hikaroka (anarana, sokajy, code-barres, DCI)"
                        "fr" -> "Rechercher (nom, catégorie, code-barres, DCI)"
                        else -> "Search (name, category, barcode, INN)"
                    }
                )
            }
        )

        Spacer(Modifier.height(12.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = categorieChoisie == CatalogueFiltre.TOUTES_CATEGORIES,
                    onClick = { onCategorieChange(CatalogueFiltre.TOUTES_CATEGORIES) },
                    label = {
                        Text(
                            when (langue) {
                                "mg" -> "Rehetra"
                                "fr" -> "Toutes"
                                else -> "All"
                            }
                        )
                    }
                )
            }
            items(categories, key = { it }) { categorie ->
                FilterChip(
                    selected = categorie == categorieChoisie,
                    onClick = { onCategorieChange(categorie) },
                    label = { Text(categorie) }
                )
            }
            item {
                FilterChip(
                    selected = stockFaibleSeulement,
                    onClick = { onStockFaibleChange(!stockFaibleSeulement) },
                    label = {
                        Text(
                            when (langue) {
                                "mg" -> "Tahiry ho lany"
                                "fr" -> "Stock faible"
                                else -> "Low stock"
                            }
                        )
                    }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = when (langue) {
                "mg" -> "${visibles.size} / ${produits.count { !it.isTemplate }} entana"
                "fr" -> "${visibles.size} produit(s) sur ${produits.count { !it.isTemplate }}"
                else -> "${visibles.size} of ${produits.count { !it.isTemplate }} products"
            },
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(8.dp))

        if (visibles.isEmpty()) {
            Text(
                text = when (langue) {
                    "mg" -> "Tsy misy entana mifanaraka amin'ny fikarohana."
                    "fr" -> "Aucun produit ne correspond à cette recherche."
                    else -> "No product matches this search."
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 24.dp)
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Clé stable sur l'identifiant du produit, comme sur les listes Android : sans
                // elle, Compose recycle les emplacements au lieu de suivre les éléments, et toute
                // mutation de la liste recompose des lignes qui n'ont pas changé.
                items(visibles, key = { it.id }) { produit ->
                    LigneProduit(produit = produit, modeBoutique = modeBoutique, langue = langue)
                }
            }
        }
    }
}

@Composable
private fun LigneProduit(produit: Product, modeBoutique: ShopMode, langue: String) {
    val enGros = PriceUtil.isWholesaleActive(produit, modeBoutique)
    val prix = PriceUtil.displayPrice(produit, modeBoutique)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = produit.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = buildString {
                        append(produit.category)
                        produit.dci?.takeIf { it.isNotBlank() }?.let { append(" · $it") }
                        produit.dosage?.takeIf { it.isNotBlank() }?.let { append(" $it") }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(
                modifier = Modifier.width(150.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    // `formatQty` connaît déjà les unités de l'application (kg, L, pcs, kapoaka) :
                    // c'est la même mise en forme que sur le téléphone, pas une approximation.
                    text = FormatUtil.formatQty(produit.stock, produit.unit),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (produit.isLowStock) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                if (produit.isLowStock) {
                    Text(
                        text = when (langue) {
                            "mg" -> "ho lany"
                            "fr" -> "stock faible"
                            else -> "low stock"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Column(
                modifier = Modifier.width(140.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "${FormatUtil.formatPrice(prix)} Ar",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (enGros) {
                    // Le badge « Gros » existe pour la même raison que sur le téléphone : sans lui,
                    // le gérant ne sait pas pourquoi le prix affiché n'est pas celui de la fiche.
                    Text(
                        text = when (langue) {
                            "mg" -> "ambongadiny"
                            "fr" -> "prix de gros"
                            else -> "wholesale"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}
