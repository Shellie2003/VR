package com.example.ui.screens

import com.example.util.enDecimal
import com.example.util.enEntier
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Debt
import com.example.ui.components.SelectionExportToolbar
import com.example.ui.viewmodel.InventoryViewModel
import com.example.util.ExportFormat
import com.example.util.ExportUtil
import com.example.util.FormatUtil
import com.example.util.LanguageManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtsScreen(
    viewModel: InventoryViewModel
) {
    val context = LocalContext.current
    val debts by viewModel.filteredDebts.collectAsState()
    val totalOutstanding by viewModel.totalOutstandingDebts.collectAsState(0.0)
    val searchQuery by viewModel.debtSearchQuery.collectAsState()
    val activeFilter by viewModel.debtFilter.collectAsState()
    val activeLang by viewModel.language.collectAsState()

    // Translater
    val t = { key: String -> LanguageManager.translate(key, activeLang) }

    // Dialog trigger states
    var showAddDebtDialog by remember { mutableStateOf(false) }
    var selectedDebtForRepay by remember { mutableStateOf<Debt?>(null) }
    // Remboursement groupé : un client solde plusieurs trosa à la fois avec une somme unique,
    // répartie automatiquement (voir DebtAllocation) plutôt que dette par dette.
    var groupForRepay by remember { mutableStateOf<DebtorGroup?>(null) }
    var debtToDelete by remember { mutableStateOf<Debt?>(null) }

    // Multi-selection state (checkbox mode for bulk delete)
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedDebtIds by remember { mutableStateOf(setOf<Int>()) }
    var showMultiDeleteConfirm by remember { mutableStateOf(false) }

    // New Debt Form states
    var debtorName by remember { mutableStateOf("") }
    var debtAmountStr by remember { mutableStateOf("") }
    var debtNote by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf(false) }
    // C.3: optional due date (échéance) for a payment reminder
    var dueDateMillis by remember { mutableStateOf<Long?>(null) }
    var showDueDatePicker by remember { mutableStateOf(false) }

    // Repayment Form states
    var repayAmountStr by remember { mutableStateOf("") }
    var repayError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Outstanding debts total card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = t("total_debts").uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${FormatUtil.formatPrice(totalOutstanding)} Ar",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Button(
                        onClick = { showAddDebtDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onErrorContainer,
                            contentColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("add_debt_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(t("new_debt"), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Selection / Export toolbar
        SelectionExportToolbar(
            activeLang = activeLang,
            isSelectionMode = isSelectionMode,
            selectedCount = selectedDebtIds.size,
            onToggleSelectionMode = {
                isSelectionMode = !isSelectionMode
                if (!isSelectionMode) selectedDebtIds = emptySet()
            },
            onSelectAll = { selectedDebtIds = debts.map { it.id }.toSet() },
            onDeleteSelected = { showMultiDeleteConfirm = true },
            onExportPdf = { ExportUtil.exportDebts(context, debts, ExportFormat.PDF) },
            onExportCsv = { ExportUtil.exportDebts(context, debts, ExportFormat.CSV) }
        )

        // Search text field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.debtSearchQuery.value = it },
            placeholder = { Text(t("search_debt_hint"), style = MaterialTheme.typography.bodyMedium) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.outline) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.debtSearchQuery.value = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = null)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .testTag("debt_search_input"),
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )

        // Filter status rows (Non payées, Payées, Toutes)
        val filterOptions = listOf("Toutes", "Non payées", "Payées")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            filterOptions.forEach { option ->
                val localizedLabel = when (option) {
                    "Toutes" -> t("filter_all")
                    "Non payées" -> t("filter_unpaid")
                    "Payées" -> t("filter_paid")
                    else -> option
                }
                val isSelected = option == activeFilter

                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.debtFilter.value = option },
                    label = { Text(localizedLabel, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                    modifier = Modifier.testTag("debt_filter_$option")
                )
            }
        }

        if (debts.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outlineVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tsy misy trosa mifanaraka amin'io fikarohana io.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Group debts by debtor name (trimmed, case-insensitive) so several Trosa entries for
            // the same person show as one consolidated card with the total owed, while still
            // keeping every individual transaction (its own date/amount/note) visible when expanded.
            val debtorGroups = remember(debts) {
                debts.groupBy { it.debtorName.trim().lowercase() }
                    .map { (_, group) ->
                        val sorted = group.sortedByDescending { it.date }
                        DebtorGroup(displayName = sorted.first().debtorName, debts = sorted)
                    }
                    .sortedByDescending { g -> g.debts.maxOf { it.date } }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(debtorGroups, key = { it.displayName.trim().lowercase() }) { group ->
                    if (group.debts.size == 1) {
                        val debt = group.debts[0]
                        DebtCard(
                            debt = debt,
                            activeLang = activeLang,
                            isSelectionMode = isSelectionMode,
                            isSelected = selectedDebtIds.contains(debt.id),
                            onToggleSelect = {
                                selectedDebtIds = if (selectedDebtIds.contains(debt.id)) {
                                    selectedDebtIds - debt.id
                                } else {
                                    selectedDebtIds + debt.id
                                }
                            },
                            onRepay = { selectedDebtForRepay = debt },
                            onDelete = { debtToDelete = debt }
                        )
                    } else {
                        DebtorGroupCard(
                            group = group,
                            activeLang = activeLang,
                            isSelectionMode = isSelectionMode,
                            selectedDebtIds = selectedDebtIds,
                            onToggleSelect = { id ->
                                selectedDebtIds = if (selectedDebtIds.contains(id)) {
                                    selectedDebtIds - id
                                } else {
                                    selectedDebtIds + id
                                }
                            },
                            onRepay = { selectedDebtForRepay = it },
                            onRepayGroup = { groupForRepay = group },
                            onDelete = { debtToDelete = it }
                        )
                    }
                }
            }
        }
    }

    // Add Debt Dialog Form
    if (showAddDebtDialog) {
        AlertDialog(
            onDismissRequest = {
                debtorName = ""
                debtAmountStr = ""
                debtNote = ""
                nameError = false
                amountError = false
                dueDateMillis = null
                showAddDebtDialog = false
            },
            title = { Text(t("new_debt"), fontWeight = FontWeight.Black) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = debtorName,
                        onValueChange = {
                            debtorName = it
                            nameError = it.isBlank()
                        },
                        label = { Text(t("debtor_name")) },
                        isError = nameError,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = debtAmountStr,
                        onValueChange = {
                            debtAmountStr = it
                            amountError = it.enDecimal() == null || it.toDouble() <= 0
                        },
                        label = { Text(t("debt_amount")) },
                        prefix = { Text("Ar ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = amountError,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = debtNote,
                        onValueChange = { debtNote = it },
                        label = { Text(t("debt_note")) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // C.3: optional due date (échéance)
                    val dueDateLabel = dueDateMillis?.let {
                        SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(Date(it))
                    } ?: when (activeLang) {
                        "mg" -> "Tsy misy fetr'andro (safidy)"
                        "fr" -> "Aucune échéance (optionnel)"
                        else -> "No due date (optional)"
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = { showDueDatePicker = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(dueDateLabel, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (dueDateMillis != null) {
                            IconButton(onClick = { dueDateMillis = null }) {
                                Icon(Icons.Default.Clear, contentDescription = null)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val nameClean = debtorName.trim()
                        val amt = debtAmountStr.enDecimal() ?: 0.0

                        nameError = nameClean.isEmpty()
                        amountError = amt <= 0

                        if (!nameError && !amountError) {
                            val d = Debt(
                                debtorName = nameClean,
                                amount = amt,
                                balance = amt,
                                date = System.currentTimeMillis(),
                                note = debtNote.trim(),
                                isPaid = false,
                                dueDate = dueDateMillis
                            )
                            viewModel.saveDebt(d)
                            // Reset and dismiss
                            debtorName = ""
                            debtAmountStr = ""
                            debtNote = ""
                            nameError = false
                            amountError = false
                            dueDateMillis = null
                            showAddDebtDialog = false
                        }
                    }
                ) {
                    Text(t("save_debt_btn"), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        debtorName = ""
                        debtAmountStr = ""
                        debtNote = ""
                        nameError = false
                        amountError = false
                        dueDateMillis = null
                        showAddDebtDialog = false
                    }
                ) {
                    Text(t("cancel_btn"))
                }
            }
        )
    }

    // C.3: due-date picker for the new-debt form above
    if (showDueDatePicker) {
        val datePickerState = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = dueDateMillis ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDueDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dueDateMillis = datePickerState.selectedDateMillis
                    showDueDatePicker = false
                }) {
                    Text(t("save_debt_btn"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDueDatePicker = false }) {
                    Text(t("cancel_btn"))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Repay Partial Dialog Form
    selectedDebtForRepay?.let { debt ->
        AlertDialog(
            onDismissRequest = {
                repayAmountStr = ""
                repayError = false
                selectedDebtForRepay = null
            },
            title = { Text(t("debt_repay_title"), fontWeight = FontWeight.Black) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${t("debtor_name")} : ${debt.debtorName}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "${t("remaining_debt")} ${FormatUtil.formatPrice(debt.balance)} Ar",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = repayAmountStr,
                        onValueChange = {
                            repayAmountStr = it
                            repayError = it.enDecimal() == null || it.toDouble() <= 0 || it.toDouble() > debt.balance
                        },
                        label = { Text(t("repay_amount_label")) },
                        prefix = { Text("Ar ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = repayError,
                        supportingText = { if (repayError) Text("Sora-bola diso", color = MaterialTheme.colorScheme.error) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val valDouble = repayAmountStr.enDecimal() ?: 0.0
                        repayError = valDouble <= 0 || valDouble > debt.balance

                        if (!repayError) {
                            viewModel.updateDebtRepayment(debt.id, valDouble)
                            repayAmountStr = ""
                            repayError = false
                            selectedDebtForRepay = null
                        }
                    }
                ) {
                    Text(t("repay_btn"), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        repayAmountStr = ""
                        repayError = false
                        selectedDebtForRepay = null
                    }
                ) {
                    Text(t("cancel_btn"))
                }
            }
        )
    }

    // Remboursement groupé : un montant unique, réparti automatiquement entre les trosa du
    // débiteur (voir InventoryViewModel.repayDebtorGroup / util.DebtAllocation).
    groupForRepay?.let { group ->
        GroupRepayDialog(
            group = group,
            activeLang = activeLang,
            onConfirm = { montant ->
                viewModel.repayDebtorGroup(group.debts, montant)
                groupForRepay = null
            },
            onDismiss = { groupForRepay = null }
        )
    }

    // Deletion debt confirmation
    debtToDelete?.let { debt ->
        AlertDialog(
            onDismissRequest = { debtToDelete = null },
            title = { Text(t("delete_action")) },
            text = { Text("${t("confirm_delete_msg").replace("produit", "trosa")} : '${debt.debtorName}' ?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteDebt(debt)
                        debtToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(t("delete_btn"), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { debtToDelete = null }) {
                    Text(t("cancel_btn"))
                }
            }
        )
    }

    // Multi-selection bulk delete confirmation
    if (showMultiDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showMultiDeleteConfirm = false },
            title = { Text(t("delete_action"), fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    when (activeLang) {
                        "mg" -> "Hofafana ny trosa ${selectedDebtIds.size} voafidy?"
                        "fr" -> "Supprimer les ${selectedDebtIds.size} dettes sélectionnées ?"
                        else -> "Delete the ${selectedDebtIds.size} selected debts?"
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        debts.filter { selectedDebtIds.contains(it.id) }.forEach { viewModel.deleteDebt(it) }
                        selectedDebtIds = emptySet()
                        isSelectionMode = false
                        showMultiDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(t("delete_btn"), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showMultiDeleteConfirm = false }) {
                    Text(t("cancel_btn"))
                }
            }
        )
    }
}

@Composable
fun DebtCard(
    debt: Debt,
    activeLang: String,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {},
    onRepay: () -> Unit,
    onDelete: () -> Unit
) {
    val t = { key: String -> LanguageManager.translate(key, activeLang) }
    val formatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE) }
    val dateStr = formatter.format(Date(debt.date))

    // Les articles du trosa sont stockés dans `note` et la carte doit rester compacte : elle les
    // tronque donc avec des « … ». Un appui sur la carte ouvre le détail complet, lisible.
    var showDetailDialog by remember(debt.id) { mutableStateOf(false) }

    if (showDetailDialog) {
        DebtDetailDialog(
            debt = debt,
            activeLang = activeLang,
            onDismiss = { showDetailDialog = false },
            onRepay = {
                showDetailDialog = false
                onRepay()
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { if (isSelectionMode) onToggleSelect() else showDetailDialog = true }
            .testTag("debt_card_${debt.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                debt.isPaid -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                if (isSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelect() },
                        modifier = Modifier.testTag("debt_checkbox_${debt.id}")
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = debt.debtorName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (debt.isPaid) Color(0xFF2E7D32).copy(alpha = 0.12f)
                                    else MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (debt.isPaid) t("repaid_badge") else t("unpaid_badge"),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = if (debt.isPaid) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                            )
                        }
                        // C.3: overdue (échéance dépassée) badge
                        if (debt.isOverdue()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.errorContainer)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = when (activeLang) {
                                        "mg" -> "TARA"
                                        "fr" -> "EN RETARD"
                                        else -> "OVERDUE"
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    debt.dueDate?.let {
                        val dueDateStr = remember(it) { SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(Date(it)) }
                        Text(
                            text = "${when (activeLang) { "mg" -> "Fetr'andro"; "fr" -> "Échéance"; else -> "Due" }}: $dueDateStr",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (debt.isOverdue()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                            fontWeight = if (debt.isOverdue()) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${FormatUtil.formatPrice(debt.balance)} Ar",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = if (debt.isPaid) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                        )
                        if (debt.balance < debt.amount) {
                            Text(
                                text = "Initiale: ${FormatUtil.formatPrice(debt.amount)} Ar",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    if (!isSelectionMode) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(28.dp)
                        ) {
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

            if (debt.note.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = debt.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
                if (!isSelectionMode) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = when (activeLang) {
                                "mg" -> "Tsindrio hijery ny antsipiriany"
                                "fr" -> "Appuyer pour voir le détail des articles"
                                else -> "Tap to see the full item list"
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (!debt.isPaid && !isSelectionMode) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onRepay,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(t("repay_btn"), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Several Trosa entries can belong to the same debtor (case-insensitive, trimmed full-name
 * match). Grouping them here is purely a UI concern — each underlying Debt row (its own date,
 * amount and note) stays untouched in the database, so nothing is lost when a name is merged.
 */
data class DebtorGroup(
    val displayName: String,
    val debts: List<Debt>
) {
    val totalBalance: Double get() = debts.sumOf { it.balance }
    val totalAmount: Double get() = debts.sumOf { it.amount }
    val isFullyPaid: Boolean get() = debts.all { it.isPaid }
    val hasOverdue: Boolean get() = debts.any { it.isOverdue() }
}

@Composable
fun DebtorGroupCard(
    group: DebtorGroup,
    activeLang: String,
    isSelectionMode: Boolean = false,
    selectedDebtIds: Set<Int>,
    onToggleSelect: (Int) -> Unit,
    onRepay: (Debt) -> Unit,
    onRepayGroup: () -> Unit,
    onDelete: (Debt) -> Unit
) {
    val t = { key: String -> LanguageManager.translate(key, activeLang) }
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("debtor_group_${group.displayName.trim().lowercase()}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                group.isFullyPaid -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = group.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (group.isFullyPaid) Color(0xFF2E7D32).copy(alpha = 0.12f)
                                    else MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (group.isFullyPaid) t("repaid_badge") else t("unpaid_badge"),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = if (group.isFullyPaid) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                            )
                        }
                        if (group.hasOverdue) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.errorContainer)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = when (activeLang) {
                                        "mg" -> "TARA"
                                        "fr" -> "EN RETARD"
                                        else -> "OVERDUE"
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                    Text(
                        text = when (activeLang) {
                            "mg" -> "${group.debts.size} trosa nakambana"
                            "fr" -> "${group.debts.size} dettes cumulées"
                            else -> "${group.debts.size} combined debts"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${FormatUtil.formatPrice(group.totalBalance)} Ar",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = if (group.isFullyPaid) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                        )
                        if (group.totalBalance < group.totalAmount) {
                            Text(
                                text = "Initiale: ${FormatUtil.formatPrice(group.totalAmount)} Ar",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }

            // Remboursement groupé : accessible sans avoir à déplier le détail, puisque c'est
            // justement le geste qu'on veut simplifier — un client qui règle un montant couvrant
            // plusieurs sections à la fois n'a pas besoin de les rembourser une par une.
            if (!group.isFullyPaid && !isSelectionMode) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onRepayGroup,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .testTag("repay_group_${group.displayName.trim().lowercase()}"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when (activeLang) {
                            "mg" -> "Handoa vola miditra amin'ny trosa rehetra"
                            "fr" -> "Régler un montant sur l'ensemble"
                            else -> "Pay an amount across all debts"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    group.debts.forEach { debt ->
                        DebtCard(
                            debt = debt,
                            activeLang = activeLang,
                            isSelectionMode = isSelectionMode,
                            isSelected = selectedDebtIds.contains(debt.id),
                            onToggleSelect = { onToggleSelect(debt.id) },
                            onRepay = { onRepay(debt) },
                            onDelete = { onDelete(debt) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Détail complet d'un trosa (dette). La carte de la liste doit rester compacte, elle tronque donc
 * la liste des articles avec des « … » ; ce dialogue affiche la même information en entier, un
 * article par ligne, avec le récapitulatif des montants (initial / déjà payé / reste à payer).
 *
 * Les articles sont enregistrés dans `Debt.note` sous la forme
 * "Article (2 x 1 500), Autre article (1 x 3 000)" au moment de la vente à crédit
 * (voir CalculatorScreen). On découpe donc sur les virgules qui suivent une parenthèse fermante,
 * ce qui préserve les noms d'articles contenant eux-mêmes une virgule. Une note saisie à la main
 * (qui ne suit pas ce format) est simplement affichée telle quelle.
 */
@Composable
fun DebtDetailDialog(
    debt: Debt,
    activeLang: String,
    onDismiss: () -> Unit,
    onRepay: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE) }
    val dueDateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE) }

    val itemLines: List<String> = remember(debt.note) {
        if (debt.note.isBlank()) {
            emptyList()
        } else {
            debt.note.split(Regex("(?<=\\)),\\s*"))
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        }
    }
    val alreadyPaid = (debt.amount - debt.balance).coerceAtLeast(0.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = debt.debtorName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = dateFormatter.format(Date(debt.date)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = when (activeLang) {
                        "mg" -> "Entana nalaina"
                        "fr" -> "Articles pris à crédit"
                        else -> "Items taken on credit"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                if (itemLines.isEmpty()) {
                    Text(
                        text = when (activeLang) {
                            "mg" -> "Tsy misy antsipiriany voarakitra."
                            "fr" -> "Aucun détail d'article enregistré."
                            else -> "No item detail recorded."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemLines.forEachIndexed { index, line ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "${index + 1}.",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                // Pas de maxLines ici : c'est tout l'intérêt du dialogue, le nom
                                // complet de l'article doit être lisible, quelle que soit sa longueur.
                                Text(
                                    text = line,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                HorizontalDivider()

                DebtDetailAmountRow(
                    label = when (activeLang) {
                        "mg" -> "Vola rehetra"
                        "fr" -> "Montant initial"
                        else -> "Initial amount"
                    },
                    value = "${FormatUtil.formatPrice(debt.amount)} Ar"
                )
                if (alreadyPaid > 0.0) {
                    DebtDetailAmountRow(
                        label = when (activeLang) {
                            "mg" -> "Efa naloa"
                            "fr" -> "Déjà payé"
                            else -> "Already paid"
                        },
                        value = "${FormatUtil.formatPrice(alreadyPaid)} Ar",
                        valueColor = Color(0xFF2E7D32)
                    )
                }
                DebtDetailAmountRow(
                    label = when (activeLang) {
                        "mg" -> "Sisa tsy maintsy aloa"
                        "fr" -> "Reste à payer"
                        else -> "Remaining balance"
                    },
                    value = "${FormatUtil.formatPrice(debt.balance)} Ar",
                    valueColor = if (debt.isPaid) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                    isBold = true
                )
                debt.dueDate?.let { due ->
                    DebtDetailAmountRow(
                        label = when (activeLang) {
                            "mg" -> "Fetr'andro"
                            "fr" -> "Échéance"
                            else -> "Due date"
                        },
                        value = dueDateFormatter.format(Date(due)),
                        valueColor = if (debt.isOverdue()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        confirmButton = {
            if (!debt.isPaid) {
                Button(onClick = onRepay) {
                    Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = LanguageManager.translate("repay_btn", activeLang),
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text(LanguageManager.translate("close_btn", activeLang))
                }
            }
        },
        dismissButton = {
            if (!debt.isPaid) {
                TextButton(onClick = onDismiss) {
                    Text(LanguageManager.translate("close_btn", activeLang))
                }
            }
        }
    )
}

@Composable
private fun DebtDetailAmountRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )
        Text(
            text = value,
            style = if (isBold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Black else FontWeight.SemiBold,
            color = valueColor
        )
    }
}

/**
 * Remboursement groupé : le client règle un montant unique qui couvre plusieurs trosa distincts
 * du même débiteur. Le montant est réparti automatiquement — dette au solde le plus élevé soldée
 * en priorité, reliquat reporté sur la suivante (voir [com.example.util.DebtAllocation]) — pour
 * éviter au gérant de calculer la répartition dette par dette au comptoir.
 *
 * L'aperçu affiché ici (quelle section se solde, ce qu'il reste) utilise EXACTEMENT le même calcul
 * que celui appliqué à la confirmation : le gérant voit donc précisément l'effet du montant qu'il
 * s'apprête à valider, sans surprise après coup.
 */
@Composable
fun GroupRepayDialog(
    group: DebtorGroup,
    activeLang: String,
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var montantStr by remember { mutableStateOf("") }
    var erreur by remember { mutableStateOf(false) }

    val montant = montantStr.enDecimal() ?: 0.0
    val plan = remember(montant, group) {
        com.example.util.DebtAllocation.calculer(group.debts, montant)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = when (activeLang) {
                    "mg" -> "Fandoavana miditra amin'ny trosa maro"
                    "fr" -> "Remboursement groupé"
                    else -> "Grouped repayment"
                },
                fontWeight = FontWeight.Black
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "${LanguageManager.translate("debtor_name", activeLang)} : ${group.displayName}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = when (activeLang) {
                        "mg" -> "Trosa ${group.debts.size} miaraka, mitotaly ${FormatUtil.formatPrice(group.totalBalance)} Ar"
                        "fr" -> "${group.debts.size} trosa cumulés, pour un total de ${FormatUtil.formatPrice(group.totalBalance)} Ar"
                        else -> "${group.debts.size} combined debts, totalling ${FormatUtil.formatPrice(group.totalBalance)} Ar"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )

                OutlinedTextField(
                    value = montantStr,
                    onValueChange = {
                        montantStr = it
                        erreur = it.enDecimal() == null || (it.enDecimal() ?: 0.0) <= 0.0
                    },
                    label = {
                        Text(
                            when (activeLang) {
                                "mg" -> "Vola raisina (Ar)"
                                "fr" -> "Montant reçu (Ar)"
                                else -> "Amount received (Ar)"
                            }
                        )
                    },
                    prefix = { Text("Ar ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = erreur,
                    supportingText = { if (erreur) Text("Sora-bola diso", color = MaterialTheme.colorScheme.error) },
                    modifier = Modifier.fillMaxWidth()
                )

                // Aperçu de la répartition : construit avec le MÊME calcul que celui qui sera
                // réellement appliqué à la confirmation.
                if (plan.lignes.isNotEmpty()) {
                    HorizontalDivider()
                    Text(
                        text = when (activeLang) {
                            "mg" -> "Ho zaraina toy izao ny vola:"
                            "fr" -> "Répartition automatique :"
                            else -> "Automatic breakdown:"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        plan.lignes.forEach { ligne ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${FormatUtil.formatPrice(ligne.soldeAvant)} Ar → ${FormatUtil.formatPrice(ligne.soldeApres)} Ar",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (ligne.estSoldee) {
                                        Text(
                                            text = when (activeLang) {
                                                "mg" -> "Voaloa tanteraka"
                                                "fr" -> "Soldée"
                                                else -> "Settled"
                                            },
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF2E7D32)
                                        )
                                    }
                                }
                                Text(
                                    text = "-${FormatUtil.formatPrice(ligne.montantApplique)} Ar",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    if (plan.montantNonAffecte > 0.0) {
                        Text(
                            text = when (activeLang) {
                                "mg" -> "Mihoatra ny trosa rehetra ny vola: ${FormatUtil.formatPrice(plan.montantNonAffecte)} Ar tsy voakasika."
                                "fr" -> "Le montant dépasse la dette totale : ${FormatUtil.formatPrice(plan.montantNonAffecte)} Ar ne seront pas affectés."
                                else -> "Amount exceeds the total debt: ${FormatUtil.formatPrice(plan.montantNonAffecte)} Ar will not be applied."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFEF6C00)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val valDouble = montantStr.enDecimal() ?: 0.0
                    erreur = valDouble <= 0.0
                    if (!erreur) {
                        onConfirm(valDouble)
                    }
                },
                enabled = plan.lignes.isNotEmpty()
            ) {
                Text(LanguageManager.translate("repay_btn", activeLang), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(LanguageManager.translate("cancel_btn", activeLang))
            }
        }
    )
}
