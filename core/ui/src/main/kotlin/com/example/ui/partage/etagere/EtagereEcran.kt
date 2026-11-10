package com.example.ui.partage.etagere

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.EtagereAvecNiveaux
import com.example.data.model.NiveauAvecProduits
import com.example.util.EtagereColors
import com.example.util.RechercheEtagere
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Le plan d'étagères, **porté depuis `EtagereScreen`** : rangée horizontale d'étagères de 168dp,
 * chacune un en-tête teinté de sa couleur à 18 % puis ses niveaux empilés en cases de 84dp
 * arrondies à 10dp, avec la planche brune de 4dp au bas de chaque case et le compteur de
 * références au-dessus.
 *
 * **La recherche « où est rangé ce produit ? » passe par [RechercheEtagere]**, la fonction pure
 * déjà écrite pour Android (section 72). Un clic sur un résultat fait défiler jusqu'à l'étagère et
 * **souligne la case** pendant trois secondes : arriver sur la bonne étagère ne suffit pas, il faut
 * encore distinguer LA case — c'est exactement le raisonnement du téléphone.
 *
 * **Le décalage d'index est traité ici, et pas dans la fonction pure.** `indexEtagere` est le rang
 * dans le plan, pas un identifiant. Le téléphone ajoute +1 parce que sa `LazyRow` commence par un
 * bouton « ajouter à gauche » ; le bureau est en lecture seule, donc il n'a pas ce bouton et
 * l'index s'utilise **tel quel**. C'est précisément pourquoi ce décalage n'appartient pas à
 * [RechercheEtagere] : il décrit une mise en page, et les deux mises en page diffèrent.
 *
 * Aucun ajout, renommage ni déplacement : le bureau est en lecture seule. Le rangement se fait
 * depuis le téléphone, où l'on est debout devant le rayon.
 */
