package com.example.ui.partage.stock

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Product
import com.example.util.CatalogueFiltre
import com.example.util.ShopMode

/**
 * L'écran Stock, **porté depuis `InventoryListScreen`** : même champ de recherche arrondi à 16dp,
 * mêmes puces de catégorie arrondies à 12dp, même ligne « nombre de produits » face à la puce
 * « Mihavitsy tahiry » en couleurs d'erreur, mêmes cartes.
 *
 * **L'adaptation PC suit exactement le raisonnement de l'accueil.** Android passe en grille de
 * 2 colonnes à partir de 600dp (12dp d'écart) ; la carte y mesure donc environ 380dp. `Adaptive(380.dp)`
 * conserve cette largeur et laisse le nombre de colonnes suivre la fenêtre — un écran large montre
 * **plus** de cartes de la même taille, et la carte Stock, qui est dense (nom, catégorie, SKU,
 * badges, prix, deux boutons, pastille d'état, pas d'ajustement), ne supporterait pas d'être
 * étirée : ses éléments s'éloigneraient jusqu'à ne plus se lire comme un groupe.
 *
 * Ascenseur visible à droite, pour la raison donnée dans `AccueilEcran` — sans lui, rien n'indique
 * sur un PC qu'il reste des produits plus bas.
 */
@Composable
fun StockEcran(
    produits: List<Product>,
    recherche: String,
    onRechercheChange: (String) -> Unit,
    categorieChoisie: String,
    onCategorieChange: (String) -> Unit,
    stockFaibleSeulement: Boolean,
    onStockFaibleChange: (Boolean) -> Unit,
    modeBoutique: ShopMode,
    langue: String,
    onEditer: ((Product) -> Unit)? = null,
    onSupprimer: ((Product) -> Unit)? = null,
    onAjusterStock: (Product, Double) -> Unit,
    // Même raison que dans `AccueilEcran` : sur un PC, un lecteur de code-barres USB se comporte
    // comme un clavier, donc le champ de recherche doit pouvoir recevoir le focus à la demande
    // (Ctrl+F) sans que l'utilisateur ait à cliquer dedans.
    focusRecherche: FocusRequester? = null,
    modifier: Modifier = Modifier
) {
    val categories = remember(produits) {
        listOf(CatalogueFiltre.TOUTES_CATEGORIES) + CatalogueFiltre.categories(produits)
    }
    val visibles = remember(produits, recherche, categorieChoisie, stockFaibleSeulement) {
        CatalogueFiltre.filtrer(
            produits = produits,
            recherche = recherche,
            categorie = categorieChoisie,
            stockFaibleSeulement = stockFaibleSeulement
        )
    }

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        OutlinedTextField(
            value = recherche,
            onValueChange = onRechercheChange,
            placeholder = {
                Text(
                    when (langue) {
                        "mg" -> "Hikaroka entana..."
                        "fr" -> "Rechercher un produit..."
                        else -> "Search a product..."
                    }
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline
                )
            },
            trailingIcon = {
                if (recherche.isNotEmpty()) {
                    IconButton(onClick = { onRechercheChange("") }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = null)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .then(focusRecherche?.let { Modifier.focusRequester(it) } ?: Modifier),
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(categories, key = { it }) { categorie ->
                val choisie = categorie.equals(categorieChoisie, ignoreCase = true)
                FilterChip(
                    selected = choisie,
                    onClick = { onCategorieChange(categorie) },
                    label = {
                        Text(
                            text = if (categorie == CatalogueFiltre.TOUTES_CATEGORIES) {
                                when (langue) {
                                    "mg" -> "Rehetra"
                                    "fr" -> "Toutes"
                                    else -> "All"
                                }
                            } else {
                                categorie
                            },
                            fontWeight = if (choisie) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${visibles.size} ${
                    when (langue) {
                        "mg" -> "entana"
                        "fr" -> "produits"
                        else -> "products"
                    }
                }",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )

            FilterChip(
                selected = stockFaibleSeulement,
                onClick = { onStockFaibleChange(!stockFaibleSeulement) },
                label = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (stockFaibleSeulement) {
                                MaterialTheme.colorScheme.onError
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        )
                        Text(
                            text = "Mihavitsy tahiry",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (stockFaibleSeulement) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.error,
                    selectedLabelColor = MaterialTheme.colorScheme.onError,
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                    labelColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
            )
        }

        if (visibles.isEmpty()) {
            Text(
                text = when (langue) {
                    "mg" -> "Tsy misy entana mifanaraka."
                    "fr" -> "Aucun produit ne correspond."
                    else -> "No product matches."
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 24.dp)
            )
        } else {
            val etatGrille = rememberLazyGridState()
            Row(modifier = Modifier.fillMaxSize()) {
                LazyVerticalGrid(
                    state = etatGrille,
                    columns = GridCells.Adaptive(minSize = 380.dp),
                    modifier = Modifier.weight(1f).fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(visibles, key = { it.id }) { produit ->
                        CarteStock(
                            produit = produit,
                            modeBoutique = modeBoutique,
                            libelleStockInitial = when (langue) {
                                "mg" -> "Tahiry"
                                "fr" -> "Stock"
                                else -> "Stock"
                            },
                            onEditer = onEditer,
                            onSupprimer = onSupprimer,
                            onAjusterStock = onAjusterStock
                        )
                    }
                }
                VerticalScrollbar(
                    adapter = rememberScrollbarAdapter(etatGrille),
                    modifier = Modifier.fillMaxHeight().padding(start = 8.dp)
                )
            }
        }
    }
}
