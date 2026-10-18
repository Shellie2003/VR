package com.example.ui.partage.dettes

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Debt
import com.example.util.FormatUtil
import com.example.util.LanguageManager
import com.example.util.RegroupementDettes

/**
 * L'écran Dettes, **porté depuis `DebtsScreen`** : champ de recherche en haut, carte de synthèse du
 * total dû, puis la liste regroupée par débiteur — une `CarteDette` seule quand la personne n'a
 * qu'un trosa, une `CarteGroupeDebiteur` dépliable dès qu'elle en a plusieurs. C'est exactement la
 * règle de la section 31, et elle vient de `RegroupementDettes` (`:core:logic`), partagée avec
 * Android plutôt que réécrite.
 *
 * **Les dates sont formatées par l'appelant** (`dateLisible`, `echeanceLisible`) et non ici : le
 * formatage de date dépend de la plateforme (`SimpleDateFormat` côté Android), et ce module
 * d'interface est volontairement sans dépendance de plateforme. C'est aussi ce qui permettra de
 * brancher le même écran sur Android le jour où les composables seront partagés.
 *
 * Ascenseur visible à droite, pour la raison de la section 88 : sur un PC, rien ne dit qu'il reste
 * des dettes plus bas avant d'avoir tenté la molette.
 */
@Composable
fun DettesEcran(
    dettes: List<Debt>,
    recherche: String,
    onRechercheChange: (String) -> Unit,
    dateLisible: (Debt) -> String,
    echeanceLisible: (Debt) -> String?,
    onRembourser: (Debt) -> Unit,
    onReglerGroupe: (List<Debt>, Double) -> Unit,
    onSupprimer: ((Debt) -> Unit)? = null,
    langue: String,
    focusRecherche: FocusRequester? = null,
    modifier: Modifier = Modifier
) {
    var detteOuverte by remember { mutableStateOf<Debt?>(null) }
    var groupeAuReglement by remember { mutableStateOf<String?>(null) }

    // Le filtrage porte sur la liste À PLAT, comme sur Android : chercher un nom doit trouver la
    // transaction, et c'est le regroupement qui s'applique ENSUITE au résultat — l'inverse ferait
    // disparaître un groupe entier dès que la recherche ne correspond pas à sa dette la plus
    // récente.
    val visibles = remember(dettes, recherche) {
        if (recherche.isBlank()) {
            dettes
        } else {
            val requete = recherche.trim().lowercase()
            dettes.filter {
                it.debtorName.lowercase().contains(requete) || it.note.lowercase().contains(requete)
            }
        }
    }
    val groupes = remember(visibles) { RegroupementDettes.grouper(visibles) }
    val totalDu = remember(visibles) { visibles.sumOf { it.balance } }

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        OutlinedTextField(
            value = recherche,
            onValueChange = onRechercheChange,
            placeholder = {
                Text(
                    when (langue) {
                        "mg" -> "Hikaroka trosa..."
                        "fr" -> "Rechercher une dette..."
                        else -> "Search a debt..."
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
                if (recherche.isNotEmpty()) {
                    IconButton(
                        onClick = { onRechercheChange("") },
                        modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                    ) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = null)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .heightIn(min = 56.dp)
                .then(focusRecherche?.let { Modifier.focusRequester(it) } ?: Modifier),
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = when (langue) {
                        "mg" -> "Totalin'ny trosa tsy voaloa"
                        "fr" -> "Total des dettes impayées"
                        else -> "Total outstanding debt"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${FormatUtil.formatPrice(totalDu)} Ar",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = when (langue) {
                        "mg" -> "${groupes.size} mpitrosa · ${visibles.size} trosa"
                        "fr" -> "${groupes.size} débiteurs · ${visibles.size} dettes"
                        else -> "${groupes.size} debtors · ${visibles.size} debts"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (groupes.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    text = if (recherche.isBlank()) {
                        when (langue) {
                            "mg" -> "Tsy misy trosa voarakitra."
                            "fr" -> "Aucune dette enregistrée."
                            else -> "No debt recorded."
                        }
                    } else {
                        when (langue) {
                            "mg" -> "Tsy misy trosa mifanaraka."
                            "fr" -> "Aucune dette ne correspond."
                            else -> "No debt matches."
                        }
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val etatListe = rememberLazyListState()
            Row(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = etatListe,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Clé = la clé de regroupement, stable d'un recalcul à l'autre : le nom
                    // affiché peut changer de graphie quand une dette plus récente arrive.
                    items(groupes, key = { RegroupementDettes.cle(it.displayName) }) { groupe ->
                        if (groupe.debts.size == 1) {
                            // Un débiteur à une seule dette garde la carte simple, STRICTEMENT
                            // comme sur Android : aucun changement visuel pour le cas courant.
                            val dette = groupe.debts.first()
                            CarteDette(
                                dette = dette,
                                langue = langue,
                                dateLisible = dateLisible(dette),
                                echeanceLisible = echeanceLisible(dette),
                                onOuvrirDetail = { detteOuverte = dette },
                                onRembourser = { onRembourser(dette) },
                                onSupprimer = onSupprimer?.let { action -> { action(dette) } }
                            )
                        } else {
                            CarteGroupeDebiteur(
                                groupe = groupe,
                                langue = langue,
                                dateLisible = dateLisible,
                                echeanceLisible = echeanceLisible,
                                onOuvrirDetail = { detteOuverte = it },
                                onRembourser = onRembourser,
                                onReglerGroupe = {
                                    groupeAuReglement = RegroupementDettes.cle(groupe.displayName)
                                },
                                onSupprimer = onSupprimer
                            )
                        }
                    }
                }
                VerticalScrollbar(
                    adapter = rememberScrollbarAdapter(etatListe),
                    modifier = Modifier.fillMaxHeight().padding(start = 8.dp)
                )
            }
        }
    }

    detteOuverte?.let { dette ->
        DialogueDetailDette(
            dette = dette,
            langue = langue,
            dateLisible = dateLisible(dette),
            echeanceLisible = echeanceLisible(dette),
            onRembourser = {
                detteOuverte = null
                onRembourser(dette)
            },
            onFermer = { detteOuverte = null }
        )
    }

    // Le groupe est retrouvé PAR SA CLÉ et non gardé en référence figée : c'est la leçon de la
    // section 53 (le dialogue d'étagère qui n'était plus d'accord avec le plan). Si le groupe
    // disparaît pendant que le dialogue est ouvert — dernière dette soldée, suppression — le
    // dialogue se referme de lui-même au lieu d'afficher un état périmé.
    groupeAuReglement?.let { cle ->
        val groupe = groupes.firstOrNull { RegroupementDettes.cle(it.displayName) == cle }
        if (groupe == null) {
            groupeAuReglement = null
        } else {
            DialogueReglementGroupe(
                groupe = groupe,
                langue = langue,
                onConfirmer = { montant ->
                    onReglerGroupe(groupe.debts, montant)
                    groupeAuReglement = null
                },
                onAnnuler = { groupeAuReglement = null }
            )
        }
    }
}
