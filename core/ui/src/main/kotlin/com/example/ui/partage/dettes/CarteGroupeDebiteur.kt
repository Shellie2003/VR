package com.example.ui.partage.dettes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Debt
import com.example.data.model.DebtorGroup
import com.example.util.FormatUtil
import com.example.util.LanguageManager

/**
 * La carte consolidée d'un débiteur qui a plusieurs trosa, **portée depuis `DebtorGroupCard`** :
 * même en-tête (nom, badges, solde total, « Initiale : » quand le solde est entamé), même chevron
 * de dépliage, même bouton « Régler un montant sur l'ensemble » visible **sans déplier**, et le
 * détail qui révèle chaque `CarteDette` du groupe.
 *
 * Le bouton de règlement groupé est dans l'en-tête et non dans le détail, exactement comme sur
 * Android (section 51) : c'est le geste qu'on veut simplifier, l'obliger à déplier d'abord
 * reviendrait à remettre l'effort qu'on vient de retirer.
 */
@Composable
fun CarteGroupeDebiteur(
    groupe: DebtorGroup,
    langue: String,
    dateLisible: (Debt) -> String,
    echeanceLisible: (Debt) -> String?,
    onOuvrirDetail: (Debt) -> Unit,
    onRembourser: (Debt) -> Unit,
    onReglerGroupe: () -> Unit,
    onSupprimer: ((Debt) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val t = { cle: String -> LanguageManager.translate(cle, langue) }
    var deplie by remember(groupe.displayName) { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (groupe.isFullyPaid) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { deplie = !deplie }
                    .pointerHoverIcon(PointerIcon.Hand),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = groupe.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        BadgeEtatDette(
                            paye = groupe.isFullyPaid,
                            libelle = if (groupe.isFullyPaid) t("repaid_badge") else t("unpaid_badge")
                        )
                        if (groupe.hasOverdue) BadgeRetard(langue = langue)
                    }
                    Text(
                        text = when (langue) {
                            "mg" -> "${groupe.debts.size} trosa nakambana"
                            "fr" -> "${groupe.debts.size} dettes cumulées"
                            else -> "${groupe.debts.size} combined debts"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${FormatUtil.formatPrice(groupe.totalBalance)} Ar",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = if (groupe.isFullyPaid) {
                                VERT_SOLDE
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        )
                        if (groupe.totalBalance < groupe.totalAmount) {
                            Text(
                                text = "Initiale: ${FormatUtil.formatPrice(groupe.totalAmount)} Ar",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                    Icon(
                        imageVector = if (deplie) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }

            if (!groupe.isFullyPaid) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onReglerGroupe,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .pointerHoverIcon(PointerIcon.Hand),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Payment,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when (langue) {
                            "mg" -> "Handoa vola miditra amin'ny trosa rehetra"
                            "fr" -> "Régler un montant sur l'ensemble"
                            else -> "Pay an amount across all debts"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (deplie) {
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    groupe.debts.forEach { dette ->
                        CarteDette(
                            dette = dette,
                            langue = langue,
                            dateLisible = dateLisible(dette),
                            echeanceLisible = echeanceLisible(dette),
                            onOuvrirDetail = { onOuvrirDetail(dette) },
                            onRembourser = { onRembourser(dette) },
                            onSupprimer = onSupprimer?.let { action -> { action(dette) } }
                        )
                    }
                }
            }
        }
    }
}
