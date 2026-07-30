package com.example.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.viewmodel.InventoryViewModel

/**
 * Sélecteur d'emplacement dans le plan d'étagères, partagé entre la fiche produit (choisir le
 * rayon d'un article) et l'écran Étagère (déplacer un article déjà rangé vers une autre case).
 * Regroupe les niveaux par étagère, avec une option « Aucun emplacement » facultative en tête. Si
 * aucune étagère n'existe encore, un message renvoie vers l'écran qui permet d'en créer plutôt que
 * d'afficher une liste vide sans explication.
 */
@Composable
fun EmplacementPickerDialog(
    plan: List<InventoryViewModel.EtagereAvecNiveaux>,
    niveauChoisiId: Long?,
    activeLang: String,
    autoriserAucun: Boolean = true,
    onChoisir: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = when (activeLang) {
                    "mg" -> "Misafidiana toerana amin'ny étagère"
                    "fr" -> "Choisir un emplacement"
                    else -> "Choose a location"
                },
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            if (plan.isEmpty()) {
                Text(
                    text = when (activeLang) {
                        "mg" -> "Mbola tsy misy étagère voarindra. Mankanesa any amin'ny Paramètres > Plan d'étagères mba hamorona."
                        "fr" -> "Aucune étagère n'a encore été créée. Rendez-vous dans Paramètres > Plan d'étagères pour en créer une."
                        else -> "No shelf has been created yet. Go to Settings > Shelf plan to create one."
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (autoriserAucun) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onChoisir(null) }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            RadioButton(selected = niveauChoisiId == null, onClick = { onChoisir(null) })
                            Text(
                                text = when (activeLang) {
                                    "mg" -> "Tsy misy toerana"
                                    "fr" -> "Aucun emplacement"
                                    else -> "No location"
                                },
                                fontWeight = FontWeight.Medium
                            )
                        }
                        HorizontalDivider()
                    }
                    plan.forEach { etagereAvecNiveaux ->
                        Text(
                            text = etagereAvecNiveaux.etagere.nom,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp, start = 8.dp, bottom = 2.dp)
                        )
                        if (etagereAvecNiveaux.niveaux.isEmpty()) {
                            Text(
                                text = when (activeLang) {
                                    "mg" -> "Tsy mbola misy rayon"
                                    "fr" -> "Aucun rayon pour l'instant"
                                    else -> "No shelf level yet"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        } else {
                            etagereAvecNiveaux.niveaux.sortedByDescending { it.niveau.position }.forEachIndexed { index, niveauAvecProduits ->
                                val nomNiveau = niveauAvecProduits.niveau.nom.ifBlank {
                                    val numero = etagereAvecNiveaux.niveaux.size - index
                                    when (activeLang) {
                                        "mg" -> "Rayon $numero"
                                        "fr" -> "Niveau $numero"
                                        else -> "Level $numero"
                                    }
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onChoisir(niveauAvecProduits.niveau.id) }
                                        .padding(vertical = 8.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    RadioButton(
                                        selected = niveauChoisiId == niveauAvecProduits.niveau.id,
                                        onClick = { onChoisir(niveauAvecProduits.niveau.id) }
                                    )
                                    Text(nomNiveau, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = when (activeLang) {
                        "mg" -> "Vita"
                        "fr" -> "Fermer"
                        else -> "Close"
                    }
                )
            }
        }
    )
}
