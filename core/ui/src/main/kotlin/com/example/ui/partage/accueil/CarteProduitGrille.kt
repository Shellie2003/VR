package com.example.ui.partage.accueil

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Product
import com.example.util.FormatUtil
import com.example.util.PriceUtil
import com.example.util.ShopMode
import com.example.util.UniteAbrege

/**
 * La carte produit de la grille d'accueil, **portée à l'identique** depuis `ProductGridCard`
 * d'Android (`app/ui/screens/HomeScreen.kt`).
 *
 * **Rien n'est redessiné ici, et c'est la consigne.** Chaque valeur est reprise de l'original :
 * coins à 16dp, élévation 1dp, fond `surface`, marge interne 8dp, zone d'image de 165dp arrondie
 * à 12dp sur `surfaceVariant`, nom en 14sp gras sur une seule ligne avec ellipse, badge de stock
 * sur `0xFFFFECE0` en `0xFFD35400`, prix en 14sp `Black` préfixé « Ar », et le badge de quantité
 * en haut à droite avec son bouton `-` de 24dp sur blanc à 25 % d'opacité.
 *
 * **Pourquoi un fichier séparé et non le composable d'Android réutilisé :** `:app` est figé sur un
 * Compose plus ancien que Compose Multiplatform, et les brancher ensemble ferait monter le runtime
 * de l'application de production de deux versions mineures (voir le KDoc du module). Le code est
 * donc recopié à dessein, et sa fidélité tient à ce qu'aucune valeur n'a été « améliorée » au
 * passage — la moindre retouche ici ferait diverger les deux interfaces.
 *
 * **Le seul écart, et il est contraint :** l'image du produit. Android la charge avec Coil depuis
 * `imageUrl`, qui est un chemin de fichier **du téléphone** — il ne désigne rien sur un PC. La
 * carte affiche donc la zone d'image vide, exactement comme le fait l'original pour un produit
 * sans photo. Afficher un cadre d'erreur serait faux : la photo existe, elle est simplement
 * ailleurs.
 */
@Composable
fun CarteProduitGrille(
    produit: Product,
    quantitePanier: Double?,
    modeBoutique: ShopMode,
    langue: String,
    couleurTheme: Color,
    onClick: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            // Zone d'image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(165.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )

                if (quantitePanier != null) {
                    val compte = if (quantitePanier % 1.0 == 0.0) {
                        quantitePanier.toInt().toString()
                    } else {
                        quantitePanier.toString()
                    }

                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(couleurTheme)
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.25f))
                                .clickable(onClick = onDecrement),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Hanafo fikitihana",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        Text(
                            text = compte,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = produit.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.Start
            ) {
                // Le libellé du badge vient de `UniteAbrege` (:core:logic), la MÊME fonction que
                // la carte d'Android appelle désormais : la table d'abréviations n'existe plus
                // qu'une fois, donc les deux plateformes ne peuvent plus se contredire.
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFFFECE0))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = UniteAbrege.libelleBadgeStock(produit.stock, produit.unit, langue),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD35400)
                    )
                }

                // Le prix passe par `PriceUtil`, la même règle que les écrans Android — c'est tout
                // l'objet du découpage en modules : un même produit ne peut pas afficher deux prix
                // différents selon la plateforme.
                val enGros = PriceUtil.isWholesaleActive(produit, modeBoutique)
                val prixAffiche = PriceUtil.displayPrice(produit, modeBoutique)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Ar ${FormatUtil.formatPrice(prixAffiche)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = if (enGros) couleurTheme else MaterialTheme.colorScheme.onSurface
                    )
                    if (enGros) {
                        Box(
                            modifier = Modifier
                                .background(couleurTheme.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "Gros",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = couleurTheme
                            )
                        }
                    }
                }
            }
        }
    }
}
