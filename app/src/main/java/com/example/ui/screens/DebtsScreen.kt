package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Debt
import com.example.ui.viewmodel.InventoryViewModel
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
    var debtToDelete by remember { mutableStateOf<Debt?>(null) }

    // New Debt Form states
    var debtorName by remember { mutableStateOf("") }
    var debtAmountStr by remember { mutableStateOf("") }
    var debtNote by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf(false) }

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
                        imageVector = Icons.Default.ReceiptLong,
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
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(debts) { debt ->
                    DebtCard(
                        debt = debt,
                        activeLang = activeLang,
                        onRepay = { selectedDebtForRepay = debt },
                        onDelete = { debtToDelete = debt }
                    )
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
                            amountError = it.toDoubleOrNull() == null || it.toDouble() <= 0
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
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val nameClean = debtorName.trim()
                        val amt = debtAmountStr.toDoubleOrNull() ?: 0.0

                        nameError = nameClean.isEmpty()
                        amountError = amt <= 0

                        if (!nameError && !amountError) {
                            val d = Debt(
                                debtorName = nameClean,
                                amount = amt,
                                balance = amt,
                                date = System.currentTimeMillis(),
                                note = debtNote.trim(),
                                isPaid = false
                            )
                            viewModel.saveDebt(d)
                            // Reset and dismiss
                            debtorName = ""
                            debtAmountStr = ""
                            debtNote = ""
                            nameError = false
                            amountError = false
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
                        showAddDebtDialog = false
                    }
                ) {
                    Text(t("cancel_btn"))
                }
            }
        )
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
                            repayError = it.toDoubleOrNull() == null || it.toDouble() <= 0 || it.toDouble() > debt.balance
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
                        val valDouble = repayAmountStr.toDoubleOrNull() ?: 0.0
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
}

@Composable
fun DebtCard(
    debt: Debt,
    activeLang: String,
    onRepay: () -> Unit,
    onDelete: () -> Unit
) {
    val t = { key: String -> LanguageManager.translate(key, activeLang) }
    val formatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE) }
    val dateStr = formatter.format(Date(debt.date))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("debt_card_${debt.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (debt.isPaid) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
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
                    }
                    Text(
                        text = dateStr,
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
            }

            if (!debt.isPaid) {
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
