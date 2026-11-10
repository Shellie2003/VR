package com.example.ui.partage.historique

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Product
import com.example.data.model.Restock
import com.example.data.model.Retour
import com.example.data.model.Sale
import com.example.util.FiltreHistorique
import com.example.util.FormatUtil

/** L'onglet affiché par l'historique. */
enum class OngletHistorique { VENTES, REAPPROS }

/**
 * L'historique des transactions, **porté depuis `SalesHistoryScreen`** : titre et date, bascule
 * Ventes / Réappro en segments arrondis à 12dp sur `surfaceVariant`, deux cartes de synthèse de
 * 132dp avec leur icône en filigrane à 8 %, champ de recherche arrondi à 12dp, puis la liste.
 *
 * ## Deux écarts voulus avec le téléphone
 *
 * **Le choix du jour se fait par flèches, pas par calendrier.** Android ouvre un
 * `DatePickerDialog` derrière une icône de calendrier. Sur un PC, « la veille » et « le lendemain »
 * sont les deux gestes qu'on répète pour remonter une semaine, et deux clics valent mieux qu'un
 * dialogue à ouvrir, viser et fermer à chaque fois. Un bouton « Aujourd'hui » ramène au présent.
 * C'est une adaptation de disposition, pas un changement de design : la ligne, sa position et sa
 * typographie sont celles du téléphone.
 *
 * **La variation par rapport à la veille est RÉELLE.** L'écran Android affichait « +12% omaly »
 * **en dur** sous la recette du jour — un chiffre inventé, le même chaque jour, lu comme une
 * mesure. Elle passe des deux côtés par [FiltreHistorique.variationVeille], et quand la veille n'a
 * rien encaissé l'écran le dit au lieu d'annoncer une progression.
 *
 * Les suppressions et les retours ne sont pas offerts ici : le bureau est en lecture seule, et un
 * bouton qui ne mènerait nulle part se lit comme une panne (même règle que l'écran Stock).
 */
