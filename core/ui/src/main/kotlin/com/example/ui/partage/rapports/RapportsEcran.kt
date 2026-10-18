package com.example.ui.partage.rapports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Product
import com.example.data.model.Retour
import com.example.data.model.Sale
import com.example.util.FormatUtil
import com.example.util.RapportFinancier

/**
 * L'écran Rapports, **porté depuis `DashboardScreen`** : puces de période, cartes de synthèse
 * arrondies à 14dp sur la couleur de leur indicateur à 10 %, graphique en barres dessiné au
 * `Canvas` (aucune bibliothèque de graphique, comme sur Android), puis le classement des meilleures
 * ventes avec sa barre de progression.
 *
 * **Aucun calcul n'est refait ici** : période, chiffre d'affaires, coût d'achat, bénéfice, taux de
 * marge, TVA, barres journalières et classement viennent tous de `RapportFinancier` (`:core:logic`),
 * la source unique partagée avec les deux écrans Android depuis la section 94. C'est précisément ce
 * que cette extraction devait permettre : le bureau n'a pas une deuxième comptabilité.
 *
 * **Les retours sont déduits comme sur le Tableau de bord** (section 28) et non laissés bruts comme
 * dans le « Kajy momba ny Vola » de l'Historique : cet écran est le portage du Tableau de bord, donc
 * il en reprend la convention — la divergence entre les deux écrans Android est documentée en
 * section 94 et reste à trancher par le gérant.
 *
 * **La largeur du contenu est bornée à 900dp et centrée.** Étirer des cartes de synthèse et un
 * graphique sur 2000 px donnerait trois chiffres perdus dans le vide, alors qu'un rapport se lit en
 * colonne. C'est la même règle que l'adaptation tablette de la section 21, portée à l'échelle d'un
 * écran de PC — et c'est l'inverse de la grille de produits, où la largeur sert à montrer PLUS
 * d'articles.
 */
