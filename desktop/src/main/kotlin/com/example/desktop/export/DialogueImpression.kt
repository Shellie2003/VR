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
 * Ce que le gérant lit quand une impression n'a pas abouti.
 *
 * ## Trois cas seulement, et c'est délibéré
 *
 * Une impression **lancée** n'a rien à annoncer : la boîte de dialogue du système vient de
 * confirmer, et le papier sort. Une impression **annulée** non plus — le gérant vient de fermer
 * cette boîte lui-même, lui opposer un message serait lui réexpliquer son propre geste. Ne restent
 * donc que les trois situations où il doit faire quelque chose, et elles sont séparées pour la
 * même raison qu'à la section 56 : un message unique ferait chercher une panne d'imprimante alors
 * que la liste affichée était simplement vide.
 */
@Composable
fun DialogueImpression(
    resultat: ResultatImpression,
    langue: String,
    onFermer: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onFermer,
        title = {
            Text(
                text = when (resultat) {
                    ResultatImpression.ListeVide -> when (langue) {
                        "mg" -> "Tsy misy na inona na inona atontaina"
                        "fr" -> "Rien à imprimer"
                        else -> "Nothing to print"
                    }
                    ResultatImpression.AucuneImprimante -> when (langue) {
                        "mg" -> "Tsy misy printy"
                        "fr" -> "Aucune imprimante"
                        else -> "No printer"
                    }
                    else -> when (langue) {
                        "mg" -> "Tsy nahomby ny fanontana"
                        "fr" -> "Impression impossible"
                        else -> "Printing failed"
                    }
                },
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = when (resultat) {
                        ResultatImpression.ListeVide -> when (langue) {
                            "mg" -> "Tsy misy andalana ao amin'ny lisitra asehon'ny efijery. " +
                                "Hamarino ny fitadiavana na ny daty voafantina."
                            "fr" -> "La liste affichée ne contient aucune ligne. " +
                                "Vérifiez la recherche ou la date sélectionnée."
                            else -> "The displayed list has no rows. " +
                                "Check the search or the selected date."
                        }
                        ResultatImpression.AucuneImprimante -> when (langue) {
                            // Le conseil le plus utile en pratique : sous Windows, « Microsoft
                            // Print to PDF » suffit et est installé d'office — il n'y a donc
                            // presque jamais de vraie raison de rester bloqué ici.
                            "mg" -> "Tsy misy printy voasoratra amin'ity solosaina ity. " +
                                "Azo ampiasaina ny « Microsoft Print to PDF » raha tsy misy " +
                                "printy marina."
                            "fr" -> "Aucune imprimante n'est déclarée sur ce PC. " +
                                "À défaut d'imprimante réelle, « Microsoft Print to PDF » " +
                                "permet d'obtenir un fichier PDF."
                            else -> "No printer is configured on this PC. " +
                                "With no real printer, “Microsoft Print to PDF” produces a " +
                                "PDF file instead."
                        }
                        else -> when (langue) {
                            "mg" -> "Tsy afaka nandefa ny fanontana."
                            "fr" -> "Le travail d'impression n'a pas pu être envoyé."
                            else -> "The print job could not be sent."
                        }
                    },
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val motif = (resultat as? ResultatImpression.Echec)?.motif
                if (motif != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = if (motif == ImpressionTableau.ERREUR_COMPOSANT_MANQUANT) {
                            // Même distinction qu'à la section 103 : dire « installation
                            // incomplète » plutôt que de laisser croire à une panne
                            // d'imprimante, sinon le fournisseur n'apprend jamais que son
                            // paquet est incomplet.
                            when (langue) {
                                "mg" -> "Tsy feno ity fametrahana ity. Lazao amin'ny mpamatsy."
                                "fr" -> "Cette installation est incomplète. " +
                                    "Signalez-le au fournisseur."
                                else -> "This installation is incomplete. Report it to the supplier."
                            }
                        } else {
                            motif
                        },
                        fontSize = 12.sp,
                        fontFamily = if (motif == ImpressionTableau.ERREUR_COMPOSANT_MANQUANT) {
                            FontFamily.Default
                        } else {
                            // Le motif technique en police à largeur fixe : c'est ce que le gérant
                            // lit au téléphone au fournisseur, et une police proportionnelle y
                            // confond l, 1 et I.
                            FontFamily.Monospace
                        },
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        confirmButton = {
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
    )
}
