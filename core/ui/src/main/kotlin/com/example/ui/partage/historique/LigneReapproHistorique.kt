package com.example.ui.partage.historique

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.data.model.Restock
import com.example.util.FiltreHistorique
import com.example.util.FormatUtil

/**
 * Un réapprovisionnement dans l'historique, **porté depuis `RestockListItem`** : même carte à
 * 16dp, même pastille d'inventaire de 48dp sur la couleur du thème à 10 %, même badge
 * « TAFIDITRA » sur `surfaceVariant`, et le même détail au dépliage (unités, coût d'achat unitaire,
 * prix de vente, fournisseur).
 *
 * **Le coût unitaire passe par [FiltreHistorique.coutUnitaireReappro]**, qui rend `null` quand le
 * réappro ne compte aucune unité. La carte Android divisait sans garde et affichait « Ar ∞ » dans
 * ce cas — un tiret dit la vérité : on ne connaît pas le coût unitaire d'une livraison dont les
 * pièces n'ont pas été comptées. Le défaut est corrigé des deux côtés par cette même fonction.
 */
@Composable
fun LigneReapproHistorique(
    reappro: Restock,
    heureLisible: (Long) -> String,
    langue: String,
    themeColor: Color,
    modifier: Modifier = Modifier
) {
    var deplie by remember { mutableStateOf(false) }
    val interactions = remember { MutableInteractionSource() }
    val survole by interactions.collectIsHoveredAsState()
    val elevation by animateDpAsState(if (survole) 6.dp else 1.dp)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .hoverable(interactions)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable { deplie = !deplie },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(themeColor.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inventory,
                            contentDescription = null,
                            tint = themeColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = reappro.productName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "${reappro.cartonsQuantity.toInt()} ${
                                when (langue) {
                                    "mg" -> "baoritra"
                                    "fr" -> "cartons"
                                    else -> "cartons"
                                }
                            } x ${reappro.itemsPerCarton.toInt()} • ${heureLisible(reappro.timestamp)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Ar ${FormatUtil.formatPrice(reappro.totalCostPrice)}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "TAFIDITRA",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = deplie,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        thickness = 1.dp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = when (langue) {
                            "mg" -> "Mombamomba ny fampidirana :"
                            "fr" -> "Détail de l'approvisionnement :"
                            else -> "Restock details:"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))

                    val coutUnitaire = FiltreHistorique.coutUnitaireReappro(reappro)
                    LigneDetail(
                        libelle = when (langue) {
                            "mg" -> "Isany manontolo"
                            "fr" -> "Total d'unités"
                            else -> "Total units"
                        },
                        valeur = FormatUtil.formatQty(reappro.totalUnits, "")
                    )
                    LigneDetail(
                        libelle = when (langue) {
                            "mg" -> "Vidiny nividianana (singany)"
                            "fr" -> "Coût d'achat (unité)"
                            else -> "Purchase cost (unit)"
                        },
                        valeur = coutUnitaire?.let { "Ar ${FormatUtil.formatPrice(it)}" } ?: "—"
                    )
                    LigneDetail(
                        libelle = when (langue) {
                            "mg" -> "Vidiny hivarotana"
                            "fr" -> "Prix de vente"
                            else -> "Selling price"
                        },
                        valeur = "Ar ${FormatUtil.formatPrice(reappro.unitSellingPrice)}"
                    )
                    LigneDetail(
                        libelle = when (langue) {
                            "mg" -> "Mpamatsy"
                            "fr" -> "Fournisseur"
                            else -> "Supplier"
                        },
                        valeur = reappro.supplierName?.takeIf { it.isNotBlank() }
                            ?: when (langue) {
                                "mg" -> "Tsy misy"
                                "fr" -> "Non renseigné"
                                else -> "Not set"
                            }
                    )

                    val auteur = listOf(reappro.vendeurNom, reappro.deviceName)
                        .filter { it.isNotBlank() }
                        .joinToString(" • ")
                    if (auteur.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = auteur,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

/** Une ligne « libellé / valeur » du détail, reprise de `DetailRow` de l'écran Android. */
@Composable
private fun LigneDetail(libelle: String, valeur: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = libelle,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = valeur,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End
        )
    }
}
