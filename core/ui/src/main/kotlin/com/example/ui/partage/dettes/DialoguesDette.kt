package com.example.ui.partage.dettes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Debt
import com.example.data.model.DebtorGroup
import com.example.util.DebtAllocation
import com.example.util.FormatUtil
import com.example.util.LanguageManager
import com.example.util.NoteTrosa
import com.example.util.enDecimal

/**
 * Le détail complet d'un trosa, **porté depuis `DebtDetailDialog`** : la carte de la liste tronque
 * la note avec des « … », ce dialogue l'affiche en entier, un article par ligne numérotée, avec le
 * récapitulatif des montants.
 *
 * Le découpage des articles vient de `NoteTrosa` (`:core:logic`), la règle partagée avec Android :
 * couper sur chaque virgule inventerait des articles dans les noms qui en contiennent une.
 */
@Composable
fun DialogueDetailDette(
    dette: Debt,
    langue: String,
    dateLisible: String,
    echeanceLisible: String?,
    onRembourser: () -> Unit,
    onFermer: () -> Unit
) {
    val articles = remember(dette.note) { NoteTrosa.articles(dette.note) }
    val dejaPaye = (dette.amount - dette.balance).coerceAtLeast(0.0)

    AlertDialog(
        onDismissRequest = onFermer,
        title = {
            Text(
                text = dette.debtorName,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = dateLisible,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )

                if (articles.isNotEmpty()) {
                    HorizontalDivider()
                    Text(
                        text = when (langue) {
                            "mg" -> "Entana nalaina"
                            "fr" -> "Articles pris"
                            else -> "Items taken"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    // Numérotés, et surtout SANS troncature : c'est exactement ce que la carte ne
                    // peut pas montrer, donc la seule raison d'ouvrir ce dialogue.
                    articles.forEachIndexed { index, article ->
                        Text(
                            text = "${index + 1}. $article",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                HorizontalDivider()
                LigneMontant(
                    libelle = when (langue) {
                        "mg" -> "Totaly tany am-piandohana"
                        "fr" -> "Montant initial"
                        else -> "Initial amount"
                    },
                    montant = dette.amount
                )
                LigneMontant(
                    libelle = when (langue) {
                        "mg" -> "Efa voaloa"
                        "fr" -> "Déjà payé"
                        else -> "Already paid"
                    },
                    montant = dejaPaye
                )
                LigneMontant(
                    libelle = when (langue) {
                        "mg" -> "Sisa tsy voaloa"
                        "fr" -> "Reste à payer"
                        else -> "Remaining"
                    },
                    montant = dette.balance,
                    accentue = true
                )
                if (echeanceLisible != null) {
                    Text(
                        text = "${
                            when (langue) {
                                "mg" -> "Fetr'andro"
                                "fr" -> "Échéance"
                                else -> "Due"
                            }
                        } : $echeanceLisible",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (dette.isOverdue()) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                        fontWeight = if (dette.isOverdue()) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        },
        confirmButton = {
            if (!dette.isPaid) {
                Button(onClick = onRembourser) {
                    Text(
                        LanguageManager.translate("repay_btn", langue),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onFermer) {
                Text(
                    when (langue) {
                        "mg" -> "Hidiana"
                        "fr" -> "Fermer"
                        else -> "Close"
                    }
                )
            }
        }
    )
}

@Composable
private fun LigneMontant(libelle: String, montant: Double, accentue: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = libelle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "${FormatUtil.formatPrice(montant)} Ar",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (accentue) FontWeight.Black else FontWeight.SemiBold,
            color = if (accentue) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

/**
 * Le remboursement groupé, **porté depuis `GroupRepayDialog`** — avec son point essentiel : l'aperçu
 * de la répartition est construit par **exactement** le même calcul que celui appliqué à la
 * confirmation (`DebtAllocation.calculer`, `:core:logic`). Deux calculs distincts, l'un pour montrer
 * et l'autre pour écrire, finiraient par ne plus dire la même chose — et le gérant découvrirait
 * l'écart après avoir validé.
 */
@Composable
fun DialogueReglementGroupe(
    groupe: DebtorGroup,
    langue: String,
    onConfirmer: (Double) -> Unit,
    onAnnuler: () -> Unit
) {
    var montantSaisi by remember { mutableStateOf("") }
    val montant = montantSaisi.enDecimal() ?: 0.0
    val erreur = montantSaisi.isNotBlank() && (montantSaisi.enDecimal()?.let { it <= 0.0 } ?: true)
    val plan = remember(montant, groupe) { DebtAllocation.calculer(groupe.debts, montant) }

    AlertDialog(
        onDismissRequest = onAnnuler,
        title = {
            Text(
                text = when (langue) {
                    "mg" -> "Fandoavana miditra amin'ny trosa maro"
                    "fr" -> "Remboursement groupé"
                    else -> "Grouped repayment"
                },
                fontWeight = FontWeight.Black
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "${LanguageManager.translate("debtor_name", langue)} : ${groupe.displayName}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = when (langue) {
                        "mg" -> "Trosa ${groupe.debts.size} miaraka, mitotaly ${FormatUtil.formatPrice(groupe.totalBalance)} Ar"
                        "fr" -> "${groupe.debts.size} trosa cumulés, pour un total de ${FormatUtil.formatPrice(groupe.totalBalance)} Ar"
                        else -> "${groupe.debts.size} combined debts, totalling ${FormatUtil.formatPrice(groupe.totalBalance)} Ar"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )

                OutlinedTextField(
                    value = montantSaisi,
                    onValueChange = { montantSaisi = it },
                    label = {
                        Text(
                            when (langue) {
                                "mg" -> "Vola raisina (Ar)"
                                "fr" -> "Montant reçu (Ar)"
                                else -> "Amount received (Ar)"
                            }
                        )
                    },
                    prefix = { Text("Ar ") },
                    isError = erreur,
                    supportingText = {
                        if (erreur) {
                            Text(
                                text = when (langue) {
                                    "mg" -> "Sora-bola diso"
                                    "fr" -> "Montant invalide"
                                    else -> "Invalid amount"
                                },
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                )

                if (plan.lignes.isNotEmpty()) {
                    HorizontalDivider()
                    Text(
                        text = when (langue) {
                            "mg" -> "Ho zaraina toy izao ny vola:"
                            "fr" -> "Répartition automatique :"
                            else -> "Automatic breakdown:"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        plan.lignes.forEach { ligne ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${FormatUtil.formatPrice(ligne.soldeAvant)} Ar → " +
                                            "${FormatUtil.formatPrice(ligne.soldeApres)} Ar",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (ligne.estSoldee) {
                                        Text(
                                            text = when (langue) {
                                                "mg" -> "Voaloa tanteraka"
                                                "fr" -> "Soldée"
                                                else -> "Settled"
                                            },
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            color = VERT_SOLDE
                                        )
                                    }
                                }
                                Text(
                                    text = "-${FormatUtil.formatPrice(ligne.montantApplique)} Ar",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    if (plan.montantNonAffecte > 0.0) {
                        Text(
                            text = when (langue) {
                                "mg" -> "Mihoatra ny trosa rehetra ny vola: ${FormatUtil.formatPrice(plan.montantNonAffecte)} Ar tsy voakasika."
                                "fr" -> "Le montant dépasse la dette totale : ${FormatUtil.formatPrice(plan.montantNonAffecte)} Ar ne seront pas affectés."
                                else -> "Amount exceeds the total debt: ${FormatUtil.formatPrice(plan.montantNonAffecte)} Ar will not be applied."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = ORANGE_AVERTISSEMENT
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (montant > 0.0) onConfirmer(montant) },
                enabled = plan.lignes.isNotEmpty()
            ) {
                Text(
                    LanguageManager.translate("repay_btn", langue),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onAnnuler) {
                Text(
                    when (langue) {
                        "mg" -> "Hajanona"
                        "fr" -> "Annuler"
                        else -> "Cancel"
                    }
                )
            }
        }
    )
}

/**
 * L'orange d'avertissement de l'écran Dettes Android, repris tel quel (section 65 : laissé en dur
 * faute de token « avertissement » dans le thème).
 */
private val ORANGE_AVERTISSEMENT = Color(0xFFEF6C00)
