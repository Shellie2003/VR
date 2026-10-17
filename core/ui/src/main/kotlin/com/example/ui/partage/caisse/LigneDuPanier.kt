package com.example.ui.partage.caisse

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RemoveCircleOutline
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CartItem
import com.example.util.AjustementStock
import com.example.util.FormatUtil

/**
 * Une ligne du panier, **portée depuis `CartItemRow`** de l'écran de caisse Android : mêmes
 * tailles (nom 13sp gras, prix unitaire 10sp, badge de quantité 9sp sur `surfaceVariant` arrondi à
 * 4dp, boutons de 24dp à icône de 18dp, total 13sp gras en couleur de thème) et même séparateur de
 * 0,5dp en fin de ligne.
 *
 * Le pas des boutons vient d'`AjustementStock` — la règle unique partagée avec Android depuis la
 * section 91, où elle existait en cinq exemplaires dont trois variantes différentes.
 *
 * **Seul ajout PC :** la ligne s'éclaire légèrement au survol. Sur un téléphone il n'existe pas
 * d'état « le doigt est au-dessus sans toucher » ; sur un PC, la souris parcourt un panier long et
 * doit pouvoir repérer la ligne qu'elle vise avant de cliquer sur un `-` de 24dp.
 */
@Composable
fun LigneDuPanier(
    item: CartItem,
    couleurTheme: Color,
    onQuantiteDelta: (Double) -> Unit,
    onEditerQuantite: (CartItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val pas = AjustementStock.pas(item.unit)
    val peutAjouter = item.quantity < item.maxStock

    val interaction = remember { MutableInteractionSource() }
    val survole by interaction.collectIsHoveredAsState()
    val opaciteFond by animateFloatAsState(
        targetValue = if (survole) 0.35f else 0f,
        label = "fondLignePanier"
    )

    Column(modifier = modifier.hoverable(interaction)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = opaciteFond))
                .padding(vertical = 4.dp, horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${FormatUtil.formatPrice(item.price)} Ar / ${item.unit}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "x${quantiteLisible(item.quantity)} ${item.unit}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = { onQuantiteDelta(-pas) },
                    modifier = Modifier.size(24.dp).pointerHoverIcon(PointerIcon.Hand)
                ) {
                    Icon(
                        imageVector = Icons.Default.RemoveCircleOutline,
                        contentDescription = null,
                        tint = couleurTheme,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = { if (peutAjouter) onQuantiteDelta(pas) },
                    enabled = peutAjouter,
                    modifier = Modifier.size(24.dp).pointerHoverIcon(PointerIcon.Hand)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddCircleOutline,
                        contentDescription = null,
                        tint = if (peutAjouter) couleurTheme else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = { onEditerQuantite(item) },
                    modifier = Modifier.size(24.dp).pointerHoverIcon(PointerIcon.Hand)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = couleurTheme,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "${FormatUtil.formatPrice(item.totalPrice)} Ar",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = couleurTheme,
                    fontSize = 13.sp,
                    maxLines = 1
                )
            }
        }
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
        )
    }
}

/**
 * La quantité telle que l'écran Android l'écrit : un entier sans décimale quand elle en est un,
 * deux décimales sinon. « x1 » et non « x1.0 » — un panier rempli de « 1.0 » se lit mal au moment
 * d'annoncer le total au client.
 */
internal fun quantiteLisible(quantite: Double): String =
    if (quantite % 1.0 == 0.0) quantite.toInt().toString() else "%.2f".format(quantite)