@Composable
fun EtagereEcran(
    plan: List<EtagereAvecNiveaux>,
    langue: String,
    focusRecherche: FocusRequester? = null,
    modifier: Modifier = Modifier
) {
    var requete by remember { mutableStateOf("") }
    var niveauSouligne by remember { mutableStateOf<Long?>(null) }
    val etatRangee = rememberLazyListState()
    val portee = rememberCoroutineScope()

    val resultats = remember(plan, requete) { RechercheEtagere.chercher(plan, requete) }

    // Le soulignement s'efface de lui-même : rien à nettoyer côté appelant, et un cadre qui
    // resterait allumé perdrait tout son sens dès la deuxième recherche.
    LaunchedEffect(niveauSouligne) {
        if (niveauSouligne != null) {
            delay(3000)
            niveauSouligne = null
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        OutlinedTextField(
            value = requete,
            onValueChange = { requete = it },
            placeholder = {
                Text(
                    when (langue) {
                        "mg" -> "Aiza ny entana ? (anarana na kaody)"
                        "fr" -> "Où est rangé cet article ? (nom ou code-barres)"
                        else -> "Where is this item? (name or barcode)"
                    }
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline
                )
            },
            trailingIcon = {
                if (requete.isNotEmpty()) {
                    IconButton(onClick = { requete = "" }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = null)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .then(focusRecherche?.let { Modifier.focusRequester(it) } ?: Modifier),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        if (requete.isNotBlank()) {
            if (resultats.isEmpty()) {
                Text(
                    text = when (langue) {
                        "mg" -> "Tsy hita ao amin'ny efitrano"
                        "fr" -> "Introuvable dans le plan d'étagères"
                        else -> "Not found in the shelf plan"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            } else {
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    resultats.take(6).forEach { emplacement ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .pointerHoverIcon(PointerIcon.Hand)
                                .clickable {
                                    niveauSouligne = emplacement.niveauId
                                    // L'index du plan s'utilise tel quel : pas de bouton d'ajout
                                    // en tête de rangée ici, contrairement au téléphone.
                                    portee.launch {
                                        etatRangee.animateScrollToItem(emplacement.indexEtagere)
                                    }
                                },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = emplacement.produitNom,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${emplacement.etagereNom} • ${emplacement.niveauNom}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        if (plan.isEmpty()) {
            Text(
                text = when (langue) {
                    "mg" -> "Mbola tsy misy efitrano voarakitra. Ao amin'ny finday no amboarina."
                    "fr" -> "Aucune étagère enregistrée. Le plan se construit depuis le téléphone."
                    else -> "No shelf recorded. The plan is built on the phone."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp)
            )
            return@Column
        }

        Column(modifier = Modifier.fillMaxSize()) {
            LazyRow(
                state = etatRangee,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(
                    plan,
                    key = { _, etagere -> etagere.etagere.id }
                ) { _, etagereAvecNiveaux ->
                    ColonneEtagere(
                        etagereAvecNiveaux = etagereAvecNiveaux,
                        niveauSouligne = niveauSouligne,
                        langue = langue
                    )
                }
            }
            // Ascenseur HORIZONTAL et non vertical : c'est la rangée d'étagères qui déborde, et
            // sur un PC rien n'indique qu'il en reste à droite avant d'avoir tenté la molette
            // (même raisonnement que l'ascenseur vertical de l'accueil, section 88).
            HorizontalScrollbar(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                adapter = rememberScrollbarAdapter(etatRangee)
            )
        }
    }
}

@Composable
private fun ColonneEtagere(
    etagereAvecNiveaux: EtagereAvecNiveaux,
    niveauSouligne: Long?,
    langue: String
) {
    val couleurEtagere = remember(etagereAvecNiveaux.etagere.couleur) {
        EtagereColors.versArgb(etagereAvecNiveaux.etagere.couleur)?.let { Color(it) }
    }

    Column(modifier = Modifier.width(LARGEUR_ETAGERE_DP.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(
                    couleurEtagere?.copy(alpha = 0.18f)
                        ?: MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                )
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Column {
                Text(
                    text = etagereAvecNiveaux.etagere.nom,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${etagereAvecNiveaux.niveaux.size} " + when (langue) {
                        "mg" -> "ambaratonga"
                        "fr" -> "niveaux"
                        else -> "levels"
                    },
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        etagereAvecNiveaux.niveaux.forEach { niveau ->
            CaseNiveau(
                niveauAvecProduits = niveau,
                souligne = niveau.niveau.id == niveauSouligne,
                langue = langue
            )
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun CaseNiveau(
    niveauAvecProduits: NiveauAvecProduits,
    souligne: Boolean,
    langue: String
) {
    val vide = niveauAvecProduits.produits.isEmpty()
    val epaisseur by animateDpAsState(if (souligne) 3.dp else 0.dp)
    val couleurNiveau = remember(niveauAvecProduits.niveau.couleur) {
        EtagereColors.versArgb(niveauAvecProduits.niveau.couleur)?.let { Color(it) }
    }
    val nomAffiche = niveauAvecProduits.niveau.nom.ifBlank {
        when (langue) {
            "mg" -> "Ambaratonga ${niveauAvecProduits.niveau.position}"
            "fr" -> "Niveau ${niveauAvecProduits.niveau.position}"
            else -> "Level ${niveauAvecProduits.niveau.position}"
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().height(HAUTEUR_NIVEAU_DP.dp),
        shape = RoundedCornerShape(10.dp),
        border = if (epaisseur > 0.dp) {
            BorderStroke(epaisseur, MaterialTheme.colorScheme.primary)
        } else {
            null
        },
        colors = CardDefaults.cardColors(
            containerColor = couleurNiveau?.copy(alpha = if (vide) 0.20f else 0.28f)
                ?: if (vide) {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                } else {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                }
        )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.weight(1f).padding(8.dp)) {
                Text(
                    text = nomAffiche,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (vide) {
                    Text(
                        text = when (langue) {
                            "mg" -> "Tsy misy entana"
                            "fr" -> "Case vide"
                            else -> "Empty slot"
                        },
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                } else {
                    Text(
                        text = niveauAvecProduits.produits.joinToString(", ") { it.name },
                        fontSize = 10.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            if (!vide) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "${niveauAvecProduits.produits.size} " + when (langue) {
                            "mg" -> "karazana"
                            "fr" -> "référence(s)"
                            else -> "product(s)"
                        },
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            // La planche stylisée du rayon, reprise telle quelle : un filet brun qui rappelle qu'il
            // s'agit d'une étagère physique. Laissé en dur, c'est un élément décoratif.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(Color(0xFF8D6E63).copy(alpha = 0.6f))
            )
        }
    }
}

private const val LARGEUR_ETAGERE_DP = 168
private const val HAUTEUR_NIVEAU_DP = 84
