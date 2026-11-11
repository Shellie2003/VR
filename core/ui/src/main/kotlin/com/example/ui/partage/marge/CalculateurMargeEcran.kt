package com.example.ui.partage.marge

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.CalculMarge
import com.example.util.FormatUtil

/**
 * Le calculateur de marges, **porté depuis l'onglet « Calculateur » de `CommissionScreen`** :
 * même carte d'astuces à ampoule ambre arrondie à 12dp, mêmes champs arrondis à 12dp avec leur
 * suffixe « Ar » en gras, même panneau d'analyse arrondi à 16dp sur `themeColor` à 5 % avec sa
 * bordure à 15 %, même badge de santé de marge arrondi à 8dp en 11sp, mêmes lignes de calcul
 * (libellé 13sp `Medium` en `onSurfaceVariant` à gauche, valeur 13sp `Bold`/`Black` alignée à
 * droite, poids 1.3 contre 1).
 *
 * ## Ce que cet écran fait, et ce qu'il ne fait pas
 *
 * Le **calcul** est réel et vient de `CalculMarge` (`:core:logic`) : exactement celui du
 * téléphone, aux mêmes arrondis près. En revanche, l'écran Android se termine par un bouton
 * « Valider le réapprovisionnement » qui **écrit** en base (stock, prix d'achat, fournisseur) —
 * impossible en lecture seule, donc absent ici plutôt que grisé : le calculateur reste utile à
 * lui seul, et un bouton qui prétendrait enregistrer serait le pire des mensonges dans un
 * logiciel de caisse.
 *
 * Pour la même raison, le sélecteur de produit et de fournisseur n'est pas porté : il sert à
 * désigner *quoi* réapprovisionner, ce que l'écran ne peut pas faire. Le gérant saisit donc ses
 * quatre nombres, et l'unité affichée est libre (`unite`), ce qui permet d'annoncer « 240 Pces »
 * comme sur le téléphone sans dépendre d'un produit choisi.
 *
 * ## Adaptation PC
 *
 * Contenu borné à 760dp et centré, même raisonnement que l'écran Rapports (section 95) : un
 * tableau de chiffres se lit en colonne, et l'étirer sur 2000 px éloignerait les libellés de
 * leurs valeurs jusqu'à les rendre illisibles. Le panneau d'analyse est **sélectionnable**
 * (`SelectionContainer`) : sur un PC, un chiffre qu'on veut reporter ailleurs se copie, ce qui
 * n'a aucun équivalent tactile.
 */
