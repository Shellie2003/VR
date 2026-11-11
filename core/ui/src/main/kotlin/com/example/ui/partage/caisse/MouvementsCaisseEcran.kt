package com.example.ui.partage.caisse

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.partage.composants.BoutonExport
import com.example.data.model.CaisseSession
import com.example.data.model.MouvementCaisse
import com.example.util.FormatUtil

/**
 * Les mouvements d'espèces et la session de caisse, **portés depuis `CaisseMouvementsScreen`** :
 * carte de session sur `surfaceVariant` à 50 %, carte de solde pleine en couleur de thème à texte
 * blanc arrondie à 20dp, et les lignes de mouvement avec leur pastille ronde de 36dp, leur flèche
 * entrante ou sortante, et leur montant signé en vert ou rouge.
 *
 * ## Ce que cet écran NE PEUT PAS afficher, et il le dit
 *
 * Le téléphone montre un **solde de caisse théorique** : fond de départ + ventes en espèces
 * + entrées manuelles − sorties manuelles (section 23). Le bureau ne peut pas le calculer, et ce
 * n'est pas une paresse : la sauvegarde ne porte **pas le mode de paiement** des ventes. Le modèle
 * `Sale` n'a jamais eu ce champ — c'est la `Vente` relationnelle qui le porte (section 19), et elle
 * ne voyage pas dans la sauvegarde.
 *
 * Afficher malgré tout un total en additionnant **toutes** les ventes gonflerait le solde de chaque
 * vente Mvola ou Orange Money — exactement le défaut que la section 23 avait eu à corriger côté
 * Android. L'écran affiche donc le **net des mouvements manuels**, qui est exact, et nomme
 * explicitement ce qui n'y est pas. Un chiffre juste et borné vaut mieux qu'un chiffre complet et
 * faux.
 *
 * Aucun bouton d'ouverture, de fermeture ni de saisie : le bureau est en lecture seule, et un
 * bouton qui ne mène nulle part se lit comme une panne.
 */
