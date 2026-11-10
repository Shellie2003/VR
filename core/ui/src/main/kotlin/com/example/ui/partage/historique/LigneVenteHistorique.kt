package com.example.ui.partage.historique

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.hoverable
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
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.rounded.ShoppingBasket
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
import com.example.data.model.Product
import com.example.data.model.Sale
import com.example.util.FormatUtil
import com.example.util.UniteAbrege

/**
 * Une vente dans l'historique, **portée depuis `SaleListItem`** : carte arrondie à 16dp, marge
 * interne de 14dp, pastille d'icône de 48dp arrondie à 12dp, nom en 15sp gras, sous-titre
 * « quantité unité • heure » en 12sp, montant en 15sp `Black`, badge du nombre d'articles en 9sp
 * sur la couleur du thème à 12 %, et le détail des articles au dépliage.
 *
 * **L'heuristique d'icône est reprise telle quelle**, mots-clés compris (menaka/siramamy → goutte,
 * biski/savony → biscuit, vary/rano → boisson, sinon le panier de la marque). Elle paraît naïve,
 * mais c'est ce que le gérant a sous les yeux depuis des mois : la remplacer par « une icône pour
 * tous » rendrait son historique moins lisible qu'avant sur son PC que sur son téléphone.
 *
 * **L'unité est en forme LONGUE**, comme sur Android : cette ligne occupe la largeur de l'écran,
 * donc « Litatra » s'y lit mieux que « L » (section 99, qui a nommé les deux formes au lieu d'en
 * imposer une).
 *
 * Deux différences avec le téléphone, toutes deux voulues :
 * - Les libellés sont **traduits dans les trois langues** là où l'écran Android les écrit en
 *   malgache quel que soit le réglage (« Vokatra », « hafa »). `AGENTS.md` section 3 l'exige pour
 *   toute interface neuve ; la position, la taille et la couleur sont identiques. À aligner dans un
 *   sens ou dans l'autre sur demande du gérant.
 * - **Survol** (élévation 1 → 6dp) et curseur en main : sur un PC, une carte qui ne réagit pas au
 *   survol se lit comme une image et non comme un élément cliquable (section 88).
 */
@Composable
fun LigneVenteHistorique(
    vente: Sale,
    produitsParId: Map<Int, Product>,
    heureLisible: (Long) -> String,
    langue: String,
    themeColor: Color,
    montantRetourne: Double = 0.0,
    modifier: Modifier = Modifier
) {
    var deplie by remember { mutableStateOf(false) }
    val interactions = remember { MutableInteractionSource() }
    val survole by interactions.collectIsHoveredAsState()
    val elevation by animateDpAsState(if (survole) 6.dp else 1.dp)

    val premierArticle = vente.items.firstOrNull()

    val titre = remember(vente.items, langue) {
        val nom = premierArticle?.name ?: when (langue) {
            "mg" -> "Vokatra"
            "fr" -> "Article"
            else -> "Item"
        }
        if (vente.items.size > 1) {
            val autres = vente.items.size - 1
            when (langue) {
                "mg" -> "$nom + $autres hafa"
                "fr" -> "$nom + $autres autres"
                else -> "$nom + $autres more"
            }
        } else {
            nom
        }
    }

    val uniteProduit = remember(premierArticle, produitsParId) {
        val brute = produitsParId[premierArticle?.productId]?.unit ?: "pcs"
        UniteAbrege.abreviation(brute, UniteAbrege.Forme.LONGUE)
    }

    val sousTitre = remember(premierArticle, uniteProduit, vente.timestamp) {
        val quantite = FormatUtil.formatQty(premierArticle?.quantity ?: 1.0, "")
        "$quantite $uniteProduit • ${heureLisible(vente.timestamp)}"
    }

    val (fondIcone, teinteIcone, icone) = remember(titre, themeColor) {
        val minuscules = titre.lowercase()
        when {
            minuscules.contains("menaka") || minuscules.contains("siramamy") ||
                minuscules.contains("oil") || minuscules.contains("sugar") ->
                Triple(themeColor.copy(alpha = 0.12f), themeColor, Icons.Default.Opacity)

            minuscules.contains("biski") || minuscules.contains("biscuit") ||
                minuscules.contains("savony") || minuscules.contains("soap") ->
                Triple(Color(0xFFFFEDD5), Color(0xFFD97706), Icons.Default.Cookie)

            minuscules.contains("vary") || minuscules.contains("rice") ||
                minuscules.contains("rano") || minuscules.contains("water") ->
                Triple(Color(0xFFF1F5F9), Color(0xFF475569), Icons.Default.LocalDrink)

            else -> Triple(themeColor.copy(alpha = 0.12f), themeColor, Icons.Rounded.ShoppingBasket)
        }
    }

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
                            .background(fondIcone),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icone,
                            contentDescription = null,
                            tint = teinteIcone,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = titre,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = sousTitre,
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
                        text = "Ar ${FormatUtil.formatPrice(vente.totalAmount)}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(themeColor.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${vente.items.size} ×",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = themeColor
                        )
                    }
                    if (montantRetourne > 0.0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (montantRetourne >= vente.totalAmount) {
                                    when (langue) {
                                        "mg" -> "Naverina"
                                        "fr" -> "Retourné"
                                        else -> "Returned"
                                    }
                                } else {
                                    when (langue) {
                                        "mg" -> "Naverina ampahany"
                                        "fr" -> "Retour partiel"
                                        else -> "Partial return"
                                    }
                                },
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
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
                            "mg" -> "Vokatra rehetra amin'ity varotra ity :"
                            "fr" -> "Tous les articles de cette vente :"
                            else -> "All items in this sale:"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    vente.items.forEach { article ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${FormatUtil.formatQty(article.quantity, "")} x ${article.name}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Ar ${FormatUtil.formatPrice(article.price * article.quantity)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(0.3f),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                    // L'auteur de la transaction : sur un PC qui agrège plusieurs postes, c'est
                    // l'information que le téléphone n'a pas besoin d'afficher (il est son propre
                    // auteur) mais que le gérant vient chercher ici (section 34).
                    val auteur = listOf(vente.vendeurNom, vente.deviceName)
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
