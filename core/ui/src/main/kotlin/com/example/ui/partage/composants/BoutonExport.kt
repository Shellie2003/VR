package com.example.ui.partage.composants

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
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
 * Le bouton d'export d'une liste, posé dans l'en-tête des écrans qui en ont un.
 *
 * ## Pourquoi un composant et non quatre boutons
 *
 * Il sert quatre écrans (stock, historique, dettes, mouvements de caisse). Écrit quatre fois, son
 * libellé aurait fini par différer d'un écran à l'autre — et la section 99 a déjà montré ce que
 * coûte un vocabulaire qui dérive : le gérant ne reconnaît plus la même fonction.
 *
 * ## `onClick` nullable chez l'appelant, pas ici
 *
 * Ce module ne sait pas écrire de fichier (il n'a aucune dépendance de plateforme, c'est ce qui le
 * rend partageable avec Android le jour où la contrainte de version de Compose tombera). C'est
 * l'écran qui reçoit un `onExporter` **nullable** et qui n'affiche pas ce bouton quand il vaut
 * `null` : un bouton qui ne mène nulle part se lit comme une panne, règle déjà appliquée au crayon
 * d'édition de l'écran Stock.
 */
@Composable
fun BoutonExport(
    onClick: () -> Unit,
    langue: String,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.pointerHoverIcon(PointerIcon.Hand)
    ) {
        Icon(
            imageVector = Icons.Default.FileDownload,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.size(6.dp))
        Text(
            text = when (langue) {
                // « Excel » et non « CSV » : c'est le nom du logiciel que le gérant ouvrira, et le
                // fichier s'y ouvre d'un double-clic. « CSV » ne dirait rien à personne ici.
                "mg" -> "Alaina (Excel)"
                "fr" -> "Exporter (Excel)"
                else -> "Export (Excel)"
            },
            fontSize = 12.sp
        )
    }
}
