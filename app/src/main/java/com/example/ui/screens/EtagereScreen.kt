package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Etagere
import com.example.data.model.Product
import com.example.data.model.ProduitNiveau
import com.example.ui.viewmodel.InventoryViewModel
import com.example.util.EtagereLayout
import com.example.util.FormatUtil

/**
 * Plan d'étagères interactif : une rangée d'étagères (gauche/droite), chacune composée de niveaux
 * empilés (haut/bas). Chaque case (niveau) se tape pour voir et gérer les produits qui y sont
 * rangés.
 *
 * Choix délibéré : une grille 2D dessinée en Compose classique, pas une scène 3D. Un moteur 3D
 * (Filament, SceneView…) ajoute des dizaines de mégaoctets à l'APK et dépend d'un GPU capable —
 * la première source de plantages et de lenteurs sur le parc Android bas de gamme que cette
 * application vise (minSdk 24). La grille 2D rend un service identique (voir en un coup d'œil ce
 * qui est rangé où) sans aucun de ces risques, et tourne à l'identique sur un Android 7 d'entrée
 * de gamme comme sur un téléphone récent.
 *
 * Performance : les étagères sont affichées dans une `LazyRow` — seules celles visibles à
 * l'écran sont composées, un magasin avec des dizaines d'étagères ne coûte donc pas plus cher
 * qu'un magasin qui n'en a que trois. Les niveaux d'une étagère restent en `Column` classique
 * (leur nombre par étagère est toujours modeste, quelques unités), ce qui évite la complexité
 * d'un `LazyColumn` imbriqué dans un `LazyRow` pour un gain nul à cette échelle.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EtagereScreen(
    viewModel: InventoryViewModel,
    onNavigateBack: () -> Unit
) {
    val activeLang by viewModel.language.collectAsState()
    val plan by viewModel.planEtagere.collectAsState()

    var niveauSelectionne by remember { mutableStateOf<Pair<Etagere, InventoryViewModel.NiveauAvecProduits>?>(null) }
    var etagereARenommer by remember { mutableStateOf<Etagere?>(null) }
    var etagereASupprimer by remember { mutableStateOf<Etagere?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = when (activeLang) {
                        "mg" -> "Fandrindrana ny efitrano"
                        "fr" -> "Plan d'étagères"
                        else -> "Shelf plan"
                    },
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("etagere_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                }
            }
        )

        if (plan.isEmpty()) {
            EtatVideEtagere(activeLang = activeLang) {
                viewModel.ajouterEtagere(EtagereLayout.Cote.FIN)
            }
        } else {
            LazyRow(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "ajouter_gauche") {
                    BoutonAjouterEtagere(
                        activeLang = activeLang,
                        cote = EtagereLayout.Cote.DEBUT,
                        onClick = { viewModel.ajouterEtagere(EtagereLayout.Cote.DEBUT) }
                    )
                }

                items(plan, key = { it.etagere.id }) { etagereAvecNiveaux ->
                    ColonneEtagere(
                        etagereAvecNiveaux = etagereAvecNiveaux,
                        activeLang = activeLang,
                        onClickNiveau = { niveauSelectionne = etagereAvecNiveaux.etagere to it },
                        onAjouterNiveau = { cote -> viewModel.ajouterNiveau(etagereAvecNiveaux.etagere.id, cote) },
                        onRenommer = { etagereARenommer = etagereAvecNiveaux.etagere },
                        onSupprimer = { etagereASupprimer = etagereAvecNiveaux.etagere }
                    )
                }

                item(key = "ajouter_droite") {
                    BoutonAjouterEtagere(
                        activeLang = activeLang,
                        cote = EtagereLayout.Cote.FIN,
                        onClick = { viewModel.ajouterEtagere(EtagereLayout.Cote.FIN) }
                    )
                }
            }
        }
    }

    niveauSelectionne?.let { (etagere, niveauAvecProduits) ->
        NiveauDetailDialog(
            etagere = etagere,
            niveauAvecProduits = niveauAvecProduits,
            activeLang = activeLang,
            allProducts = viewModel.allProducts.collectAsState().value,
            onRenommerNiveau = { nom -> viewModel.renommerNiveau(niveauAvecProduits.niveau, nom) },
            onAjouterProduit = { produitId -> viewModel.ajouterProduitAuNiveau(niveauAvecProduits.niveau.id, produitId) },
            onRetirerProduit = { lien -> viewModel.retirerProduitDuNiveau(lien) },
            onSupprimerNiveau = {
                viewModel.supprimerNiveau(niveauAvecProduits.niveau)
                niveauSelectionne = null
            },
            onDismiss = { niveauSelectionne = null }
        )
    }

    etagereARenommer?.let { etagere ->
        RenommerDialog(
            titre = when (activeLang) {
                "mg" -> "Hanova ny anaran'ny étagère"
                "fr" -> "Renommer l'étagère"
                else -> "Rename shelf"
            },
            valeurInitiale = etagere.nom,
            onConfirm = { nom ->
                viewModel.renommerEtagere(etagere, nom)
                etagereARenommer = null
            },
            onDismiss = { etagereARenommer = null }
        )
    }

    etagereASupprimer?.let { etagere ->
        AlertDialog(
            onDismissRequest = { etagereASupprimer = null },
            title = {
                Text(
                    text = when (activeLang) {
                        "mg" -> "Hofafana io étagère io?"
                        "fr" -> "Supprimer cette étagère ?"
                        else -> "Delete this shelf?"
                    },
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = when (activeLang) {
                        "mg" -> "Ho fafana miaraka aminy avokoa ny rayon sy ny fandraketana entana ao aminy. Tsy voakasika ny tahirin'ny entana."
                        "fr" -> "Tous ses rayons et le rangement des produits qui y sont associés seront supprimés. Le stock des produits n'est pas affecté."
                        else -> "All its shelf levels and product placements will be deleted. Product stock is not affected."
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.supprimerEtagere(etagere)
                        etagereASupprimer = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(
                        text = when (activeLang) {
                            "mg" -> "Hofafana"
                            "fr" -> "Supprimer"
                            else -> "Delete"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { etagereASupprimer = null }) {
                    Text(
                        text = when (activeLang) {
                            "mg" -> "Aoka"
                            "fr" -> "Annuler"
                            else -> "Cancel"
                        }
                    )
                }
            }
        )
    }
}

@Composable
private fun EtatVideEtagere(activeLang: String, onCreerPremiere: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.GridView,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(56.dp)
            )
            Text(
                text = when (activeLang) {
                    "mg" -> "Mbola tsy misy étagère voarindra."
                    "fr" -> "Aucune étagère pour l'instant."
                    else -> "No shelf yet."
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onCreerPremiere,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("creer_premiere_etagere_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (activeLang) {
                        "mg" -> "Hamorona ny étagère voalohany"
                        "fr" -> "Créer ma première étagère"
                        else -> "Create my first shelf"
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun BoutonAjouterEtagere(
    activeLang: String,
    cote: EtagereLayout.Cote,
    onClick: () -> Unit
) {
    val label = when (cote) {
        EtagereLayout.Cote.DEBUT -> when (activeLang) {
            "mg" -> "Ankavia"
            "fr" -> "À gauche"
            else -> "Left"
        }
        EtagereLayout.Cote.FIN -> when (activeLang) {
            "mg" -> "Ankavanana"
            "fr" -> "À droite"
            else -> "Right"
        }
    }
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .width(64.dp)
            .fillMaxHeight()
            .testTag("ajouter_etagere_${cote.name.lowercase()}"),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(4.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}

private const val LARGEUR_ETAGERE_DP = 168
private const val HAUTEUR_NIVEAU_DP = 84

@Composable
private fun ColonneEtagere(
    etagereAvecNiveaux: InventoryViewModel.EtagereAvecNiveaux,
    activeLang: String,
    onClickNiveau: (InventoryViewModel.NiveauAvecProduits) -> Unit,
    onAjouterNiveau: (EtagereLayout.Cote) -> Unit,
    onRenommer: () -> Unit,
    onSupprimer: () -> Unit
) {
    var menuOuvert by remember { mutableStateOf(false) }
    // Niveau de plus grande position affiché en haut : "ajouter en haut" (FIN) empile visuellement
    // au sommet de l'étagère, exactement comme on poserait une planche supplémentaire en haut.
    val niveauxAffiches = remember(etagereAvecNiveaux) { etagereAvecNiveaux.niveaux.sortedByDescending { it.niveau.position } }

    Column(
        modifier = Modifier
            .width(LARGEUR_ETAGERE_DP.dp)
            .fillMaxHeight()
            .testTag("etagere_${etagereAvecNiveaux.etagere.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                .clickable { onRenommer() }
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = etagereAvecNiveaux.etagere.nom,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Box {
                IconButton(onClick = { menuOuvert = true }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.MoreVert, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                DropdownMenu(expanded = menuOuvert, onDismissRequest = { menuOuvert = false }) {
                    DropdownMenuItem(
                        text = { Text(when (activeLang) { "mg" -> "Hanova anarana"; "fr" -> "Renommer"; else -> "Rename" }) },
                        onClick = { menuOuvert = false; onRenommer() }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = when (activeLang) { "mg" -> "Hofafana"; "fr" -> "Supprimer"; else -> "Delete" },
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = { menuOuvert = false; onSupprimer() }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        TextButton(
            onClick = { onAjouterNiveau(EtagereLayout.Cote.FIN) },
            modifier = Modifier.fillMaxWidth().testTag("ajouter_niveau_haut_${etagereAvecNiveaux.etagere.id}"),
            contentPadding = PaddingValues(vertical = 2.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = when (activeLang) { "mg" -> "Rayon (ambony)"; "fr" -> "Rayon (haut)"; else -> "Shelf (top)" },
                fontSize = 10.sp
            )
        }

        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            niveauxAffiches.forEachIndexed { index, niveauAvecProduits ->
                CelluleNiveau(
                    niveauAvecProduits = niveauAvecProduits,
                    numeroAffiche = niveauxAffiches.size - index,
                    activeLang = activeLang,
                    onClick = { onClickNiveau(niveauAvecProduits) }
                )
            }
        }

        TextButton(
            onClick = { onAjouterNiveau(EtagereLayout.Cote.DEBUT) },
            modifier = Modifier.fillMaxWidth().testTag("ajouter_niveau_bas_${etagereAvecNiveaux.etagere.id}"),
            contentPadding = PaddingValues(vertical = 2.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = when (activeLang) { "mg" -> "Rayon (ambany)"; "fr" -> "Rayon (bas)"; else -> "Shelf (bottom)" },
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun CelluleNiveau(
    niveauAvecProduits: InventoryViewModel.NiveauAvecProduits,
    numeroAffiche: Int,
    activeLang: String,
    onClick: () -> Unit
) {
    val estVide = niveauAvecProduits.produits.isEmpty()
    val nomAffiche = niveauAvecProduits.niveau.nom.ifBlank {
        when (activeLang) {
            "mg" -> "Rayon $numeroAffiche"
            "fr" -> "Niveau $numeroAffiche"
            else -> "Level $numeroAffiche"
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(HAUTEUR_NIVEAU_DP.dp)
            .clickable { onClick() }
            .testTag("niveau_${niveauAvecProduits.niveau.id}"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (estVide) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
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
                if (estVide) {
                    Text(
                        text = when (activeLang) {
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
            if (!estVide) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = when (activeLang) {
                            "mg" -> "${niveauAvecProduits.produits.size} karazana"
                            "fr" -> "${niveauAvecProduits.produits.size} référence(s)"
                            else -> "${niveauAvecProduits.produits.size} product(s)"
                        },
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            // Ledge visuelle du rayon : une planche stylisée en bas de la case, pour rappeler
            // qu'il s'agit d'une étagère physique — un simple filet de couleur, aucun coût de
            // rendu au-delà d'une Box supplémentaire.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(Color(0xFF8D6E63).copy(alpha = 0.6f))
            )
        }
    }
}

@Composable
private fun RenommerDialog(
    titre: String,
    valeurInitiale: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var valeur by remember { mutableStateOf(valeurInitiale) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(titre, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = valeur,
                onValueChange = { valeur = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("renommer_input")
            )
        },
        confirmButton = {
            Button(
                onClick = { if (valeur.isNotBlank()) onConfirm(valeur.trim()) },
                enabled = valeur.isNotBlank()
            ) { Text("OK", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

/**
 * Détail d'une case (niveau) : renommage, liste des produits rangés avec retrait individuel, et
 * recherche pour en ajouter un nouveau. Le catalogue étant de taille modeste dans une épicerie
 * (quelques centaines de références au plus), le filtrage se fait en mémoire sur la liste déjà
 * chargée par l'écran — pas de nouvelle requête base de données à chaque frappe.
 */
