package com.example.ui.partage.caisse

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import com.example.data.model.Product
import com.example.util.AjustementStock
import com.example.util.FormatUtil
import com.example.util.PriceUtil
import com.example.util.ShopMode

/**
 * La carte produit du sélecteur de caisse, **portée depuis `CashRegisterProductCard`** : carte
 * arrondie à 24dp, zone d'image de 56dp arrondie à 16dp et bordée, « T : <stock> » en 13sp, prix en
 * 15sp gras, nom en 14sp normal, puis la rangée de trois pastilles de saisie rapide arrondies à
 * 16dp sur le bleu doux `0xFF8CC1F0` à texte noir.
 *
 * Les trois pastilles suivent la nature de l'unité (`AjustementStock.typeUnite`, règle unique
 * depuis la section 91) : « 1/4 L · 1/2 L · +1 » pour un liquide, « 1/4 kg · 1/2 kg · +1 » pour un
 * poids, « +1 · +5 · +10 » pour tout le reste.
 *
 * **Le bleu et le noir des pastilles sont conservés tels quels**, comme sur Android : ce sont des
 * couleurs de maquette assumées (AGENTS.md section 64 les laisse volontairement en dur), et les
 * remplacer par des tokens changerait le design que ce portage a pour but de préserver.
 *
 * **Pas d'image réelle**, pour la raison déjà donnée dans `CarteProduitGrille` : Coil est une
 * bibliothèque Android, et `imageUrl` désigne de toute façon un fichier du téléphone. La zone
 * d'image garde donc son icône de remplacement, exactement comme sur un produit sans photo.
 */
@Composable
fun CarteProduitCaisse(
    produit: Product,
    modeBoutique: ShopMode,
    couleurTheme: Color,
    onAjouter: (Product, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val enRupture = produit.stock <= 0
    val type = AjustementStock.typeUnite(produit.unit)

    val interaction = remember { MutableInteractionSource() }
    val survole by interaction.collectIsHoveredAsState()
    val elevation by animateDpAsState(
        targetValue = if (survole) 6.dp else 0.dp,
        label = "elevationCarteCaisse"
    )

    Card(
        modifier = modifier.hoverable(interaction),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            RoundedCornerShape(16.dp)
                        )
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    val stockLisible = if (produit.stock % 1.0 == 0.0) {
                        produit.stock.toInt().toString()
                    } else {
                        "%.1f".format(produit.stock)
                    }
                    Text(
                        text = "T : $stockLisible",
                        color = if (enRupture) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        // `PriceUtil` et non `produit.price` : le mode grossiste doit facturer le
                        // prix de gros ici comme ailleurs, c'est toute la raison d'être de cette
                        // règle unique (section 32).
                        text = "${FormatUtil.formatPrice(PriceUtil.displayPrice(produit, modeBoutique))} Ar",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = produit.name,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val pastilles: List<Pair<String, Double>> = when (type) {
                    AjustementStock.TypeUnite.LIQUIDE ->
                        listOf("1/4 L" to 0.25, "1/2 L" to 0.5, "+1" to 1.0)
                    AjustementStock.TypeUnite.POIDS ->
                        listOf("1/4 kg" to 0.25, "1/2 kg" to 0.5, "+1" to 1.0)
                    AjustementStock.TypeUnite.AUTRE ->
                        listOf("+1" to 1.0, "+5" to 5.0, "+10" to 10.0)
                }
                pastilles.forEach { (libelle, quantite) ->
                    PastilleSaisieRapide(
                        texte = libelle,
                        onClick = { onAjouter(produit, quantite) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/** La pastille de saisie rapide, identique à `PillPresetButton` d'Android. */
@Composable
private fun PastilleSaisieRapide(
    texte: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF8CC1F0))
            .clickable(onClick = onClick)
            .pointerHoverIcon(PointerIcon.Hand),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = texte,
            color = Color.Black,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}