@Composable
fun HistoriqueEcran(
    ventes: List<Sale>,
    reappros: List<Restock>,
    retours: List<Retour>,
    produits: List<Product>,
    onglet: OngletHistorique,
    onOngletChange: (OngletHistorique) -> Unit,
    jourAffiche: Long,
    onJourChange: (Long) -> Unit,
    recherche: String,
    onRechercheChange: (String) -> Unit,
    dateLisible: (Long) -> String,
    heureLisible: (Long) -> String,
    estAujourdhui: Boolean,
    langue: String,
    themeColor: Color,
    // Même raison que les autres écrans : sur un PC, Ctrl+F doit donner le focus au champ de
    // l'écran qu'on consulte, sans déplacer l'utilisateur ailleurs.
    focusRecherche: FocusRequester? = null,
    modifier: Modifier = Modifier
) {
    val ventesDuJour = remember(ventes, jourAffiche) {
        FiltreHistorique.ventesDuJour(ventes, jourAffiche).sortedByDescending { it.timestamp }
    }
    val reapprosDuJour = remember(reappros, jourAffiche) {
        FiltreHistorique.reapprosDuJour(reappros, jourAffiche).sortedByDescending { it.timestamp }
    }
    val ventesVisibles = remember(ventesDuJour, recherche) {
        FiltreHistorique.chercherVentes(ventesDuJour, recherche)
    }
    val reapprosVisibles = remember(reapprosDuJour, recherche) {
        FiltreHistorique.chercherReappros(reapprosDuJour, recherche)
    }
    val recette = remember(ventesDuJour) { ventesDuJour.sumOf { it.totalAmount } }
    val coutReappros = remember(reapprosDuJour) { reapprosDuJour.sumOf { it.totalCostPrice } }
    val variation = remember(ventes, jourAffiche) {
        FiltreHistorique.variationVeille(ventes, jourAffiche)
    }
    val produitsParId = remember(produits) { produits.associateBy { it.id } }

    // Le montant déjà retourné sur chaque vente, pour le badge « Retourné / Retour partiel ».
    //
    // **Le garde `id != 0` n'est pas de la prudence, il évite un badge faux sur CHAQUE vente.** La
    // sauvegarde ne sérialise pas l'identifiant d'une vente (les ids sont locaux à un appareil, et
    // la fusion passe par des clés naturelles — section 30), donc toute vente lue depuis une
    // sauvegarde porte `id = 0`. Sans ce garde, un seul retour enregistré avec `saleId = 0` serait
    // attribué à l'intégralité de l'historique : le gérant lirait « Retourné » partout.
    //
    // Conséquence assumée : sur des données venues de la sauvegarde, le badge reste éteint. Il est
    // juste de ne rien annoncer quand on ne peut pas rattacher le retour à sa vente ; le montant
    // des retours reste visible, lui, dans l'écran Rapports, qui n'a pas besoin de ce lien.
    val retoursParVente = remember(retours) {
        retours.filter { it.saleId != 0 }
            .groupBy { it.saleId }
            .mapValues { (_, liste) -> liste.sumOf { it.totalAmount } }
    }

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = if (onglet == OngletHistorique.VENTES) {
                        when (langue) {
                            "mg" -> "Tantara Varotra"
                            "fr" -> "Historique Ventes"
                            else -> "Sales History"
                        }
                    } else {
                        when (langue) {
                            "mg" -> "Fampidirana Entana"
                            "fr" -> "Réapprovisionnements"
                            else -> "Restock History"
                        }
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    color = themeColor
                )
                Text(
                    text = dateLisible(jourAffiche),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = { onJourChange(jourAffiche - UN_JOUR_MS) },
                    modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = when (langue) {
                            "mg" -> "Omaly"
                            "fr" -> "Jour précédent"
                            else -> "Previous day"
                        },
                        tint = themeColor
                    )
                }
                OutlinedButton(
                    onClick = { onJourChange(System.currentTimeMillis()) },
                    enabled = !estAujourdhui,
                    modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(
                        text = when (langue) {
                            "mg" -> "Androany"
                            "fr" -> "Aujourd'hui"
                            else -> "Today"
                        },
                        fontSize = 12.sp
                    )
                }
                IconButton(
                    onClick = { onJourChange(jourAffiche + UN_JOUR_MS) },
                    enabled = !estAujourdhui,
                    modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = when (langue) {
                            "mg" -> "Ampitso"
                            "fr" -> "Jour suivant"
                            else -> "Next day"
                        },
                        tint = themeColor
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            OngletHistorique.entries.forEach { valeur ->
                val choisi = valeur == onglet
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (choisi) themeColor else Color.Transparent)
                        .pointerHoverIcon(PointerIcon.Hand)
                        .clickable { onOngletChange(valeur) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = libelleOnglet(valeur, langue),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (choisi) {
                            Color.White
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CarteSynthese(
                titre = if (onglet == OngletHistorique.VENTES) {
                    if (estAujourdhui) {
                        when (langue) {
                            "mg" -> "Vola maty androany"
                            "fr" -> "Recette d'aujourd'hui"
                            else -> "Today's Revenue"
                        }
                    } else {
                        when (langue) {
                            "mg" -> "Vola maty tamin'io daty io"
                            "fr" -> "Recette de ce jour"
                            else -> "Selected Day Revenue"
                        }
                    }
                } else {
                    when (langue) {
                        "mg" -> "Vola fampidirana"
                        "fr" -> "Achats de ce jour"
                        else -> "Restock Spend"
                    }
                },
                valeur = if (onglet == OngletHistorique.VENTES) {
                    "Ar ${FormatUtil.formatPrice(recette)}"
                } else {
                    "Ar ${FormatUtil.formatPrice(coutReappros)}"
                },
                couleurValeur = themeColor,
                legende = if (onglet == OngletHistorique.VENTES) {
                    variation?.let {
                        val signe = if (it >= 0) "+" else ""
                        "$signe${FormatUtil.formatPrice(it)} % " + when (langue) {
                            "mg" -> "noho ny omaly"
                            "fr" -> "vs la veille"
                            else -> "vs yesterday"
                        }
                    } ?: when (langue) {
                        "mg" -> "Tsy nisy varotra omaly"
                        "fr" -> "Aucune vente la veille"
                        else -> "No sales yesterday"
                    }
                } else {
                    null
                },
                icone = Icons.Default.Payments,
                modifier = Modifier.weight(1f)
            )
            CarteSynthese(
                titre = if (onglet == OngletHistorique.VENTES) {
                    when (langue) {
                        "mg" -> "Isan'ny varotra"
                        "fr" -> "Nombre de ventes"
                        else -> "Sales Count"
                    }
                } else {
                    when (langue) {
                        "mg" -> "Isan'ny fampidirana"
                        "fr" -> "Nbr approvisionnements"
                        else -> "Restocks Count"
                    }
                },
                valeur = if (onglet == OngletHistorique.VENTES) {
                    ventesDuJour.size.toString()
                } else {
                    reapprosDuJour.size.toString()
                },
                // Le même ambre que l'écran Android, laissé en dur pour la raison établie par les
                // sections 62 à 67 : c'est un accent décoratif, pas un rôle du thème.
                couleurValeur = Color(0xFFB45309),
                legende = dateLisible(jourAffiche),
                icone = if (onglet == OngletHistorique.VENTES) {
                    Icons.Default.ShoppingCart
                } else {
                    Icons.Default.Inventory
                },
                modifier = Modifier.weight(1f)
            )
        }

        OutlinedTextField(
            value = recherche,
            onValueChange = onRechercheChange,
            placeholder = {
                Text(
                    text = if (onglet == OngletHistorique.VENTES) {
                        when (langue) {
                            "mg" -> "Hitady varotra..."
                            "fr" -> "Rechercher une vente..."
                            else -> "Search sales..."
                        }
                    } else {
                        when (langue) {
                            "mg" -> "Hitady fampidirana..."
                            "fr" -> "Rechercher approvisionnement..."
                            else -> "Search restocks..."
                        }
                    },
                    fontSize = 14.sp
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
                if (recherche.isNotEmpty()) {
                    IconButton(onClick = { onRechercheChange("") }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = null)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .then(focusRecherche?.let { Modifier.focusRequester(it) } ?: Modifier),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        val vide = if (onglet == OngletHistorique.VENTES) {
            ventesVisibles.isEmpty()
        } else {
            reapprosVisibles.isEmpty()
        }
        if (vide) {
            Text(
                text = if (onglet == OngletHistorique.VENTES) {
                    when (langue) {
                        "mg" -> "Tsy nisy varotra tamin'io daty io."
                        "fr" -> "Aucune vente pour ce jour."
                        else -> "No sale on this day."
                    }
                } else {
                    when (langue) {
                        "mg" -> "Tsy nisy fampidirana tamin'io daty io."
                        "fr" -> "Aucun approvisionnement pour ce jour."
                        else -> "No restock on this day."
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 24.dp)
            )
            return@Column
        }

        Row(modifier = Modifier.fillMaxSize()) {
            val etatListe = rememberLazyListState()
            LazyColumn(
                state = etatListe,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // La clé inclut l'INDEX, et c'est nécessaire : les ventes lues depuis une
                // sauvegarde portent toutes `id = 0`, donc une clé bâtie sur l'id et
                // l'horodatage seuls serait dupliquée dès que deux ventes tombent dans la même
                // milliseconde — et une clé dupliquée fait lever `LazyColumn`.
                if (onglet == OngletHistorique.VENTES) {
                    itemsIndexed(
                        ventesVisibles,
                        key = { index, vente -> "v$index-${vente.id}-${vente.timestamp}" }
                    ) { _, vente ->
                        LigneVenteHistorique(
                            vente = vente,
                            produitsParId = produitsParId,
                            heureLisible = heureLisible,
                            langue = langue,
                            themeColor = themeColor,
                            montantRetourne = if (vente.id != 0) {
                                retoursParVente[vente.id] ?: 0.0
                            } else {
                                0.0
                            }
                        )
                    }
                } else {
                    itemsIndexed(
                        reapprosVisibles,
                        key = { index, reappro -> "r$index-${reappro.id}-${reappro.timestamp}" }
                    ) { _, reappro ->
                        LigneReapproHistorique(
                            reappro = reappro,
                            heureLisible = heureLisible,
                            langue = langue,
                            themeColor = themeColor
                        )
                    }
                }
            }
            VerticalScrollbar(
                modifier = Modifier.fillMaxHeight().padding(start = 8.dp),
                adapter = rememberScrollbarAdapter(etatListe)
            )
        }
    }
}

/** Une carte de synthèse de l'en-tête, reprise des deux cartes de 132dp de l'écran Android. */
@Composable
private fun CarteSynthese(
    titre: String,
    valeur: String,
    couleurValeur: Color,
    legende: String?,
    icone: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(132.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Icon(
                imageVector = icone,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f),
                modifier = Modifier.size(72.dp).align(Alignment.BottomEnd)
            )
            Column(
                modifier = Modifier.fillMaxSize().padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = titre,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = valeur,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = couleurValeur
                )
                Text(
                    text = legende ?: "",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

private fun libelleOnglet(onglet: OngletHistorique, langue: String): String = when (onglet) {
    OngletHistorique.VENTES -> when (langue) {
        "mg" -> "Varotra"
        "fr" -> "Ventes"
        else -> "Sales"
    }
    OngletHistorique.REAPPROS -> when (langue) {
        "mg" -> "Fampidirana"
        "fr" -> "Approvisionnements"
        else -> "Restocks"
    }
}

/**
 * Un jour en millisecondes, pour déplacer le curseur de date.
 *
 * **C'est volontairement une approximation, et elle est sans conséquence ici** : la valeur ne sert
 * qu'à viser « la veille » ou « le lendemain », après quoi [FiltreHistorique] recalcule les vraies
 * bornes par `Calendar`. Un décalage d'une heure au changement d'heure retomberait de toute façon
 * dans la bonne journée civile. C'est ce qui permet de garder ce module sans dépendance de
 * plateforme — le calcul des bornes, lui, n'est jamais fait en millisecondes (section 94).
 */
private const val UN_JOUR_MS = 24L * 60 * 60 * 1000