@Composable
private fun NiveauDetailDialog(
    etagere: Etagere,
    niveauAvecProduits: InventoryViewModel.NiveauAvecProduits,
    activeLang: String,
    allProducts: List<Product>,
    onRenommerNiveau: (String) -> Unit,
    onAjouterProduit: (Int) -> Unit,
    onRetirerProduit: (ProduitNiveau) -> Unit,
    onSupprimerNiveau: () -> Unit,
    onDismiss: () -> Unit
) {
    var nomInput by remember(niveauAvecProduits.niveau.id) { mutableStateOf(niveauAvecProduits.niveau.nom) }
    var rechercheProduit by remember { mutableStateOf("") }
    var confirmerSuppression by remember { mutableStateOf(false) }

    val idsDejaPresents = remember(niveauAvecProduits) { niveauAvecProduits.liens.map { it.produitId }.toSet() }
    val resultatsRecherche = remember(allProducts, rechercheProduit, idsDejaPresents) {
        if (rechercheProduit.isBlank()) {
            emptyList()
        } else {
            allProducts
                .filter { it.id !in idsDejaPresents && it.name.contains(rechercheProduit, ignoreCase = true) }
                .take(20)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(etagere.nom, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                Text(
                    text = when (activeLang) {
                        "mg" -> "Antsipirian'ny rayon"
                        "fr" -> "Détail du rayon"
                        else -> "Shelf level detail"
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = nomInput,
                    onValueChange = { nomInput = it },
                    label = {
                        Text(
                            when (activeLang) {
                                "mg" -> "Anaran'ny rayon (tsy voatery)"
                                "fr" -> "Nom du rayon (facultatif)"
                                else -> "Shelf level name (optional)"
                            }
                        )
                    },
                    singleLine = true,
                    trailingIcon = {
                        if (nomInput != niveauAvecProduits.niveau.nom) {
                            IconButton(onClick = { onRenommerNiveau(nomInput) }) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("niveau_nom_input")
                )

                HorizontalDivider()

                Text(
                    text = when (activeLang) {
                        "mg" -> "Entana ao anatiny (${niveauAvecProduits.produits.size})"
                        "fr" -> "Produits rangés ici (${niveauAvecProduits.produits.size})"
                        else -> "Products stored here (${niveauAvecProduits.produits.size})"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                if (niveauAvecProduits.produits.isEmpty()) {
                    Text(
                        text = when (activeLang) {
                            "mg" -> "Tsy misy entana voarakitra eto."
                            "fr" -> "Aucun produit rangé ici pour l'instant."
                            else -> "No product stored here yet."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        niveauAvecProduits.liens.forEach { lien ->
                            val produit = niveauAvecProduits.produits.find { it.id == lien.produitId } ?: return@forEach
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(produit.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        text = "${FormatUtil.formatPrice(produit.price)} Ar · ${FormatUtil.formatQty(produit.stock, produit.unit)}",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                                IconButton(onClick = { onRetirerProduit(lien) }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()

                OutlinedTextField(
                    value = rechercheProduit,
                    onValueChange = { rechercheProduit = it },
                    label = {
                        Text(
                            when (activeLang) {
                                "mg" -> "Karohy hampidirina eto"
                                "fr" -> "Chercher un produit à ranger ici"
                                else -> "Search a product to add here"
                            }
                        )
                    },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("recherche_produit_niveau_input")
                )

                resultatsRecherche.forEach { produit ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                onAjouterProduit(produit.id)
                                rechercheProduit = ""
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(produit.name, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                }
                if (rechercheProduit.isNotBlank() && resultatsRecherche.isEmpty()) {
                    Text(
                        text = when (activeLang) {
                            "mg" -> "Tsy nisy hita."
                            "fr" -> "Aucun résultat."
                            else -> "No results."
                        },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                HorizontalDivider()

                TextButton(
                    onClick = { confirmerSuppression = true },
                    modifier = Modifier.fillMaxWidth().testTag("supprimer_niveau_button")
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when (activeLang) {
                            "mg" -> "Hofafana ity rayon ity"
                            "fr" -> "Supprimer ce rayon"
                            else -> "Delete this shelf level"
                        },
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = when (activeLang) {
                        "mg" -> "Vita"
                        "fr" -> "Fermer"
                        else -> "Close"
                    }
                )
            }
        }
    )

    if (confirmerSuppression) {
        AlertDialog(
            onDismissRequest = { confirmerSuppression = false },
            title = {
                Text(
                    text = when (activeLang) {
                        "mg" -> "Hofafana ity rayon ity?"
                        "fr" -> "Supprimer ce rayon ?"
                        else -> "Delete this shelf level?"
                    },
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = when (activeLang) {
                        "mg" -> "Ho fafana ny fandraketan'ny entana rehetra ato aminy. Tsy voakasika ny tahiriny."
                        "fr" -> "Le rangement de tous les produits qui y sont associés sera supprimé. Leur stock n'est pas affecté."
                        else -> "The placement of every product stored here will be removed. Their stock is not affected."
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = { confirmerSuppression = false; onSupprimerNiveau() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(
                        text = when (activeLang) { "mg" -> "Hofafana"; "fr" -> "Supprimer"; else -> "Delete" },
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmerSuppression = false }) {
                    Text(when (activeLang) { "mg" -> "Aoka"; "fr" -> "Annuler"; else -> "Cancel" })
                }
            }
        )
    }
}
