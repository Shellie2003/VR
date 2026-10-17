package com.example.ui.partage.caisse

import androidx.compose.foundation.VerticalScrollbar
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CartItem
import com.example.data.model.Product
import com.example.util.CatalogueFiltre
import com.example.util.LanguageManager
import com.example.util.Monnaie
import com.example.util.ShopMode
import com.example.util.enDecimal

/**
 * L'écran de caisse, **porté depuis la disposition TABLETTE d'Android** (`isTablet && subTab ==
 * "checkout"`) et non depuis celle du téléphone.
 *
 * C'est le choix évident et il tient en une phrase : la disposition tablette est déjà deux panneaux
 * côte à côte — sélecteur de produits à gauche (`weight(1.1f)`), panier et facturation à droite
 * (`weight(1f)`) — c'est-à-dire exactement ce qu'un écran de PC demande. La disposition téléphone,
 * elle, empile des onglets pour tenir sur une colonne étroite, contrainte qui n'existe pas ici.
 * L'utilisateur l'avait d'ailleurs dit : « la version tablette qu'on a déjà implémentée nous aide à
 * aller plus vite ».
 *
 * **Ce que la caisse de bureau ne fait PAS encore, et le dit à l'écran :** enregistrer la vente.
 * Sans base de données partagée (Room KMP, remis à plus tard parce qu'il touche au fichier de base
 * de chaque boutique en service), il n'y a rien où écrire. Le bouton « Valider » est donc désactivé
 * avec une raison affichée sous lui, plutôt que de laisser croire à un encaissement qui n'aurait
 * lieu nulle part — ce serait le pire mensonge possible dans un logiciel de caisse.
 *
 * Tout le calcul, en revanche, est réel et partagé avec Android : total, TVA, suffisance, monnaie
 * à rendre, prix de gros, pas d'ajustement.
 */
@Composable
fun CaisseEcran(
    produits: List<Product>,
    panier: List<CartItem>,
    recherche: String,
    onRechercheChange: (String) -> Unit,
    onAjouterProduit: (Product, Double) -> Unit,
    onAjouterDivers: (Double) -> Unit,
    onQuantiteDelta: (CartItem, Double) -> Unit,
    onFixerQuantite: (CartItem, Double) -> Unit,
    onViderPanier: () -> Unit,
    modeBoutique: ShopMode,
    langue: String,
    couleurTheme: Color,
    messageValidationImpossible: String? = null,
    focusRecherche: FocusRequester? = null,
    modifier: Modifier = Modifier
) {
    var montantRecuSaisi by remember { mutableStateOf("") }
    var modePaiement by remember { mutableStateOf("ESPECES") }
    var ligneEnEdition by remember { mutableStateOf<CartItem?>(null) }
    var confirmerVidage by remember { mutableStateOf(false) }

    val montantRecu = montantRecuSaisi.enDecimal() ?: 0.0
    val total = Monnaie.total(panier)

    // Le champ de recherche prend le focus à l'ouverture : sur un PC, un lecteur de code-barres USB
    // tape le code puis valide, exactement comme un clavier. Sans champ actif, le vendeur scanne et
    // rien ne se passe. C'est la raison la plus concrète d'une caisse de bureau.
    LaunchedEffect(Unit) { runCatching { focusRecherche?.requestFocus() } }

    val visibles = remember(produits, recherche) {
        CatalogueFiltre.filtrer(produits = produits, recherche = recherche)
    }

    Row(
        modifier = modifier.fillMaxSize().padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ===== PANNEAU GAUCHE : sélecteur de produits =====
        Column(modifier = Modifier.weight(1.1f).fillMaxHeight()) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = LanguageManager.translate("select_product", langue),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = couleurTheme
                )
                Spacer(modifier = Modifier.width(12.dp))
                OutlinedTextField(
                    value = recherche,
                    onValueChange = onRechercheChange,
                    placeholder = {
                        Text(LanguageManager.translate("search_placeholder", langue), fontSize = 12.sp)
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    // Pas d'icône d'appareil photo, contrairement à Android : sur un PC il n'y a
                    // pas de caméra à ouvrir, le lecteur de code-barres écrit directement dans ce
                    // champ. Un bouton qui ouvrirait une caméra inexistante serait un leurre.
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .then(focusRecherche?.let { Modifier.focusRequester(it) } ?: Modifier),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (visibles.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = LanguageManager.translate("no_products_found", langue),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                val etatGrille = rememberLazyGridState()
                Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    LazyVerticalGrid(
                        state = etatGrille,
                        // Android fixe 2 colonnes sur une tablette, où la carte mesure ~300dp.
                        // `Adaptive` conserve cette largeur et laisse le nombre de colonnes suivre
                        // la fenêtre : un grand écran montre plus de produits à la même taille, au
                        // lieu de trois cartes étirées dont les pastilles s'éloignent des chiffres.
                        columns = GridCells.Adaptive(minSize = 300.dp),
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        items(visibles, key = { it.id }) { produit ->
                            CarteProduitCaisse(
                                produit = produit,
                                modeBoutique = modeBoutique,
                                couleurTheme = couleurTheme,
                                onAjouter = onAjouterProduit,
                                modifier = Modifier.fillMaxWidth().height(175.dp)
                            )
                        }
                    }
                    VerticalScrollbar(
                        adapter = rememberScrollbarAdapter(etatGrille),
                        modifier = Modifier.fillMaxHeight().padding(start = 8.dp)
                    )
                }
            }
        }

        // ===== PANNEAU DROIT : divers rapide, panier, facturation =====
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = when (langue) {
                    "mg" -> "Vola Mivantana"
                    "fr" -> "Divers rapide"
                    else -> "Quick Miscellaneous"
                },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = couleurTheme,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(end = 16.dp)
            ) {
                items(MONTANTS_DIVERS, key = { it }) { montant ->
                    SuggestionChip(
                        onClick = { onAjouterDivers(montant.toDouble()) },
                        label = {
                            Text(text = "+$montant Ar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = couleurTheme.copy(alpha = 0.12f),
                            labelColor = couleurTheme
                        ),
                        border = null,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                    )
                }
            }

            HorizontalDivider(thickness = 0.5.dp)

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (panier.isEmpty()) {
                    Text(
                        text = when (langue) {
                            "mg" -> "Mbola tsy misy entana ao anaty harona."
                            "fr" -> "Le panier est vide."
                            else -> "The cart is empty."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    val etatPanier = rememberLazyListState()
                    Row(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = etatPanier,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        ) {
                            items(panier, key = { it.id }) { ligne ->
                                LigneDuPanier(
                                    item = ligne,
                                    couleurTheme = couleurTheme,
                                    onQuantiteDelta = { delta -> onQuantiteDelta(ligne, delta) },
                                    onEditerQuantite = { ligneEnEdition = it }
                                )
                            }
                        }
                        VerticalScrollbar(
                            adapter = rememberScrollbarAdapter(etatPanier),
                            modifier = Modifier.fillMaxHeight().padding(start = 4.dp)
                        )
                    }
                }
            }

            CarteFacturation(
                total = total,
                montantRecuSaisi = montantRecuSaisi,
                onMontantRecuChange = { montantRecuSaisi = it },
                montantRecu = montantRecu,
                modePaiement = modePaiement,
                onModePaiementChange = { modePaiement = it },
                panierVide = panier.isEmpty(),
                onViderPanier = { confirmerVidage = true },
                onValider = { /* Désactivé : voir `messageValidationImpossible`. */ },
                validationPossible = messageValidationImpossible == null,
                messageValidationImpossible = messageValidationImpossible,
                couleurTheme = couleurTheme,
                langue = langue
            )
        }
    }

    ligneEnEdition?.let { ligne ->
        DialogueQuantite(
            ligne = ligne,
            langue = langue,
            onValider = { quantite ->
                onFixerQuantite(ligne, quantite)
                ligneEnEdition = null
            },
            onAnnuler = { ligneEnEdition = null }
        )
    }

    if (confirmerVidage) {
        AlertDialog(
            onDismissRequest = { confirmerVidage = false },
            title = {
                Text(
                    when (langue) {
                        "mg" -> "Hofafana ny harona ?"
                        "fr" -> "Vider le panier ?"
                        else -> "Clear the cart?"
                    }
                )
            },
            text = {
                Text(
                    when (langue) {
                        "mg" -> "Ho voafafa ny entana rehetra ao anaty harona."
                        "fr" -> "Tous les articles du panier seront retirés."
                        else -> "Every item in the cart will be removed."
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onViderPanier()
                    confirmerVidage = false
                }) {
                    Text(
                        when (langue) {
                            "mg" -> "Hofafana"
                            "fr" -> "Vider"
                            else -> "Clear"
                        }
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmerVidage = false }) {
                    Text(
                        when (langue) {
                            "mg" -> "Hajanona"
                            "fr" -> "Annuler"
                            else -> "Cancel"
                        }
                    )
                }
            }
        )
    }
}

