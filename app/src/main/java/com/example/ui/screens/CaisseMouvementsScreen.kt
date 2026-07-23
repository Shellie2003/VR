package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MouvementCaisse
import com.example.ui.viewmodel.InventoryViewModel
import com.example.util.FormatUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TYPE_ENTREE = "ENTREE"
private const val TYPE_SORTIE = "SORTIE"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaisseMouvementsScreen(
    viewModel: InventoryViewModel,
    onNavigateBack: () -> Unit
) {
    val activeLang by viewModel.language.collectAsState()
    val themeColor by viewModel.themeColor.collectAsState()
    val mouvements by viewModel.allMouvementsCaisse.collectAsState()
    val soldeCaisse by viewModel.soldeCaisse.collectAsState()

    val isDark = MaterialTheme.colorScheme.background == Color(0xFF002114)
    val mainTextColor = if (isDark) Color.White else Color(0xFF1E293B)
    val secondaryTextColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    val screenTitle = when (activeLang) {
        "mg" -> "Vola an-Kesty"
        "fr" -> "Mouvements de caisse"
        else -> "Cash Movements"
    }
    val balanceLabel = when (activeLang) {
        "mg" -> "Vola ao an-kesty ankehitriny"
        "fr" -> "Solde de caisse actuel"
        else -> "Current cash balance"
    }
    val entreeBtn = when (activeLang) {
        "mg" -> "Miditra"
        "fr" -> "Entrée"
        else -> "Cash In"
    }
    val sortieBtn = when (activeLang) {
        "mg" -> "Mivoaka"
        "fr" -> "Sortie"
        else -> "Cash Out"
    }
    val emptyMsg = when (activeLang) {
        "mg" -> "Mbola tsy misy fivoahana/fidiran-bola voasoratra."
        "fr" -> "Aucun mouvement de caisse enregistré."
        else -> "No cash movement recorded yet."
    }
    val motifLabel = when (activeLang) {
        "mg" -> "Antony"
        "fr" -> "Motif"
        else -> "Reason"
    }
    val amountLabel = when (activeLang) {
        "mg" -> "Vola (Ar)"
        "fr" -> "Montant (Ar)"
        else -> "Amount (Ar)"
    }
    val noteLabel = when (activeLang) {
        "mg" -> "Fanamarihana (Safidy)"
        "fr" -> "Note (Optionnel)"
        else -> "Note (Optional)"
    }
    val saveBtnText = when (activeLang) {
        "mg" -> "Hitahiry"
        "fr" -> "Enregistrer"
        else -> "Save"
    }
    val cancelBtnText = when (activeLang) {
        "mg" -> "Hanafoana"
        "fr" -> "Annuler"
        else -> "Cancel"
    }
    val deleteConfirmMsg = when (activeLang) {
        "mg" -> "Hofafana ve ity mouvement ity?"
        "fr" -> "Supprimer ce mouvement de caisse ?"
        else -> "Delete this cash movement?"
    }
    val motifSuggestions = when (activeLang) {
        "mg" -> listOf("Vola fanombohana", "Fandaniana", "Fanalana vola", "Hafa")
        "fr" -> listOf("Fond de caisse", "Dépense", "Retrait gérant", "Autre")
        else -> listOf("Opening float", "Expense", "Manager withdrawal", "Other")
    }

    var dialogType by remember { mutableStateOf<String?>(null) } // TYPE_ENTREE / TYPE_SORTIE / null
    var mouvementToDelete by remember { mutableStateOf<MouvementCaisse?>(null) }

    var amountStr by remember { mutableStateOf("") }
    var motif by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var amountError by remember { mutableStateOf(false) }
    var motifError by remember { mutableStateOf(false) }

    fun resetForm() {
        amountStr = ""
        motif = ""
        note = ""
        amountError = false
        motifError = false
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 1.dp,
                color = MaterialTheme.colorScheme.background
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF1B4332) else Color(0xFFF1F5F9))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Hiverina",
                            tint = mainTextColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = screenTitle,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = mainTextColor,
                            fontSize = 20.sp
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp)
        ) {
            // Balance card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .testTag("caisse_balance_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = themeColor)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = balanceLabel.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Text(
                        text = "${FormatUtil.formatPrice(soldeCaisse)} Ar",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { resetForm(); dialogType = TYPE_ENTREE },
                            modifier = Modifier.weight(1f).height(42.dp).testTag("caisse_entree_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = themeColor
                            )
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(entreeBtn, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { resetForm(); dialogType = TYPE_SORTIE },
                            modifier = Modifier.weight(1f).height(42.dp).testTag("caisse_sortie_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.18f),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(sortieBtn, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (mouvements.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outlineVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = emptyMsg,
                            style = MaterialTheme.typography.bodyMedium,
                            color = secondaryTextColor
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 24.dp, top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(mouvements, key = { it.id }) { mouvement ->
                        MouvementCaisseCard(
                            mouvement = mouvement,
                            mainTextColor = mainTextColor,
                            secondaryTextColor = secondaryTextColor,
                            onDelete = { mouvementToDelete = mouvement }
                        )
                    }
                }
            }
        }
    }

    // Add movement dialog (shared for Entrée / Sortie)
    dialogType?.let { type ->
        val isEntree = type == TYPE_ENTREE
        AlertDialog(
            onDismissRequest = { resetForm(); dialogType = null },
            title = { Text(if (isEntree) entreeBtn else sortieBtn, fontWeight = FontWeight.Black) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = {
                            amountStr = it
                            amountError = it.toDoubleOrNull() == null || (it.toDoubleOrNull() ?: 0.0) <= 0.0
                        },
                        label = { Text(amountLabel) },
                        prefix = { Text("Ar ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = amountError,
                        modifier = Modifier.fillMaxWidth().testTag("caisse_amount_input")
                    )

                    OutlinedTextField(
                        value = motif,
                        onValueChange = {
                            motif = it
                            motifError = it.isBlank()
                        },
                        label = { Text(motifLabel) },
                        isError = motifError,
                        modifier = Modifier.fillMaxWidth().testTag("caisse_motif_input")
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        motifSuggestions.forEach { suggestion ->
                            AssistChip(
                                onClick = { motif = suggestion; motifError = false },
                                label = { Text(suggestion, fontSize = 11.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text(noteLabel) },
                        modifier = Modifier.fillMaxWidth().testTag("caisse_note_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = amountStr.toDoubleOrNull() ?: 0.0
                        val motifClean = motif.trim()
                        amountError = amt <= 0.0
                        motifError = motifClean.isEmpty()
                        if (!amountError && !motifError) {
                            viewModel.saveMouvementCaisse(
                                MouvementCaisse(
                                    type = type,
                                    montant = amt,
                                    motif = motifClean,
                                    note = note.trim()
                                )
                            )
                            resetForm()
                            dialogType = null
                        }
                    },
                    modifier = Modifier.testTag("caisse_save_button")
                ) {
                    Text(saveBtnText, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { resetForm(); dialogType = null }) {
                    Text(cancelBtnText)
                }
            }
        )
    }

    // Delete confirmation
    mouvementToDelete?.let { mouvement ->
        AlertDialog(
            onDismissRequest = { mouvementToDelete = null },
            title = { Text(deleteConfirmMsg, fontWeight = FontWeight.Bold) },
            text = { Text("${mouvement.motif} : ${FormatUtil.formatPrice(mouvement.montant)} Ar") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteMouvementCaisse(mouvement)
                        mouvementToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(when (activeLang) {
                        "mg" -> "Hamafa"
                        "fr" -> "Supprimer"
                        else -> "Delete"
                    }, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { mouvementToDelete = null }) {
                    Text(cancelBtnText)
                }
            }
        )
    }
}

@Composable
private fun MouvementCaisseCard(
    mouvement: MouvementCaisse,
    mainTextColor: Color,
    secondaryTextColor: Color,
    onDelete: () -> Unit
) {
    val isEntree = mouvement.type == TYPE_ENTREE
    val accentColor = if (isEntree) Color(0xFF2E7D32) else Color(0xFFC62828)
    val formatter = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE) }

    Card(
        modifier = Modifier.fillMaxWidth().testTag("caisse_mouvement_card_${mouvement.id}"),
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
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isEntree) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mouvement.motif,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = mainTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatter.format(Date(mouvement.date)),
                    style = MaterialTheme.typography.bodySmall,
                    color = secondaryTextColor
                )
                if (mouvement.note.isNotEmpty()) {
                    Text(
                        text = mouvement.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Text(
                text = "${if (isEntree) "+" else "-"}${FormatUtil.formatPrice(mouvement.montant)} Ar",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = accentColor
            )

            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