@Composable
fun RapportsEcran(
    ventes: List<Sale>,
    retours: List<Retour>,
    produits: List<Product>,
    langue: String,
    couleurTheme: Color,
    modifier: Modifier = Modifier
) {
    var choixPeriode by remember { mutableStateOf(7) }

    val periode = remember(choixPeriode) { RapportFinancier.periode(choixPeriode) }
    val produitsParId = remember(produits) { produits.associateBy { it.id } }

    val ventesPeriode = remember(ventes, periode) { ventes.filter { it.timestamp >= periode.debut } }
    val retoursPeriode = remember(retours, periode) {
        retours.filter { it.timestamp >= periode.debut }
    }

    val totalRetours = remember(retoursPeriode) { retoursPeriode.sumOf { it.totalAmount } }
    val coutRetours = remember(retoursPeriode, produitsParId) {
        retoursPeriode.sumOf { RapportFinancier.coutMarchandises(it.items, produitsParId) }
    }
    val chiffreAffaires = remember(ventesPeriode, totalRetours) {
        RapportFinancier.chiffreAffaires(ventesPeriode) - totalRetours
    }
    val cout = remember(ventesPeriode, produitsParId, coutRetours) {
        RapportFinancier.coutDesVentes(ventesPeriode, produitsParId) - coutRetours
    }
    val benefice = chiffreAffaires - cout
    val tauxMarge = RapportFinancier.tauxMarge(benefice, cout)
    val taxe = remember(ventesPeriode) { RapportFinancier.taxeCollectee(ventesPeriode) }
    val parJour = remember(ventesPeriode, periode) {
        RapportFinancier.chiffreAffairesParJour(ventesPeriode, periode.debut, periode.nombreDeJours)
    }
    val meilleures = remember(ventesPeriode) { RapportFinancier.meilleuresVentes(ventesPeriode) }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 900.dp)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    7 to when (langue) {
                        "mg" -> "7 andro"
                        "fr" -> "7 jours"
                        else -> "7 days"
                    },
                    30 to when (langue) {
                        "mg" -> "30 andro"
                        "fr" -> "30 jours"
                        else -> "30 days"
                    },
                    RapportFinancier.CE_MOIS to when (langue) {
                        "mg" -> "Ity volana ity"
                        "fr" -> "Ce mois-ci"
                        else -> "This month"
                    }
                ).forEach { (valeur, libelle) ->
                    FilterChip(
                        selected = choixPeriode == valeur,
                        onClick = { choixPeriode = valeur },
                        label = { Text(libelle, fontSize = 12.sp) },
                        modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CarteSynthese(
                    modifier = Modifier.weight(1f),
                    libelle = when (langue) {
                        "mg" -> "Vola miditra"
                        "fr" -> "Chiffre d'affaires"
                        else -> "Revenue"
                    },
                    valeur = "${FormatUtil.formatPrice(chiffreAffaires)} Ar",
                    couleur = couleurTheme
                )
                CarteSynthese(
                    modifier = Modifier.weight(1f),
                    libelle = when (langue) {
                        "mg" -> "Tombom-barotra"
                        "fr" -> "Bénéfice net"
                        else -> "Net profit"
                    },
                    valeur = "${FormatUtil.formatPrice(benefice)} Ar",
                    couleur = if (benefice >= 0) VERT_POSITIF else ROUGE_NEGATIF
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            CarteSynthese(
                modifier = Modifier.fillMaxWidth(),
                libelle = when (langue) {
                    "mg" -> "Taham-bidiny"
                    "fr" -> "Taux de marge"
                    else -> "Margin rate"
                },
                valeur = "${FormatUtil.formatPrice(tauxMarge)} %",
                couleur = when {
                    tauxMarge < 0 -> ROUGE_NEGATIF
                    tauxMarge < RapportFinancier.SEUIL_MARGE_FAIBLE -> ORANGE_FAIBLE
                    else -> VERT_POSITIF
                }
            )

            if (taxe > 0.0) {
                Spacer(modifier = Modifier.height(10.dp))
                CarteSynthese(
                    modifier = Modifier.fillMaxWidth(),
                    libelle = when (langue) {
                        "mg" -> "TVA (efa tafiditra ao anaty vola miditra)"
                        "fr" -> "TVA collectée (incluse dans le CA)"
                        else -> "VAT collected (included in revenue)"
                    },
                    valeur = "${FormatUtil.formatPrice(taxe)} Ar",
                    couleur = VIOLET_TAXE
                )
            }

            if (totalRetours > 0.0) {
                Spacer(modifier = Modifier.height(10.dp))
                CarteSynthese(
                    modifier = Modifier.fillMaxWidth(),
                    libelle = when (langue) {
                        "mg" -> "Vidin'ny fanavereana (${retoursPeriode.size})"
                        "fr" -> "Retours/remboursements (${retoursPeriode.size})"
                        else -> "Returns/refunds (${retoursPeriode.size})"
                    },
                    valeur = "-${FormatUtil.formatPrice(totalRetours)} Ar",
                    couleur = ROUGE_NEGATIF
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = when (langue) {
                            "mg" -> "Vola miditra isan'andro"
                            "fr" -> "Chiffre d'affaires par jour"
                            else -> "Daily revenue"
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    GraphiqueBarres(valeurs = parJour, couleurBarre = couleurTheme)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = when (langue) {
                    "mg" -> "Entana be mividy indrindra"
                    "fr" -> "Produits les plus vendus"
                    else -> "Best-selling products"
                },
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (meilleures.isEmpty()) {
                Text(
                    text = when (langue) {
                        "mg" -> "Mbola tsy misy varotra tamin'ity vanim-potoana ity."
                        "fr" -> "Aucune vente sur cette période."
                        else -> "No sale in this period."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                // Le maximum est borné à 1 pour que la barre ne divise jamais par zéro : une
                // période dont toutes les ventes sont à 0 Ar existe (lignes « divers » annulées).
                val maximum = meilleures.maxOf { it.chiffreAffaires }.coerceAtLeast(1.0)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    meilleures.forEachIndexed { index, article ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${index + 1}. ${article.nom}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "${FormatUtil.formatPrice(article.chiffreAffaires)} Ar",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        color = couleurTheme
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    // Surcharge à lambda et non `progress: Float` : cette dernière
                                    // est dépréciée depuis Material3 1.6, et `:core:ui` compile
                                    // contre 1.8.2 (épinglé par Compose Multiplatform 1.9).
                                    progress = { (article.chiffreAffaires / maximum).toFloat() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = couleurTheme,
                                    trackColor = couleurTheme.copy(alpha = 0.12f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${FormatUtil.formatQty(article.quantite, "").trim()} " +
                                        when (langue) {
                                            "mg" -> "voavidy"
                                            "fr" -> "vendu(s)"
                                            else -> "sold"
                                        },
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** La carte de synthèse, identique à `SummaryStatCard` d'Android. */
@Composable
private fun CarteSynthese(
    libelle: String,
    valeur: String,
    couleur: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = couleur.copy(alpha = 0.10f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = libelle.uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = couleur
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = valeur,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = couleur,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Le graphique en barres, identique à `RevenueBarChart` d'Android : un `Canvas` et rien d'autre,
 * aucune bibliothèque de graphique ajoutée.
 *
 * Un jour sans vente garde sa barre, en couleur très atténuée plutôt qu'absente : un trou ferait
 * croire à une donnée manquante là où il n'y a simplement pas eu de vente.
 */
@Composable
private fun GraphiqueBarres(valeurs: List<Double>, couleurBarre: Color) {
    val maximum = (valeurs.maxOrNull() ?: 0.0).coerceAtLeast(1.0)
    Canvas(modifier = Modifier.fillMaxWidth().height(140.dp)) {
        val nombre = valeurs.size
        if (nombre == 0) return@Canvas
        val ecart = 2.dp.toPx()
        val largeur = ((size.width - ecart * (nombre - 1)) / nombre).coerceAtLeast(1f)
        valeurs.forEachIndexed { index, valeur ->
            val ratio = (valeur / maximum).toFloat().coerceIn(0f, 1f)
            val hauteur = (ratio * size.height).coerceAtLeast(if (valeur > 0) 2f else 0f)
            drawRect(
                color = if (valeur > 0) couleurBarre else couleurBarre.copy(alpha = 0.15f),
                topLeft = Offset(index * (largeur + ecart), size.height - hauteur),
                size = Size(largeur, hauteur.coerceAtLeast(1f))
            )
        }
    }
}

/**
 * Les trois couleurs d'état du tableau de bord Android, reprises telles quelles : vert positif,
 * rouge négatif, orange de marge faible (seuil 15 %), plus le violet de la TVA. Les sections 62 à
 * 67 les ont volontairement laissées en dur — Material3 n'a de token ni « succès » ni
 * « avertissement », et migrer une seule branche d'un feu tricolore le casserait.
 */
private val VERT_POSITIF = Color(0xFF2E7D32)
private val ROUGE_NEGATIF = Color(0xFFC62828)
private val ORANGE_FAIBLE = Color(0xFFF57C00)
private val VIOLET_TAXE = Color(0xFF6A1B9A)
