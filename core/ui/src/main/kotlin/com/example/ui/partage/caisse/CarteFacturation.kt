package com.example.ui.partage.caisse

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.example.util.FormatUtil
import com.example.util.LanguageManager
import com.example.util.Monnaie

/**
 * La carte de facturation, **portée depuis le panneau droit de la caisse tablette d'Android** :
 * carte arrondie à 16dp sur `surfaceVariant` à 40 % avec bordure `outlineVariant` à 15 %, libellé
 * « argent reçu » en 11sp gras suivi des coupures de saisie rapide, champ de montant de 56dp
 * minimum en 17sp gras, indicateur de rendu, sélecteur de mode de paiement, puis la rangée total +
 * bouton de validation de 44dp.
 *
 * Tous les calculs viennent de `Monnaie` (`:core:logic`) : total, suffisance, libellé du rendu et
 * liste des coupures. C'est la règle partagée avec Android depuis la section 90, où elle existait
 * en trois exemplaires dans le seul écran de caisse.
 *
 * **La loupe du champ de montant n'existe pas**, et c'est volontaire : la section 73 l'a retirée de
 * l'écran Android parce qu'elle était un reste de copier-coller depuis la barre de recherche,
 * sémantiquement fausse sur un montant, et qu'elle mangeait la largeur dont les chiffres ont
 * besoin. La reproduire ici réintroduirait un défaut déjà corrigé.
 */
@Composable
fun CarteFacturation(
    total: Double,
    montantRecuSaisi: String,
    onMontantRecuChange: (String) -> Unit,
    montantRecu: Double,
    modePaiement: String,
    onModePaiementChange: (String) -> Unit,
    panierVide: Boolean,
    onViderPanier: () -> Unit,
    onValider: () -> Unit,
    validationPossible: Boolean,
    messageValidationImpossible: String?,
    couleurTheme: Color,
    langue: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = LanguageManager.translate("cash_given", langue),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(Monnaie.coupuresRapides, key = { it }) { coupure ->
                        OutlinedButton(
                            onClick = {
                                // Même geste que sur Android : la coupure s'AJOUTE au montant déjà
                                // saisi, pour qu'un client qui tend deux billets se compose en deux
                                // appuis au lieu d'un calcul mental.
                                val courant = montantRecu
                                onMontantRecuChange((courant + coupure).toInt().toString())
                            },
                            modifier = Modifier.height(24.dp).pointerHoverIcon(PointerIcon.Hand),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            val libelle = if (coupure >= 1000) "${coupure / 1000}k" else "$coupure"
                            Text(text = "$libelle Ar", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                IconButton(
                    onClick = { onMontantRecuChange("") },
                    modifier = Modifier.size(16.dp).pointerHoverIcon(PointerIcon.Hand)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = montantRecuSaisi,
                    onValueChange = onMontantRecuChange,
                    placeholder = {
                        Text("${LanguageManager.translate("cash_given", langue)} (Ar)", fontSize = 11.sp)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.weight(1.9f).heightIn(min = 56.dp),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                if (montantRecu > 0.0) {
                    val suffisant = Monnaie.suffisant(total, montantRecu)
                    val couleur = if (suffisant) VERT_SUFFISANT else MaterialTheme.colorScheme.error

                    Box(
                        modifier = Modifier
                            .weight(1.1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(couleur.copy(alpha = 0.1f))
                            .border(1.dp, couleur, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (suffisant) {
                                    Icons.Default.CheckCircle
                                } else {
                                    Icons.Default.ErrorOutline
                                },
                                contentDescription = null,
                                tint = couleur,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = Monnaie.libelleRendu(total, montantRecu, langue),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = couleur,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            RangeeModePaiement(
                selectionne = modePaiement,
                onSelectionner = onModePaiementChange,
                langue = langue,
                couleurTheme = couleurTheme
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1.1f)) {
                    Text(
                        text = when (langue) {
                            "mg" -> "Totalin'ny fandoavana:"
                            "fr" -> "Total à payer :"
                            else -> "Total to pay:"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${FormatUtil.formatPrice(total)} Ar",
                            color = couleurTheme,
                            style = MaterialTheme.typography.headlineLarge,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (total > 0.0) {
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                // Le raccourci « le client paie exactement » : de loin le cas le
                                // plus fréquent, et le saisir à la main sur un total à cinq
                                // chiffres est la saisie la plus fautive de l'écran.
                                onClick = { onMontantRecuChange(total.toInt().toString()) },
                                modifier = Modifier.size(28.dp).pointerHoverIcon(PointerIcon.Hand)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    tint = couleurTheme,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                if (!panierVide) {
                    IconButton(
                        onClick = onViderPanier,
                        modifier = Modifier.size(36.dp).pointerHoverIcon(PointerIcon.Hand)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                ElevatedButton(
                    onClick = onValider,
                    enabled = !panierVide && validationPossible,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = couleurTheme,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .weight(1.8f)
                        .height(44.dp)
                        .pointerHoverIcon(PointerIcon.Hand)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircleOutline,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when (langue) {
                                "mg" -> "Hamarina"
                                "fr" -> "Valider"
                                else -> "Checkout"
                            },
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Dire POURQUOI le bouton est gris plutôt que de laisser le gérant cliquer dans le
            // vide : un bouton désactivé sans explication se lit comme une panne.
            if (messageValidationImpossible != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = messageValidationImpossible,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Le sélecteur de mode de paiement, identique à `PaymentModeRow` d'Android. */
@Composable
private fun RangeeModePaiement(
    selectionne: String,
    onSelectionner: (String) -> Unit,
    langue: String,
    couleurTheme: Color
) {
    val options = listOf(
        "ESPECES" to when (langue) {
            "mg" -> "Vola"
            "fr" -> "Espèces"
            else -> "Cash"
        },
        "MVOLA" to "Mvola",
        "ORANGE_MONEY" to "Orange Money"
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { (valeur, libelle) ->
            FilterChip(
                selected = selectionne == valeur,
                onClick = { onSelectionner(valeur) },
                label = {
                    Text(libelle, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = couleurTheme.copy(alpha = 0.18f),
                    selectedLabelColor = couleurTheme
                ),
                modifier = Modifier.weight(1f).height(30.dp).pointerHoverIcon(PointerIcon.Hand)
            )
        }
    }
}

/**
 * Le vert de « montant suffisant », repris tel quel de l'écran Android (section 64 : Material3 n'a
 * pas de token « succès », et les sections 62 à 67 ont volontairement laissé ces verts en dur).
 */
private val VERT_SUFFISANT = Color(0xFF2E7D32)