/**
 * La saisie directe d'une quantité, derrière le crayon de la ligne de panier — même rôle que le
 * dialogue d'Android.
 *
 * Une saisie illisible **n'est pas validée** : `NombreUtil` rend `null`, et transformer cela en
 * zéro ferait disparaître la ligne sur une simple faute de frappe, au moment où le vendeur annonce
 * le total au client.
 */
@Composable
private fun DialogueQuantite(
    ligne: CartItem,
    langue: String,
    onValider: (Double) -> Unit,
    onAnnuler: () -> Unit
) {
    var saisie by remember(ligne.id) { mutableStateOf(quantiteLisible(ligne.quantity)) }
    val quantite = saisie.enDecimal()
    val valide = quantite != null && quantite > 0.0

    AlertDialog(
        onDismissRequest = onAnnuler,
        title = { Text(ligne.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            Column {
                Text(
                    text = when (langue) {
                        "mg" -> "Isa (${ligne.unit})"
                        "fr" -> "Quantité (${ligne.unit})"
                        else -> "Quantity (${ligne.unit})"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = saisie,
                    onValueChange = { saisie = it },
                    singleLine = true,
                    isError = saisie.isNotBlank() && !valide,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { quantite?.let(onValider) },
                enabled = valide
            ) {
                Text(
                    when (langue) {
                        "mg" -> "Ekena"
                        "fr" -> "Valider"
                        else -> "Apply"
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onAnnuler) {
                Text(
                    when (langue) {
                        "mg" -> "Hajanona"
                        "fr" -> "Annuler"
                        else -> "Cancel"
                    }
                )
            }
        }
    )
}

/**
 * Les montants de « divers rapide », repris tels quels de l'écran Android. Ce sont les petites
 * sommes qu'un client paie sans article identifiable au catalogue (un sachet, un service rendu).
 */
private val MONTANTS_DIVERS = listOf(200, 300, 500, 1000, 2000, 5000)