@Composable
fun MouvementsCaisseEcran(
    mouvements: List<MouvementCaisse>,
    sessions: List<CaisseSession>,
    dateHeureLisible: (Long) -> String,
    langue: String,
    themeColor: Color,
    onExporter: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val triees = remember(mouvements) { mouvements.sortedByDescending { it.date } }
    val totalEntrees = remember(mouvements) {
        mouvements.filter { it.type == TYPE_ENTREE }.sumOf { it.montant }
    }
    val totalSorties = remember(mouvements) {
        mouvements.filter { it.type == TYPE_SORTIE }.sumOf { it.montant }
    }
    val sessionOuverte = remember(sessions) { sessions.firstOrNull { it.isOpen } }
    val derniereFermee = remember(sessions) {
        sessions.filter { !it.isOpen }.maxByOrNull { it.dateFermeture ?: 0L }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Column(modifier = Modifier.widthIn(max = 760.dp)) {
            CarteSession(
                sessionOuverte = sessionOuverte,
                derniereFermee = derniereFermee,
                dateHeureLisible = dateHeureLisible,
                langue = langue
            )

            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = themeColor)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = when (langue) {
                            "mg" -> "VOLA NIDITRA SY NIVOAKA (NET)"
                            "fr" -> "MOUVEMENTS D'ESPÈCES (NET)"
                            else -> "CASH MOVEMENTS (NET)"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Text(
                        text = "${FormatUtil.formatPrice(totalEntrees - totalSorties)} Ar",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        TotalSigne(
                            libelle = when (langue) {
                                "mg" -> "Niditra"
                                "fr" -> "Entrées"
                                else -> "Cash in"
                            },
                            montant = totalEntrees,
                            signe = "+"
                        )
                        TotalSigne(
                            libelle = when (langue) {
                                "mg" -> "Nivoaka"
                                "fr" -> "Sorties"
                                else -> "Cash out"
                            },
                            montant = totalSorties,
                            signe = "−"
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = when (langue) {
                                "mg" -> "Tsy tafiditra ny varotra : tsy entin'ny tahiry ny fomba " +
                                    "fandoavana."
                                "fr" -> "Les ventes ne sont pas comptées ici : la sauvegarde ne " +
                                    "porte pas le mode de paiement."
                                else -> "Sales are not counted here: the backup does not carry " +
                                    "the payment method."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }

        if (onExporter != null && triees.isNotEmpty()) {
            BoutonExport(
                onClick = onExporter,
                langue = langue,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        if (triees.isEmpty()) {
            Text(
                text = when (langue) {
                    "mg" -> "Mbola tsy nisy fihetsika vola."
                    "fr" -> "Aucun mouvement de caisse enregistré."
                    else -> "No cash movement recorded."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp)
            )
            return@Column
        }

        Row(modifier = Modifier.fillMaxSize()) {
            val etat = rememberLazyListState()
            LazyColumn(
                state = etat,
                modifier = Modifier.weight(1f).widthIn(max = 760.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Index dans la clé : la sauvegarde ne porte pas l'id d'un mouvement, donc deux
                // sorties de même motif à la même seconde donneraient une clé dupliquée, ce qui
                // fait lever `LazyColumn`.
                itemsIndexed(
                    triees,
                    key = { index, mvt -> "$index-${mvt.id}-${mvt.date}" }
                ) { _, mouvement ->
                    LigneMouvementCaisse(
                        mouvement = mouvement,
                        dateHeureLisible = dateHeureLisible
                    )
                }
            }
            VerticalScrollbar(
                modifier = Modifier.fillMaxHeight().padding(start = 8.dp),
                adapter = rememberScrollbarAdapter(etat)
            )
        }
    }
}

@Composable
private fun TotalSigne(libelle: String, montant: Double, signe: String) {
    Column {
        Text(
            text = libelle,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.75f)
        )
        Text(
            text = "$signe${FormatUtil.formatPrice(montant)} Ar",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun CarteSession(
    sessionOuverte: CaisseSession?,
    derniereFermee: CaisseSession?,
    dateHeureLisible: (Long) -> String,
    langue: String
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = when (langue) {
                    "mg" -> "FIZARANA ASA (KESTY)"
                    "fr" -> "SESSION DE CAISSE"
                    else -> "CASH SESSION"
                },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))

            if (sessionOuverte == null) {
                Text(
                    text = when (langue) {
                        "mg" -> "Mbola tsy voasokatra ny kesty."
                        "fr" -> "Aucune session ouverte."
                        else -> "No open session."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = when (langue) {
                        "mg" -> "Nosokafana tamin'ny ${dateHeureLisible(sessionOuverte.dateOuverture)}"
                        "fr" -> "Ouverte depuis le ${dateHeureLisible(sessionOuverte.dateOuverture)}"
                        else -> "Opened since ${dateHeureLisible(sessionOuverte.dateOuverture)}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = when (langue) {
                        "mg" -> "Vola fanombohana"
                        "fr" -> "Fond de départ"
                        else -> "Starting float"
                    } + " : ${FormatUtil.formatPrice(sessionOuverte.montantOuverture)} Ar",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (derniereFermee != null) {
                val ecart = derniereFermee.ecart ?: 0.0
                // Le même feu tricolore que l'écran Android, laissé en dur pour la raison des
                // sections 64 à 67 : n'en migrer qu'une branche casserait le jeu de couleurs.
                val couleurEcart = when {
                    kotlin.math.abs(ecart) < 1.0 -> Color(0xFF2E7D32)
                    ecart > 0 -> Color(0xFFF57C00)
                    else -> Color(0xFFC62828)
                }
                Spacer(Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Text(
                    text = when (langue) {
                        "mg" -> "Fizarana farany nohidiana"
                        "fr" -> "Dernière session fermée"
                        else -> "Last closed session"
                    } + " (${dateHeureLisible(derniereFermee.dateFermeture ?: 0L)})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = when (langue) {
                        "mg" -> "Fahasamihafana"
                        "fr" -> "Écart"
                        else -> "Gap"
                    } + " : ${if (ecart >= 0) "+" else ""}${FormatUtil.formatPrice(ecart)} Ar",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = couleurEcart
                )
            }
        }
    }
}

@Composable
private fun LigneMouvementCaisse(
    mouvement: MouvementCaisse,
    dateHeureLisible: (Long) -> String
) {
    val estEntree = mouvement.type == TYPE_ENTREE
    val accent = if (estEntree) Color(0xFF2E7D32) else Color(0xFFC62828)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (estEntree) {
                        Icons.Default.ArrowDownward
                    } else {
                        Icons.Default.ArrowUpward
                    },
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mouvement.motif,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = dateHeureLisible(mouvement.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (mouvement.note.isNotEmpty()) {
                    Text(
                        text = mouvement.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                val auteur = listOf(mouvement.vendeurNom, mouvement.deviceName)
                    .filter { it.isNotBlank() }
                    .joinToString(" • ")
                if (auteur.isNotBlank()) {
                    Text(
                        text = auteur,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            Text(
                text = "${if (estEntree) "+" else "-"}${FormatUtil.formatPrice(mouvement.montant)} Ar",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = accent
            )
        }
    }
}

private const val TYPE_ENTREE = "ENTREE"
private const val TYPE_SORTIE = "SORTIE"
