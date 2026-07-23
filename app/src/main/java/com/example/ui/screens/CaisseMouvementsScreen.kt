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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CaisseSession
import com.example.data.model.MouvementCaisse
import com.example.ui.components.SelectionExportToolbar
import com.example.ui.viewmodel.InventoryViewModel
import com.example.util.ExportFormat
import com.example.util.ExportUtil
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
    val context = LocalContext.current
    // Tablet/large-screen layout: cap the column's width and center it.
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600
    val activeLang by viewModel.language.collectAsState()
    val themeColor by viewModel.themeColor.collectAsState()
    val mouvements by viewModel.allMouvementsCaisse.collectAsState()
    val soldeCaisse by viewModel.soldeCaisse.collectAsState()
    val openSession by viewModel.openCaisseSession.collectAsState()
    val allSessions by viewModel.allCaisseSessions.collectAsState()

    // C.2: session open/close dialog states
    var showOpenSessionDialog by remember { mutableStateOf(false) }
    var openAmountStr by remember { mutableStateOf("") }
    var showCloseSessionDialog by remember { mutableStateOf(false) }
    var closeAmountStr by remember { mutableStateOf("") }
    var closeNote by remember { mutableStateOf("") }

    // Multi-selection state (checkbox mode for bulk delete)
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedMouvementIds by remember { mutableStateOf(setOf<Long>()) }
    var showMultiDeleteConfirm by remember { mutableStateOf(false) }

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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = if (isTablet) Alignment.TopCenter else Alignment.TopStart
        ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .then(if (isTablet) Modifier.widthIn(max = 720.dp) else Modifier.fillMaxWidth())
                .padding(horizontal = 16.dp)
        ) {
            // C.2: cash session (ouverture/fermeture de caisse) status card
            CaisseSessionCard(
                activeLang = activeLang,
                mainTextColor = mainTextColor,
                secondaryTextColor = secondaryTextColor,
                openSession = openSession,
                lastClosedSession = allSessions.firstOrNull { !it.isOpen },
                onOpenClick = { openAmountStr = ""; showOpenSessionDialog = true },
                onCloseClick = { closeAmountStr = ""; closeNote = ""; showCloseSessionDialog = true }
            )

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

            SelectionExportToolbar(
                activeLang = activeLang,
                isSelectionMode = isSelectionMode,
                selectedCount = selectedMouvementIds.size,
                onToggleSelectionMode = {
                    isSelectionMode = !isSelectionMode
                    if (!isSelectionMode) selectedMouvementIds = emptySet()
                },
                onSelectAll = { selectedMouvementIds = mouvements.map { it.id }.toSet() },
                onDeleteSelected = { showMultiDeleteConfirm = true },
                onExportPdf = { ExportUtil.exportCaisseMouvements(context, mouvements, ExportFormat.PDF) },
                onExportCsv = { ExportUtil.exportCaisseMouvements(context, mouvements, ExportFormat.CSV) }
            )

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
                            isSelectionMode = isSelectionMode,
                            isSelected = selectedMouvementIds.contains(mouvement.id),
                            onToggleSelect = {
                                selectedMouvementIds = if (selectedMouvementIds.contains(mouvement.id)) {
                                    selectedMouvementIds - mouvement.id
                                } else {
                                    selectedMouvementIds + mouvement.id
                                }
                            },
                            onDelete = { mouvementToDelete = mouvement }
                        )
                    }
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

    // C.2: open a new cash session (fond de caisse de départ)
    if (showOpenSessionDialog) {
        val openTitle = when (activeLang) {
            "mg" -> "Sokafy ny kesty"
            "fr" -> "Ouvrir la caisse"
            else -> "Open cash session"
        }
        val openAmountLabel = when (activeLang) {
            "mg" -> "Vola fanombohana (Ar)"
            "fr" -> "Fond de caisse de départ (Ar)"
            else -> "Starting cash float (Ar)"
        }
        val openAmountErr = openAmountStr.toDoubleOrNull() == null || (openAmountStr.toDoubleOrNull() ?: -1.0) < 0.0
        AlertDialog(
            onDismissRequest = { showOpenSessionDialog = false },
            title = { Text(openTitle, fontWeight = FontWeight.Black) },
            text = {
                OutlinedTextField(
                    value = openAmountStr,
                    onValueChange = { openAmountStr = it },
                    label = { Text(openAmountLabel) },
                    prefix = { Text("Ar ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = openAmountErr,
                    modifier = Modifier.fillMaxWidth().testTag("caisse_open_amount_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = openAmountStr.toDoubleOrNull() ?: 0.0
                        if (amt >= 0.0) {
                            viewModel.openCaisseSession(amt)
                            showOpenSessionDialog = false
                        }
                    },
                    modifier = Modifier.testTag("caisse_open_confirm_button")
                ) {
                    Text(saveBtnText, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showOpenSessionDialog = false }) { Text(cancelBtnText) }
            }
        )
    }

    // C.2: close the currently open cash session (comptage & écart)
    if (showCloseSessionDialog && openSession != null) {
        val session = openSession!!
        val closeTitle = when (activeLang) {
            "mg" -> "Hidio ny kesty"
            "fr" -> "Fermer la caisse"
            else -> "Close cash session"
        }
        val closeAmountLabel = when (activeLang) {
            "mg" -> "Vola voaisa (Ar)"
            "fr" -> "Montant compté en caisse (Ar)"
            else -> "Counted cash amount (Ar)"
        }
        val closeAmountErr = closeAmountStr.toDoubleOrNull() == null || (closeAmountStr.toDoubleOrNull() ?: -1.0) < 0.0
        AlertDialog(
            onDismissRequest = { showCloseSessionDialog = false },
            title = { Text(closeTitle, fontWeight = FontWeight.Black) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = closeAmountStr,
                        onValueChange = { closeAmountStr = it },
                        label = { Text(closeAmountLabel) },
                        prefix = { Text("Ar ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = closeAmountErr,
                        modifier = Modifier.fillMaxWidth().testTag("caisse_close_amount_input")
                    )
                    OutlinedTextField(
                        value = closeNote,
                        onValueChange = { closeNote = it },
                        label = { Text(noteLabel) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = closeAmountStr.toDoubleOrNull() ?: 0.0
                        if (amt >= 0.0) {
                            viewModel.closeCaisseSession(session, amt, closeNote.trim())
                            showCloseSessionDialog = false
                        }
                    },
                    modifier = Modifier.testTag("caisse_close_confirm_button")
                ) {
                    Text(saveBtnText, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCloseSessionDialog = false }) { Text(cancelBtnText) }
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

    // Multi-selection bulk delete confirmation
    if (showMultiDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showMultiDeleteConfirm = false },
            title = { Text(deleteConfirmMsg, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    when (activeLang) {
                        "mg" -> "Hofafana ny mouvement ${selectedMouvementIds.size} voafidy?"
                        "fr" -> "Supprimer les ${selectedMouvementIds.size} mouvements sélectionnés ?"
                        else -> "Delete the ${selectedMouvementIds.size} selected movements?"
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        mouvements.filter { selectedMouvementIds.contains(it.id) }.forEach { viewModel.deleteMouvementCaisse(it) }
                        selectedMouvementIds = emptySet()
                        isSelectionMode = false
                        showMultiDeleteConfirm = false
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
                TextButton(onClick = { showMultiDeleteConfirm = false }) {
                    Text(cancelBtnText)
                }
            }
        )
    }
}

// C.2: shows whether a cash session is currently open, lets the user open/close one, and recaps
// the écart (gap between counted and theoretical cash) of the last closed session.
@Composable
private fun CaisseSessionCard(
    activeLang: String,
    mainTextColor: Color,
    secondaryTextColor: Color,
    openSession: CaisseSession?,
    lastClosedSession: CaisseSession?,
    onOpenClick: () -> Unit,
    onCloseClick: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE) }
    val sessionLabel = when (activeLang) {
        "mg" -> "Fizarana asa (Kesty)"
        "fr" -> "Session de caisse"
        else -> "Cash session"
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp).testTag("caisse_session_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = sessionLabel.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = secondaryTextColor
            )
            Spacer(modifier = Modifier.height(6.dp))

            if (openSession == null) {
                val closedMsg = when (activeLang) {
                    "mg" -> "Mbola tsy voasokatra ny kesty."
                    "fr" -> "Aucune session ouverte."
                    else -> "No open session."
                }
                Text(closedMsg, style = MaterialTheme.typography.bodySmall, color = secondaryTextColor)
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onOpenClick,
                    modifier = Modifier.testTag("caisse_open_session_button")
                ) {
                    Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(when (activeLang) { "mg" -> "Sokafy ny kesty"; "fr" -> "Ouvrir la caisse"; else -> "Open cash session" }, fontWeight = FontWeight.Bold)
                }
            } else {
                val openMsg = when (activeLang) {
                    "mg" -> "Nosokafana tamin'ny ${formatter.format(Date(openSession.dateOuverture))}"
                    "fr" -> "Ouverte depuis le ${formatter.format(Date(openSession.dateOuverture))}"
                    else -> "Opened since ${formatter.format(Date(openSession.dateOuverture))}"
                }
                Text(openMsg, style = MaterialTheme.typography.bodySmall, color = mainTextColor, fontWeight = FontWeight.Bold)
                Text(
                    text = "${when (activeLang) { "mg" -> "Vola fanombohana"; "fr" -> "Fond de départ"; else -> "Starting float" }}: ${FormatUtil.formatPrice(openSession.montantOuverture)} Ar",
                    style = MaterialTheme.typography.bodySmall,
                    color = secondaryTextColor
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onCloseClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                    modifier = Modifier.testTag("caisse_close_session_button")
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(when (activeLang) { "mg" -> "Hidio ny kesty"; "fr" -> "Fermer la caisse"; else -> "Close cash session" }, fontWeight = FontWeight.Bold)
                }
            }

            if (lastClosedSession != null) {
                val ecart = lastClosedSession.ecart ?: 0.0
                val ecartColor = when {
                    kotlin.math.abs(ecart) < 1.0 -> Color(0xFF2E7D32)
                    ecart > 0 -> Color(0xFFF57C00)
                    else -> Color(0xFFC62828)
                }
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = when (activeLang) {
                        "mg" -> "Fizarana farany nohidiana (${formatter.format(Date(lastClosedSession.dateFermeture ?: 0L))})"
                        "fr" -> "Dernière session fermée (${formatter.format(Date(lastClosedSession.dateFermeture ?: 0L))})"
                        else -> "Last closed session (${formatter.format(Date(lastClosedSession.dateFermeture ?: 0L))})"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = secondaryTextColor
                )
                Text(
                    text = "${when (activeLang) { "mg" -> "Fahasamihafana"; "fr" -> "Écart"; else -> "Gap" }}: ${if (ecart >= 0) "+" else ""}${FormatUtil.formatPrice(ecart)} Ar",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = ecartColor
                )
            }
        }
    }
}

@Composable
private fun MouvementCaisseCard(
    mouvement: MouvementCaisse,
    mainTextColor: Color,
    secondaryTextColor: Color,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {},
    onDelete: () -> Unit
) {
    val isEntree = mouvement.type == TYPE_ENTREE
    val accentColor = if (isEntree) Color(0xFF2E7D32) else Color(0xFFC62828)
    val formatter = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (isSelectionMode) it.clickable { onToggleSelect() } else it }
            .testTag("caisse_mouvement_card_${mouvement.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() },
                    modifier = Modifier.testTag("caisse_checkbox_${mouvement.id}")
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
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

            if (!isSelectionMode) {
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
}
