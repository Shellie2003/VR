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
import com.example.data.model.LotProduit
import com.example.data.model.Product
import com.example.ui.viewmodel.InventoryViewModel
import com.example.util.FormatUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val EXPIRY_WARNING_WINDOW_MS = 7L * 24 * 60 * 60 * 1000

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeremptionScreen(
    viewModel: InventoryViewModel,
    onNavigateBack: () -> Unit
) {
    val activeLang by viewModel.language.collectAsState()
    val themeColor by viewModel.themeColor.collectAsState()
    val allProducts by viewModel.allProducts.collectAsState()
    val allLots by viewModel.allLots.collectAsState()

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    val isDark = MaterialTheme.colorScheme.background == Color(0xFF002114)
    val mainTextColor = if (isDark) Color.White else Color(0xFF1E293B)
    val secondaryTextColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    val now = remember { System.currentTimeMillis() }
    val warningThreshold = now + EXPIRY_WARNING_WINDOW_MS

    val eligibleProducts = remember(allProducts) { allProducts.filter { it.gerePeremption } }

    val sortedLots = remember(allLots) { allLots.sortedBy { it.datePeremption } }
    val expiredCount = remember(allLots) { allLots.count { it.datePeremption < now } }
    val expiringSoonCount = remember(allLots) { allLots.count { it.datePeremption in now..warningThreshold } }

    var showAddLotDialog by remember { mutableStateOf(false) }
    var lotToEdit by remember { mutableStateOf<LotProduit?>(null) }
    var lotToDelete by remember { mutableStateOf<LotProduit?>(null) }

    val screenTitle = when (activeLang) {
        "mg" -> "Fetr'andro Peremptiona"
        "fr" -> "Alertes de péremption"
        else -> "Expiry alerts"
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
            // Summary card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .testTag("peremption_summary_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (expiredCount > 0) Color(0xFFC62828) else themeColor
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = when (activeLang) { "mg" -> "TARA"; "fr" -> "Périmés"; else -> "Expired" },
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                            Text(
                                text = "$expiredCount",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = when (activeLang) { "mg" -> "AKAIKY"; "fr" -> "Bientôt (7j)"; else -> "Soon (7d)" },
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                            Text(
                                text = "$expiringSoonCount",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { showAddLotDialog = true },
                        enabled = eligibleProducts.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth().height(42.dp).testTag("add_lot_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = themeColor)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when (activeLang) { "mg" -> "Hanampy lot"; "fr" -> "Ajouter un lot"; else -> "Add batch" },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (sortedLots.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outlineVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = when (activeLang) {
                                "mg" -> "Mbola tsy misy lot voasoratra."
                                "fr" -> "Aucun lot enregistré."
                                else -> "No batch recorded yet."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = secondaryTextColor
                        )
                        if (eligibleProducts.isEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = when (activeLang) {
                                    "mg" -> "Alefaso ny \"Misy peremptiona\" amin'ny ficard entana aloha."
                                    "fr" -> "Activez \"Gère la péremption\" sur une fiche produit d'abord."
                                    else -> "Enable \"Track expiry\" on a product first."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = secondaryTextColor,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 24.dp, top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(sortedLots, key = { it.id }) { lot ->
                        val product = remember(lot.produitId, allProducts) {
                            allProducts.find { it.id.toLong() == lot.produitId }
                        }
                        LotCard(
                            lot = lot,
                            product = product,
                            activeLang = activeLang,
                            mainTextColor = mainTextColor,
                            secondaryTextColor = secondaryTextColor,
                            now = now,
                            warningThreshold = warningThreshold,
                            onEdit = { lotToEdit = lot },
                            onDelete = { lotToDelete = lot }
                        )
                    }
                }
            }
        }
        }
    }

    if (showAddLotDialog) {
        LotEditDialog(
            viewModel = viewModel,
            lot = null,
            eligibleProducts = eligibleProducts,
            activeLang = activeLang,
            themeColor = themeColor,
            onDismiss = { showAddLotDialog = false }
        )
    }
    lotToEdit?.let { lot ->
        LotEditDialog(
            viewModel = viewModel,
            lot = lot,
            eligibleProducts = eligibleProducts,
            activeLang = activeLang,
            themeColor = themeColor,
            onDismiss = { lotToEdit = null }
        )
    }
    lotToDelete?.let { lot ->
        AlertDialog(
            onDismissRequest = { lotToDelete = null },
            title = {
                Text(
                    text = when (activeLang) { "mg" -> "Hamafa"; "fr" -> "Supprimer"; else -> "Delete" },
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    when (activeLang) {
                        "mg" -> "Hofafana ity lot ity?"
                        "fr" -> "Supprimer ce lot ?"
                        else -> "Delete this batch?"
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteLot(lot); lotToDelete = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                ) {
                    Text(when (activeLang) { "mg" -> "Hamafa"; "fr" -> "Supprimer"; else -> "Delete" }, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { lotToDelete = null }) {
                    Text(when (activeLang) { "mg" -> "Hanafoana"; "fr" -> "Annuler"; else -> "Cancel" })
                }
            }
        )
    }
}

@Composable
private fun LotCard(
    lot: LotProduit,
    product: Product?,
    activeLang: String,
    mainTextColor: Color,
    secondaryTextColor: Color,
    now: Long,
    warningThreshold: Long,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE) }
    val isExpired = lot.datePeremption < now
    val isExpiringSoon = !isExpired && lot.datePeremption <= warningThreshold
    val accentColor = when {
        isExpired -> Color(0xFFC62828)
        isExpiringSoon -> Color(0xFFF57C00)
        else -> Color(0xFF2E7D32)
    }

    Card(
        modifier = Modifier.fillMaxWidth().testTag("lot_card_${lot.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
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
                    imageVector = if (isExpired) Icons.Default.Warning else Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product?.name ?: when (activeLang) { "mg" -> "Entana tsy fantatra"; "fr" -> "Produit inconnu"; else -> "Unknown product" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = mainTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${FormatUtil.formatQty(lot.quantite, product?.unit ?: "")} • ${lot.numeroLot?.takeIf { it.isNotBlank() } ?: "—"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = secondaryTextColor
                )
                Text(
                    text = "${when (activeLang) { "mg" -> "Fetr'andro"; "fr" -> "Expire le"; else -> "Expires" }}: ${formatter.format(Date(lot.datePeremption))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = accentColor,
                    fontWeight = if (isExpired || isExpiringSoon) FontWeight.Bold else FontWeight.Normal
                )
            }

            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = secondaryTextColor, modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = null, tint = Color(0xFFC62828), modifier = Modifier.size(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LotEditDialog(
    viewModel: InventoryViewModel,
    lot: LotProduit?,
    eligibleProducts: List<Product>,
    activeLang: String,
    themeColor: Color,
    onDismiss: () -> Unit
) {
    val isEditing = lot != null
    var selectedProduct by remember {
        mutableStateOf(eligibleProducts.find { it.id.toLong() == lot?.produitId })
    }
    var showProductDropdown by remember { mutableStateOf(false) }
    var numeroLot by remember { mutableStateOf(lot?.numeroLot ?: "") }
    var quantiteStr by remember { mutableStateOf(lot?.quantite?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "") }
    var dateMillis by remember { mutableStateOf(lot?.datePeremption ?: (System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000)) }
    var showDatePicker by remember { mutableStateOf(false) }
    var productError by remember { mutableStateOf(false) }
    var qtyError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) {
                    when (activeLang) { "mg" -> "Hanova lot"; "fr" -> "Modifier le lot"; else -> "Edit batch" }
                } else {
                    when (activeLang) { "mg" -> "Lot vaovao"; "fr" -> "Nouveau lot"; else -> "New batch" }
                },
                fontWeight = FontWeight.Black
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box {
                    OutlinedButton(
                        onClick = { showProductDropdown = true },
                        modifier = Modifier.fillMaxWidth().testTag("lot_product_selector"),
                        enabled = !isEditing
                    ) {
                        Text(
                            text = selectedProduct?.name ?: when (activeLang) {
                                "mg" -> "Safidio ny entana"
                                "fr" -> "Choisir un produit"
                                else -> "Choose a product"
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    DropdownMenu(expanded = showProductDropdown, onDismissRequest = { showProductDropdown = false }) {
                        eligibleProducts.forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p.name) },
                                onClick = {
                                    selectedProduct = p
                                    productError = false
                                    showProductDropdown = false
                                }
                            )
                        }
                    }
                }
                if (productError) {
                    Text(
                        text = when (activeLang) { "mg" -> "Safidio ny entana"; "fr" -> "Choisissez un produit"; else -> "Choose a product" },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                OutlinedTextField(
                    value = numeroLot,
                    onValueChange = { numeroLot = it },
                    label = { Text(when (activeLang) { "mg" -> "Nomeraon'ny lot (safidy)"; "fr" -> "Numéro de lot (optionnel)"; else -> "Batch number (optional)" }) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = quantiteStr,
                    onValueChange = { quantiteStr = it; qtyError = false },
                    label = { Text(when (activeLang) { "mg" -> "Isa"; "fr" -> "Quantité"; else -> "Quantity" }) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = qtyError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("lot_quantity_input")
                )

                val dateLabel = remember(dateMillis) { SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(Date(dateMillis)) }
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth().testTag("lot_date_selector")
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("${when (activeLang) { "mg" -> "Fetr'andro"; "fr" -> "Date de péremption"; else -> "Expiry date" }}: $dateLabel")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val product = selectedProduct
                    val qty = quantiteStr.toDoubleOrNull()
                    productError = product == null
                    qtyError = qty == null || qty <= 0.0

                    if (!productError && !qtyError && product != null && qty != null) {
                        val toSave = (lot ?: LotProduit(
                            produitId = product.id.toLong(),
                            quantite = qty,
                            datePeremption = dateMillis
                        )).copy(
                            numeroLot = numeroLot.trim().ifBlank { null },
                            quantite = qty,
                            datePeremption = dateMillis
                        )
                        viewModel.saveLot(toSave)
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("lot_save_button")
            ) {
                Text(when (activeLang) { "mg" -> "Hitahiry"; "fr" -> "Enregistrer"; else -> "Save" }, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(when (activeLang) { "mg" -> "Hanafoana"; "fr" -> "Annuler"; else -> "Cancel" })
            }
        }
    )

    if (showDatePicker) {
        val datePickerState = androidx.compose.material3.rememberDatePickerState(initialSelectedDateMillis = dateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { dateMillis = it }
                    showDatePicker = false
                }) {
                    Text(when (activeLang) { "mg" -> "Hitahiry"; "fr" -> "OK"; else -> "OK" })
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(when (activeLang) { "mg" -> "Hanafoana"; "fr" -> "Annuler"; else -> "Cancel" })
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
