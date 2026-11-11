package com.example.ui.partage.composants

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Le bouton d'impression d'une liste, posé à côté du bouton d'export.
 *
 * ## Pourquoi un bouton d'impression alors que l'export Excel existe
 *
 * L'export CSV **suppose qu'Excel ou LibreOffice soit installé**. Sur le PC nu d'une épicerie, un
 * `.csv` s'ouvre dans le Bloc-notes : colonnes collées, accents cassés, impression illisible. Or
 * les deux usages les plus concrets d'un PC en boutique sont justement du papier — la liste de
 * stock qu'on emporte dans les rayons pour compter, et la liste des trosa qu'on garde sous la main
 * pour réclamer.
 *
 * Et comme Windows installe d'office « Microsoft Print to PDF », ce bouton donne **aussi** le PDF
 * que la section 105 déclarait hors de portée, sans bibliothèque ni ligne de plus.
 *
 * ## Deux boutons et non un menu
 *
 * Un menu « Excel / Imprimer » garderait l'en-tête plus propre mais coûterait un clic de plus à
 * chaque fois. Sur un écran de caisse, le clic compte davantage que la sobriété — et « Imprimer »
 * est un mot que personne n'a besoin de déchiffrer.
 *
 * Comme pour `BoutonExport`, c'est l'écran qui reçoit un `onImprimer` **nullable** et n'affiche pas
 * ce bouton quand il vaut `null` : ce module ne sait pas imprimer (aucune dépendance de
 * plateforme), et un bouton qui ne mène nulle part se lit comme une panne.
 */
@Composable
fun BoutonImprimer(
    onClick: () -> Unit,
    langue: String,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.pointerHoverIcon(PointerIcon.Hand)
    ) {
        Icon(
            imageVector = Icons.Default.Print,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.size(6.dp))
        Text(
            text = when (langue) {
                "mg" -> "Atontao"
                "fr" -> "Imprimer"
                else -> "Print"
            },
            fontSize = 12.sp
        )
    }
}
