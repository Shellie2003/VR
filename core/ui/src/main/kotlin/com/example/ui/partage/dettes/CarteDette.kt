package com.example.ui.partage.dettes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.data.model.Debt
import com.example.util.FormatUtil
import com.example.util.LanguageManager

/**
 * Une carte de dette, **portée depuis `DebtCard`** : carte arrondie à 16dp, 14dp de marge interne,
 * nom en `titleMedium` gras, badge payé/impayé en 9sp... — mêmes tailles, mêmes couleurs, même
 * structure, y compris le fond `surfaceVariant` à 40 % quand la dette est soldée.
 *
 * Le vert `0xFF2E7D32` est conservé tel quel, comme sur Android : Material3 n'a pas de token
 * « succès » et les sections 62 à 67 ont volontairement laissé ces verts en dur.
 *
 * Un clic ouvre le détail — c'est le geste de l'écran Android, et il répond au vrai besoin : la
 * carte tronque la liste des articles, le détail l'affiche en entier (section 33).
 */
@Composable
fun CarteDette(
    dette: Debt,
    langue: String,
    dateLisible: String,
    echeanceLisible: String?,
    onOuvrirDetail: () -> Unit,
    onRembourser: () -> Unit,
    onSupprimer: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val t = { cle: String -> LanguageManager.translate(cle, langue) }
    val enRetard = dette.isOverdue()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOuvrirDetail)
            .pointerHoverIcon(PointerIcon.Hand),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (dette.isPaid) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = dette.debtorName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        BadgeEtatDette(paye = dette.isPaid, libelle = if (dette.isPaid) {
                            t("repaid_badge")
                        } else {
                            t("unpaid_badge")
                        })
                        if (enRetard) BadgeRetard(langue = langue)
                    }
                    Text(
                        text = dateLisible,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    if (echeanceLisible != null) {
                        Text(
                            text = "${
                                when (langue) {
                                    "mg" -> "Fetr'andro"
                                    "fr" -> "Échéance"
                                    else -> "Due"
                                }
                            }: $echeanceLisible",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (enRetard) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.outline
                            },
                            fontWeight = if (enRetard) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${FormatUtil.formatPrice(dette.balance)} Ar",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = if (dette.isPaid) VERT_SOLDE else MaterialTheme.colorScheme.error
                        )
                        if (dette.balance < dette.amount) {
                            Text(
                                text = "Initiale: ${FormatUtil.formatPrice(dette.amount)} Ar",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Pas de bouton de suppression quand l'appelant n'en fournit pas : un bouton
                    // qui ne mène nulle part est pire que son absence (même règle que `CarteStock`).
                    if (onSupprimer != null) {
                        BoutonSupprimerDette(onClick = onSupprimer)
                    }
                }
            }

            if (dette.note.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = dette.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        // « Cliquer » et non « Appuyer » : sur un PC on ne touche pas l'écran, et
                        // une invite qui décrit le mauvais geste donne l'impression d'un portage
                        // bâclé — c'est la seule retouche de texte de cette carte.
                        text = when (langue) {
                            "mg" -> "Kitiho hijery ny antsipiriany"
                            "fr" -> "Cliquer pour voir le détail des articles"
                            else -> "Click to see the full item list"
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (!dette.isPaid) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onRembourser,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .pointerHoverIcon(PointerIcon.Hand),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
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
                    Text(t("repay_btn"), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/** Le badge payé / impayé, identique sur la carte de dette et sur l'en-tête de groupe. */
@Composable
internal fun BadgeEtatDette(paye: Boolean, libelle: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (paye) {
                    VERT_SOLDE.copy(alpha = 0.12f)
                } else {
                    MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                }
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = libelle,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            color = if (paye) VERT_SOLDE else MaterialTheme.colorScheme.error
        )
    }
}

/** Le badge « EN RETARD », sur la paire `errorContainer`/`onErrorContainer` comme sur Android. */
@Composable
internal fun BadgeRetard(langue: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = when (langue) {
                "mg" -> "TARA"
                "fr" -> "EN RETARD"
                else -> "OVERDUE"
            },
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

@Composable
private fun BoutonSupprimerDette(onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(28.dp).pointerHoverIcon(PointerIcon.Hand)
    ) {
        Icon(
            imageVector = Icons.Default.DeleteOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(16.dp)
        )
    }
}

/** Le vert « soldé », repris tel quel de l'écran Android (pas de token « succès » en Material3). */
internal val VERT_SOLDE = Color(0xFF2E7D32)
