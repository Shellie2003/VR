package com.example.ui.partage.stock

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Product
import com.example.util.AjustementStock
import com.example.util.FormatUtil
import com.example.util.PriceUtil
import com.example.util.ShopMode

/**
 * La carte de l'écran Stock, **portée à l'identique** depuis `ProductInventoryCard` d'Android.
 *
 * Valeurs reprises telles quelles : coins 16dp, marge interne 12dp, fond qui passe à
 * `errorContainer` à 12 % quand le stock est faible, bordure de 2dp en `error` à 60 % dans ce cas
 * (1dp en `outlineVariant` à 40 % sinon), `FlowRow` pour la catégorie / le SKU / les badges — c'est
 * ce `FlowRow` qui empêche la mise en page d'être écrasée par un nom de catégorie long —, badges
 * « ALERT » et « LOW QTY » en 9sp `Black` sur la paire `error`/`onError`, séparateur à 30 %, pastille
 * d'état de 8dp (rouge épuisé / `0xFFFF9800` faible / `0xFF4CAF50` correct), et le pas à trois
 * éléments `-` / quantité / `+` avec ses boutons de 32dp sur `surfaceVariant` arrondis à 8dp.
 *
 * **Deux différences avec l'original, et aucune n'est esthétique.**
 *  1. Android passe le `viewModel` à la carte ; ici tout arrive en paramètres. C'est la règle du
 *     module, et c'est aussi ce qui rend la carte utilisable sans base de données.
 *  2. Le survol et le curseur main, qui n'ont pas d'équivalent tactile (voir `CarteProduitGrille`).
 *     L'élévation au repos reste celle de l'original : aucune.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CarteStock(
    produit: Product,
    modeBoutique: ShopMode,
    libelleStockInitial: String,
    /**
     * Rappels **facultatifs**, et le bouton n'est pas dessiné quand il n'y en a pas. Même principe
     * que le bouton Paramètres de l'en-tête d'accueil : un bouton qui ne mène nulle part est pire
     * qu'un bouton absent — il se clique, rien ne se passe, et l'utilisateur conclut que le
     * logiciel est cassé. La fiche produit n'étant pas encore portée, l'édition est simplement
     * absente du bureau au lieu d'être présente et muette.
     */
    onEditer: ((Product) -> Unit)? = null,
    onSupprimer: ((Product) -> Unit)? = null,
    onAjusterStock: (Product, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val interactions = remember { MutableInteractionSource() }
    val survole by interactions.collectIsHoveredAsState()
    val elevation by animateDpAsState(if (survole) 4.dp else 0.dp)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .hoverable(interactions),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (produit.isLowStock) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.12f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        border = BorderStroke(
            width = if (produit.isLowStock) 2.dp else 1.dp,
            color = if (produit.isLowStock) {
                MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            }
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = produit.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    // `FlowRow` et non `Row` : c'est le correctif qui empêche catégorie, SKU et
                    // badges de s'écraser quand un nom de catégorie est long (section 2 de la carte
                    // des fonctionnalités). Le remplacer par une rangée simple réintroduirait
                    // exactement ce défaut.
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = produit.category,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (produit.sku.isNotEmpty()) {
                            Text(
                                text = "•  SKU: ${produit.sku}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        if (produit.isLowStock) BadgeAlerte("ALERT")
                        if (produit.stock < 5) BadgeAlerte("LOW QTY")
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val enGros = PriceUtil.isWholesaleActive(produit, modeBoutique)
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            // « Ar » en SUFFIXE ici, alors que la carte d'accueil le met en
                            // préfixe : c'est ainsi dans les deux écrans d'Android, et aligner les
                            // deux serait un choix de design, pas un portage.
                            text = "${FormatUtil.formatPrice(PriceUtil.displayPrice(produit, modeBoutique))} Ar",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        if (enGros) {
                            Text(
                                text = "Gros",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    if (onEditer != null) {
                        IconButton(
                            onClick = { onEditer(produit) },
                            modifier = Modifier.size(32.dp).pointerHoverIcon(PointerIcon.Hand)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Hanova",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    if (onSupprimer != null) {
                        IconButton(
                            onClick = { onSupprimer(produit) },
                            modifier = Modifier.size(32.dp).pointerHoverIcon(PointerIcon.Hand)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Hamafa",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val disponible = produit.stock > 0
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    !disponible -> MaterialTheme.colorScheme.error
                                    produit.isLowStock -> Color(0xFFFF9800)
                                    else -> Color(0xFF4CAF50)
                                }
                            )
                    )
                    Column {
                        Text(
                            text = if (disponible) {
                                "$libelleStockInitial: ${FormatUtil.formatQty(produit.stock, produit.unit)}"
                            } else {
                                "Lany tahiry!"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (disponible) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        )
                        val faible = produit.stock < 5
                        Text(
                            // `product.stock` et non `stock_quantity` : ce dernier n'est jamais
                            // décrémenté à la vente, et l'afficher montrait une quantité figée
                            // depuis la création du produit (correctif de la section 13).
                            text = "Qty: ${produit.stock.toInt()}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (faible) FontWeight.Bold else FontWeight.Normal,
                            color = if (faible) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(
                        onClick = {
                            onAjusterStock(produit, AjustementStock.apresRetrait(produit.stock, produit.unit))
                        },
                        enabled = produit.stock > 0,
                        modifier = Modifier
                            .size(32.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                            .pointerHoverIcon(PointerIcon.Hand)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                    }

                    Text(
                        text = FormatUtil.formatQty(produit.stock, "").replace(" ", ""),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(min = 36.dp)
                    )

                    IconButton(
                        onClick = {
                            onAjusterStock(produit, AjustementStock.apresAjout(produit.stock, produit.unit))
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                            .pointerHoverIcon(PointerIcon.Hand)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

/** Les deux badges « ALERT » et « LOW QTY » ont strictement la même forme : un seul composable. */
@Composable
private fun BadgeAlerte(texte: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.error)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = texte,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onError
        )
    }
}
