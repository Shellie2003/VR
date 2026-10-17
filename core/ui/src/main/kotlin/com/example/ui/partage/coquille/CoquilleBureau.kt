package com.example.ui.partage.coquille

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * La structure de la fenêtre de bureau : un menu latéral permanent à gauche, le contenu à droite.
 *
 * **Un menu latéral et non la barre du bas d'Android.** Ce n'est pas une préférence esthétique :
 * un écran de bureau est large et peu haut, donc la hauteur est la ressource rare et la largeur
 * abondante — exactement l'inverse d'un téléphone. Un menu permanent coûte 220 px de large, qu'on
 * a, et rend toutes les sections atteignables en un clic sans rien replier.
 *
 * L'état est entièrement remonté (`sectionActive` / `onSectionChange`) : aucune mémoire interne,
 * aucun ViewModel. C'est ce qui rend cette coquille déplaçable telle quelle dans un module partagé
 * avec Android le jour où la montée de version de Compose côté `:app` sera traitée.
 */
@Composable
fun CoquilleBureau(
    nomApplication: String,
    sousTitre: String,
    sectionActive: SectionBureau,
    onSectionChange: (SectionBureau) -> Unit,
    langue: String,
    modifier: Modifier = Modifier,
    piedMenu: @Composable () -> Unit = {},
    contenu: @Composable (SectionBureau) -> Unit
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Row(modifier = Modifier.fillMaxSize()) {
            MenuLateral(
                nomApplication = nomApplication,
                sousTitre = sousTitre,
                sectionActive = sectionActive,
                onSectionChange = onSectionChange,
                langue = langue,
                piedMenu = piedMenu
            )
            Box(modifier = Modifier.fillMaxSize()) {
                contenu(sectionActive)
            }
        }
    }
}

@Composable
private fun MenuLateral(
    nomApplication: String,
    sousTitre: String,
    sectionActive: SectionBureau,
    onSectionChange: (SectionBureau) -> Unit,
    langue: String,
    piedMenu: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .width(240.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.primary)
            .padding(16.dp)
    ) {
        Text(
            text = nomApplication,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary
        )
        Text(
            text = sousTitre,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
        )

        Spacer(Modifier.height(24.dp))

        SectionBureau.entries.forEach { section ->
            EntreeMenu(
                libelle = section.libelle(langue),
                active = section == sectionActive,
                disponible = section.disponible,
                langue = langue,
                onClick = { onSectionChange(section) }
            )
            Spacer(Modifier.height(4.dp))
        }

        // Les réglages en bas du menu latéral, et non dans un écran à part : c'est la convention
        // des logiciels de bureau, et surtout la place qui reste disponible sans rien replier.
        Spacer(Modifier.weight(1f))
        piedMenu()
    }
}

@Composable
private fun EntreeMenu(
    libelle: String,
    active: Boolean,
    disponible: Boolean,
    langue: String,
    onClick: () -> Unit
) {
    val couleurTexte = when {
        active -> MaterialTheme.colorScheme.primary
        // Une section à venir reste cliquable — elle explique alors ce qu'elle attend. La griser
        // sans rien dire laisserait l'utilisateur deviner si c'est une panne ou un chantier.
        disponible -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.55f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = libelle,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
            color = couleurTexte
        )
        if (!disponible) {
            Text(
                text = when (langue) {
                    "mg" -> "ho avy"
                    "fr" -> "à venir"
                    else -> "soon"
                },
                style = MaterialTheme.typography.labelSmall,
                color = couleurTexte
            )
        }
    }
}

/**
 * Le contenu d'une section qui n'est pas encore servie.
 *
 * Elle dit ce qui manque — la base de données partagée — plutôt que « bientôt disponible » : c'est
 * ce qui distingue un chantier annoncé d'une fonctionnalité qui semble cassée.
 */
@Composable
fun SectionAVenir(titre: String, langue: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(40.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = titre,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = when (langue) {
                "mg" -> "Mbola miandry ny fampitoviana ny tahiry (base de données) " +
                    "amin'ny finday ity pejy ity."
                "fr" -> "Cette section attend le partage de la base de données avec le téléphone. " +
                    "Le catalogue, lui, fonctionne déjà avec le code de calcul commun."
                else -> "This section is waiting for the database to be shared with the phone. " +
                    "The catalogue already runs on the shared calculation code."
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
