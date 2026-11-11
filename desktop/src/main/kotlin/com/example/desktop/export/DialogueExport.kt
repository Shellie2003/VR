package com.example.desktop.export

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Ce que le gérant lit après un export.
 *
 * ## Pourquoi un dialogue et non un message discret
 *
 * La plainte la plus courante sur les exports est « je ne retrouve pas le fichier ». Un message
 * furtif en bas d'écran disparaît avant qu'on ait lu le chemin ; un dialogue reste jusqu'à ce que
 * le gérant l'ait fermé, et il porte le bouton qui ouvre le dossier — donc il n'a même pas à
 * retenir le chemin.
 *
 * ## Les trois issues sont séparées
 *
 * Même raisonnement qu'à la section 56 : un message unique ferait chercher un fichier qui n'a
 * jamais été écrit. « Rien à exporter » dit que le filtre ne retenait aucune ligne, ce qui est une
 * information utile et non une panne ; « échec » porte le motif technique, qui est la seule chose
 * exploitable si le gérant le signale.
 */
@Composable
fun DialogueExport(
    resultat: ResultatExport,
    langue: String,
    onFermer: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onFermer,
        title = {
            Text(
                text = when (resultat) {
                    is ResultatExport.Reussi -> when (langue) {
                        "mg" -> "Voatahiry ny rakitra"
                        "fr" -> "Fichier enregistré"
                        else -> "File saved"
                    }
                    ResultatExport.ListeVide -> when (langue) {
                        "mg" -> "Tsy misy na inona na inona alaina"
                        "fr" -> "Rien à exporter"
                        else -> "Nothing to export"
                    }
                    is ResultatExport.Echec -> when (langue) {
                        "mg" -> "Tsy nahomby ny fanalana"
                        "fr" -> "Export impossible"
                        else -> "Export failed"
                    }
                },
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = when (resultat) {
                        is ResultatExport.Reussi -> when (langue) {
                            "mg" -> "Azo sokafana amin'ny Excel avy hatrany ny rakitra."
                            "fr" -> "Le fichier s'ouvre directement dans Excel."
                            else -> "The file opens directly in Excel."
                        }
                        ResultatExport.ListeVide -> when (langue) {
                            "mg" -> "Tsy misy andalana ao amin'ny lisitra asehon'ny efijery. " +
                                "Hamarino ny fitadiavana na ny daty voafantina."
                            "fr" -> "La liste affichée ne contient aucune ligne. " +
                                "Vérifiez la recherche ou la date sélectionnée."
                            else -> "The displayed list has no rows. " +
                                "Check the search or the selected date."
                        }
                        is ResultatExport.Echec -> when (langue) {
                            "mg" -> "Tsy voasoratra ny rakitra."
                            "fr" -> "Le fichier n'a pas pu être écrit."
                            else -> "The file could not be written."
                        }
                    },
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // Le chemin — ou le motif d'échec — en police à largeur fixe : c'est ce que le
                // gérant recopie ou lit au téléphone au fournisseur, et une police proportionnelle
                // y confond facilement l l 1 I.
                val detail = when (resultat) {
                    is ResultatExport.Reussi -> resultat.chemin
                    is ResultatExport.Echec -> resultat.motif
                    ResultatExport.ListeVide -> null
                }
                if (detail != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = detail,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        confirmButton = {
            // « Ouvrir le dossier » n'apparaît qu'en cas de succès : proposer d'ouvrir un dossier
            // où rien n'a été écrit ferait conclure que le fichier a disparu.
            if (resultat is ResultatExport.Reussi) {
                TextButton(onClick = {
                    ExportFichier.ouvrirDossier()
                    onFermer()
                }) {
                    Text(
                        when (langue) {
                            "mg" -> "Sokafy ny lahatahiry"
                            "fr" -> "Ouvrir le dossier"
                            else -> "Open folder"
                        }
                    )
                }
            } else {
                TextButton(onClick = onFermer) {
                    Text(
                        when (langue) {
                            "mg" -> "Ekena"
                            "fr" -> "D'accord"
                            else -> "OK"
                        }
                    )
                }
            }
        },
        dismissButton = if (resultat is ResultatExport.Reussi) {
            {
                TextButton(onClick = onFermer) {
                    Text(
                        when (langue) {
                            "mg" -> "Akatona"
                            "fr" -> "Fermer"
                            else -> "Close"
                        }
                    )
                }
            }
        } else {
            null
        }
    )
}
