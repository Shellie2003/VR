package com.example.ui.partage.accueil

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.ShoppingBasket
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * L'en-tête de l'accueil, **porté à l'identique** depuis `HomeHeaderContent` d'Android : pastille
 * de marque de 42dp arrondie à 12dp sur la couleur du thème avec l'icône panier, nom de l'épicerie
 * en `headlineMedium` `Black` 24sp, champ de recherche de 56dp arrondi à 24dp posé sur
 * `surfaceVariant` **sans bordure visible**, puis la ligne de section « Entana farany nampidirina »
 * avec son lien d'accent orange `0xFFF57C00`.
 *
 * `Icons.Rounded.ShoppingBasket` et non une autre icône de panier : `AGENTS.md` section 2 l'impose
 * explicitement pour que l'icône de la marque soit la même partout. C'est la raison d'être de la
 * dépendance aux icônes étendues dans le build de ce module.
 *
 * **Une seule adaptation au format PC, et c'est une suppression de doublon, pas un changement de
 * design :** le bouton Paramètres de l'en-tête est facultatif (`onOuvrirReglages`). Sur un
 * téléphone il est le seul accès aux réglages ; sur le bureau, le menu latéral les porte déjà en
 * permanence, donc le redessiner ici donnerait deux chemins vers le même endroit à 200 px l'un de
 * l'autre. Le paramètre existe pour que la forme de l'en-tête reste disponible telle quelle si un
 * futur écran en a besoin.
 */
@Composable
fun EnteteAccueil(
    nomEpicerie: String,
    couleurTheme: Color,
    recherche: String,
    onRechercheChange: (String) -> Unit,
    titreSection: String,
    libelleVoirTout: String,
    onVoirTout: () -> Unit,
    onOuvrirReglages: (() -> Unit)? = null,
    /**
     * Permet à l'appelant de donner le focus au champ de recherche. **Propre au bureau** : sur un
     * PC, un lecteur de code-barres USB se comporte comme un clavier — il tape les chiffres puis
     * valide. Sans un champ qui a déjà le focus, le scan part dans le vide. Sur un téléphone la
     * question ne se pose pas : la saisie passe par la caméra, pas par le clavier.
     */
    focusRequester: FocusRequester? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(66.dp)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(couleurTheme),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.ShoppingBasket,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = nomEpicerie,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = couleurTheme,
                    fontSize = 24.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 180.dp)
            )
        }

        if (onOuvrirReglages != null) {
            IconButton(
                onClick = onOuvrirReglages,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(couleurTheme.copy(alpha = 0.12f))
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Fikirakirana / Settings",
                    tint = couleurTheme,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }

    OutlinedTextField(
        value = recherche,
        onValueChange = onRechercheChange,
        placeholder = {
            Text(
                text = "Hitady Entana...",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        },
        trailingIcon = {
            if (recherche.isNotEmpty()) {
                IconButton(onClick = { onRechercheChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .then(
                if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier
            ),
        shape = RoundedCornerShape(24.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
        )
    )

    Spacer(modifier = Modifier.height(16.dp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = titreSection,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp
            )
        )

        Row(
            modifier = Modifier.clickable { onVoirTout() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = libelleVoirTout,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF57C00)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFFF57C00),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
