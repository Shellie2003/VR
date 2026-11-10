package com.example.ui.partage.peremption

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LotLisible
import com.example.util.AlertePeremption
import com.example.util.EtatLot
import com.example.util.FormatUtil

/**
 * Les alertes de péremption, **portées depuis `PeremptionScreen`** : carte de synthèse arrondie à
 * 20dp qui passe au rouge dès qu'un lot est périmé, compteurs « Périmés » et « Bientôt (7j) », puis
 * la liste des lots triée par échéance la plus proche, chaque carte bordée de la couleur de son
 * état.
 *
 * **La classification vient de [AlertePeremption]**, partagée avec l'écran Android et avec la
 * notification quotidienne. La fenêtre de sept jours était écrite dans ces deux endroits : la
 * porter ici l'aurait écrite trois fois, et c'est un seuil que le gérant peut vouloir déplacer.
 *
 * **L'instant de référence est figé une fois** pour toute la liste (`remember`). Sans cela deux
 * cartes rendues à une milliseconde d'écart pourraient se classer différemment, et le compteur de
 * l'en-tête ne correspondrait plus aux couleurs en dessous.
 *
 * Pas de bouton « Ajouter un lot » : le bureau est en lecture seule, et la saisie d'un lot est une
 * écriture. Le téléphone reste l'endroit où on enregistre.
 */
@Composable
fun PeremptionEcran(
    lots: List<LotLisible>,
    dateLisible: (Long) -> String,
    langue: String,
    themeColor: Color,
    modifier: Modifier = Modifier
) {
    val maintenant = remember { System.currentTimeMillis() }
    val triees = remember(lots) { lots.sortedBy { it.datePeremption } }
    val perimes = remember(lots, maintenant) {
        AlertePeremption.compterPerimes(lots.map { it.datePeremption }, maintenant)
    }
    val bientot = remember(lots, maintenant) {
        AlertePeremption.compterBientot(lots.map { it.datePeremption }, maintenant)
    }

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Card(
            modifier = Modifier.widthIn(max = 760.dp).fillMaxWidth().padding(vertical = 12.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                // Le rouge dès qu'un lot est périmé, comme sur Android : laissé en dur parce que
                // c'est un bandeau plein à contenu blanc explicite — y mettre `error` réintroduirait
                // le blanc-sur-clair corrigé en section 64.
                containerColor = if (perimes > 0) Color(0xFFC62828) else themeColor
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = when (langue) {
                                "mg" -> "TARA"
                                "fr" -> "Périmés"
                                else -> "Expired"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Text(
                            text = "$perimes",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = when (langue) {
                                "mg" -> "AKAIKY"
                                "fr" -> "Bientôt (7j)"
                                else -> "Soon (7d)"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Text(
                            text = "$bientot",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }
            }
        }

        if (triees.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.outlineVariant
                )
                Spacer(Modifier.size(12.dp))
                Text(
                    text = when (langue) {
                        "mg" -> "Tsy misy lot voarakitra."
                        "fr" -> "Aucun lot enregistré."
                        else -> "No batch recorded."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = when (langue) {
                        "mg" -> "Ampidiro ao amin'ny finday ny lot."
                        "fr" -> "Les lots se saisissent depuis le téléphone."
                        else -> "Batches are entered on the phone."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            return@Column
        }

        Row(modifier = Modifier.fillMaxSize()) {
            val etat = rememberLazyListState()
            LazyColumn(
                state = etat,
                modifier = Modifier.weight(1f).widthIn(max = 760.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Index dans la clé : un lot n'a pas d'identifiant dans la sauvegarde, et deux
                // lots du même produit sans numéro et de même date sont parfaitement possibles.
                itemsIndexed(
                    triees,
                    key = { index, lot -> "$index-${lot.produitNom}-${lot.datePeremption}" }
                ) { _, lot ->
                    LigneLot(
                        lot = lot,
                        etatLot = AlertePeremption.etat(lot.datePeremption, maintenant),
                        dateLisible = dateLisible,
                        langue = langue
                    )
                }
            }
            VerticalScrollbar(
                modifier = Modifier.fillMaxHeight().padding(start = 8.dp),
                adapter = rememberScrollbarAdapter(etat)
            )
        }
    }
}

@Composable
private fun LigneLot(
    lot: LotLisible,
    etatLot: EtatLot,
    dateLisible: (Long) -> String,
    langue: String
) {
    // Le même feu tricolore que l'écran Android, laissé en dur : n'en migrer qu'une branche
    // casserait le jeu de couleurs (sections 64 à 67).
    val accent = when (etatLot) {
        EtatLot.PERIME -> Color(0xFFC62828)
        EtatLot.BIENTOT -> Color(0xFFF57C00)
        EtatLot.BON -> Color(0xFF2E7D32)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (etatLot == EtatLot.PERIME) {
                        Icons.Default.Warning
                    } else {
                        Icons.Default.CalendarToday
                    },
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = lot.produitNom,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${FormatUtil.formatQty(lot.quantite, lot.unite)} • " +
                        (lot.numeroLot?.takeIf { it.isNotBlank() } ?: "—"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = dateLisible(lot.datePeremption),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
                Text(
                    text = when (etatLot) {
                        EtatLot.PERIME -> when (langue) {
                            "mg" -> "Tara"
                            "fr" -> "Périmé"
                            else -> "Expired"
                        }
                        EtatLot.BIENTOT -> when (langue) {
                            "mg" -> "Akaiky"
                            "fr" -> "Bientôt"
                            else -> "Soon"
                        }
                        EtatLot.BON -> when (langue) {
                            "mg" -> "Tsara"
                            "fr" -> "Bon"
                            else -> "OK"
                        }
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = accent
                )
            }
        }
    }
}