@Composable
fun CalculateurMargeEcran(
    cartons: String,
    onCartonsChange: (String) -> Unit,
    unitesParCarton: String,
    onUnitesParCartonChange: (String) -> Unit,
    coutTotalAchat: String,
    onCoutTotalAchatChange: (String) -> Unit,
    prixVenteUnitaire: String,
    onPrixVenteUnitaireChange: (String) -> Unit,
    resultat: CalculMarge.Resultat,
    langue: String,
    themeColor: Color,
    unite: String = "",
    focusPremierChamp: FocusRequester? = null,
    modifier: Modifier = Modifier
) {
    val defilement = rememberScrollState()

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxWidth()
                .verticalScroll(defilement)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CarteAstuce(langue = langue)

            Text(
                text = when (langue) {
                    "mg" -> "1. Ampidiro ny nividianana"
                    "fr" -> "1. Saisir l'achat"
                    else -> "1. Enter the purchase"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ChampNombre(
                    valeur = cartons,
                    onChange = onCartonsChange,
                    libelle = when (langue) {
                        "mg" -> "Isan'ny baoritra"
                        "fr" -> "Nombre de cartons"
                        else -> "Number of cartons"
                    },
                    modifier = Modifier
                        .weight(1f)
                        .then(focusPremierChamp?.let { Modifier.focusRequester(it) } ?: Modifier)
                )
                ChampNombre(
                    valeur = unitesParCarton,
                    onChange = onUnitesParCartonChange,
                    libelle = when (langue) {
                        "mg" -> "Singany isaky ny baoritra"
                        "fr" -> "Pièces par carton"
                        else -> "Items per carton"
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ChampNombre(
                    valeur = coutTotalAchat,
                    onChange = onCoutTotalAchatChange,
                    libelle = when (langue) {
                        "mg" -> "Vola manontolo nividianana"
                        "fr" -> "Coût d'achat total"
                        else -> "Total purchase cost"
                    },
                    avecAr = true,
                    modifier = Modifier.weight(1f)
                )
                ChampNombre(
                    valeur = prixVenteUnitaire,
                    onChange = onPrixVenteUnitaireChange,
                    libelle = when (langue) {
                        "mg" -> "Vidiny hivarotana ny singany"
                        "fr" -> "Prix de vente unitaire"
                        else -> "Unit selling price"
                    },
                    avecAr = true,
                    // Le vert de mise en valeur du champ ajustable, repris tel quel du téléphone :
                    // c'est le seul des quatre que le gérant fait varier pour chercher sa marge.
                    couleurBordureFocus = Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )
            }

            PanneauAnalyse(
                resultat = resultat,
                unite = unite,
                langue = langue,
                themeColor = themeColor
            )
        }
    }
}

@Composable
private fun CarteAstuce(langue: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = null,
                // Ambre décoratif, laissé en dur comme sur le téléphone (sections 60 et 62).
                tint = Color(0xFFEAB308),
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = when (langue) {
                    "mg" -> "Fampiasana: Ampidiro ny isan'ny baoritra sy ny singany ao anatiny, " +
                        "avy eo ny vola manontolo nividianana azy ary farany ny vidiny " +
                        "hivarotanao ny singany iray mba hahitana ny tombony."
                    "fr" -> "Mode d'emploi : Entrez le nombre de cartons, les pièces par carton, " +
                        "le montant total payé, puis le prix de vente unitaire pour analyser " +
                        "vos gains."
                    else -> "How to use: Enter the number of cartons, items per carton, total " +
                        "purchase price, and finally your desired retail selling price per " +
                        "unit to analyze margins."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun ChampNombre(
    valeur: String,
    onChange: (String) -> Unit,
    libelle: String,
    modifier: Modifier = Modifier,
    avecAr: Boolean = false,
    couleurBordureFocus: Color? = null
) {
    OutlinedTextField(
        value = valeur,
        onValueChange = onChange,
        label = { Text(libelle) },
        // Un `if` explicite plutôt qu'un `?.let { { … } }` : ce dernier ferait inférer une lambda
        // `@Composable` à travers un `let` générique, forme que rien ici ne permet de compiler
        // pour s'en assurer. « Ar » est de toute façon le seul suffixe employé, comme sur Android.
        suffix = if (avecAr) {
            { Text("Ar", fontWeight = FontWeight.Bold) }
        } else {
            null
        },
        singleLine = true,
        // 56dp au minimum, le minimum Material3 : le `.height(52.dp)` imposé rognait les chiffres
        // saisis sur les champs de montant de la caisse (section 73).
        modifier = modifier.heightIn(min = 56.dp),
        shape = RoundedCornerShape(12.dp),
        // Même raison que le suffixe : `colors()` est `@Composable`, et un `if` explicite ne
        // laisse aucune place à l'inférence.
        colors = if (couleurBordureFocus != null) {
            OutlinedTextFieldDefaults.colors(focusedBorderColor = couleurBordureFocus)
        } else {
            OutlinedTextFieldDefaults.colors()
        }
    )
}

@Composable
private fun PanneauAnalyse(
    resultat: CalculMarge.Resultat,
    unite: String,
    langue: String,
    themeColor: Color
) {
    val vert = Color(0xFF10B981)
    val vertFonce = Color(0xFF059669)
    val couleurSante = when (resultat.sante) {
        CalculMarge.SanteMarge.PERTE -> MaterialTheme.colorScheme.error
        CalculMarge.SanteMarge.NULLE,
        CalculMarge.SanteMarge.INACTIVE -> MaterialTheme.colorScheme.onSurfaceVariant
        CalculMarge.SanteMarge.FAIBLE -> Color(0xFFF59E0B)
        CalculMarge.SanteMarge.EXCELLENTE -> vert
    }
    val libelleSante = CalculMarge.libelleSante(resultat.sante, langue)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = themeColor.copy(alpha = 0.05f)),
        border = BorderStroke(1.dp, themeColor.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                        contentDescription = null,
                        tint = themeColor
                    )
                    Text(
                        text = when (langue) {
                            "mg" -> "Kajy Tombony & Elanelana"
                            "fr" -> "Analyse des Marges"
                            else -> "Margin Analysis"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = themeColor
                    )
                }

                if (libelleSante.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(couleurSante.copy(alpha = 0.15f))
                            .border(1.dp, couleurSante.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = libelleSante,
                            color = couleurSante,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            HorizontalDivider(color = themeColor.copy(alpha = 0.1f))

            // Sélectionnable : sur un PC, un chiffre qu'on veut reporter dans un cahier ou un
            // message se copie. C'est gratuit ici et sans équivalent tactile.
            SelectionContainer {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val uniteAffichee = unite.ifBlank {
                        when (langue) {
                            "mg" -> "Singany"
                            "fr" -> "Pces"
                            else -> "Pcs"
                        }
                    }
                    LigneCalcul(
                        libelle = when (langue) {
                            "mg" -> "Isan'ny singany tafiditra manontolo"
                            "fr" -> "Nombre d'unités approvisionnées"
                            else -> "Total Units Restocked"
                        },
                        valeur = "${resultat.totalUnites.toInt()} $uniteAffichee",
                        gras = true
                    )
                    LigneCalcul(
                        libelle = when (langue) {
                            "mg" -> "Vidiny nividianana ny 1 Baoritra"
                            "fr" -> "Prix d'achat par Carton"
                            else -> "Purchase Cost per Carton"
                        },
                        valeur = "${FormatUtil.formatPrice(resultat.coutParCarton)} Ar"
                    )
                    LigneCalcul(
                        libelle = when (langue) {
                            "mg" -> "Vidiny nividianana ny 1 Singany"
                            "fr" -> "Prix d'achat unitaire (Revient)"
                            else -> "Purchase Cost per Unit"
                        },
                        valeur = "${FormatUtil.formatPrice(resultat.coutParUnite)} Ar"
                    )

                    HorizontalDivider(color = themeColor.copy(alpha = 0.1f))

                    LigneCalcul(
                        libelle = when (langue) {
                            "mg" -> "Vola miditra manontolo (Chiffre d'Affaires)"
                            "fr" -> "Chiffre d'affaires potentiel"
                            else -> "Potential Turnover / Revenue"
                        },
                        valeur = "${FormatUtil.formatPrice(resultat.recetteTotale)} Ar"
                    )
                    LigneCalcul(
                        libelle = when (langue) {
                            "mg" -> "TOMBOM-BAROTRA MANONTOLO"
                            "fr" -> "BÉNÉFICE GLOBAL POTENTIEL"
                            else -> "TOTAL POTENTIAL PROFIT"
                        },
                        valeur = "${FormatUtil.formatPrice(resultat.beneficeTotal)} Ar",
                        couleur = if (resultat.beneficeTotal >= 0.0) {
                            vert
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                        gras = true
                    )
                    LigneCalcul(
                        libelle = when (langue) {
                            "mg" -> "Tombony amin'ny 1 Baoritra"
                            "fr" -> "Bénéfice net par Carton"
                            else -> "Net Profit per Carton"
                        },
                        valeur = "${FormatUtil.formatPrice(resultat.beneficeParCarton)} Ar",
                        couleur = if (resultat.beneficeParCarton >= 0.0) {
                            vertFonce
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                        gras = true
                    )
                    LigneCalcul(
                        libelle = when (langue) {
                            "mg" -> "Tombony amin'ny 1 Singany"
                            "fr" -> "Bénéfice net par Singany (unité)"
                            else -> "Net Profit per Unit"
                        },
                        valeur = "${FormatUtil.formatPrice(resultat.beneficeParUnite)} Ar",
                        couleur = if (resultat.beneficeParUnite >= 0.0) {
                            vertFonce
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                    LigneCalcul(
                        libelle = when (langue) {
                            "mg" -> "Tahan'ny Tombom-barotra (%)"
                            "fr" -> "Taux de Marge (%)"
                            else -> "Margin Rate (%)"
                        },
                        // EXACTEMENT l'expression du téléphone. Elle dépend de la langue du
                        // système (virgule décimale sur une machine française), trait qui
                        // existait déjà côté Android : le reproduire garde les deux appareils
                        // d'accord, alors que « corriger » un seul des deux les ferait diverger.
                        valeur = "%.1f %%".format(resultat.tauxMarge),
                        couleur = themeColor,
                        gras = true
                    )
                }
            }
        }
    }
}

@Composable
private fun LigneCalcul(
    libelle: String,
    valeur: String,
    couleur: Color = MaterialTheme.colorScheme.onSurface,
    gras: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = libelle,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1.3f)
        )
        Text(
            text = valeur,
            fontSize = 13.sp,
            color = couleur,
            fontWeight = if (gras) FontWeight.Black else FontWeight.Bold,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
    }
}
